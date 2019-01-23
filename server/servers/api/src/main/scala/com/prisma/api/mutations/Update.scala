package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.mutactions.DatabaseMutactions
import com.prisma.shared.models.{Model, Project}
import com.prisma.util.coolArgs.CoolArgs
import sangria.schema

import scala.concurrent.Future

case class Update(
    model: Model,
    project: Project,
    args: schema.Args,
    selectedFields: SelectedFields,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {
  val coolArgs = CoolArgs.fromSchemaArgs(args.raw)
  val where    = CoolArgs(args.raw).extractNodeSelectorFromWhereField(model)

  lazy val updateMutaction = DatabaseMutactions(project).getMutactionsForUpdate(model, where, coolArgs)

  def prepareMutactions(): Future[TopLevelDatabaseMutaction] = Future.successful { updateMutaction }

  override def getReturnValue(results: MutactionResults): Future[ReturnValueResult] = {
    val updateResult = results.results.collectFirst { case r: UpdateNodeResult if r.mutaction.id == updateMutaction.id => r }.head
    returnValueByUnique(NodeSelector.forId(model, updateResult.id), selectedFields)
  }
}
