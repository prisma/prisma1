package cool.graph.subscriptions

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import cool.graph.aws.AwsInitializers
import cool.graph.aws.cloudwatch.CloudwatchImpl
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.client.FeatureMetricActor
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.finder.ProjectFetcherImpl
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.messagebus.{Conversions, PubSubPublisher, PubSubSubscriber, QueueConsumer}
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, KinesisPublisherImplementation, TestableTime, TestableTimeImplementation}
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
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
  val globalDatabaseManager: GlobalDatabaseManager
  val kinesisApiMetricsPublisher: KinesisPublisher
  val featureMetricActor: ActorRef
  val apiMetricsMiddleware: ApiMetricsMiddleware

  lazy val config                  = ConfigFactory.load()
  lazy val testableTime            = new TestableTimeImplementation
  lazy val apiMetricsFlushInterval = 10
  lazy val clientAuth              = ClientAuthImpl()
  implicit lazy val bugsnagger     = BugSnaggerImpl(sys.env.getOrElse("BUGSNAG_API_KEY", ""))

  bind[BugSnagger] toNonLazy bugsnagger
  bind[TestableTime] toNonLazy testableTime
  bind[ClientAuth] toNonLazy clientAuth

  binding identifiedBy "config" toNonLazy config
  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
  binding identifiedBy "actorMaterializer" toNonLazy materializer
  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")
}

case class SimpleSubscriptionDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SimpleSubscriptionApiDependencies {
  import cool.graph.subscriptions.protocol.Converters._

  implicit val unmarshaller              = (_: Array[Byte]) => SchemaInvalidated
  val globalRabbitUri                    = sys.env("GLOBAL_RABBIT_URI")
  val clusterLocalRabbitUri              = sys.env("RABBITMQ_URI")
  val apiMatrixFactory: ApiMatrixFactory = ApiMatrixFactory(DefaultApiMatrix)

  val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = RabbitAkkaPubSub.subscriber[SchemaInvalidatedMessage](
    globalRabbitUri,
    "project-schema-invalidation",
    durable = true
  )

  val sssEventsSubscriber = RabbitAkkaPubSub.subscriber[String](
    clusterLocalRabbitUri,
    "sss-events",
    durable = true
  )(bugsnagger, system, Conversions.Unmarshallers.ToString)

  val responsePubSubPublisher: PubSubPublisher[String] = RabbitAkkaPubSub.publisher[String](
    clusterLocalRabbitUri,
    "subscription-responses",
    durable = true
  )(bugsnagger, Conversions.Marshallers.FromString)

  val responsePubSubPublisherV05 = responsePubSubPublisher.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  val responsePubSubPublisherV07 = responsePubSubPublisher.map[SubscriptionSessionResponse](converterResponse07ToString)
  val requestsQueueConsumer      = RabbitQueue.consumer[SubscriptionRequest](clusterLocalRabbitUri, "subscription-requests", durableExchange = true)
  val cloudwatch                 = CloudwatchImpl()
  val globalDatabaseManager      = GlobalDatabaseManager.initializeForSingleRegion(config)
  val kinesis                    = AwsInitializers.createKinesis()
  val kinesisApiMetricsPublisher = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
  val featureMetricActor         = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
  val apiMetricsMiddleware       = new ApiMetricsMiddleware(testableTime, featureMetricActor)

  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy kinesisApiMetricsPublisher
  bind[QueueConsumer[SubscriptionRequest]] identifiedBy "subscription-requests-consumer" toNonLazy requestsQueueConsumer
  bind[PubSubPublisher[SubscriptionSessionResponseV05]] identifiedBy "subscription-responses-publisher-05" toNonLazy responsePubSubPublisherV05
  bind[PubSubPublisher[SubscriptionSessionResponse]] identifiedBy "subscription-responses-publisher-07" toNonLazy responsePubSubPublisherV07
  bind[PubSubSubscriber[SchemaInvalidatedMessage]] identifiedBy "schema-invalidation-subscriber" toNonLazy invalidationSubscriber
  bind[PubSubSubscriber[String]] identifiedBy "sss-events-subscriber" toNonLazy sssEventsSubscriber
  bind[ApiMatrixFactory] toNonLazy apiMatrixFactory
  bind[GlobalDatabaseManager] toNonLazy globalDatabaseManager

  binding identifiedBy "cloudwatch" toNonLazy cloudwatch
  binding identifiedBy "project-schema-fetcher" toNonLazy ProjectFetcherImpl(blockedProjectIds = Vector.empty, config)
  binding identifiedBy "kinesis" toNonLazy kinesis
  binding identifiedBy "featureMetricActor" to featureMetricActor
  binding identifiedBy "api-metrics-middleware" toNonLazy apiMetricsMiddleware
}
