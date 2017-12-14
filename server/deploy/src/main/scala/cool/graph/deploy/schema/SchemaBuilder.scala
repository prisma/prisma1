package cool.graph.deploy.schema

import akka.actor.ActorSystem
import cool.graph.deploy.DeployDependencies
import cool.graph.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import cool.graph.deploy.migration.{DesiredProjectInferer, MigrationStepsProposer, Migrator, RenameInferer}
import cool.graph.deploy.schema.fields.{AddProjectField, DeployField, ManualMarshallerHelpers}
import cool.graph.deploy.schema.mutations._
import cool.graph.deploy.schema.types._
import cool.graph.shared.models.Project
import cool.graph.utils.future.FutureUtils.FutureOpt
import sangria.relay.Mutation
import sangria.schema._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future

case class SystemUserContext()

trait SchemaBuilder {
  def apply(userContext: SystemUserContext): Schema[SystemUserContext, Unit]
}

object SchemaBuilder {
  def apply()(implicit system: ActorSystem, dependencies: DeployDependencies): SchemaBuilder =
    new SchemaBuilder {
      override def apply(userContext: SystemUserContext) = {
        SchemaBuilderImpl(userContext).build()
      }
    }
}

case class SchemaBuilderImpl(
    userContext: SystemUserContext
)(implicit system: ActorSystem, dependencies: DeployDependencies) {
  import ManualMarshallerHelpers._
  import system.dispatcher

  val internalDb: DatabaseDef                        = dependencies.internalDb
  val clientDb: DatabaseDef                          = dependencies.clientDb
  val projectPersistence: ProjectPersistence         = dependencies.projectPersistence
  val migrationPersistence: MigrationPersistence     = dependencies.migrationPersistence
  val migrator: Migrator                             = dependencies.migrator
  val desiredProjectInferer: DesiredProjectInferer   = DesiredProjectInferer()
  val migrationStepsProposer: MigrationStepsProposer = MigrationStepsProposer()
  val renameInferer: RenameInferer                   = RenameInferer

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
    clusterInfoField
  )

  def getMutationFields: Vector[Field[SystemUserContext, Unit]] = Vector(
    deployField,
    addProjectField
  )

  val migrationStatusField: Field[SystemUserContext, Unit] = Field(
    "migrationStatus",
    MigrationType.Type,
    arguments = projectIdArguments,
    description = Some(
      "Shows the status of the next migration in line to be applied to the project. If no such migration exists, it shows the last applied migration."
    ),
    resolve = (ctx) => {
      val projectId = ctx.args.raw.projectId
      FutureOpt(migrationPersistence.getNextMigration(projectId)).fallbackTo(migrationPersistence.getLastMigration(projectId)).map {
        case Some(migration) => migration
        case None            => throw InvalidProjectId(projectId)
      }
    }
  )

  val listProjectsField: Field[SystemUserContext, Unit] = Field(
    "listProjects",
    ListType(ProjectType.Type),
    description = Some("Shows all projects the caller has access to."),
    resolve = (ctx) => {
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
      migrationPersistence.loadAll(ctx.args.raw.projectId)
    }
  )

  val projectField: Field[SystemUserContext, Unit] = Field(
    "project",
    ProjectType.Type,
    arguments = projectIdArguments,
    description = Some("Gets a project by name and stage."),
    resolve = (ctx) => {
      val projectId = ctx.args.raw.projectId
      for {
        projectOpt <- projectPersistence.load(projectId)
      } yield {
        projectOpt.getOrElse(throw InvalidProjectId(projectId))
      }
    }
  )

  val clusterInfoField: Field[SystemUserContext, Unit] = Field(
    "clusterInfo",
    ClusterInfoType.Type,
    description = Some("Information about the cluster"),
    resolve = (ctx) => ()
  )

  def deployField: Field[SystemUserContext, Unit] = {
    import DeployField.fromInput
    Mutation.fieldWithClientMutationId[SystemUserContext, Unit, DeployMutationPayload, DeployMutationInput](
      fieldName = "deploy",
      typeName = "Deploy",
      inputFields = DeployField.inputFields,
      outputFields = sangria.schema.fields[SystemUserContext, DeployMutationPayload](
        Field("project", OptionType(ProjectType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.project),
        Field("errors", ListType(SchemaErrorType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.errors),
        Field("migration", MigrationType.Type, resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.migration)
      ),
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          for {
            project <- getProjectOrThrow(args.projectId)
            result <- DeployMutation(
                       args = args,
                       project = project,
                       desiredProjectInferer = desiredProjectInferer,
                       migrationStepsProposer = migrationStepsProposer,
                       renameInferer = renameInferer,
                       migrationPersistence = migrationPersistence,
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
        Field("project", OptionType(ProjectType.Type), resolve = (ctx: Context[SystemUserContext, AddProjectMutationPayload]) => ctx.value.project)
      ),
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          AddProjectMutation(
            args = args,
            projectPersistence = projectPersistence,
            migrationPersistence = migrationPersistence,
            clientDb = clientDb
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
      projectOpt.getOrElse(throw InvalidProjectId(projectId))
    }
  }
}
