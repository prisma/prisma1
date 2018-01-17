package com.prisma.subscriptions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.api.ApiDependencies
import com.prisma.api.database.Databases
import com.prisma.api.project.{ProjectFetcher, ProjectFetcherImpl}
import com.prisma.api.schema.SchemaBuilder
import com.prisma.auth.AuthImpl
import com.prisma.messagebus._
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import com.prisma.messagebus.queue.inmemory.InMemoryAkkaQueue
import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import com.prisma.subscriptions.protocol.SubscriptionRequest
import com.prisma.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
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

  lazy val apiMetricsFlushInterval = 10
  lazy val clientAuth              = AuthImpl
  val keepAliveIntervalSeconds: Long
}

// todo this needs rewiring
case class SubscriptionDependenciesImpl()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SubscriptionDependencies {
  override implicit def self: ApiDependencies = this

  import com.prisma.subscriptions.protocol.Converters._

  implicit val unmarshaller             = (_: Array[Byte]) => SchemaInvalidated
  lazy val globalRabbitUri              = sys.env("GLOBAL_RABBIT_URI")
  lazy val clusterLocalRabbitUri        = sys.env("RABBITMQ_URI")
  override val keepAliveIntervalSeconds = 10

  lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = RabbitAkkaPubSub.subscriber[SchemaInvalidatedMessage](
    globalRabbitUri,
    "project-schema-invalidation",
    durable = true
  )

  override lazy val sssEventsPubSub: InMemoryAkkaPubSub[String]   = InMemoryAkkaPubSub[String]()
  override lazy val sssEventsSubscriber: PubSubSubscriber[String] = sssEventsPubSub

  lazy val responsePubSubSubscriber   = InMemoryAkkaPubSub[String]()
  lazy val responsePubSubPublisherV05 = responsePubSubSubscriber.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  lazy val responsePubSubPublisherV07 = responsePubSubSubscriber.map[SubscriptionSessionResponse](converterResponse07ToString)

  lazy val requestsQueuePublisher: InMemoryAkkaQueue[Request] = InMemoryAkkaQueue[Request]()
  lazy val requestsQueueConsumer: QueueConsumer[SubscriptionRequest] = requestsQueuePublisher.map[SubscriptionRequest] { req: Request =>
    SubscriptionRequest(req.sessionId, req.projectId, req.body)
  }

  lazy val projectFetcher: ProjectFetcher = {
    val schemaManagerEndpoint = config.getString("schemaManagerEndpoint")
    val schemaManagerSecret   = config.getString("schemaManagerSecret")
    ProjectFetcherImpl(Vector.empty, config, schemaManagerEndpoint = schemaManagerEndpoint, schemaManagerSecret = schemaManagerSecret)
  }

  val databases        = Databases.initialize(config)
  val apiSchemaBuilder = SchemaBuilder()(system, this)

  override val webhookPublisher = ???
}
