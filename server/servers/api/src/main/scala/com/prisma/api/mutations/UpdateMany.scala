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

  val coolArgs = CoolArgs.fromSchemaArgs(args.raw)

  def prepareMutactions(): Future[TopLevelDatabaseMutaction] =
    Future.successful(DatabaseMutactions(project).getMutactionsForUpdateMany(model, whereFilter, coolArgs))

  override def getReturnValue(results: MutactionResults): Future[BatchPayload] = Future.successful(BatchPayload(count = ManyHelper.getManyCount(results)))
}
