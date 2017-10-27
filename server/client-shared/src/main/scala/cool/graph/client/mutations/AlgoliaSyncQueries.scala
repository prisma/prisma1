package cool.graph.client.mutations

import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions.SyncDataItemToAlgolia
import cool.graph.shared.models._
import scaldi.Injector

object AlgoliaSyncQueries {
  def extract(dataResolver: DataResolver, project: Project, model: Model, nodeId: String, operation: String)(
      implicit inj: Injector): List[SyncDataItemToAlgolia] = {
    project.integrations
      .filter(_.isEnabled)
      .filter(_.integrationType == IntegrationType.SearchProvider)
      .filter(_.name == IntegrationName.SearchProviderAlgolia)
      .collect {
        case searchProviderAlgolia: SearchProviderAlgolia =>
          searchProviderAlgolia.algoliaSyncQueries
      }
      .flatten
      .filter(_.isEnabled)
      .filter(_.model.id == model.id)
      .map(syncQuery =>
        SyncDataItemToAlgolia(
          model = model,
          project = project,
          nodeId = nodeId,
          syncQuery = syncQuery,
          searchProviderAlgolia = project
            .getSearchProviderAlgoliaByAlgoliaSyncQueryId(syncQuery.id)
            .get,
          requestId = dataResolver.requestContext.map(_.requestId).getOrElse(""),
          operation = operation
      ))
  }
}
