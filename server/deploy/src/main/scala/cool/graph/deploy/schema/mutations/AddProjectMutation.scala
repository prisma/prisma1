package cool.graph.deploy.schema.mutations

import cool.graph.cuid.Cuid
import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.shared.models._
import cool.graph.shared.project_dsl.TestProject
import scala.concurrent.{ExecutionContext, Future}

case class AddProjectMutation(
    args: AddProjectInput,
    client: Client,
    projectPersistence: ProjectPersistence
)(
    implicit ec: ExecutionContext
) extends Mutation[AddProjectMutationPayload] {

  override def execute: Future[MutationResult[AddProjectMutationPayload]] = {
    val newProject = Project(
      id = Cuid.createCuid(),
      name = args.name,
      alias = args.alias,
      projectDatabase = TestProject.database,
      ownerId = client.id
    )
    projectPersistence.save(newProject, MigrationSteps.empty).map { _ =>
      MutationSuccess(AddProjectMutationPayload(args.clientMutationId, newProject))
    }
  }
}

case class AddProjectMutationPayload(
    clientMutationId: Option[String],
    project: Project
) extends sangria.relay.Mutation

case class AddProjectInput(
    clientMutationId: Option[String],
    name: String,
    alias: Option[String]
) extends sangria.relay.Mutation
