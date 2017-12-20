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

  import apiDependencies.system.dispatcher

  val count = dataResolver.countByModel(model, where)

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    for {
      _ <- count // make sure that count query has been resolved before proceeding
    } yield {
      val deleteItems          = DeleteDataItems(project, model, where)
      val transactionMutaction = Transaction(List(deleteItems), dataResolver)
      List(
        MutactionGroup(mutactions = List(transactionMutaction), async = false)
      )
    }
  }

  override def getReturnValue: Future[BatchPayload] = {
    for {
      count <- count
    } yield {
      BatchPayload(count = count)
    }
  }

}
