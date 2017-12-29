package cool.graph.subscriptions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import cool.graph.api.ApiDependencies
import cool.graph.api.database.Databases
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder
import cool.graph.api.server.AuthImpl
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.messagebus.{Conversions, PubSubPublisher, PubSubSubscriber, QueueConsumer}
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}

trait SubscriptionDependencies extends ApiDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  val sssEventsSubscriber: PubSubSubscriber[String]
  val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05]
  val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse]
  val requestsQueueConsumer: QueueConsumer[SubscriptionRequest]
  def projectFetcher: ProjectFetcher

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

  lazy val responsePubSubPublisher: PubSubPublisher[String] = RabbitAkkaPubSub.publisher[String](
    clusterLocalRabbitUri,
    "subscription-responses",
    durable = true
  )(bugSnagger, Conversions.Marshallers.FromString)

  lazy val responsePubSubPublisherV05              = responsePubSubPublisher.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  lazy val responsePubSubPublisherV07              = responsePubSubPublisher.map[SubscriptionSessionResponse](converterResponse07ToString)
  lazy val requestsQueueConsumer                   = RabbitQueue.consumer[SubscriptionRequest](clusterLocalRabbitUri, "subscription-requests", durableExchange = true)
  override lazy val projectFetcher: ProjectFetcher = ProjectFetcherImpl(blockedProjectIds = Vector.empty, config)

  val databases        = Databases.initialize(config)
  val apiSchemaBuilder = SchemaBuilder()(system, this)
}
