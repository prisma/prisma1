package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.{DataResolver, Filter}
import com.prisma.api.mutactions.DatabaseMutactions
import com.prisma.shared.models.{Model, Project}
import com.prisma.util.coolArgs.CoolArgs
import sangria.schema

import scala.concurrent.Future

case class UpdateMany(
    project: Project,
    model: Model,
    args: schema.Args,
    whereFilter: Option[Filter],
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation[BatchPayload] {

  import apiDependencies.system.dispatcher

  val coolArgs   = CoolArgs.fromSchemaArgs(args.raw)
  lazy val count = dataResolver.countByModel(model, whereFilter)

  def prepareMutactions(): Future[PreparedMutactions] = {
    count map { _ =>
      val sqlMutactions          = DatabaseMutactions(project).getMutactionsForUpdateMany(model, whereFilter, coolArgs)
      val subscriptionMutactions = Vector.empty
      val sssActions             = Vector.empty

      PreparedMutactions(
        databaseMutactions = sqlMutactions,
        sideEffectMutactions = sssActions ++ subscriptionMutactions
      )
    }
  }

  override def getReturnValue(results: MutactionResults): Future[BatchPayload] = count.map(value => BatchPayload(count = value))

}
