package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.{DesiredProjectInferer, MigrationStepsExecutor, MigrationStepsProposer, RenameInferer}
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
    renameInferer: RenameInferer,
    projectPersistence: ProjectPersistence
)(
    implicit ec: ExecutionContext
) extends Mutation[DeployMutationPayload] {
  import cool.graph.util.or.OrExtensions._

  val graphQlSdl = QueryParser.parse(args.types).get

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    for {
      desiredProject <- desiredProjectInferer.infer(graphQlSdl).toFuture
      renames        = renameInferer.infer(graphQlSdl)
      migrationSteps = migrationStepsProposer.propose(project, desiredProject, renames)
      _              <- projectPersistence.save(desiredProject, migrationSteps)
    } yield {
      MutationSuccess(DeployMutationPayload(args.clientMutationId, desiredProject))
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
