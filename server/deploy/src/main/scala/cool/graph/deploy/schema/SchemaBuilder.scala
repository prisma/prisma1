package cool.graph.deploy.schema

import akka.actor.ActorSystem
import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.{DesiredProjectInferer, MigrationStepsExecutor, MigrationStepsProposer}
import cool.graph.deploy.schema.fields.{AddProjectField, DeployField}
import cool.graph.deploy.schema.mutations._
import cool.graph.deploy.schema.types.ProjectType
import cool.graph.shared.models.Project
import sangria.relay.Mutation
import sangria.schema._

import scala.concurrent.Future

trait SystemUserContext

trait SchemaBuilder {
  def apply(userContext: SystemUserContext): Schema[SystemUserContext, Unit]
}

object SchemaBuilder {
  def apply(fn: SystemUserContext => Schema[SystemUserContext, Unit]): SchemaBuilder = new SchemaBuilder {
    override def apply(userContext: SystemUserContext) = fn(userContext)
  }
}

class SchemaBuilderImpl(
    userContext: SystemUserContext
)(implicit system: ActorSystem) {
  import system.dispatcher

  val projectPersistence: ProjectPersistence         = ???
  val migrationStepsProposer: MigrationStepsProposer = ???
  val desiredProjectInferer: DesiredProjectInferer   = ???

  def build(): Schema[SystemUserContext, Unit] = {
    val Query = ObjectType(
      "Query",
      viewerField() :: Nil
    )

    val Mutation = ObjectType(
      "Mutation",
      getFields.toList
    )

    Schema(Query, Some(Mutation))
  }

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
        Field("project", OptionType(ProjectType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.project)
      ),
      mutateAndGetPayload = (args, ctx) =>
        handleMutationResult {
          for {
            project <- getProjectOrThrow(args.projectId)
            result <- DeployMutation(
                       args = args,
                       project = project,
                       migrationStepsExecutor = MigrationStepsExecutor,
                       desiredProjectInferer = desiredProjectInferer,
                       migrationStepsProposer = migrationStepsProposer,
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
            client = ???,
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
