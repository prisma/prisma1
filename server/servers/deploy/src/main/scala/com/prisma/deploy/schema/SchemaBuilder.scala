package com.prisma.deploy.schema

import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.SchemaMapper
import com.prisma.deploy.migration.inference.{MigrationStepsInferrer, SchemaInferrer}
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.schema.fields._
import com.prisma.deploy.schema.mutations._
import com.prisma.deploy.schema.types._
import com.prisma.jwt.JwtGrant
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import com.prisma.utils.future.FutureUtils.FutureOpt
import sangria.relay.Mutation
import sangria.schema._

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class SystemUserContext(authorizationHeader: Option[String])

trait SchemaBuilder {
  def apply(userContext: SystemUserContext): Schema[SystemUserContext, Unit]
}

object SchemaBuilder {
  def apply()(implicit dependencies: DeployDependencies): SchemaBuilder =
    (userContext: SystemUserContext) => { SchemaBuilderImpl(userContext).build() }
}

case class SchemaBuilderImpl(
    userContext: SystemUserContext
)(implicit dependencies: DeployDependencies) {
  import ManualMarshallerHelpers._
  import dependencies.executionContext

  val projectPersistence: ProjectPersistence         = dependencies.projectPersistence
  val migrationPersistence: MigrationPersistence     = dependencies.migrationPersistence
  val deployConnector: DeployConnector               = dependencies.deployConnector
  val migrator: Migrator                             = dependencies.migrator
  val schemaInferrer: SchemaInferrer                 = SchemaInferrer(deployConnector.capabilities)
  val migrationStepsInferrer: MigrationStepsInferrer = MigrationStepsInferrer()
  val schemaMapper: SchemaMapper                     = SchemaMapper
  val projectIdEncoder: ProjectIdEncoder             = dependencies.projectIdEncoder
  val projectTokenExpiration                         = 1.day.toSeconds
  val managementSecret                               = dependencies.managementSecret

  def build(): Schema[SystemUserContext, Unit] = {
    val Query = ObjectType[SystemUserContext, Unit](
      "Query",
      getQueryFields.toList
    )

    val Mutation = ObjectType(
      "Mutation",
      getMutationFields.toList
    )

    Schema(Query, Some(Mutation), additionalTypes = MigrationStepType.allTypes)
  }

  def getQueryFields: Vector[Field[SystemUserContext, Unit]] = Vector(
    migrationStatusField,
    listProjectsField,
    listMigrationsField,
    projectField,
    serverInfoField,
    generateProjectTokenField
  )

  def getMutationFields: Vector[Field[SystemUserContext, Unit]] = Vector(
    deployField,
    addProjectField,
    deleteProjectField,
    setCloudSecretField
  )

  val migrationStatusField: Field[SystemUserContext, Unit] = Field(
    "migrationStatus",
    MigrationType.Type,
    arguments = projectIdArguments,
    description = Some(
      "Shows the status of the next migration in line to be applied to the project. If no such migration exists, it shows the last applied migration."
    ),
    resolve = (ctx) => {
      val name  = ctx.args.raw.name
      val stage = ctx.args.raw.stage

      val projectId = projectIdEncoder.toEncodedString(name, stage)

      verifyAuthOrThrow(name, stage, ctx.ctx.authorizationHeader)

      FutureOpt(migrationPersistence.getNextMigration(projectId)).fallbackTo(migrationPersistence.getLastMigration(projectId)).map {
        case Some(migration) => migration
        case None            => throw InvalidProjectId(projectIdEncoder.fromEncodedString(projectId))
      }
    }
  )

  val listProjectsField: Field[SystemUserContext, Unit] = Field(
    "listProjects",
    ListType(ProjectType.Type(projectIdEncoder, migrationPersistence)),
    description = Some("Shows all projects the caller has access to."),
    resolve = (ctx) => {
      // Only accessible via */* token, like the one the Cloud API uses
      verifyAuthOrThrow("", "", ctx.ctx.authorizationHeader)
      projectPersistence.loadAll()
    }
  )

  // todo remove if not used anymore
  val listMigrationsField: Field[SystemUserContext, Unit] = Field(
    "listMigrations",
    ListType(MigrationType.Type),
    arguments = projectIdArguments,
    description = Some("Shows all migrations for the project. Debug query, will likely be removed in the future."),
    resolve = (ctx) => {
      val name      = ctx.args.raw.name
      val stage     = ctx.args.raw.stage
      val projectId = projectIdEncoder.toEncodedString(name, stage)

      verifyAuthOrThrow(name, stage, ctx.ctx.authorizationHeader)
      migrationPersistence.loadAll(projectId)
    }
  )

  val projectField: Field[SystemUserContext, Unit] = Field(
    "project",
    ProjectType.Type(projectIdEncoder, migrationPersistence),
    arguments = projectIdArguments,
    description = Some("Gets a project by name and stage."),
    resolve = (ctx) => {
      val name      = ctx.args.raw.name
      val stage     = ctx.args.raw.stage
      val projectId = projectIdEncoder.toEncodedString(name, stage)

      verifyAuthOrThrow(name, stage, ctx.ctx.authorizationHeader)

      for {
        projectOpt <- projectPersistence.load(projectId)
      } yield {
        projectOpt.getOrElse(throw InvalidProjectId(projectIdEncoder.fromEncodedString(projectId)))
      }
    }
  )

  val serverInfoField: Field[SystemUserContext, Unit] = Field(
    "serverInfo",
    ServerInfoType.Type(dependencies.config),
    description = Some("Information about the server"),
    resolve = (ctx) => ()
  )

  val generateProjectTokenField: Field[SystemUserContext, Unit] = Field(
    "generateProjectToken",
    StringType,
    arguments = projectIdArguments,
    description = Some("generates a token for the given project"),
    resolve = (ctx) => {
      val name      = ctx.args.raw.name
      val stage     = ctx.args.raw.stage
      val projectId = projectIdEncoder.toEncodedString(name, stage)
      verifyAuthOrThrow(name, stage, ctx.ctx.authorizationHeader)

      for {
        projectOpt <- projectPersistence.load(projectId)
        project    = projectOpt.getOrElse(throw InvalidProjectId(projectIdEncoder.fromEncodedString(projectId)))
      } yield {
        project.secrets.headOption match {
          case Some(secret) =>
            dependencies.auth.createToken(secret, Some(projectTokenExpiration)) match {
              case Success(token) => token
              case Failure(err)   => throw AuthFailure(err.getMessage)
            }

          case None => ""
        }
      }
    }
  )

  def deployField: Field[SystemUserContext, Unit] = {
    import DeployField.fromInput
    Mutation.fieldWithClientMutationId[SystemUserContext, Unit, DeployMutationPayload, DeployMutationInput](
      fieldName = "deploy",
      typeName = "Deploy",
      inputFields = DeployField.inputFields,
      outputFields = sangria.schema.fields[SystemUserContext, DeployMutationPayload](
        Field("errors", ListType(DeployErrorType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.errors),
        Field("migration", OptionType(MigrationType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.migration),
        Field("warnings", ListType(DeployWarningType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.warnings),
        Field("steps", ListType(MigrationStepType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.steps),
      ),
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          for {
            project   <- getProjectOrThrow(projectIdEncoder.toEncodedString(args.name, args.stage))
            projectId = projectIdEncoder.fromEncodedString(project.id)
            _         = verifyAuthOrThrow(projectId.name, projectId.stage, ctx.ctx.authorizationHeader)
            result <- DeployMutation(
                       args = args,
                       project = project,
                       schemaInferrer = schemaInferrer,
                       migrationStepsInferrer = migrationStepsInferrer,
                       schemaMapper = schemaMapper,
                       migrationPersistence = migrationPersistence,
                       projectPersistence = projectPersistence,
                       migrator = migrator,
                       functionValidator = dependencies.functionValidator,
                       invalidationPublisher = dependencies.invalidationPublisher,
                       capabilities = deployConnector.capabilities,
                       clientDbQueries = deployConnector.clientDBQueries(project),
                       databaseIntrospectionInferrer = deployConnector.databaseIntrospectionInferrer(project.id),
                       fieldRequirements = deployConnector.fieldRequirements,
                       isActive = deployConnector.isActive,
                       deployConnector = deployConnector
                     ).execute
          } yield result
      }
    )
  }

  def addProjectField: Field[SystemUserContext, Unit] = {
    import AddProjectField.fromInput
    Mutation.fieldWithClientMutationId[SystemUserContext, Unit, AddProjectMutationPayload, AddProjectInput](
      fieldName = "addProject",
      typeName = "AddProject",
      inputFields = AddProjectField.inputFields,
      outputFields = sangria.schema.fields[SystemUserContext, AddProjectMutationPayload](
        Field(
          "project",
          OptionType(ProjectType.Type(projectIdEncoder, migrationPersistence)),
          resolve = (ctx: Context[SystemUserContext, AddProjectMutationPayload]) => ctx.value.project
        )
      ),
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          verifyAuthOrThrow(args.name, args.stage, ctx.ctx.authorizationHeader)

          AddProjectMutation(
            args = args,
            projectPersistence = projectPersistence,
            migrationPersistence = migrationPersistence,
            deployConnector = dependencies.deployConnector,
            connectorCapabilities = dependencies.deployConnector.capabilities
          ).execute
      }
    )
  }

  def deleteProjectField: Field[SystemUserContext, Unit] = {
    import DeleteProjectField.fromInput
    Mutation.fieldWithClientMutationId[SystemUserContext, Unit, DeleteProjectMutationPayload, DeleteProjectInput](
      fieldName = "deleteProject",
      typeName = "DeleteProject",
      inputFields = DeleteProjectField.inputFields,
      outputFields = sangria.schema.fields[SystemUserContext, DeleteProjectMutationPayload](
        Field(
          "project",
          OptionType(ProjectType.Type(projectIdEncoder, migrationPersistence)),
          resolve = (ctx: Context[SystemUserContext, DeleteProjectMutationPayload]) => ctx.value.project
        )
      ),
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          verifyAuthOrThrow(args.name, args.stage, ctx.ctx.authorizationHeader)
          DeleteProjectMutation(
            args = args,
            projectPersistence = projectPersistence,
            invalidationPubSub = dependencies.invalidationPublisher,
            deployConnector = dependencies.deployConnector,
            connectorCapabilities = dependencies.deployConnector.capabilities
          ).execute
      }
    )
  }

  def setCloudSecretField: Field[SystemUserContext, Unit] = {
    import SetCloudSecretField.fromInput
    Mutation.fieldWithClientMutationId[SystemUserContext, Unit, SetCloudSecretMutationPayload, SetCloudSecretMutationInput](
      fieldName = "setCloudSecret",
      typeName = "SetCloudSecret",
      inputFields = SetCloudSecretField.inputFields,
      outputFields = List.empty,
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          verifyAuthOrThrow("", "", ctx.ctx.authorizationHeader)
          SetCloudSecretMutation(args).execute
      }
    )
  }

  private def handleMutationResult[T](result: Future[MutationResult[T]]): Future[T] = {
    result.map {
      case MutationSuccess(x) => x
      case error              => sys.error(s"The mutation failed with the error: $error")
    }
  }

  private def getProjectOrThrow(projectId: String): Future[Project] = {
    projectPersistence.load(projectId).map { projectOpt =>
      projectOpt.getOrElse(throw InvalidProjectId(projectIdEncoder.fromEncodedString(projectId)))
    }
  }

  private def verifyAuthOrThrow(name: String, stage: String, authHeader: Option[String]) = {
    val auth  = dependencies.managementAuth
    val token = auth.extractToken(authHeader)
    val grant = Some(JwtGrant(name, stage, "*"))

    auth.verifyToken(token, Vector(managementSecret), expectedGrant = grant).get
  }
}
