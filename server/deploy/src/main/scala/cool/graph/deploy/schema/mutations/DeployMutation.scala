package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.{DesiredProjectInferer, MigrationStepsExecutor, MigrationStepsProposer}
import cool.graph.shared.models.{MigrationSteps, Project}
import org.scalactic.Or
import sangria.parser.QueryParser

import scala.concurrent.{ExecutionContext, Future}

case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    migrationStepsExecutor: MigrationStepsExecutor,
    desiredProjectInferer: DesiredProjectInferer,
    migrationStepsProposer: MigrationStepsProposer,
    projectPersistence: ProjectPersistence
)(
    implicit ec: ExecutionContext
) extends Mutation[DeployMutationPayload] {
  import cool.graph.util.or.OrExtensions._

  val graphQlSdl = QueryParser.parse(args.types).get

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    for {
      steps          <- migrationSteps.toFuture
      updatedProject <- migrationStepsExecutor.execute(project, steps).toFuture
      desiredProject = desiredProjectInferer.infer(graphQlSdl)
      _ = if (updatedProject != desiredProject) {
        val proposal = migrationStepsProposer.propose(project, desiredProject)
        sys.error(s"the desired project does not line up with the project created by the migrations. The following steps are a proposal: $proposal")
      }
      _ <- projectPersistence.save(updatedProject, steps)
    } yield {
      MutationSuccess(DeployMutationPayload(args.clientMutationId, updatedProject))
    }
  }

  lazy val migrationSteps: MigrationSteps Or Exception = {
    // todo: parse out of args
    // it should just return the steps that have not yet been applied on this server
    ???
  }
}

case class DeployMutationInput(
    clientMutationId: Option[String],
    projectId: String,
    config: String,
    types: String
) extends sangria.relay.Mutation

case class DeployMutationPayload(
    clientMutationId: Option[String],
    project: Project
) extends sangria.relay.Mutation

/**
  * SKETCH
  */
trait DeployMutationSketch {
  def deploy(desiredProject: Project, migrationSteps: MigrationSteps): DeployResultSketch
}

sealed trait DeployResultSketch
case class DeploySucceeded(project: Project, descriptions: Vector[VerbalDescription]) extends DeployResultSketch
case class MigrationsDontSuffice(proposal: MigrationSteps)                            extends DeployResultSketch

trait VerbalDescription {
  def description: String
}
