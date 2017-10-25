package cool.graph.system.mutations

import com.typesafe.config.Config
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors.NotFoundException
import cool.graph.shared.models
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.mutactions.client.SyncModelToAlgoliaViaRequest
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateAlgoliaSyncQuery}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global

case class UpdateAlgoliaSyncQueryMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateAlgoliaSyncQueryInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateAlgoliaSyncQueryPayload]
    with Injectable {

  var algoliaSyncQuery: Option[models.AlgoliaSyncQuery]           = None
  var searchProviderAlgolia: Option[models.SearchProviderAlgolia] = None
  val config: Config                                              = inject[Config]("config")

  override def prepareActions(): List[Mutaction] = {
    algoliaSyncQuery = project.getAlgoliaSyncQueryById(args.algoliaSyncQueryId)

    val pendingActions: List[Mutaction] = algoliaSyncQuery match {
      case Some(algoliaSyncQueryToUpdate: models.AlgoliaSyncQuery) =>
        searchProviderAlgolia = project.getSearchProviderAlgoliaByAlgoliaSyncQueryId(args.algoliaSyncQueryId)
        val oldAlgoliaSyncQuery = algoliaSyncQueryToUpdate
        algoliaSyncQuery = mergeInputValuesToAlgoliaSyncQuery(oldAlgoliaSyncQuery, args)

        val updateAlgoliaSyncQueryInProject =
          UpdateAlgoliaSyncQuery(
            oldAlgoliaSyncQuery = oldAlgoliaSyncQuery,
            newAlgoliaSyncQuery = algoliaSyncQuery.get
          )

        val reSyncModelToAlgolia = algoliaSyncQuery.get.isEnabled match {
          case false =>
            List.empty
          case true =>
            List(
              SyncModelToAlgoliaViaRequest(
                project = project,
                model = project.getModelById_!(algoliaSyncQuery.get.model.id),
                algoliaSyncQuery = algoliaSyncQuery.get,
                config = config
              )
            )
        }

        List(updateAlgoliaSyncQueryInProject, BumpProjectRevision(project = project), InvalidateSchema(project = project)) ++ reSyncModelToAlgolia

      case None =>
        List(InvalidInput(NotFoundException("This algoliaSearchQueryId does not correspond to an existing AlgoliaSearchQuery")))

    }

    actions = pendingActions
    actions
  }

  private def mergeInputValuesToAlgoliaSyncQuery(existingAlgoliaSyncQuery: models.AlgoliaSyncQuery,
                                                 updateValues: UpdateAlgoliaSyncQueryInput): Option[models.AlgoliaSyncQuery] = {
    Some(
      existingAlgoliaSyncQuery.copy(
        indexName = updateValues.indexName,
        fragment = updateValues.fragment,
        isEnabled = updateValues.isEnabled
      )
    )
  }

  override def getReturnValue: Option[UpdateAlgoliaSyncQueryPayload] = {
    val updatedSearchProviderAlgolia = searchProviderAlgolia.get.copy(
      algoliaSyncQueries =
        searchProviderAlgolia.get.algoliaSyncQueries
          .filterNot(_.id == algoliaSyncQuery.get.id) :+ algoliaSyncQuery.get)
    val updatedProject = project.copy(
      integrations =
        project.authProviders
          .filterNot(_.id == searchProviderAlgolia.get.id) :+ updatedSearchProviderAlgolia)

    Some(
      UpdateAlgoliaSyncQueryPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        algoliaSyncQuery = algoliaSyncQuery.get,
        searchProviderAlgolia = searchProviderAlgolia.get
      ))
  }
}

case class UpdateAlgoliaSyncQueryPayload(clientMutationId: Option[String],
                                         project: models.Project,
                                         algoliaSyncQuery: models.AlgoliaSyncQuery,
                                         searchProviderAlgolia: models.SearchProviderAlgolia)
    extends Mutation

case class UpdateAlgoliaSyncQueryInput(clientMutationId: Option[String], algoliaSyncQueryId: String, indexName: String, fragment: String, isEnabled: Boolean)
