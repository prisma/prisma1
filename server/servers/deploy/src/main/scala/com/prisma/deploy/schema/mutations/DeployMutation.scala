package com.prisma.deploy.schema.mutations

import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.{DeployConnector, InferredTables, MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.migration._
import com.prisma.deploy.migration.inference._
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.migration.validation._
import com.prisma.deploy.schema.InvalidQuery
import com.prisma.deploy.validation.DestructiveChanges
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.{Function, Migration, MigrationStep, Project, Schema, ServerSideSubscriptionFunction, UpdateSecrets, WebhookDelivery}
import com.prisma.utils.await.AwaitUtils
import com.prisma.utils.future.FutureUtils.FutureOr
import org.scalactic.{Bad, Good, Or}
import sangria.ast.Document
import sangria.parser.QueryParser

import scala.collection.{Seq, immutable}
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
    deployConnector: DeployConnector,
    migrator: Migrator
)(
    implicit ec: ExecutionContext,
    dependencies: DeployDependencies
) extends Mutation[DeployMutationPayload]
    with AwaitUtils {

  val projectId = dependencies.projectIdEncoder.toEncodedString(args.name, args.stage)

  //parse Schema
  //validate Schema
  //infer Tables
  //map Schema
  //create prismaSDL
  //inferSchema
  //inferred Table Validations
  //get Function Models -> validateFunctionInputs
  //infer MigrationSteps
  //check For Destructive Changes
  //check for Force
  //update Secrets
  //invalidateSchema
  //schedule Migration
  //if something fails during the migration steps we have no way of returning that error i think

  val graphQlSdl: Document = QueryParser.parse(args.types) match {
    case Success(res) => res
    case Failure(e)   => throw InvalidQuery(e.getMessage)
  }

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    performDeploy.map {
      case Good(payload) => MutationSuccess(payload)
      case Bad(errors)   => MutationSuccess(DeployMutationPayload(args.clientMutationId, None, errors = errors, warnings = Vector.empty))
    }
  }

  private def performDeploy: Future[DeployMutationPayload Or Vector[SchemaError]] = {
    val x = for {
      prismaSdl          <- FutureOr(validateSyntax)
      schemaMapping      = schemaMapper.createMapping(graphQlSdl)
      inferredTables     <- FutureOr(inferTables)
      inferredNextSchema = schemaInferrer.infer(project.schema, schemaMapping, prismaSdl, inferredTables)
      _                  <- FutureOr(checkSchemaAgainstInferredTables(inferredNextSchema, inferredTables))
      functions          <- FutureOr(Future.successful(getFunctionModelsOrErrors(args.functions)))
      steps              <- FutureOr(inferMigrationSteps(inferredNextSchema, schemaMapping))
      warnings           <- FutureOr(checkForDestructiveChanges(inferredNextSchema, steps))

      result <- (args.force.getOrElse(false), warnings.isEmpty) match {
                 case (_, true)     => FutureOr(persist(inferredNextSchema, steps, functions))
                 case (true, false) => FutureOr(persist(inferredNextSchema, steps, functions))
                 case (false, false) =>
                   FutureOr(Future.successful(
                     Good(DeployMutationPayload(args.clientMutationId, Some(Migration.empty(project.id)), errors = Vector.empty, warnings)))) // don't do it
               }
    } yield result

    x.future
  }

  private def validateSyntax: Future[PrismaSdl Or Vector[SchemaError]] = Future.successful {
    val validator = SchemaSyntaxValidator(args.types, isActive = deployConnector.isActive)
    val errors    = validator.validate
    if (errors.isEmpty) {
      Good(validator.generateSDL)
    } else {
      Bad(errors.toVector)
    }
  }

  private def inferTables: Future[InferredTables Or Vector[SchemaError]] = {
    deployConnector.databaseIntrospectionInferrer(project.id).infer().map(Good(_))
  }

  private def checkSchemaAgainstInferredTables(nextSchema: Schema, inferredTables: InferredTables): Future[Unit Or Vector[SchemaError]] = {
    if (deployConnector.isPassive) {
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

  private def inferMigrationSteps(nextSchema: Schema, schemaMapping: SchemaMapping): Future[Vector[MigrationStep] Or Vector[SchemaError]] = {
    val steps = if (deployConnector.isActive) {
      migrationStepsInferrer.infer(project.schema, nextSchema, schemaMapping)
    } else {
      Vector.empty
    }
    Future.successful(Good(steps))
  }

  private def checkForDestructiveChanges(nextSchema: Schema, steps: Vector[MigrationStep]): Future[Vector[SchemaWarning] Or Vector[SchemaError]] = {
    val existingDataValidation = DestructiveChanges(deployConnector, project, nextSchema, steps)
    val checkResults           = existingDataValidation.checkAgainstExistingData
    checkResults.map { results =>
      val destructiveWarnings: Vector[SchemaWarning] = results.collect { case warning: SchemaWarning => warning }
      val inconsistencyErrors: Vector[SchemaError]   = results.collect { case error: SchemaError     => error }
      if (inconsistencyErrors.isEmpty) {
        Good(destructiveWarnings)
      } else {
        Bad(inconsistencyErrors)
      }
    }
  }

  private def getFunctionModelsOrErrors(fns: Vector[FunctionInput]): Vector[Function] Or Vector[SchemaError] = {
    val errors = validateFunctionInputs(fns)
    if (errors.nonEmpty) {
      Bad(errors)
    } else {
      Good(args.functions.map(convertFunctionInput))
    }
  }

  private def validateFunctionInputs(fns: Vector[FunctionInput]): Vector[SchemaError] =
    fns.flatMap(dependencies.functionValidator.validateFunctionInput(project, _))

  private def convertFunctionInput(fnInput: FunctionInput): ServerSideSubscriptionFunction = {
    ServerSideSubscriptionFunction(
      name = fnInput.name,
      isActive = true,
      delivery = WebhookDelivery(
        url = fnInput.url,
        headers = fnInput.headers.map(header => header.name -> header.value)
      ),
      query = fnInput.query
    )
  }

  private def persist(nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function]): Future[Good[DeployMutationPayload]] = {
    for {
      secretsStep <- updateSecretsIfNecessary()
      migration   <- handleMigration(nextSchema, steps ++ secretsStep, functions)
    } yield {
      Good(DeployMutationPayload(args.clientMutationId, migration, errors = Vector.empty, warnings = Vector.empty))
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
    val migrationNeeded = if (deployConnector.isActive) {
      steps.nonEmpty || functions.nonEmpty
    } else {
      project.schema != nextSchema
    }
    val isNotDryRun = !args.dryRun.getOrElse(false)
    if (migrationNeeded && isNotDryRun) {
      invalidateSchema()
      migrator.schedule(project.id, nextSchema, steps, functions).map(Some(_))
    } else {
      Future.successful(None)
    }
  }

  private def invalidateSchema(): Unit = dependencies.invalidationPublisher.publish(Only(project.id), project.id)
}

case class DeployMutationInput(
    clientMutationId: Option[String],
    name: String,
    stage: String,
    types: String,
    dryRun: Option[Boolean],
    force: Option[Boolean],
    secrets: Vector[String],
    functions: Vector[FunctionInput]
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
    errors: Seq[SchemaError],
    warnings: Seq[SchemaWarning]
) extends sangria.relay.Mutation
