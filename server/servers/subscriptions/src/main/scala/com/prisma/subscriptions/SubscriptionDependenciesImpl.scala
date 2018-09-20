package com.prisma.subscriptions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.messagebus._
import com.prisma.shared.messages.SchemaInvalidatedMessage
import com.prisma.shared.models.ProjectIdEncoder

trait SubscriptionDependencies extends ApiDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  def invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  def sssEventsSubscriber: PubSubSubscriber[String]
  def keepAliveIntervalSeconds: Long

  override def projectIdEncoder: ProjectIdEncoder
}
