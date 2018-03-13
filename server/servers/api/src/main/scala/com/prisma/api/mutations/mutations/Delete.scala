package com.prisma.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.{NodeSelector, Path}
import com.prisma.api.database.mutactions.mutactions.ServerSideSubscription
import com.prisma.api.database.{DataItem, DataResolver}
import com.prisma.api.mutations._
import com.prisma.api.schema.{APIErrors, ObjectTypeBuilder}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

case class Delete(
    model: Model,
    modelObjectTypes: ObjectTypeBuilder,
    project: Project,
    args: schema.Args,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {

  implicit val system: ActorSystem             = apiDependencies.system
  implicit val materializer: ActorMaterializer = apiDependencies.materializer

  var deletedItemOpt: Option[DataItem] = None
  val requestId: Id                    = "" // dataResolver.requestContext.map(_.requestId).getOrElse("")

  val coolArgs            = CoolArgs(args.raw)
  val where: NodeSelector = coolArgs.extractNodeSelectorFromWhereField(model)

  override def prepareMutactions(): Future[PreparedMutactions] = {
    dataResolver
      .resolveByUnique(where)
      .andThen {
        case Success(x) => deletedItemOpt = x.map(dataItem => dataItem) // todo: replace with GC Values
        // todo: do we need the fromSql stuff?
        //GraphcoolDataTypes.fromSql(dataItem.userData, model.fields)
      }
      .map { _ =>
        val itemToDelete           = deletedItemOpt.getOrElse(throw APIErrors.NodeNotFoundForWhereError(where))
        val sqlMutactions          = SqlMutactions(dataResolver).getMutactionsForDelete(Path.empty(where), itemToDelete, itemToDelete.id).toList
        val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions).toList
        val sssActions             = ServerSideSubscription.extractFromMutactions(project, sqlMutactions, requestId).toList

        PreparedMutactions(
          databaseMutactions = sqlMutactions.toVector,
          sideEffectMutactions = (subscriptionMutactions ++ sssActions).toVector
        )
      }
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    val dataItem = deletedItemOpt.get
    Future.successful(ReturnValue(dataItem))
  }
}
