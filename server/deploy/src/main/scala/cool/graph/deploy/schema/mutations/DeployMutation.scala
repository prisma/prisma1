package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.MigrationPersistence
import cool.graph.deploy.migration.validation.{SchemaError, SchemaErrors, SchemaSyntaxValidator}
import cool.graph.deploy.migration._
import cool.graph.shared.models.{Migration, Project}
import org.scalactic.{Bad, Good}
import sangria.parser.QueryParser

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    nextProjectInferrer: NextProjectInferrer,
    migrationStepsProposer: MigrationStepsProposer,
    renameInferer: RenameInferer,
    migrationPersistence: MigrationPersistence,
    migrator: Migrator
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
    nextProjectInferrer.infer(baseProject = project, graphQlSdl) match {
      case Good(inferredProject) =>
        val nextProject    = inferredProject.copy(secrets = args.secrets)
        val renames        = renameInferer.infer(graphQlSdl)
        val migrationSteps = migrationStepsProposer.propose(project, nextProject, renames)
        val migration      = Migration(nextProject.id, 0, hasBeenApplied = false, migrationSteps) // how to get to the revision...?

        for {
          savedMigration <- handleMigration(nextProject, migration)
        } yield {
          MutationSuccess(DeployMutationPayload(args.clientMutationId, nextProject, savedMigration, schemaErrors))
        }

      case Bad(err) =>
        Future.successful {
          MutationSuccess(
            DeployMutationPayload(
              clientMutationId = args.clientMutationId,
              project = project,
              migration = Migration.empty(project),
              errors = List(err match {
                case RelationDirectiveNeeded(t1, t1Fields, t2, t2Fields) => SchemaError.global(s"Relation directive required for types $t1 and $t2.")
                case InvalidGCValue(err)                                 => SchemaError.global(s"Invalid value '${err.value}' for type ${err.typeIdentifier}.")
              })
            ))
        }
    }
  }

  private def handleMigration(nextProject: Project, migration: Migration): Future[Migration] = {
    val changesDetected = migration.steps.nonEmpty || project.secrets != args.secrets

    if (changesDetected && !args.dryRun.getOrElse(false)) {
      for {
        savedMigration <- migrationPersistence.create(nextProject, migration)
      } yield {
        migrator.schedule(savedMigration)
        savedMigration
      }
    } else {
      Future.successful(migration)
    }
  }
}

case class DeployMutationInput(
    clientMutationId: Option[String],
    projectId: String,
    types: String,
    dryRun: Option[Boolean],
    secrets: Vector[String]
) extends sangria.relay.Mutation

case class DeployMutationPayload(
    clientMutationId: Option[String],
    project: Project,
    migration: Migration,
    errors: Seq[SchemaError]
) extends sangria.relay.Mutation

///**
//  * SKETCH
//  */
//trait DeployMutationSketch {
//  def deploy(desiredProject: Project, migrationSteps: Migration): DeployResultSketch
//}
//
//sealed trait DeployResultSketch
//case class DeploySucceeded(project: Project, descriptions: Vector[VerbalDescription]) extends DeployResultSketch
//case class MigrationsDontSuffice(proposal: Migration)                                 extends DeployResultSketch
//
//trait VerbalDescription {
//  def description: String
//}
