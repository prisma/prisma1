package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.mutactions.DatabaseMutactions
import com.prisma.shared.models.{Model, Project}
import com.prisma.util.coolArgs.CoolArgs
import sangria.schema

import scala.concurrent.Future

case class Upsert(
    model: Model,
    project: Project,
    args: schema.Args,
    selectedFields: SelectedFields,
    dataResolver: DataResolver,
    allowSettingManagedFields: Boolean = false
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {

  import apiDependencies.system.dispatcher

  val coolArgs = CoolArgs(args.raw)

  val outerWhere: NodeSelector = coolArgs.extractNodeSelectorFromWhereField(model)

  val updateArgs: CoolArgs = coolArgs.updateArgumentsAsCoolArgs
  val upsertMutaction      = DatabaseMutactions(project).getMutactionsForUpsert(outerWhere, coolArgs)

  override def prepareMutactions: Future[TopLevelDatabaseMutaction] = Future.successful(upsertMutaction)

  override def getReturnValue(results: MutactionResults): Future[ReturnValueResult] = {
    val firstResult = results.results.collectFirst {
      case r: FurtherNestedMutactionResult if r.mutaction.id == upsertMutaction.create.id || r.mutaction.id == upsertMutaction.update.id => r
    }.get
    val selector   = NodeSelector.forId(model, firstResult.id)
    val itemFuture = dataResolver.getNodeByWhere(selector, selectedFields)
    itemFuture.map {
      case Some(prismaNode) => ReturnValue(prismaNode)
      case None             => sys.error("Could not find an item after an Upsert. This should not be possible.")
    }
  }
}
