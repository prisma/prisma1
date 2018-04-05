package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector.{CoolArgs, DataResolver, UpdateDataItems}
import com.prisma.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.Future

case class UpdateMany(
    project: Project,
    model: Model,
    args: schema.Args,
    where: DataItemFilterCollection,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation[BatchPayload] {

  import apiDependencies.system.dispatcher

  //todo this does not support scalar lists at the moment

  val count              = dataResolver.countByModel(model, Some(where))
  val coolArgs: CoolArgs = CoolArgs.fromSchemaArgs(args.raw)

  def prepareMutactions(): Future[PreparedMutactions] = {
    for {
      _ <- count // make sure that count query has been resolved before proceeding
    } yield {
      val updateItems = UpdateDataItems(project, model, coolArgs, where)
      PreparedMutactions(
        databaseMutactions = Vector(updateItems),
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
