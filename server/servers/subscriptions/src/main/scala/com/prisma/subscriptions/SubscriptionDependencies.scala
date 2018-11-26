package com.prisma.subscriptions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.messagebus._
import com.prisma.metrics.MetricsRegistry
import com.prisma.shared.messages.SchemaInvalidatedMessage
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.subscriptions.metrics.SubscriptionMetrics
import com.prisma.websocket.metrics.SubscriptionWebsocketMetrics

// todo these deps should not be mashed together like that
trait SubscriptionDependencies extends ApiDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  def invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  def sssEventsSubscriber: PubSubSubscriber[String]
  def keepAliveIntervalSeconds: Long

  val metricsRegistry: MetricsRegistry

  def initializeSubscriptionDependencies(): Unit = {
    SubscriptionMetrics.init(metricsRegistry)
    SubscriptionWebsocketMetrics.init(metricsRegistry)
  }

  override def projectIdEncoder: ProjectIdEncoder
}
