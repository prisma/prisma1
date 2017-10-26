package cool.graph.private_api.mutations

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions.SyncModelToAlgolia
import cool.graph.shared.models.Project
import sangria.relay.Mutation
import scaldi.Injector

import scala.concurrent.Future

case class SyncModelToAlgoliaMutation(project: Project, input: SyncModelToAlgoliaInput, dataResolver: DataResolver)(implicit inj: Injector)
    extends PrivateMutation[SyncModelToAlgoliaPayload] {

  val model = project.getModelById_!(input.modelId)

  val searchProvider = project.getSearchProviderAlgoliaByAlgoliaSyncQueryId_!(input.syncQueryId)
  val syncQuery      = project.getAlgoliaSyncQueryById_!(input.syncQueryId)

  override def prepare(): Future[List[Mutaction]] = {
    Future.successful {
      List(
        SyncModelToAlgolia(
          model = model,
          project = project,
          syncQuery = syncQuery,
          searchProviderAlgolia = searchProvider,
          requestId = dataResolver.requestContext.map(_.requestId).getOrElse("")
        )
      )
    }
  }

  override val result = SyncModelToAlgoliaPayload(input.clientMutationId)
}

case class SyncModelToAlgoliaInput(
    clientMutationId: Option[String],
    modelId: String,
    syncQueryId: String
)

case class SyncModelToAlgoliaPayload(clientMutationId: Option[String]) extends Mutation
