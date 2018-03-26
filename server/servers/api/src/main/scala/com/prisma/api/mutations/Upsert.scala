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

  val outerWhere: NodeSelector = CoolArgs(args.raw).extractNodeSelectorFromWhereField(model)

  val idOfNewItem: String       = Cuid.createCuid()
  val createWhere: NodeSelector = NodeSelector.forId(model, idOfNewItem)
  val updateArgs: CoolArgs      = CoolArgs(args.raw).updateArgumentsAsCoolArgs.generateNonListUpdateArgs(model)
  val updatedWhere: NodeSelector = updateArgs.raw.get(outerWhere.field.name) match {
    case Some(_) => updateArgs.extractNodeSelector(model)
    case None    => outerWhere
  }

  val path = Path.empty(outerWhere)

  override def prepareMutactions(): Future[PreparedMutactions] = {
    val sqlMutactions          = DatabaseMutactions(project).getMutactionsForUpsert(path, createWhere, updatedWhere, CoolArgs(args.raw))
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

//    val uniques = Vector(createWhere, updatedWhere)
//    dataResolver.resolveByUniques(model, uniques).map { items =>
//      items.headOption match {
//        case Some(item) => ReturnValue(item)
//        case None       => sys.error("Could not find an item after an Upsert. This should not be possible.") // Todo: what should we do here?
//      }
//    }

    val createItemFuture = dataResolver.resolveByUnique(createWhere)
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
