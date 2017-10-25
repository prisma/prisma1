package cool.graph.subscriptions

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.typesafe.config.ConfigFactory
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.client.FeatureMetricActor
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.finder.ProjectFetcherImpl
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.cloudwatch.CloudwatchImpl
import cool.graph.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.messagebus.{Conversions, PubSubPublisher, PubSubSubscriber, QueueConsumer}
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, KinesisPublisherImplementation, TestableTime, TestableTimeImplementation}
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import scaldi._

trait SimpleSubscriptionApiDependencies extends Module {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  val sssEventsSubscriber: PubSubSubscriber[String]
  val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05]
  val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse]
  val requestsQueueConsumer: QueueConsumer[SubscriptionRequest]

  lazy val config                  = ConfigFactory.load()
  lazy val testableTime            = new TestableTimeImplementation
  lazy val apiMetricsFlushInterval = 10
  lazy val kinesis                 = createKinesis()
  lazy val apiMetricsPublisher     = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
  lazy val clientAuth              = ClientAuthImpl()
  lazy val featureMetricActor      = system.actorOf(Props(new FeatureMetricActor(apiMetricsPublisher, apiMetricsFlushInterval)))

  implicit lazy val bugsnagger = BugSnaggerImpl(sys.env.getOrElse("BUGSNAG_API_KEY", ""))

  bind[GlobalDatabaseManager] toNonLazy GlobalDatabaseManager.initializeForSingleRegion(config)
  bind[BugSnagger] toNonLazy bugsnagger
  bind[TestableTime] toNonLazy new TestableTimeImplementation
  bind[ClientAuth] toNonLazy clientAuth
  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy new KinesisPublisherImplementation(
    streamName = sys.env("KINESIS_STREAM_API_METRICS"),
    kinesis
  )

  binding identifiedBy "kinesis" toNonLazy kinesis
  binding identifiedBy "cloudwatch" toNonLazy CloudwatchImpl()
  binding identifiedBy "config" toNonLazy ConfigFactory.load()
  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
  binding identifiedBy "actorMaterializer" toNonLazy materializer
  binding identifiedBy "featureMetricActor" to featureMetricActor
  binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")

  protected def createKinesis(): AmazonKinesis = {
    val credentials = new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonKinesisClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build()
  }
}

case class SimpleSubscriptionDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SimpleSubscriptionApiDependencies {
  import SubscriptionRequest._
  import cool.graph.subscriptions.protocol.Converters._

  val globalRabbitUri       = sys.env("GLOBAL_RABBIT_URI")
  implicit val unmarshaller = (_: Array[Byte]) => SchemaInvalidated
  val apiMatrixFactory: ApiMatrixFactory = ApiMatrixFactory(DefaultApiMatrix(_))

  val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] =
    RabbitAkkaPubSub.subscriber[SchemaInvalidatedMessage](globalRabbitUri, "project-schema-invalidation", durable = true)

  val clusterLocalRabbitUri = sys.env("RABBITMQ_URI")

  val sssEventsSubscriber =
    RabbitAkkaPubSub.subscriber[String](clusterLocalRabbitUri, "sss-events", durable = true)(bugsnagger, system, Conversions.Unmarshallers.ToString)

  val responsePubSubPublisher: PubSubPublisher[String] =
    RabbitAkkaPubSub.publisher[String](clusterLocalRabbitUri, "subscription-responses", durable = false)(bugsnagger, Conversions.Marshallers.FromString)

  val responsePubSubPublisherV05 = responsePubSubPublisher.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  val responsePubSubPublisherV07 = responsePubSubPublisher.map[SubscriptionSessionResponse](converterResponse07ToString)
  val requestsQueueConsumer      = RabbitQueue.consumer[SubscriptionRequest](clusterLocalRabbitUri, "subscription-requests")

  bind[QueueConsumer[SubscriptionRequest]] identifiedBy "subscription-requests-consumer" toNonLazy requestsQueueConsumer
  bind[PubSubPublisher[SubscriptionSessionResponseV05]] identifiedBy "subscription-responses-publisher-05" toNonLazy responsePubSubPublisherV05
  bind[PubSubPublisher[SubscriptionSessionResponse]] identifiedBy "subscription-responses-publisher-07" toNonLazy responsePubSubPublisherV07
  bind[PubSubSubscriber[SchemaInvalidatedMessage]] identifiedBy "schema-invalidation-subscriber" toNonLazy invalidationSubscriber
  bind[PubSubSubscriber[String]] identifiedBy "sss-events-subscriber" toNonLazy sssEventsSubscriber
  bind[ApiMatrixFactory] toNonLazy apiMatrixFactory

  binding identifiedBy "project-schema-fetcher" toNonLazy ProjectFetcherImpl(blockedProjectIds = Vector.empty, config)
}
