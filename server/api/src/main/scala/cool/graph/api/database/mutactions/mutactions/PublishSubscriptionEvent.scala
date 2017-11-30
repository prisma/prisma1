package cool.graph.api.database.mutactions.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.api.database.mutactions.{Mutaction, MutactionExecutionResult, MutactionExecutionSuccess}
import cool.graph.messagebus.PubSubPublisher
import cool.graph.messagebus.pubsub.Only
import cool.graph.shared.models.Project
import scaldi._
import spray.json._
import cool.graph.util.json.JsonFormats.AnyJsonFormat

import scala.concurrent.Future

case class PublishSubscriptionEvent(project: Project, value: Map[String, Any], mutationName: String) extends Mutaction with LazyLogging {
  import EventJsonProtocol._

  //todo: inject
//  val publisher = inject[PubSubPublisher[String]](identified by "sss-events-publisher")

  override def execute: Future[MutactionExecutionResult] = {
    val topic = Only(s"subscription:event:${project.id}:$mutationName")

//    publisher.publish(topic, value.toJson.compactPrint)
    Future.successful(MutactionExecutionSuccess())
  }
}

case class MutationCallbackEvent(id: String, url: String, payload: String, headers: JsObject = JsObject.empty)

object EventJsonProtocol extends DefaultJsonProtocol {
  implicit val mutationCallbackEventFormat = jsonFormat4(MutationCallbackEvent)
}
