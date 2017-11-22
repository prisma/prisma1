package cool.graph.deploy.schema

import akka.actor.ActorSystem
import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.MigrationStepsExecutor
import cool.graph.deploy.schema.fields.DeployField
import cool.graph.deploy.schema.mutations.{DeployMutation, DeployMutationInput, DeployMutationPayload, MutationSuccess}
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

  val projectPersistence: ProjectPersistence = ???

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
    getDeployField
  )

  def getDeployField: Field[SystemUserContext, Unit] = {
    import DeployField.fromInput
    Mutation.fieldWithClientMutationId[SystemUserContext, Unit, DeployMutationPayload, DeployMutationInput](
      fieldName = "deploy",
      typeName = "Deploy",
      inputFields = DeployField.inputFields,
      outputFields = sangria.schema.fields[SystemUserContext, DeployMutationPayload](
        Field("project", OptionType(ProjectType.Type), resolve = (ctx: Context[SystemUserContext, DeployMutationPayload]) => ctx.value.project)
      ),
      mutateAndGetPayload = (args, ctx) => {
        for {
          project <- getProjectOrThrow(args.projectId)
          mutation = DeployMutation(
            args = args,
            project = project,
            migrationStepsExecutor = MigrationStepsExecutor,
            projectPersistence = projectPersistence
          )
          result <- mutation.execute
        } yield {
          result match {
            case MutationSuccess(result) => result
            case _                       => ???
          }
        }
      }
    )
  }

  def getProjectOrThrow(projectId: String): Future[Project] = {
    projectPersistence.load(projectId).map { projectOpt =>
      projectOpt.getOrElse(throw InvalidProjectId(projectId))
    }
  }
}
