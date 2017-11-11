package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors.NotFoundException
import cool.graph.shared.models
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteAlgoliaSyncQuery, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteAlgoliaSyncQueryMutation(client: models.Client,
                                          project: models.Project,
                                          args: DeleteAlgoliaSyncQueryInput,
                                          projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteAlgoliaSyncQueryPayload] {

  var algoliaSyncQuery: Option[models.AlgoliaSyncQuery]           = None
  var searchProviderAlgolia: Option[models.SearchProviderAlgolia] = None

  override def prepareActions(): List[Mutaction] = {
    algoliaSyncQuery = project.getAlgoliaSyncQueryById(args.algoliaSyncQueryId)

    val pendingActions: List[Mutaction] = algoliaSyncQuery match {
      case Some(algoliaSyncQueryToDelete: models.AlgoliaSyncQuery) =>
        searchProviderAlgolia = project.getSearchProviderAlgoliaByAlgoliaSyncQueryId(args.algoliaSyncQueryId)

        val removeAlgoliaSyncQueryFromProject =
          DeleteAlgoliaSyncQuery(
            searchProviderAlgolia = searchProviderAlgolia.get,
            algoliaSyncQuery = algoliaSyncQueryToDelete
          )
        List(removeAlgoliaSyncQueryFromProject, BumpProjectRevision(project = project), InvalidateSchema(project = project))

      case None =>
        List(InvalidInput(NotFoundException("This algoliaSearchQueryId does not correspond to an existing AlgoliaSearchQuery")))
    }

    actions = pendingActions
    actions
  }

  override def getReturnValue: Option[DeleteAlgoliaSyncQueryPayload] = {
    val updatedSearchProviderAlgolia = searchProviderAlgolia.get.copy(
      algoliaSyncQueries = searchProviderAlgolia.get.algoliaSyncQueries
        .filterNot(_.id == algoliaSyncQuery.get.id))
    val updatedProject = project.copy(integrations = project.authProviders.filter(_.id != searchProviderAlgolia.get.id) :+ updatedSearchProviderAlgolia)

    Some(
      DeleteAlgoliaSyncQueryPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        algoliaSyncQuery = algoliaSyncQuery.get,
        searchProviderAlgolia = searchProviderAlgolia.get.copy(
          algoliaSyncQueries = searchProviderAlgolia.get.algoliaSyncQueries
            .filter(_.id != algoliaSyncQuery.get.id)
        )
      ))
  }
}

case class DeleteAlgoliaSyncQueryPayload(clientMutationId: Option[String],
                                         project: models.Project,
                                         algoliaSyncQuery: models.AlgoliaSyncQuery,
                                         searchProviderAlgolia: models.SearchProviderAlgolia)
    extends Mutation

case class DeleteAlgoliaSyncQueryInput(clientMutationId: Option[String], algoliaSyncQueryId: String)
