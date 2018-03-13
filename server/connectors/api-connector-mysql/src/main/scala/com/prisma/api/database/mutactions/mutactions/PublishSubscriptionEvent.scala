//package com.prisma.api.database.mutactions.mutactions
//
//import com.prisma.api.database.mutactions.{Mutaction, MutactionExecutionResult, MutactionExecutionSuccess}
//import com.prisma.messagebus.PubSub
//import com.prisma.messagebus.pubsub.Only
//import com.prisma.shared.models.Project
//import spray.json._
//
//import scala.concurrent.Future
//
//case class PublishSubscriptionEventOld(
//    project: Project,
//    value: Map[String, Any],
//    mutationName: String,
//    publisher: PubSub[String]
//) extends Mutaction {
//
//  implicit val writer: JsonWriter[Map[String, Any]] = ???
//
//  override def execute: Future[MutactionExecutionResult] = {
//    val topic = Only(s"subscription:event:${project.id}:$mutationName")
//
//    println(s"PUBLISHING SUBSCRIPTION EVENT TO $topic")
//
//    publisher.publish(topic, value.toJson.compactPrint)
//    Future.successful(MutactionExecutionSuccess())
//  }
//}
//
//case class MutationCallbackEvent(id: String, url: String, payload: String, headers: JsObject = JsObject.empty)
//
//object EventJsonProtocol extends DefaultJsonProtocol {
//  implicit val mutationCallbackEventFormat = jsonFormat4(MutationCallbackEvent)
//}
