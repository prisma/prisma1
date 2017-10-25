package cool.graph.system.mutations

import cool.graph._
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.SystemErrors.{SchemaError, SystemApiError, WithSchemaError}
import cool.graph.shared.models
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.metrics.SystemMetrics
import cool.graph.system.migration.dataSchema._
import cool.graph.system.migration.dataSchema.SchemaFileHeader
import cool.graph.system.migration.dataSchema.validation.{SchemaErrors, SchemaValidator}
import cool.graph.system.mutactions.internal.UpdateTypeAndFieldPositions
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.collection.Seq
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class MigrateSchemaMutation(client: models.Client,
                                 project: models.Project,
                                 args: MigrateSchemaInput,
                                 schemaFileHeader: SchemaFileHeader,
                                 projectDbsFn: models.Project => InternalAndProjectDbs,
                                 clientDbQueries: ClientDbQueries)(implicit inj: Injector)
    extends InternalProjectMutation[MigrateSchemaMutationPayload]
    with Injectable {
  import scala.concurrent.ExecutionContext.Implicits.global

  val projectQueries: ProjectQueries = inject[ProjectQueries](identified by "projectQueries")

  var verbalDescriptions: Seq[VerbalDescription] = Seq.empty
  var errors: Seq[SchemaError]                   = Seq.empty

  override def prepareActions(): List[Mutaction] = {
    errors = SchemaValidator(project, args.newSchema, schemaFileHeader).validate()
    if (errors.nonEmpty) {
      return List.empty
    }

    val migrator                     = SchemaMigrator(project, args.newSchema, args.clientMutationId)
    val actions: UpdateSchemaActions = migrator.determineActionsForUpdate

    verbalDescriptions = actions.verbalDescriptions

    if (actions.isDestructive && !args.force) {
      errors = Seq[SchemaError](SchemaErrors.forceArgumentRequired)

      return List.empty
    }

    val (mutations, _) = actions.determineMutations(client, project, _ => InternalAndProjectDbs(internalDatabase), clientDbQueries)

    // UPDATE PROJECT
    val updateTypeAndFieldPositions = UpdateTypeAndFieldPositions(
      project = project,
      client = client,
      newSchema = migrator.diffResult.newSchema,
      internalDatabase = internalDatabase.databaseDef,
      projectQueries = projectQueries
    )

    this.actions = mutations.toList.flatMap(_.prepareActions()) ++ List(updateTypeAndFieldPositions)

    MigrateSchemaMutation.migrateSchemaCount.incBy(1)
    MigrateSchemaMutation.migrateSchemaMutactionsCount.incBy(this.actions.length)

    this.actions
  }

  override def verifyActions(): Future[List[Try[MutactionVerificationSuccess]]] = {
    super.verifyActions().map { verifications =>
      verifications.map {
        case Failure(sysError: WithSchemaError) =>
          val fallbackError = SchemaError.global(sysError.getMessage)
          val schemaError   = sysError.schemaError.getOrElse(fallbackError)
          errors = errors :+ schemaError
          verbalDescriptions = List.empty
          this.actions = List.empty
          Success(MutactionVerificationSuccess())

        case verification =>
          verification
      }
    }
  }

  override def performActions(requestContext: Option[SystemRequestContextTrait]): Future[List[MutactionExecutionResult]] = {
    if (args.isDryRun) {
      Future.successful(List(MutactionExecutionSuccess()))
    } else {
      super.performActions(requestContext)
    }
  }

  override def getReturnValue(): Option[MigrateSchemaMutationPayload] = {
    Some(
      MigrateSchemaMutationPayload(
        clientMutationId = args.clientMutationId,
        client = client,
        project = project,
        verbalDescriptions = verbalDescriptions,
        errors = errors
      )
    )
  }
}

object MigrateSchemaMutation {

  val migrateSchemaMutactionsCount = SystemMetrics.defineCounter("migrateSchemaMutactionsCount")
  val migrateSchemaCount           = SystemMetrics.defineCounter("migrateSchemaCount")

}

case class MigrateSchemaMutationPayload(clientMutationId: Option[String],
                                        client: models.Client,
                                        project: models.Project,
                                        verbalDescriptions: Seq[VerbalDescription],
                                        errors: Seq[SchemaError])
    extends Mutation

case class MigrateSchemaInput(clientMutationId: Option[String], newSchema: String, isDryRun: Boolean, force: Boolean)
