package com.prisma.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.api.database.DataResolver
import com.prisma.api.database.mutactions.mutactions.{CreateDataItem, ServerSideSubscription}
import com.prisma.api.database.mutactions.{MutactionGroup, TransactionMutaction}
import com.prisma.api.mutations._
import cool.graph.cuid.Cuid
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models._
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Create(
    model: Model,
    project: Project,
    args: schema.Args,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {

  implicit val system: ActorSystem             = apiDependencies.system
  implicit val materializer: ActorMaterializer = apiDependencies.materializer

  val id: Id            = Cuid.createCuid()
  val requestId: String = "" //                        = dataResolver.requestContext.map(_.requestId).getOrElse("")

  val coolArgs: CoolArgs = {
    val argsPointer: Map[String, Any] = args.raw.get("data") match {
      case Some(value) => value.asInstanceOf[Map[String, Any]]
      case None        => args.raw
    }

    CoolArgs(argsPointer)
  }

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    val createMutactionsResult = SqlMutactions(dataResolver).getMutactionsForCreate(model, coolArgs, id)
    val transactionMutaction   = TransactionMutaction(createMutactionsResult.allMutactions.toList, dataResolver)
    val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, createMutactionsResult.allMutactions)
    val sssActions             = ServerSideSubscription.extractFromMutactions(project, createMutactionsResult.allMutactions, requestId)

    Future.successful {
      List(
        MutactionGroup(mutactions = List(transactionMutaction), async = false),
        MutactionGroup(mutactions = sssActions.toList ++ subscriptionMutactions.toList, async = true)
      )
    }

  }

  override def getReturnValue: Future[ReturnValueResult] = {
    for {
      returnValue <- returnValueByUnique(NodeSelector.forId(model, id))
      dataItem    = returnValue.asInstanceOf[ReturnValue].dataItem
    } yield {
      ReturnValue(dataItem)
    }
  }
}
