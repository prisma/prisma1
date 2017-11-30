package cool.graph.deploy.schema

import akka.actor.ActorSystem
import cool.graph.deploy.database.persistence.{ProjectPersistence, ProjectPersistenceImpl}
import cool.graph.deploy.migration.{DesiredProjectInferer, MigrationStepsProposer, RenameInferer}
import cool.graph.deploy.schema.fields.{AddProjectField, DeployField}
import cool.graph.deploy.schema.mutations._
import cool.graph.deploy.schema.types.{MigrationStepType, ProjectType, SchemaErrorType}
import cool.graph.shared.models.{Client, Project}
import sangria.relay.Mutation
import sangria.schema.{Field, _}
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future

case class SystemUserContext(client: Client)

trait SchemaBuilder {
  def apply(userContext: SystemUserContext): Schema[SystemUserContext, Unit]
}

object SchemaBuilder {
  def apply(internalDb: DatabaseDef, projectPersistence: ProjectPersistence)(implicit system: ActorSystem): SchemaBuilder = new SchemaBuilder {
    override def apply(userContext: SystemUserContext) = {
      SchemaBuilderImpl(userContext, internalDb, projectPersistence).build()
    }
  }
}

case class SchemaBuilderImpl(
    userContext: SystemUserContext,
    internalDb: DatabaseDef,
    projectPersistence: ProjectPersistence
)(implicit system: ActorSystem) {
  import system.dispatcher

  val desiredProjectInferer: DesiredProjectInferer   = DesiredProjectInferer()
  val migrationStepsProposer: MigrationStepsProposer = MigrationStepsProposer()
  val renameInferer: RenameInferer                   = RenameInferer

  def build(): Schema[SystemUserContext, Unit] = {
    val Query = ObjectType[SystemUserContext, Unit](
      "Query",
      List(dummyField)
    )

    val Mutation = ObjectType(
      "Mutation",
      getFields.toList
    )

    Schema(Query, Some(Mutation), additionalTypes = MigrationStepType.allTypes)
  }

  val dummyField: Field[SystemUserContext, Unit] = Field(
    "dummy",
    description = Some("This is only a dummy field due to the API of Schema of Sangria, as Query is not optional"),
    fieldType = StringType,
    resolve = (ctx) => "this is dumb"
  )

  def viewerField(): Field[SystemUserContext, Unit] = {
//    Field(
//      "viewer",
//      fieldType = viewerType,
//      resolve = _ => ViewerModel()
//    )
    ???
  }

  def getFields: Vector[Field[SystemUserContext, Unit]] = Vector(
    deployField,
    addProjectField
  )

  def deployField: Field[SystemUserContext, Unit] = {
    import DeployField.fromInput
    Mutation.fieldWithClientMutationId[SystemUserContext, Unit, DeployMutationPayload, DeployMutationInput](
      fieldName = "deploy",
      typeName = "Deploy",
      inputFields = DeployField.inputFields,
      outputFields = sangria.schema.fields[SystemUserContext, DeployMutationPayload](
        Field("project", OptionType(ProjectType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.project),
        Field("steps", ListType(MigrationStepType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.steps.steps.toList),
        Field("errors", ListType(SchemaErrorType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.errors)
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
                       projectPersistence = projectPersistence
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
            client = ctx.ctx.client,
            projectPersistence = projectPersistence
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
