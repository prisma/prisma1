package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.JsonFormats.AnyJsonFormat
import cool.graph._
import cool.graph.client.ClientInjector
import cool.graph.deprecated.actions.EventJsonProtocol
import cool.graph.messagebus.pubsub.Only
import cool.graph.shared.models.Project
import spray.json._

import scala.concurrent.Future

case class PublishSubscriptionEvent(project: Project, value: Map[String, Any], mutationName: String)(implicit injector: ClientInjector)
    extends Mutaction
    with LazyLogging {
  import EventJsonProtocol._

  val publisher = injector.sssEventsPublisher

  override def execute: Future[MutactionExecutionResult] = {
    val topic = Only(s"subscription:event:${project.id}:$mutationName")

    publisher.publish(topic, value.toJson.compactPrint)
    Future.successful(MutactionExecutionSuccess())
  }
}
