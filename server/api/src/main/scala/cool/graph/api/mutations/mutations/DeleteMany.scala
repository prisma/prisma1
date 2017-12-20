package cool.graph.api.mutations.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.api.database.mutactions.mutactions.DeleteDataItems
import cool.graph.api.database.mutactions.{MutactionGroup, Transaction}
import cool.graph.api.mutations._
import cool.graph.shared.models.{Model, Project}

import scala.concurrent.Future

case class DeleteMany(
    project: Project,
    model: Model,
    where: DataItemFilterCollection,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation[BatchPayload] {

  def prepareMutactions(): Future[List[MutactionGroup]] = Future.successful {
    val deleteItems          = DeleteDataItems(project, model, where)
    val transactionMutaction = Transaction(List(deleteItems), dataResolver)
    List(
      MutactionGroup(mutactions = List(transactionMutaction), async = false)
    )
  }

  override def getReturnValue: Future[BatchPayload] = Future.successful {
    BatchPayload(count = 1)
  }

}
