package cool.graph.api.mutations

import cool.graph.api.database.mutactions._
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.cuid.Cuid
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.Model

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ClientMutation {
  val mutationId: Id = Cuid.createCuid()

  def dataResolver: DataResolver

  def prepareMutactions(): Future[List[MutactionGroup]]

  def getReturnValue: Future[ReturnValueResult]

  def returnValueById(model: Model, id: Id): Future[ReturnValueResult] = {
    dataResolver.resolveByModelAndId(model, id).map {
      case Some(dataItem) => ReturnValue(dataItem)
      case None           => NoReturnValue(id)
    }
  }
}

sealed trait ReturnValueResult
case class ReturnValue(dataItem: DataItem) extends ReturnValueResult
case class NoReturnValue(id: Id)           extends ReturnValueResult
