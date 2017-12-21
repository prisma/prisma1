package cool.graph.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.api.database.mutactions.mutactions.{DeleteDataItems, UpdateDataItems}
import cool.graph.api.database.mutactions.{MutactionGroup, Transaction}
import cool.graph.api.mutations._
import cool.graph.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.Future

case class UpdateMany(
    project: Project,
    model: Model,
    args: schema.Args,
    where: DataItemFilterCollection,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation[BatchPayload] {

  import apiDependencies.system.dispatcher

  val count = dataResolver.countByModel(model, where)
  val coolArgs: CoolArgs = {
    val argsPointer: Map[String, Any] = args.raw.get("data") match {
      case Some(value) => value.asInstanceOf[Map[String, Any]]
      case None        => args.raw
    }
    CoolArgs(argsPointer)
  }

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    for {
      _ <- count // make sure that count query has been resolved before proceeding
    } yield {
      val updateItems          = UpdateDataItems(project, model, coolArgs, where)
      val transactionMutaction = Transaction(List(updateItems), dataResolver)
      List(
        MutactionGroup(mutactions = List(transactionMutaction), async = false)
      )
    }
  }

  override def getReturnValue: Future[BatchPayload] = {
    for {
      count <- count
    } yield {
      BatchPayload(count = count)
    }
  }

}
