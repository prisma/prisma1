package cool.graph.system.mutations

import com.typesafe.config.Config
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors.RequiredSearchProviderAlgoliaNotPresent
import cool.graph.shared.models
import cool.graph.shared.models.{IntegrationName, IntegrationType}
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.mutactions.client.SyncModelToAlgoliaViaRequest
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateAlgoliaSyncQuery, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global

case class AddAlgoliaSyncQueryMutation(client: models.Client,
                                       project: models.Project,
                                       args: AddAlgoliaSyncQueryInput,
                                       projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[AddAlgoliaSyncQueryPayload]
    with Injectable {

  var newAlgoliaSyncQuery: Option[models.AlgoliaSyncQuery]        = None
  var searchProviderAlgolia: Option[models.SearchProviderAlgolia] = None
  val config                                                      = inject[Config]("config")

  override def prepareActions(): List[Mutaction] = {
    val integration = project.getIntegrationByTypeAndName(IntegrationType.SearchProvider, IntegrationName.SearchProviderAlgolia)

    val pendingMutactions: List[Mutaction] = integration match {
      case Some(searchProvider) =>
        val existingSearchProviderAlgolia = searchProvider.asInstanceOf[models.SearchProviderAlgolia]
        val model                         = project.getModelById_!(args.modelId)
        searchProviderAlgolia = Some(existingSearchProviderAlgolia)
        newAlgoliaSyncQuery = Some(
          models.AlgoliaSyncQuery(
            id = Cuid.createCuid(),
            indexName = args.indexName,
            fragment = args.fragment,
            isEnabled = true,
            model = model
          )
        )

        val addAlgoliaSyncQueryToProject =
          CreateAlgoliaSyncQuery(
            searchProviderAlgolia = searchProviderAlgolia.get,
            algoliaSyncQuery = newAlgoliaSyncQuery.get
          )

        val syncModelToAlgolia = SyncModelToAlgoliaViaRequest(project = project, model = model, algoliaSyncQuery = newAlgoliaSyncQuery.get, config = config)
        val bumpRevision       = BumpProjectRevision(project = project)

        List(addAlgoliaSyncQueryToProject, syncModelToAlgolia, bumpRevision, InvalidateSchema(project = project))

      case None =>
        List(InvalidInput(RequiredSearchProviderAlgoliaNotPresent()))
    }
    actions = pendingMutactions
    actions
  }

  override def getReturnValue(): Option[AddAlgoliaSyncQueryPayload] = {
    val updatedSearchProviderAlgolia = searchProviderAlgolia.get.copy(
      algoliaSyncQueries =
        searchProviderAlgolia.get.algoliaSyncQueries
          .filter(_.id != newAlgoliaSyncQuery.get.id) :+ newAlgoliaSyncQuery.get)
    val updatedProject = project.copy(
      integrations =
        project.authProviders
          .filter(_.id != searchProviderAlgolia.get.id) :+ updatedSearchProviderAlgolia)

    Some(
      AddAlgoliaSyncQueryPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        model = project.getModelById_!(args.modelId),
        algoliaSyncQuery = newAlgoliaSyncQuery.get,
        searchProviderAlgolia = searchProviderAlgolia.get.copy(algoliaSyncQueries = searchProviderAlgolia.get.algoliaSyncQueries :+ newAlgoliaSyncQuery.get)
      ))
  }
}

case class AddAlgoliaSyncQueryPayload(clientMutationId: Option[String],
                                      project: models.Project,
                                      model: models.Model,
                                      algoliaSyncQuery: models.AlgoliaSyncQuery,
                                      searchProviderAlgolia: models.SearchProviderAlgolia)
    extends Mutation

case class AddAlgoliaSyncQueryInput(clientMutationId: Option[String], modelId: String, indexName: String, fragment: String)
