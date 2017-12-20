package cool.graph.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.mutactions.mutactions.{ServerSideSubscription, UpdateDataItem}
import cool.graph.api.database.mutactions.{ClientSqlMutaction, MutactionGroup, Transaction}
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.mutations._
import cool.graph.api.schema.APIErrors
import cool.graph.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UpdateItems(
    model: Model,
    project: Project,
    args: schema.Args,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation[BatchPayload] {

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

  def prepareMutactions(): Future[List[MutactionGroup]] = Future.successful {
//    val transactionMutaction = Transaction(sqlMutactions, dataResolver)
//    List(
//      MutactionGroup(mutactions = List(transactionMutaction), async = false)
//    )
    List.empty
  }

  override def getReturnValue: Future[BatchPayload] = Future.successful {
    BatchPayload(count = 1)
  }

}
