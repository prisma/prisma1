package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.validation.{SchemaError, SchemaSyntaxValidator}
import cool.graph.deploy.migration.{DesiredProjectInferer, MigrationStepsProposer, RenameInferer}
import cool.graph.shared.models.{MigrationSteps, Project}
import sangria.parser.QueryParser

import scala.collection.Seq
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

  val validator    = SchemaSyntaxValidator(args.types)
  val schemaErrors = validator.validate()

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    if (schemaErrors.nonEmpty) {
      Future.successful {
        MutationSuccess(
          DeployMutationPayload(
            clientMutationId = args.clientMutationId,
            project = project,
            steps = MigrationSteps.empty,
            errors = schemaErrors
          ))
      }
    } else {
      performDeployment
    }
  }

  private def performDeployment: Future[MutationSuccess[DeployMutationPayload]] = {
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
      MutationSuccess(DeployMutationPayload(args.clientMutationId, desiredProject, migrationSteps, schemaErrors))
    }
  }
}

case class DeployMutationInput(
    clientMutationId: Option[String],
    projectId: String,
    types: String
) extends sangria.relay.Mutation

case class DeployMutationPayload(
    clientMutationId: Option[String],
    project: Project,
    steps: MigrationSteps,
    errors: Seq[SchemaError]
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
