package com.prisma.api.database.mutactions.mutactions

import com.typesafe.scalalogging.LazyLogging
import com.prisma.api.ApiDependencies
import com.prisma.api.database.mutactions.{Mutaction, MutactionExecutionResult, MutactionExecutionSuccess}
import com.prisma.messagebus.PubSubPublisher
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.Project
import spray.json._
import com.prisma.util.json.JsonFormats.AnyJsonFormat

import scala.concurrent.Future

case class PublishSubscriptionEvent(project: Project, value: Map[String, Any], mutationName: String)(implicit apiDependencies: ApiDependencies)
    extends Mutaction
    with LazyLogging {
  import EventJsonProtocol._

  val publisher = apiDependencies.sssEventsPublisher

  override def execute: Future[MutactionExecutionResult] = {
    val topic = Only(s"subscription:event:${project.id}:$mutationName")

    println(s"PUBLISHING SUBSCRIPTION EVENT TO $topic")

    publisher.publish(topic, value.toJson.compactPrint)
    Future.successful(MutactionExecutionSuccess())
  }
}

case class MutationCallbackEvent(id: String, url: String, payload: String, headers: JsObject = JsObject.empty)

object EventJsonProtocol extends DefaultJsonProtocol {
  implicit val mutationCallbackEventFormat = jsonFormat4(MutationCallbackEvent)
}
