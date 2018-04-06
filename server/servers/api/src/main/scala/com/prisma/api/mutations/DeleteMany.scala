package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.{DataResolver, DeleteDataItems, DeleteManyRelationChecks}
import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.mutactions.DatabaseMutactions
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

  lazy val count = dataResolver.countByModel(model, Some(whereFilter))

  def prepareMutactions(): Future[PreparedMutactions] = {
    count map { _ =>
      val sqlMutactions          = DatabaseMutactions(project).getMutactionsForDeleteMany(model, whereFilter)
      val subscriptionMutactions = Vector.empty
      val sssActions             = Vector.empty

      PreparedMutactions(
        databaseMutactions = sqlMutactions,
        sideEffectMutactions = sssActions ++ subscriptionMutactions
      )
    }
  }

  override def getReturnValue: Future[BatchPayload] = count.map(value => BatchPayload(count = value))

}
