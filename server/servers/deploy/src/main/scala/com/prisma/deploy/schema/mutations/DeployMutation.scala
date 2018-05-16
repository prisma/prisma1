package com.prisma.deploy.schema.mutations

import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.{DeployConnector, MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.migration._
import com.prisma.deploy.migration.inference._
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.migration.validation._
import com.prisma.deploy.schema.InvalidQuery
import com.prisma.deploy.validation.DestructiveChanges
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.{Function, Migration, MigrationStep, Project, Schema, ServerSideSubscriptionFunction, UpdateSecrets, WebhookDelivery}
import com.prisma.utils.await.AwaitUtils
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
  val graphQlSdl: Document = QueryParser.parse(args.types) match {
    case Success(res) => res
    case Failure(e)   => throw InvalidQuery(e.getMessage)
  }

  val validator                                = SchemaSyntaxValidator(args.types, isActive = deployConnector.isActive)
  val schemaErrors: immutable.Seq[SchemaError] = validator.validate
  val prismaSdl: PrismaSdl                     = validator.generateSDL
  val inferredTablesFuture                     = deployConnector.databaseIntrospectionInferrer(project.id).infer()

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    if (schemaErrors.nonEmpty) {
      Future.successful {
        MutationSuccess(
          DeployMutationPayload(
            clientMutationId = args.clientMutationId,
            migration = None,
            errors = schemaErrors,
            warnings = Seq.empty
          ))
      }
    } else {
      performDeployment
    }
  }

  private def performDeployment: Future[MutationSuccess[DeployMutationPayload]] = inferredTablesFuture.flatMap { inferredTables =>
    val schemaMapping      = schemaMapper.createMapping(graphQlSdl)
    val inferredNextSchema = schemaInferrer.infer(project.schema, schemaMapping, prismaSdl, inferredTables)
    // todo check relation against inferred tables InferredTablesValidator
    val functionsOrErrors = getFunctionModelsOrErrors(args.functions)

    functionsOrErrors match {
      case Bad(errors) =>
        Future.successful(
          MutationSuccess(DeployMutationPayload(args.clientMutationId, Some(Migration.empty(project.id)), errors = schemaErrors ++ errors, Seq.empty)))

      case Good(functionsForInput) =>
        val steps = if (deployConnector.isActive) {
          migrationStepsInferrer.infer(project.schema, inferredNextSchema, schemaMapping)
        } else {
          Vector.empty
        }
        val existingDataValidation = DestructiveChanges(deployConnector, project, inferredNextSchema, steps)
        val checkResults           = existingDataValidation.checkAgainstExistingData

        checkResults.flatMap { results =>
          val destructiveWarnings: Vector[SchemaWarning] = results.collect { case warning: SchemaWarning => warning }
          val inconsistencyErrors: Vector[SchemaError]   = results.collect { case error: SchemaError     => error }

          (inconsistencyErrors, destructiveWarnings, args.force.getOrElse(false)) match {

            case (errors, warnings, _) if errors.nonEmpty =>
              Future.successful(
                MutationSuccess(DeployMutationPayload(args.clientMutationId, Some(Migration.empty(project.id)), errors = schemaErrors ++ errors, warnings)))

            case (_, warnings, _) if warnings.isEmpty =>
              val secretsUpdatedFuture = updateSecretsIfNecessary()
              val migration            = secretsUpdatedFuture.flatMap(secret => handleMigration(inferredNextSchema, steps ++ secret, functionsForInput))
              migration.map(mig => MutationSuccess(DeployMutationPayload(args.clientMutationId, mig, errors = schemaErrors, warnings)))

            case (_, warnings, true) =>
              val secretsUpdatedFuture = updateSecretsIfNecessary()
              val migration            = secretsUpdatedFuture.flatMap(secret => handleMigration(inferredNextSchema, steps ++ secret, functionsForInput))
              migration.map(mig => MutationSuccess(DeployMutationPayload(args.clientMutationId, mig, errors = schemaErrors, warnings)))

            case (_, warnings, false) =>
              Future.successful(
                MutationSuccess(DeployMutationPayload(args.clientMutationId, Some(Migration.empty(project.id)), errors = schemaErrors, warnings)))
          }
        }
    }
  }

  private def updateSecretsIfNecessary(): Future[Option[MigrationStep]] = {
    if (project.secrets != args.secrets && !args.dryRun.getOrElse(false)) {
      projectPersistence.update(project.copy(secrets = args.secrets)).map(_ => Some(UpdateSecrets(args.secrets)))
    } else {
      Future.successful(None)
    }
  }

  def getFunctionModelsOrErrors(fns: Vector[FunctionInput]): Vector[Function] Or Vector[SchemaError] = {
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
