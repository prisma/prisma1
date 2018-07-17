package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.mutactions.{DatabaseMutactions, NodeIds, ServerSideSubscriptions, SubscriptionEvents}
import com.prisma.shared.models.{Model, Project}
import com.prisma.util.coolArgs.CoolArgs
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
//  val updatedWhere: NodeSelector = updateArgs.raw.get(outerWhere.field.name) match {
//    case Some(_) => updateArgs.extractNodeSelector(model)
//    case None    => outerWhere
//  }

//  val updatePath = Path.empty(outerWhere)
//  val createPath = Path.empty(NodeSelector.forIdGCValue(model, NodeIds.createNodeIdForModel(model)))

  override def prepareMutactions: Future[TopLevelDatabaseMutaction] = {
//    val sqlMutactions          = DatabaseMutactions(project).getMutactionsForUpsert(createPath, updatePath, coolArgs)
//    val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions)
//    val sssActions             = ServerSideSubscriptions.extractFromMutactions(project, sqlMutactions, requestId = "")
//
//    Future.successful {
//      PreparedMutactions(
//        databaseMutactions = sqlMutactions,
//        sideEffectMutactions = sssActions ++ subscriptionMutactions
//      )
//    }

    Future.successful(DatabaseMutactions(project).getMutactionsForUpsert(outerWhere, coolArgs))
  }

  override def getReturnValue(results: MutactionResults): Future[ReturnValueResult] = {
    val firstResult = results.allResults.collectFirst { case m: FurtherNestedMutactionResult => m }.get
    val selector    = NodeSelector.forIdGCValue(model, firstResult.id)
    val itemFuture  = dataResolver.getNodeByWhere(selector)
    itemFuture.map {
      case Some(prismaNode) => ReturnValue(prismaNode)
      case None             => sys.error("Could not find an item after an Upsert. This should not be possible.")
    }
  }
}
