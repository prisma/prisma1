package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.{DesiredProjectInferer, MigrationStepsProposer, RenameInferer}
import cool.graph.shared.models.{MigrationSteps, Project}
import sangria.parser.QueryParser

import scala.concurrent.{ExecutionContext, Future}

case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    desiredProjectInferer: DesiredProjectInferer,
    migrationStepsProposer: MigrationStepsProposer,
    renameInferer: RenameInferer,
    projectPersistence: ProjectPersistence
)(
    implicit ec: ExecutionContext
) extends Mutation[DeployMutationPayload] {
  import cool.graph.util.or.OrExtensions._

  val graphQlSdl = QueryParser.parse(args.types).get

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    for {
      desiredProject <- desiredProjectInferer.infer(baseProject = project, graphQlSdl).toFuture
      renames        = renameInferer.infer(graphQlSdl)
      migrationSteps = migrationStepsProposer.propose(project, desiredProject, renames)
      _ <- if (migrationSteps.steps.nonEmpty) {
            projectPersistence.save(desiredProject, migrationSteps)
          } else {
            Future.successful(())
          }
    } yield {
      MutationSuccess(DeployMutationPayload(args.clientMutationId, desiredProject, migrationSteps))
    }
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
    project: Project,
    steps: MigrationSteps
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
