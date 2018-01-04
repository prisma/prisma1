package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.MigrationPersistence
import cool.graph.deploy.migration.validation.{SchemaError, SchemaErrors, SchemaSyntaxValidator}
import cool.graph.deploy.migration._
import cool.graph.deploy.migration.inference.{MigrationStepsInferrer, SchemaInferrer}
import cool.graph.deploy.migration.migrator.Migrator
import cool.graph.shared.models.{Migration, MigrationStep, Project}
import org.scalactic.{Bad, Good}
import sangria.parser.QueryParser

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

// todo should the deploy mutation work with schemas only?
case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    schemaInferrer: SchemaInferrer,
    migrationStepsProposer: MigrationStepsInferrer,
    renameInferer: SchemaMapper,
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
            migration = None,
            errors = schemaErrors
          ))
      }
    } else {
      performDeployment
    }
  }

  private def performDeployment: Future[MutationSuccess[DeployMutationPayload]] = {
    schemaInferrer.infer(baseProject = project, graphQlSdl) match {
      case Good(inferredProject) =>
        val nextProject = inferredProject.copy(secrets = args.secrets)
        val renames     = renameInferer.createMapping(graphQlSdl)
        val steps       = migrationStepsProposer.propose(project, nextProject, renames)

        handleMigration(nextProject, steps).map { migration =>
          MutationSuccess(
            DeployMutationPayload(
              args.clientMutationId,
              nextProject,
              migration,
              schemaErrors
            ))
        }

      case Bad(err) =>
        Future.successful {
          MutationSuccess(
            DeployMutationPayload(
              clientMutationId = args.clientMutationId,
              project = project,
              migration = None,
              errors = List(err match {
                case RelationDirectiveNeeded(t1, t1Fields, t2, t2Fields) => SchemaError.global(s"Relation directive required for types $t1 and $t2.")
                case InvalidGCValue(err)                                 => SchemaError.global(s"Invalid value '${err.value}' for type ${err.typeIdentifier}.")
              })
            ))
        }
    }
  }

  private def handleMigration(nextProject: Project, steps: Vector[MigrationStep]): Future[Option[Migration]] = {
    val changesDetected = steps.nonEmpty || project.secrets != args.secrets

    if (changesDetected && !args.dryRun.getOrElse(false)) {
      migrator.schedule(nextProject, steps).map(Some(_))
    } else {
      Future.successful(None)
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
    migration: Option[Migration],
    errors: Seq[SchemaError]
) extends sangria.relay.Mutation
