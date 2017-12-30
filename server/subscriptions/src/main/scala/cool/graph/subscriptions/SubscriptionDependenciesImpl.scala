package cool.graph.subscriptions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.Databases
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder
import cool.graph.api.server.AuthImpl
import cool.graph.messagebus._
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import cool.graph.websocket.protocol.Request

trait SubscriptionDependencies extends ApiDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  val sssEventsSubscriber: PubSubSubscriber[String]
  val responsePubSubscriber: PubSubSubscriber[String]
  val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05]
  val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse]
  val requestsQueueConsumer: QueueConsumer[SubscriptionRequest]
  val requestsQueuePublisher: QueuePublisher[Request]

  lazy val apiMetricsFlushInterval = 10
  lazy val clientAuth              = AuthImpl

//  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
//  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")
}

case class SubscriptionDependenciesImpl()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SubscriptionDependencies {
  override implicit def self: ApiDependencies = this

  import cool.graph.subscriptions.protocol.Converters._

  implicit val unmarshaller      = (_: Array[Byte]) => SchemaInvalidated
  lazy val globalRabbitUri       = sys.env("GLOBAL_RABBIT_URI")
  lazy val clusterLocalRabbitUri = sys.env("RABBITMQ_URI")

  lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = RabbitAkkaPubSub.subscriber[SchemaInvalidatedMessage](
    globalRabbitUri,
    "project-schema-invalidation",
    durable = true
  )

  lazy val sssEventsSubscriber = RabbitAkkaPubSub.subscriber[String](
    clusterLocalRabbitUri,
    "sss-events",
    durable = true
  )(bugSnagger, system, Conversions.Unmarshallers.ToString)

  lazy val responsePubSubscriber      = InMemoryAkkaPubSub[String]()
  lazy val responsePubSubPublisherV05 = responsePubSubscriber.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  lazy val responsePubSubPublisherV07 = responsePubSubscriber.map[SubscriptionSessionResponse](converterResponse07ToString)

  lazy val requestsQueuePublisher: InMemoryAkkaQueue[Request] = InMemoryAkkaQueue[Request]()
  lazy val requestsQueueConsumer: QueueConsumer[SubscriptionRequest] = requestsQueuePublisher.map[SubscriptionRequest] { req: Request =>
    SubscriptionRequest(req.sessionId, req.projectId, req.body)
  }
  override lazy val projectFetcher: ProjectFetcher = ProjectFetcherImpl(blockedProjectIds = Vector.empty, config)

  val databases        = Databases.initialize(config)
  val apiSchemaBuilder = SchemaBuilder()(system, this)
}
