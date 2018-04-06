package com.prisma.api.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.mutactions.{DatabaseMutactions, ServerSideSubscriptions, SubscriptionEvents}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Update(
    model: Model,
    project: Project,
    args: schema.Args,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {

  implicit val system: ActorSystem             = apiDependencies.system
  implicit val materializer: ActorMaterializer = apiDependencies.materializer

  val coolArgs: CoolArgs = {
    val argsPointer: Map[String, Any] = args.raw.get("data") match {
      case Some(value) => value.asInstanceOf[Map[String, Any]]
      case None        => args.raw
    }
    CoolArgs(argsPointer)
  }

  val where = CoolArgs(args.raw).extractNodeSelectorFromWhereField(model)

  lazy val prismaNodes: Future[Option[PrismaNode]] = dataResolver.resolveByUnique(where)

  def prepareMutactions(): Future[PreparedMutactions] = {
    prismaNodes map {
      case Some(prismaNode) =>
        val sqlMutactions          = DatabaseMutactions(project).getMutactionsForUpdate(Path.empty(where), coolArgs, prismaNode.id, prismaNode)
        val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions)
        val sssActions             = ServerSideSubscriptions.extractFromMutactions(project, sqlMutactions, requestId = "")

        PreparedMutactions(
          databaseMutactions = sqlMutactions,
          sideEffectMutactions = sssActions ++ subscriptionMutactions
        )
      case None =>
        throw APIErrors.NodeNotFoundForWhereError(where)
    }
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    prismaNodes flatMap {
      case Some(prismaNode) => returnValueByUnique(NodeSelector.forIdGCValue(model, prismaNode.id))
      case None             => Future.successful(NoReturnValue(where))
    }
  }

}
