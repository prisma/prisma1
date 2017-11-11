package cool.graph.deprecated

import com.amazonaws.services.kinesis.AmazonKinesisClient
import cool.graph.shared.models.{Model, Relation}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ModelMutationType extends Enumeration {
  val Created, Updated, Deleted = Value
}

object RelationMutationType extends Enumeration {
  val Added, Removed = Value
}

abstract class ActionTrigger {
  def getPayload: Future[Option[String]]
}

class ModelMutationTrigger(model: Model, mutationType: ModelMutationType.Value, fragment: String) extends ActionTrigger {

  def getPayload: Future[Option[String]] = {
    Future.successful(Some("model"))
  }
}

class RelationMutationTrigger(relation: Relation, mutationType: RelationMutationType.Value, fragment: String) extends ActionTrigger {

  def getPayload: Future[Option[String]] = {
    Future.successful(Some("relation"))
  }
}

class Action(trigger: ActionTrigger, handler: ActionHandler, isActive: Boolean) {
  def run(): Future[Unit] = {
    trigger.getPayload.flatMap(handler.run)
  }
}

abstract class ActionHandler {
  def run(payload: Option[String]): Future[Unit]
}

class WebhookActionHandler(url: String, kinesis: AmazonKinesisClient) extends ActionHandler {
  def run(payload: Option[String]): Future[Unit] = {
    Future.successful(())
  }
}
