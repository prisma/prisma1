package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
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
  lazy val count = dataResolver.countByModel(model, Some(QueryArguments.withFilter(whereFilter)))

  def prepareMutactions(): Future[TopLevelDatabaseMutaction] = {
    count map { _ =>
//      val sqlMutactions          = DatabaseMutactions(project).getMutactionsForUpdateMany(model, whereFilter, coolArgs)
//      val subscriptionMutactions = Vector.empty
//      val sssActions             = Vector.empty
//
//      PreparedMutactions(
//        databaseMutactions = sqlMutactions,
//        sideEffectMutactions = sssActions ++ subscriptionMutactions
//      )

      DatabaseMutactions(project).getMutactionsForUpdateMany(model, whereFilter, coolArgs)
    }
  }

  override def getReturnValue(results: MutactionResults): Future[BatchPayload] = count.map(value => BatchPayload(count = value))

}
