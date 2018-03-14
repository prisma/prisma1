package com.prisma.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.NodeSelector
import com.prisma.api.database.DataResolver
import com.prisma.api.mutactions.{DatabaseMutactions, ServerSideSubscriptions, SubscriptionEvents}
import com.prisma.api.mutations._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models._
import cool.graph.cuid.Cuid
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

  def prepareMutactions(): Future[PreparedMutactions] = {
    val createMutactionsResult = DatabaseMutactions(project).getMutactionsForCreate(model, coolArgs, id).toVector
    val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, createMutactionsResult)
    val sssActions             = ServerSideSubscriptions.extractFromMutactions(project, createMutactionsResult, requestId)

    Future.successful {
      PreparedMutactions(
        databaseMutactions = createMutactionsResult,
        sideEffectMutactions = sssActions ++ subscriptionMutactions
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
