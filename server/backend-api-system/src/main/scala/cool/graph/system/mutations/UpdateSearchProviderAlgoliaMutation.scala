package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{IntegrationName, IntegrationType, SearchProviderAlgolia}
import cool.graph.system.mutactions.internal._
import cool.graph.{InternalProjectMutation, Mutaction, SystemSqlMutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateSearchProviderAlgoliaMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateSearchProviderAlgoliaInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateSearchProviderAlgoliaPayload] {

  var searchProviderAlgolia: Option[SearchProviderAlgolia] = None

  override def prepareActions(): List[Mutaction] = {
    val integration =
      project.getIntegrationByTypeAndName(IntegrationType.SearchProvider, IntegrationName.SearchProviderAlgolia)

    val pendingMutactions: List[Mutaction] = integration match {
      case Some(searchProvider) =>
        val existingSearchProviderAlgolia =
          searchProvider.asInstanceOf[models.SearchProviderAlgolia]
        var mutactions: List[SystemSqlMutaction] = List()

        searchProviderAlgolia = mergeInputValuesToSearchProviderAlgolia(existingSearchProviderAlgolia, args)

        mutactions :+= UpdateSearchProviderAlgolia(existingSearchProviderAlgolia, searchProviderAlgolia.get)

        if (existingSearchProviderAlgolia.isEnabled != args.isEnabled) {
          mutactions :+= UpdateIntegration(project, existingSearchProviderAlgolia, searchProviderAlgolia.get)
        }

        mutactions

      case None =>
        searchProviderAlgolia = generateNewSearchProviderAlgolia()

        // Need to add both separately, as on DB level these are two tables
        List(addIntegrationToProject(searchProviderAlgolia.get), addSearchProviderAlgoliaToProject(searchProviderAlgolia.get))
    }

    actions = pendingMutactions :+ BumpProjectRevision(project = project) :+ InvalidateSchema(project = project)
    actions
  }

  override def getReturnValue: Option[UpdateSearchProviderAlgoliaPayload] = {
    Some(
      UpdateSearchProviderAlgoliaPayload(
        clientMutationId = args.clientMutationId,
        project = project.copy(integrations =
          project.authProviders.filter(_.id != searchProviderAlgolia.get.id) :+ searchProviderAlgolia.get),
        searchProviderAlgolia = searchProviderAlgolia.get
      ))
  }

  private def mergeInputValuesToSearchProviderAlgolia(existingAlgoliaSearchProvider: models.SearchProviderAlgolia,
                                                      updateValues: UpdateSearchProviderAlgoliaInput): Option[models.SearchProviderAlgolia] = {
    Some(
      existingAlgoliaSearchProvider.copy(
        applicationId = updateValues.applicationId,
        apiKey = updateValues.apiKey,
        isEnabled = updateValues.isEnabled
      )
    )
  }

  private def generateNewSearchProviderAlgolia(): Option[models.SearchProviderAlgolia] = {
    Some(
      models.SearchProviderAlgolia(
        id = Cuid.createCuid(),
        subTableId = Cuid.createCuid(),
        applicationId = args.applicationId,
        apiKey = args.apiKey,
        algoliaSyncQueries = List(),
        isEnabled = true,
        name = IntegrationName.SearchProviderAlgolia
      )
    )
  }

  private def addSearchProviderAlgoliaToProject(searchProviderAlgolia: models.SearchProviderAlgolia): Mutaction = {
    CreateSearchProviderAlgolia(
      project = project,
      searchProviderAlgolia = searchProviderAlgolia
    )
  }

  private def addIntegrationToProject(integration: models.Integration): Mutaction = {
    CreateIntegration(
      project = project,
      integration = integration
    )
  }
}

case class UpdateSearchProviderAlgoliaPayload(clientMutationId: Option[String], project: models.Project, searchProviderAlgolia: models.SearchProviderAlgolia)
    extends Mutation

case class UpdateSearchProviderAlgoliaInput(
    clientMutationId: Option[String],
    projectId: String,
    applicationId: String,
    apiKey: String,
    isEnabled: Boolean
)
