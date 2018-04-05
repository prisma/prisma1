package com.prisma.api.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.{CoolArgs, DataResolver, NodeSelector, Path}
import com.prisma.api.mutactions.{DatabaseMutactions, ServerSideSubscriptions, SubscriptionEvents}
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

  val coolArgs: CoolArgs = CoolArgs.fromSchemaArgs(args.raw)

  val path = Path.empty(NodeSelector.forId(model, id))

  def prepareMutactions(): Future[PreparedMutactions] = {
    val createMutactionsResult = DatabaseMutactions(project).getMutactionsForCreate(path, coolArgs)
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
      prismaNode  = returnValue.asInstanceOf[ReturnValue].prismaNode
    } yield {
      ReturnValue(prismaNode)
    }
  }
}
