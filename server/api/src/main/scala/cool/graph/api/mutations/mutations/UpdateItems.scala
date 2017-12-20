package cool.graph.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.api.database.mutactions.mutactions.UpdateDataItems
import cool.graph.api.database.mutactions.{MutactionGroup, Transaction}
import cool.graph.api.mutations._
import cool.graph.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.Future

case class UpdateItems(
    project: Project,
    model: Model,
    args: schema.Args,
    where: DataItemFilterCollection,
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

  def prepareMutactions(): Future[List[MutactionGroup]] = Future.successful {
    val updateItems          = UpdateDataItems(project, model, coolArgs, where)
    val transactionMutaction = Transaction(List(updateItems), dataResolver)
    List(
      MutactionGroup(mutactions = List(transactionMutaction), async = false)
    )
  }

  override def getReturnValue: Future[BatchPayload] = Future.successful {
    BatchPayload(count = 1)
  }

}
