package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.{CoolArgs, DataResolver, NodeSelector, Path}
import com.prisma.api.mutactions.{DatabaseMutactions, ServerSideSubscriptions, SubscriptionEvents}
import com.prisma.shared.models.{Model, Project}
import cool.graph.cuid.Cuid
import sangria.schema

import scala.concurrent.Future

case class Upsert(
    model: Model,
    project: Project,
    args: schema.Args,
    dataResolver: DataResolver,
    allowSettingManagedFields: Boolean = false
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {

  import apiDependencies.system.dispatcher

  val coolArgs = CoolArgs(args.raw)

  val outerWhere: NodeSelector = coolArgs.extractNodeSelectorFromWhereField(model)

  val updateArgs: CoolArgs = coolArgs.updateArgumentsAsCoolArgs
  val updatedWhere: NodeSelector = updateArgs.raw.get(outerWhere.field.name) match {
    case Some(_) => updateArgs.extractNodeSelector(model)
    case None    => outerWhere
  }

  val updatePath = Path.empty(outerWhere)
  val createPath = Path.empty(NodeSelector.forId(model, Cuid.createCuid()))

  override def prepareMutactions(): Future[PreparedMutactions] = {
    val sqlMutactions          = DatabaseMutactions(project).getMutactionsForUpsert(createPath, updatePath, coolArgs)
    val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions)
    val sssActions             = ServerSideSubscriptions.extractFromMutactions(project, sqlMutactions, requestId = "")

    Future.successful {
      PreparedMutactions(
        databaseMutactions = sqlMutactions,
        sideEffectMutactions = sssActions ++ subscriptionMutactions
      )
    }
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    val createItemFuture = dataResolver.resolveByUnique(createPath.lastCreateWhere_!)
    val upsertItemFuture = dataResolver.resolveByUnique(updatedWhere)

    for {
      createItem <- createItemFuture
      updateItem <- upsertItemFuture
    } yield {
      (createItem, updateItem) match {
        case (Some(create), _) => ReturnValue(create)
        case (_, Some(update)) => ReturnValue(update)
        case (None, None)      => sys.error("Could not find an item after an Upsert. This should not be possible.")
      }
    }
  }
}
