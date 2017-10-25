package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.JsonFormats.AnyJsonFormat
import cool.graph._
import cool.graph.deprecated.actions.EventJsonProtocol
import cool.graph.messagebus.PubSubPublisher
import cool.graph.messagebus.pubsub.Only
import cool.graph.shared.models.Project
import scaldi._
import spray.json._

import scala.concurrent.Future

case class PublishSubscriptionEvent(project: Project, value: Map[String, Any], mutationName: String)(implicit inj: Injector)
    extends Mutaction
    with Injectable
    with LazyLogging {
  import EventJsonProtocol._

  val publisher = inject[PubSubPublisher[String]](identified by "sss-events-publisher")

  override def execute: Future[MutactionExecutionResult] = {
    val topic = Only(s"subscription:event:${project.id}:$mutationName")

    publisher.publish(topic, value.toJson.compactPrint)
    Future.successful(MutactionExecutionSuccess())
  }
}
