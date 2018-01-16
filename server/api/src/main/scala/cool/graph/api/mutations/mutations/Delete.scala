package cool.graph.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.mutactions.mutactions.ServerSideSubscription
import cool.graph.api.database.mutactions.{MutactionGroup, TransactionMutaction}
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.mutations._
import cool.graph.api.schema.{APIErrors, ObjectTypeBuilder}
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Model, Project}
import cool.graph.util.gc_value.GCStringConverter
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

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
    dataResolver
      .resolveByUnique(where)
      .andThen {
        case Success(x) => deletedItemOpt = x.map(dataItem => dataItem) // todo: replace with GC Values
        // todo: do we need the fromSql stuff?
        //GraphcoolDataTypes.fromSql(dataItem.userData, model.fields)
      }
      .map(_ => {
        val itemToDelete = deletedItemOpt.getOrElse(throw APIErrors.NodeNotFoundForWhereError(where))

        val sqlMutactions          = SqlMutactions(dataResolver).getMutactionsForDelete(model, itemToDelete.id, itemToDelete, where)
        val transactionMutaction   = TransactionMutaction(sqlMutactions, dataResolver)
        val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions).toList
        val sssActions             = ServerSideSubscription.extractFromMutactions(project, sqlMutactions, requestId).toList

        List(
          MutactionGroup(mutactions = List(transactionMutaction), async = false),
          MutactionGroup(mutactions = sssActions ++ subscriptionMutactions, async = true)
        )
      })
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    val dataItem = deletedItemOpt.get
    Future.successful(ReturnValue(dataItem))
  }
}
