package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import cool.graph.deploy.migration.validation.{SchemaError, SchemaErrors, SchemaSyntaxValidator}
import cool.graph.deploy.migration._
import cool.graph.deploy.migration.inference.{InvalidGCValue, MigrationStepsInferrer, RelationDirectiveNeeded, SchemaInferrer}
import cool.graph.deploy.migration.migrator.Migrator
import cool.graph.shared.models.{Migration, MigrationStep, Project, Schema}
import org.scalactic.{Bad, Good}
import sangria.parser.QueryParser

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

// todo should the deploy mutation work with schemas only?
case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    schemaInferrer: SchemaInferrer,
    migrationStepsInferrer: MigrationStepsInferrer,
    schemaMapper: SchemaMapper,
    migrationPersistence: MigrationPersistence,
    projectPersistence: ProjectPersistence,
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
            migration = None,
            errors = schemaErrors
          ))
      }
    } else {
      performDeployment
    }
  }

  private def performDeployment: Future[MutationSuccess[DeployMutationPayload]] = {
    schemaInferrer.infer(project.schema, graphQlSdl) match {
      case Good(inferredNextSchema) =>
        val schemaMapping = schemaMapper.createMapping(graphQlSdl)
        val steps         = migrationStepsInferrer.infer(project.schema, inferredNextSchema, schemaMapping)

        handleProjectUpdate().flatMap(_ =>
          handleMigration(inferredNextSchema, steps).map { migration =>
            MutationSuccess(
              DeployMutationPayload(
                args.clientMutationId,
                migration,
                schemaErrors
              ))
        })

      case Bad(err) =>
        Future.successful {
          MutationSuccess(
            DeployMutationPayload(
              clientMutationId = args.clientMutationId,
              migration = None,
              errors = List(err match {
                case RelationDirectiveNeeded(t1, t1Fields, t2, t2Fields) => SchemaError.global(s"Relation directive required for types $t1 and $t2.")
                case InvalidGCValue(err)                                 => SchemaError.global(s"Invalid value '${err.value}' for type ${err.typeIdentifier}.")
              })
            ))
        }
    }
  }

  private def handleProjectUpdate(): Future[_] = {
    if (project.secrets != args.secrets && !args.dryRun.getOrElse(false)) {
      projectPersistence.update(project.copy(secrets = args.secrets))
    } else {
      Future.unit
    }
  }

  private def handleMigration(nextSchema: Schema, steps: Vector[MigrationStep]): Future[Option[Migration]] = {
    if (steps.nonEmpty && !args.dryRun.getOrElse(false)) {
      migrator.schedule(project.id, nextSchema, steps).map(Some(_))
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
    migration: Option[Migration],
    errors: Seq[SchemaError]
) extends sangria.relay.Mutation
