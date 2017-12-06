package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.MigrationPersistence
import cool.graph.deploy.migration.validation.{SchemaError, SchemaSyntaxValidator}
import cool.graph.deploy.migration.{DesiredProjectInferer, MigrationStepsProposer, RenameInferer}
import cool.graph.shared.models.{Migration, Project}
import sangria.parser.QueryParser

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    desiredProjectInferer: DesiredProjectInferer,
    migrationStepsProposer: MigrationStepsProposer,
    renameInferer: RenameInferer,
    migrationPersistence: MigrationPersistence
)(
    implicit ec: ExecutionContext
) extends Mutation[DeployMutationPayload] {
  import cool.graph.util.or.OrExtensions._

  val graphQlSdl   = QueryParser.parse(args.types).get
  val validator    = SchemaSyntaxValidator(args.types)
  val schemaErrors = validator.validate()

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    if (schemaErrors.nonEmpty) {
      Future.successful {
        MutationSuccess(
          DeployMutationPayload(
            clientMutationId = args.clientMutationId,
            project = project,
            migration = Migration.empty(project),
            errors = schemaErrors
          ))
      }
    } else {
      performDeployment
    }
  }

  private def performDeployment: Future[MutationSuccess[DeployMutationPayload]] = {
    for {
      nextProject    <- desiredProjectInferer.infer(baseProject = project, graphQlSdl).toFuture
      renames        = renameInferer.infer(graphQlSdl)
      migrationSteps = migrationStepsProposer.propose(project, nextProject, renames)
      migration      = Migration(nextProject.id, 0, hasBeenApplied = false, migrationSteps) // how to get to the revision...?
      savedMigration <- handleMigration(nextProject, migration)
    } yield {
      MutationSuccess(DeployMutationPayload(args.clientMutationId, nextProject, savedMigration, schemaErrors))
    }
  }

  private def handleMigration(nextProject: Project, migration: Migration): Future[Migration] = {
    if (migration.steps.nonEmpty && !args.dryRun.getOrElse(false)) {
      migrationPersistence.create(nextProject, migration)
    } else {
      Future.successful(migration)
    }
  }
}

case class DeployMutationInput(
    clientMutationId: Option[String],
    projectId: String,
    types: String,
    dryRun: Option[Boolean]
) extends sangria.relay.Mutation

case class DeployMutationPayload(
    clientMutationId: Option[String],
    project: Project,
    migration: Migration,
    errors: Seq[SchemaError]
) extends sangria.relay.Mutation

/**
  * SKETCH
  */
trait DeployMutationSketch {
  def deploy(desiredProject: Project, migrationSteps: Migration): DeployResultSketch
}

sealed trait DeployResultSketch
case class DeploySucceeded(project: Project, descriptions: Vector[VerbalDescription]) extends DeployResultSketch
case class MigrationsDontSuffice(proposal: Migration)                                 extends DeployResultSketch

trait VerbalDescription {
  def description: String
}
