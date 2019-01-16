package com.prisma.deploy.schema.mutations

import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.connector._
import com.prisma.deploy.migration._
import com.prisma.deploy.migration.inference._
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.migration.validation._
import com.prisma.deploy.schema.InvalidQuery
import com.prisma.deploy.schema.types.MigrationStepType.MigrationStepAndSchema
import com.prisma.deploy.validation.DestructiveChanges
import com.prisma.messagebus.PubSubPublisher
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.ConnectorCapability.{IntrospectionCapability, LegacyDataModelCapability}
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils
import com.prisma.utils.future.FutureUtils.FutureOr
import org.scalactic.{Bad, Good, Or}
import sangria.ast.Document
import sangria.parser.QueryParser

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    schemaInferrer: SchemaInferrer,
    migrationStepsInferrer: MigrationStepsInferrer,
    schemaMapper: SchemaMapper,
    migrationPersistence: MigrationPersistence,
    projectPersistence: ProjectPersistence,
    migrator: Migrator,
    functionValidator: FunctionValidator,
    invalidationPublisher: PubSubPublisher[String],
    capabilities: ConnectorCapabilities,
    clientDbQueries: ClientDbQueries,
    databaseIntrospectionInferrer: DatabaseIntrospectionInferrer,
    fieldRequirements: FieldRequirementsInterface,
    isActive: Boolean
)(
    implicit ec: ExecutionContext
) extends Mutation[DeployMutationPayload]
    with AwaitUtils {

  val graphQlSdl: Document = QueryParser.parse(args.types) match {
    case Success(res) => res
    case Failure(e)   => throw InvalidQuery(e.getMessage)
  }

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    internalExecute.map {
      case Good(payload) => MutationSuccess(payload)
      case Bad(errors)   => MutationSuccess(DeployMutationPayload(args.clientMutationId, None, errors = errors, warnings = Vector.empty, steps = Vector.empty))
    }
  }

  private def internalExecute: Future[DeployMutationPayload Or Vector[DeployError]] = {
    val x = for {
      prismaSdl          <- FutureOr(validateSyntax)
      schemaMapping      = schemaMapper.createMapping(graphQlSdl)
      inferredTables     <- FutureOr(inferTables)
      inferredNextSchema = schemaInferrer.infer(project.schema, schemaMapping, prismaSdl, inferredTables)
      _                  <- FutureOr(checkSchemaAgainstInferredTables(inferredNextSchema, inferredTables))
      functions          <- FutureOr(getFunctionModels(inferredNextSchema, args.functions))
      steps              <- FutureOr(inferMigrationSteps(inferredNextSchema, schemaMapping))
      warnings           <- FutureOr(checkForDestructiveChanges(inferredNextSchema, steps))

      result <- (args.force.getOrElse(false), warnings.isEmpty) match {
                 case (_, true)     => FutureOr(performDeployment(inferredNextSchema, steps, functions))
                 case (true, false) => FutureOr(performDeployment(inferredNextSchema, steps, functions)).map(_.copy(warnings = warnings))
                 case (false, false) =>
                   FutureOr(Future.successful(Good {
                     DeployMutationPayload(
                       clientMutationId = args.clientMutationId,
                       migration = None,
                       errors = Vector.empty,
                       warnings = warnings,
                       steps = convertStepsToCorrectType(steps, inferredNextSchema)
                     )
                   }))
               }
    } yield result

    x.future
  }

  private def validateSyntax: Future[PrismaSdl Or Vector[DeployError]] = Future.successful {
    val validator = if (capabilities.has(LegacyDataModelCapability)) {
      LegacyDataModelValidator
    } else {
      DataModelValidatorImpl
    }
    validator.validate(args.types, fieldRequirements, capabilities)
  }

  private def inferTables: Future[InferredTables Or Vector[DeployError]] = {
    databaseIntrospectionInferrer.infer().map(Good(_))
  }

  private def checkSchemaAgainstInferredTables(nextSchema: Schema, inferredTables: InferredTables): Future[Unit Or Vector[DeployError]] = {
    if (!isActive && capabilities.has(IntrospectionCapability)) {
      val errors = InferredTablesValidator.checkRelationsAgainstInferredTables(nextSchema, inferredTables)
      if (errors.isEmpty) {
        Future.successful(Good(()))
      } else {
        Future.successful(Bad(errors.toVector))
      }
    } else {
      Future.successful(Good(()))
    }
  }

  private def inferMigrationSteps(nextSchema: Schema, schemaMapping: SchemaMapping): Future[Vector[MigrationStep] Or Vector[DeployError]] = {
    val steps = if (isActive) {
      migrationStepsInferrer.infer(project.schema, nextSchema, schemaMapping)
    } else {
      Vector.empty
    }
    Future.successful(Good(steps))
  }

  private def checkForDestructiveChanges(nextSchema: Schema, steps: Vector[MigrationStep]): Future[Vector[DeployWarning] Or Vector[DeployError]] = {
    DestructiveChanges(clientDbQueries, project, nextSchema, steps).check
  }

  private def getFunctionModels(nextSchema: Schema, fns: Vector[FunctionInput]): Future[Vector[Function] Or Vector[DeployError]] = Future.successful {
    functionValidator.validateFunctionInputs(nextSchema, fns)
  }

  private def performDeployment(nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function]): Future[Good[DeployMutationPayload]] = {
    for {
      secretsStep <- updateSecretsIfNecessary()
      migration   <- handleMigration(nextSchema, steps ++ secretsStep, functions)
    } yield {
      invalidationPublisher.publish(Only(project.id), project.id)
      Good(
        DeployMutationPayload(
          clientMutationId = args.clientMutationId,
          migration = migration,
          errors = Vector.empty,
          warnings = Vector.empty,
          steps = convertStepsToCorrectType(steps, nextSchema)
        ))
    }
  }

  private def updateSecretsIfNecessary(): Future[Option[MigrationStep]] = {
    if (project.secrets != args.secrets && !args.dryRun.getOrElse(false)) {
      projectPersistence.update(project.copy(secrets = args.secrets)).map(_ => Some(UpdateSecrets(args.secrets)))
    } else {
      Future.successful(None)
    }
  }

  private def handleMigration(nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function]): Future[Option[Migration]] = {
    val migrationNeeded = steps.nonEmpty || functions.nonEmpty || project.schema != nextSchema
    val isNotDryRun     = !args.dryRun.getOrElse(false)
    if (migrationNeeded && isNotDryRun) {
      invalidateSchema()
      migrator.schedule(project.id, nextSchema, steps, functions, args.types).map(Some(_))
    } else {
      Future.successful(None)
    }
  }

  private def invalidateSchema(): Unit = invalidationPublisher.publish(Only(project.id), project.id)

  private def convertStepsToCorrectType(steps: Vector[MigrationStep], nextSchema: Schema): Vector[MigrationStepAndSchema[MigrationStep]] = {
    steps.map(step => MigrationStepAndSchema(step = step, schema = nextSchema, previous = project.schema))
  }
}

case class DeployMutationInput(
    clientMutationId: Option[String],
    name: String,
    stage: String,
    types: String,
    dryRun: Option[Boolean],
    force: Option[Boolean],
    secrets: Vector[String],
    functions: Vector[FunctionInput],
    noMigration: Option[Boolean]
) extends sangria.relay.Mutation

case class FunctionInput(
    name: String,
    query: String,
    url: String,
    headers: Vector[HeaderInput]
)

case class HeaderInput(
    name: String,
    value: String
)

case class DeployMutationPayload(
    clientMutationId: Option[String],
    migration: Option[Migration],
    errors: Seq[DeployError],
    warnings: Seq[DeployWarning],
    steps: Vector[MigrationStepAndSchema[MigrationStep]]
) extends sangria.relay.Mutation
