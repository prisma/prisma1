package com.prisma.subscriptions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.messagebus._
import com.prisma.metrics.PrismaCloudSecretLoader
import com.prisma.shared.messages.SchemaInvalidatedMessage
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.subscriptions.metrics.SubscriptionMetrics
import com.prisma.websocket.metrics.SubscriptionWebsocketMetrics

trait SubscriptionDependencies extends ApiDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  def invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  def sssEventsSubscriber: PubSubSubscriber[String]
  def keepAliveIntervalSeconds: Long

  def initializeSubscriptionDependencies(secretLoader: PrismaCloudSecretLoader): Unit = {
    SubscriptionMetrics.initialize(secretLoader, system)
    SubscriptionWebsocketMetrics.initialize(secretLoader, system)
  }

  override def projectIdEncoder: ProjectIdEncoder
}
