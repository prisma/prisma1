package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.MigrationStepsExecutor
import cool.graph.shared.models.{MigrationSteps, Project}
import org.scalactic.Or

import scala.concurrent.{ExecutionContext, Future}

case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    migrationStepsExecutor: MigrationStepsExecutor,
    projectPersistence: ProjectPersistence
)(
    implicit ec: ExecutionContext
) extends Mutation[DeployMutationPayload] {
  import cool.graph.util.or.OrExtensions._

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    for {
      steps          <- migrationSteps.toFuture
      updatedProject <- migrationStepsExecutor.execute(project, steps).toFuture
      _              <- projectPersistence.save(updatedProject, steps)
    } yield {
      MutationSuccess(DeployMutationPayload(args.clientMutationId, updatedProject))
    }
  }

  lazy val migrationSteps: MigrationSteps Or Exception = ??? // todo: parse out of args
}

case class DeployMutationInput(
    clientMutationId: Option[String],
    projectId: String,
    config: String
) extends sangria.relay.Mutation

case class DeployMutationPayload(
    clientMutationId: Option[String],
    project: Project
) extends sangria.relay.Mutation
