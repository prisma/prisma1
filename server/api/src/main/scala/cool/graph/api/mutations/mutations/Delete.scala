package cool.graph.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.mutactions.mutactions.ServerSideSubscription
import cool.graph.api.database.mutactions.{MutactionGroup, Transaction}
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.mutations._
import cool.graph.api.mutations.definitions.DeleteDefinition
import cool.graph.api.schema.ObjectTypeBuilder
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Model, Project}
import sangria.schema
import scaldi.{Injectable, Injector}

import scala.concurrent.Future
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

class Delete[ManyDataItemType](model: Model, modelObjectTypes: ObjectTypeBuilder, project: Project, args: schema.Args, dataResolver: DataResolver)(
    implicit apiDependencies: ApiDependencies)
    extends ClientMutation(model, args, dataResolver) {

  override val mutationDefinition = DeleteDefinition(project)

  implicit val system: ActorSystem             = apiDependencies.system
  implicit val materializer: ActorMaterializer = apiDependencies.materializer

  val id: Id = extractIdFromScalarArgumentValues_!(args, "id")

  var deletedItem: Option[DataItem] = None
  val requestId: Id                 = "" // dataResolver.requestContext.map(_.requestId).getOrElse("")

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
    dataResolver
      .resolveByModelAndIdWithoutValidation(model, id)
      .andThen {
        case Success(x) => deletedItem = x.map(dataItem => dataItem) // todo: replace with GC Values
        //GraphcoolDataTypes.fromSql(dataItem.userData, model.fields)

      }
      .map(_ => {

        val sqlMutactions        = SqlMutactions(dataResolver).getMutactionsForDelete(model, project, id, deletedItem.getOrElse(DataItem(id)))
        val transactionMutaction = Transaction(sqlMutactions, dataResolver)

        val nodeData: Map[String, Any] = deletedItem
          .map(_.userData)
          .getOrElse(Map.empty[String, Option[Any]])
          .collect {
            case (key, Some(value)) => (key, value)
          } + ("id" -> id)

        val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions).toList

        val sssActions = ServerSideSubscription.extractFromMutactions(project, sqlMutactions, requestId).toList

        List(
          MutactionGroup(mutactions = List(transactionMutaction), async = false),
          MutactionGroup(mutactions = sssActions ++ subscriptionMutactions, async = true)
        )
      })
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    val dataItem = deletedItem.get
    Future.successful(ReturnValue(dataItem))
  }
}
