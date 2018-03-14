package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.{DeleteDataItems, DeleteManyRelationChecks}
import com.prisma.api.connector.mysql.database.DataResolver
import com.prisma.api.connector.mysql.database.Types.DataItemFilterCollection
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

  def prepareMutactions(): Future[PreparedMutactions] = {
    for {
      _ <- count // make sure that count query has been resolved before proceeding
    } yield {
      val requiredRelationChecks = DeleteManyRelationChecks(project, model, whereFilter)
      val deleteItems            = DeleteDataItems(project, model, whereFilter)
      PreparedMutactions(
        databaseMutactions = Vector(requiredRelationChecks, deleteItems),
        sideEffectMutactions = Vector.empty
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
