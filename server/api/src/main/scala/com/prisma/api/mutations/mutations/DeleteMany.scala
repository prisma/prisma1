package com.prisma.api.mutations.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.database.DataResolver
import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.database.mutactions.mutactions.DeleteDataItems
import com.prisma.api.database.mutactions.{MutactionGroup, TransactionMutaction}
import com.prisma.api.mutations._
import com.prisma.shared.models.{Model, Project}

import scala.concurrent.Future

case class DeleteMany(
    project: Project,
    model: Model,
    whereFilter: DataItemFilterCollection,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation[BatchPayload] {

  import apiDependencies.system.dispatcher

  val count = dataResolver.countByModel(model, whereFilter)

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    for {
      _ <- count // make sure that count query has been resolved before proceeding
    } yield {
      val deleteItems          = DeleteDataItems(project, model, whereFilter)
      val transactionMutaction = TransactionMutaction(List(deleteItems), dataResolver)
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
