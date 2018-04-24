package com.prisma.deploy.schema

import akka.actor.ActorSystem
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.{DeployConnector, MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.migration.SchemaMapper
import com.prisma.deploy.migration.inference.{MigrationStepsInferrer, SchemaInferrer}
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.schema.fields.{AddProjectField, DeleteProjectField, DeployField, ManualMarshallerHelpers}
import com.prisma.deploy.schema.mutations._
import com.prisma.deploy.schema.types._
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import com.prisma.utils.future.FutureUtils.FutureOpt
import sangria.relay.Mutation
import sangria.schema._

import scala.concurrent.Future

case class SystemUserContext(authorizationHeader: Option[String])

trait SchemaBuilder {
  def apply(userContext: SystemUserContext): Schema[SystemUserContext, Unit]
}

object SchemaBuilder {
  def apply()(implicit system: ActorSystem, dependencies: DeployDependencies): SchemaBuilder =
    (userContext: SystemUserContext) => { SchemaBuilderImpl(userContext).build() }
}

case class SchemaBuilderImpl(
    userContext: SystemUserContext
)(implicit system: ActorSystem, dependencies: DeployDependencies) {
  import ManualMarshallerHelpers._
  import system.dispatcher

  val projectPersistence: ProjectPersistence         = dependencies.projectPersistence
  val migrationPersistence: MigrationPersistence     = dependencies.migrationPersistence
  val deployConnector: DeployConnector               = dependencies.deployConnector
  val migrator: Migrator                             = dependencies.migrator
  val schemaInferrer: SchemaInferrer                 = SchemaInferrer()
  val migrationStepsInferrer: MigrationStepsInferrer = MigrationStepsInferrer()
  val schemaMapper: SchemaMapper                     = SchemaMapper
  val projectIdEncoder: ProjectIdEncoder             = dependencies.projectIdEncoder

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
    clusterInfoField,
    generateProjectTokenField
  )

  def getMutationFields: Vector[Field[SystemUserContext, Unit]] = Vector(
    deployField,
    addProjectField,
    deleteProjectField
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
    ListType(ProjectType.Type(projectIdEncoder)),
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
    ProjectType.Type(projectIdEncoder),
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

  val clusterInfoField: Field[SystemUserContext, Unit] = Field(
    "clusterInfo",
    ClusterInfoType.Type,
    description = Some("Information about the cluster"),
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
        dependencies.apiAuth.createToken(project.secrets)
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
        Field("errors", ListType(SchemaErrorType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.errors),
        Field("migration", OptionType(MigrationType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.migration),
        Field("warnings", ListType(SchemaWarningType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.warnings)
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
                       deployConnector = deployConnector,
                       migrator = migrator
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
        Field("project",
              OptionType(ProjectType.Type(projectIdEncoder)),
              resolve = (ctx: Context[SystemUserContext, AddProjectMutationPayload]) => ctx.value.project)
      ),
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          verifyAuthOrThrow(args.name, args.stage, ctx.ctx.authorizationHeader)

          AddProjectMutation(
            args = args,
            projectPersistence = projectPersistence,
            migrationPersistence = migrationPersistence,
            deployConnector = dependencies.deployConnector
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
        Field("project",
              OptionType(ProjectType.Type(projectIdEncoder)),
              resolve = (ctx: Context[SystemUserContext, DeleteProjectMutationPayload]) => ctx.value.project)
      ),
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          verifyAuthOrThrow(args.name, args.stage, ctx.ctx.authorizationHeader)
          DeleteProjectMutation(
            args = args,
            projectPersistence = projectPersistence,
            invalidationPubSub = dependencies.invalidationPublisher,
            deployConnector = dependencies.deployConnector
          ).execute
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
    dependencies.managementAuth.verify(name, stage, authHeader).get
  }
}
