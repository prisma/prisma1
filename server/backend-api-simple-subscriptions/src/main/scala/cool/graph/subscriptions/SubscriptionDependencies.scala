package cool.graph.subscriptions

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.s3.AmazonS3
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.aws.AwsInitializers
import cool.graph.aws.cloudwatch.CloudwatchImpl
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.client._
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.client.finder.ProjectFetcherImpl
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.client.server.GraphQlRequestHandlerImpl
import cool.graph.messagebus.Conversions.{ByteMarshaller, ByteUnmarshaller, Unmarshallers}
import cool.graph.messagebus._
import cool.graph.messagebus.pubsub.rabbit.{RabbitAkkaPubSub, RabbitAkkaPubSubPublisher, RabbitAkkaPubSubSubscriber}
import cool.graph.messagebus.queue.rabbit.{RabbitQueue, RabbitQueueConsumer, RabbitQueuePublisher}
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, KinesisPublisherImplementation, TestableTimeImplementation}
import cool.graph.shared.functions.LiveEndpointResolver
import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import cool.graph.util.ErrorHandlerFactory
import cool.graph.webhook.{Webhook, WebhookCallerImplementation}
import scaldi._

import scala.concurrent.ExecutionContext
import scala.util.Try

//subscriptionsdependencies

trait SimpleSubscriptionInjector extends ClientInjector {

  val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  val sssEventsSubscriber: PubSubSubscriber[String]
  val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05]
  val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse]
  val requestsQueueConsumer: QueueConsumer[SubscriptionRequest]
  val clientAuth: ClientAuth
  val environment: String
  val serviceName: String
}

class SimpleSubscriptionInjectorImpl(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SimpleSubscriptionInjector {
  import cool.graph.subscriptions.protocol.Converters._

  implicit lazy val injector: SimpleSubscriptionInjectorImpl = this
  implicit lazy val toScaldi: Module                         = new Module {}

  implicit lazy val unmarshaller: Array[Byte] => SubscriptionsManagerForProject.SchemaInvalidated.type = (_: Array[Byte]) => SchemaInvalidated
  lazy val globalRabbitUri                                                                             = sys.env("GLOBAL_RABBIT_URI")
  lazy val clusterLocalRabbitUri                                                                       = sys.env("RABBITMQ_URI")
  lazy val apiMatrixFactory: ApiMatrixFactory                                                          = ApiMatrixFactory(DefaultApiMatrix)

  lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = RabbitAkkaPubSub.subscriber[SchemaInvalidatedMessage](
    globalRabbitUri,
    "project-schema-invalidation",
    durable = true
  )

  override lazy val projectSchemaInvalidationSubscriber: RabbitAkkaPubSubSubscriber[String] = {
    val globalRabbitUri                                 = sys.env("GLOBAL_RABBIT_URI")
    implicit val unmarshaller: ByteUnmarshaller[String] = Unmarshallers.ToString

    RabbitAkkaPubSub.subscriber[String](globalRabbitUri, "project-schema-invalidation", durable = true)
  }

  lazy val blockedProjectIds: Vector[String] = Try { sys.env("BLOCKED_PROJECT_IDS").split(",").toVector }.getOrElse(Vector.empty)

  override lazy val functionEnvironment = LambdaFunctionEnvironment(
    sys.env.getOrElse("LAMBDA_AWS_ACCESS_KEY_ID", "whatever"),
    sys.env.getOrElse("LAMBDA_AWS_SECRET_ACCESS_KEY", "whatever")
  )

  override implicit lazy val dispatcher: ExecutionContext = system.dispatcher
  lazy val webhookCaller                                  = new WebhookCallerImplementation()

  lazy val webhookPublisher: RabbitQueuePublisher[cool.graph.webhook.Webhook] =
    RabbitQueue.publisher(clusterLocalRabbitUri, "webhooks")(bugsnagger, Webhook.marshaller)
  lazy val sssEventsPublisher: RabbitAkkaPubSubPublisher[String] =
    RabbitAkkaPubSub.publisher[String](clusterLocalRabbitUri, "sss-events", durable = true)(bugsnagger, fromStringMarshaller)

  lazy val globalApiEndpointManager = GlobalApiEndpointManager(
    euWest1 = sys.env("API_ENDPOINT_EU_WEST_1"),
    usWest2 = sys.env("API_ENDPOINT_US_WEST_2"),
    apNortheast1 = sys.env("API_ENDPOINT_AP_NORTHEAST_1")
  )

  lazy val deferredResolver: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new SimpleToManyDeferredResolver, new SimpleManyModelDeferredResolver)

  lazy val projectSchemaBuilder: Nothing = ???
  lazy val graphQlRequestHandler = GraphQlRequestHandlerImpl(
    errorHandlerFactory = errorHandlerFactory,
    log = log,
    apiVersionMetric = FeatureMetric.ApiSimple,
    apiMetricsMiddleware = apiMetricsMiddleware,
    deferredResolver = deferredResolver
  )

  lazy val fromStringMarshaller: ByteMarshaller[String] = Conversions.Marshallers.FromString
  lazy val endpointResolver                             = LiveEndpointResolver()
  lazy val logsPublisher: RabbitQueuePublisher[String]  = RabbitQueue.publisher[String](clusterLocalRabbitUri, "function-logs")(bugsnagger, fromStringMarshaller)
  lazy val requestPrefix: String                        = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))
  lazy val kinesisAlgoliaSyncQueriesPublisher           = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_ALGOLIA_SYNC_QUERY"), kinesis)
  lazy val log: String => Unit                          = println
  lazy val errorHandlerFactory                          = ErrorHandlerFactory(log, cloudwatch, bugsnagger)
  lazy val s3: AmazonS3                                 = AwsInitializers.createS3()
  lazy val s3Fileupload: AmazonS3                       = AwsInitializers.createS3Fileupload()

  lazy val sssEventsSubscriber: RabbitAkkaPubSubSubscriber[String] = RabbitAkkaPubSub.subscriber[String](
    clusterLocalRabbitUri,
    "sss-events",
    durable = true
  )(bugsnagger, system, Conversions.Unmarshallers.ToString)

  lazy val responsePubSubPublisher: PubSubPublisher[String] = RabbitAkkaPubSub.publisher[String](
    clusterLocalRabbitUri,
    "subscription-responses",
    durable = true
  )(bugsnagger, Conversions.Marshallers.FromString)

  lazy val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05] =
    responsePubSubPublisher.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  lazy val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse] =
    responsePubSubPublisher.map[SubscriptionSessionResponse](converterResponse07ToString)
  lazy val requestsQueueConsumer: RabbitQueueConsumer[SubscriptionRequest] =
    RabbitQueue.consumer[SubscriptionRequest](clusterLocalRabbitUri, "subscription-requests", durableExchange = true)
  lazy val cloudwatch                                   = CloudwatchImpl()
  lazy val globalDatabaseManager: GlobalDatabaseManager = GlobalDatabaseManager.initializeForSingleRegion(config)
  lazy val kinesis: AmazonKinesis                       = AwsInitializers.createKinesis()
  lazy val kinesisApiMetricsPublisher: KinesisPublisher = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
  lazy val featureMetricActor: ActorRef                 = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
  lazy val apiMetricsMiddleware                         = new ApiMetricsMiddleware(testableTime, featureMetricActor)
  lazy val projectSchemaFetcher                         = ProjectFetcherImpl(blockedProjectIds = Vector.empty, config)
  lazy val config: Config                               = ConfigFactory.load()
  lazy val testableTime                                 = new TestableTimeImplementation
  lazy val apiMetricsFlushInterval                      = 10
  lazy val clientAuth                                   = ClientAuthImpl()
  implicit lazy val bugsnagger: BugSnaggerImpl          = BugSnaggerImpl(sys.env.getOrElse("BUGSNAG_API_KEY", ""))
  lazy val environment: String                          = sys.env.getOrElse("ENVIRONMENT", "local")
  lazy val serviceName: String                          = sys.env.getOrElse("SERVICE_NAME", "local")
}

//
//trait SimpleSubscriptionApiDependencies extends Module {
//  implicit val system: ActorSystem
//  implicit val materializer: ActorMaterializer
//
//  val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
//  val sssEventsSubscriber: PubSubSubscriber[String]
//  val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05]
//  val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse]
//  val requestsQueueConsumer: QueueConsumer[SubscriptionRequest]
//  val globalDatabaseManager: GlobalDatabaseManager
//  val kinesisApiMetricsPublisher: KinesisPublisher
//  val featureMetricActor: ActorRef
//  val apiMetricsMiddleware: ApiMetricsMiddleware
//
//  lazy val config                  = ConfigFactory.load()
//  lazy val testableTime            = new TestableTimeImplementation
//  lazy val apiMetricsFlushInterval = 10
//  lazy val clientAuth              = ClientAuthImpl()
//  implicit lazy val bugsnagger     = BugSnaggerImpl(sys.env.getOrElse("BUGSNAG_API_KEY", ""))
//
//  bind[BugSnagger] toNonLazy bugsnagger
//  bind[TestableTime] toNonLazy testableTime
//  bind[ClientAuth] toNonLazy clientAuth
//
//  binding identifiedBy "config" toNonLazy config
//  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
//  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
//  binding identifiedBy "actorMaterializer" toNonLazy materializer
//  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
//  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")
//}
//
//case class SimpleSubscriptionDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SimpleSubscriptionApiDependencies {
//  import cool.graph.subscriptions.protocol.Converters._
//
//  implicit val unmarshaller                   = (_: Array[Byte]) => SchemaInvalidated
//  lazy val globalRabbitUri                    = sys.env("GLOBAL_RABBIT_URI")
//  lazy val clusterLocalRabbitUri              = sys.env("RABBITMQ_URI")
//  lazy val apiMatrixFactory: ApiMatrixFactory = ApiMatrixFactory(DefaultApiMatrix)
//
//  lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = RabbitAkkaPubSub.subscriber[SchemaInvalidatedMessage](
//    globalRabbitUri,
//    "project-schema-invalidation",
//    durable = true
//  )
//
//  lazy val sssEventsSubscriber = RabbitAkkaPubSub.subscriber[String](
//    clusterLocalRabbitUri,
//    "sss-events",
//    durable = true
//  )(bugsnagger, system, Conversions.Unmarshallers.ToString)
//
//  lazy val responsePubSubPublisher: PubSubPublisher[String] = RabbitAkkaPubSub.publisher[String](
//    clusterLocalRabbitUri,
//    "subscription-responses",
//    durable = true
//  )(bugsnagger, Conversions.Marshallers.FromString)
//
//  lazy val responsePubSubPublisherV05 = responsePubSubPublisher.map[SubscriptionSessionResponseV05](converterResponse05ToString)
//  lazy val responsePubSubPublisherV07 = responsePubSubPublisher.map[SubscriptionSessionResponse](converterResponse07ToString)
//  lazy val requestsQueueConsumer      = RabbitQueue.consumer[SubscriptionRequest](clusterLocalRabbitUri, "subscription-requests", durableExchange = true)
//  lazy val cloudwatch                 = CloudwatchImpl()
//  lazy val globalDatabaseManager      = GlobalDatabaseManager.initializeForSingleRegion(config)
//  lazy val kinesis                    = AwsInitializers.createKinesis()
//  lazy val kinesisApiMetricsPublisher = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
//  lazy val featureMetricActor         = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
//  lazy val apiMetricsMiddleware       = new ApiMetricsMiddleware(testableTime, featureMetricActor)
//
//  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy kinesisApiMetricsPublisher
//  bind[QueueConsumer[SubscriptionRequest]] identifiedBy "subscription-requests-consumer" toNonLazy requestsQueueConsumer
//  bind[PubSubPublisher[SubscriptionSessionResponseV05]] identifiedBy "subscription-responses-publisher-05" toNonLazy responsePubSubPublisherV05
//  bind[PubSubPublisher[SubscriptionSessionResponse]] identifiedBy "subscription-responses-publisher-07" toNonLazy responsePubSubPublisherV07
//  bind[PubSubSubscriber[SchemaInvalidatedMessage]] identifiedBy "schema-invalidation-subscriber" toNonLazy invalidationSubscriber
//  bind[PubSubSubscriber[String]] identifiedBy "sss-events-subscriber" toNonLazy sssEventsSubscriber
//  bind[ApiMatrixFactory] toNonLazy apiMatrixFactory
//  bind[GlobalDatabaseManager] toNonLazy globalDatabaseManager
//
//  binding identifiedBy "cloudwatch" toNonLazy cloudwatch
//  binding identifiedBy "project-schema-fetcher" toNonLazy ProjectFetcherImpl(blockedProjectIds = Vector.empty, config)
//  binding identifiedBy "kinesis" toNonLazy kinesis
//  binding identifiedBy "featureMetricActor" to featureMetricActor
//  binding identifiedBy "api-metrics-middleware" toNonLazy apiMetricsMiddleware
//}
