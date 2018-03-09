package com.prisma.subscriptions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.messagebus._
import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import com.prisma.subscriptions.protocol.SubscriptionRequest
import com.prisma.subscriptions.resolving.SubscriptionsManagerForProject.SchemaInvalidatedMessage
import com.prisma.websocket.protocol.Request

trait SubscriptionDependencies extends ApiDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  def invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  def sssEventsSubscriber: PubSubSubscriber[String]
  def responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05]
  def responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse]
  def requestsQueueConsumer: QueueConsumer[SubscriptionRequest]
  def requestsQueuePublisher: QueuePublisher[Request]
  def responsePubSubSubscriber: PubSubSubscriber[String]
  def keepAliveIntervalSeconds: Long
}
