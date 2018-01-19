package com.prisma.api.mutations.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.database.DataResolver
import com.prisma.api.database.mutactions.mutactions.UpsertDataItem
import com.prisma.api.database.mutactions.{MutactionGroup, TransactionMutaction}
import com.prisma.api.mutations._
import cool.graph.cuid.Cuid
import com.prisma.shared.models.{Model, Project}
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

  val outerwhere = CoolArgs(args.raw).extractNodeSelectorFromWhereField(model)

  val idOfNewItem = Cuid.createCuid()
  val createArgs  = CoolArgs(args.raw).createArgumentsAsCoolArgs.generateCreateArgs(model, idOfNewItem)
  val updateArgs  = CoolArgs(args.raw).updateArgumentsAsCoolArgs.generateUpdateArgs(model)
  val createWhere = NodeSelector.forId(model, idOfNewItem)

  val upsert = UpsertDataItem(project, outerwhere, createArgs, updateArgs)

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
    val transaction = TransactionMutaction(List(upsert), dataResolver)
    Future.successful(List(MutactionGroup(List(transaction), async = false)))
  }

//  override def prepareMutactions(): Future[List[MutactionGroup]] = {
//
//    val sqlMutactions        = SqlMutactions(dataResolver).getMutactionsForUpsert(outerwhere, createWhere, CoolArgs(args.raw), createArgs, updateArgs)
//    val transactionMutaction = TransactionMutaction(sqlMutactions, dataResolver)
////    val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions).toList
////    val sssActions             = ServerSideSubscription.extractFromMutactions(project, sqlMutactions, requestId = "").toList
//
//    Future(
//      List(
//        MutactionGroup(mutactions = List(transactionMutaction), async = false)
////    ,  MutactionGroup(mutactions = sssActions ++ subscriptionMutactions, async = true)
//      ))
////    val transaction = TransactionMutaction(List(upsert), dataResolver)
////    Future.successful(List(MutactionGroup(List(transaction), async = false)))
//  }

  override def getReturnValue: Future[ReturnValueResult] = {
    val newWhere = updateArgs.raw.get(outerwhere.field.name) match {
      case Some(_) => updateArgs.extractNodeSelector(model)
      case None    => outerwhere
    }

    val uniques = Vector(NodeSelector.forId(model, idOfNewItem), newWhere)
    dataResolver.resolveByUniques(model, uniques).map { items =>
      items.headOption match {
        case Some(item) => ReturnValue(item)
        case None       => sys.error("Could not find an item after an Upsert. This should not be possible.") // Todo: what should we do here?
      }
    }
  }
}
