package cool.graph.singleserver

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.typesafe.config.ConfigFactory
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.client.FeatureMetricActor
import cool.graph.client.authorization.ClientAuthImpl
import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.client.schema.simple.SimpleApiClientDependencies
import cool.graph.messagebus._
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
import cool.graph.relay.RelayApiClientDependencies
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.schemamanager.SchemaManagerApiDependencies
import cool.graph.shared.externalServices.{KinesisPublisherImplementation, TestableTimeImplementation}
import cool.graph.shared.functions.dev.DevFunctionEnvironment
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, LocalEndpointResolver}
import cool.graph.subscriptions.SimpleSubscriptionApiDependencies
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import cool.graph.subscriptions.websockets.services.{WebsocketDevDependencies, WebsocketServices}
import cool.graph.system.SystemApiDependencies
import cool.graph.system.database.finder.{CachedProjectResolver, CachedProjectResolverImpl, UncachedProjectResolver}
import cool.graph.webhook.Webhook
import cool.graph.websockets.protocol.{Request => WebsocketRequest}
import cool.graph.worker.payloads.{LogItem, Webhook => WorkerWebhook}
import cool.graph.worker.services.{WorkerDevServices, WorkerServices}
import play.api.libs.json.Json

trait SingleServerApiDependencies
    extends SystemApiDependencies
    with SimpleApiClientDependencies
    with RelayApiClientDependencies
    with SchemaManagerApiDependencies
    with SimpleSubscriptionApiDependencies {

  override lazy val internalDb              = setupAndGetInternalDatabase()
  override lazy val kinesis                 = createKinesis()
  override lazy val config                  = ConfigFactory.load()
  override lazy val testableTime            = new TestableTimeImplementation
  override lazy val apiMetricsFlushInterval = 10
  override lazy val apiMetricsPublisher     = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
  override lazy val featureMetricActor      = system.actorOf(Props(new FeatureMetricActor(apiMetricsPublisher, apiMetricsFlushInterval)))
  override lazy val globalDatabaseManager   = GlobalDatabaseManager.initializeForSingleRegion(config)
  override lazy val clientAuth              = ClientAuthImpl()

  override implicit lazy val bugsnagger = BugSnaggerImpl("")

  override protected def createKinesis(): AmazonKinesis = {
    val credentials = new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonKinesisClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build()
  }
}

case class SingleServerDependencies(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SingleServerApiDependencies {
  val pubSub: InMemoryAkkaPubSub[String]                                 = InMemoryAkkaPubSub[String]()
  val projectSchemaInvalidationSubscriber: PubSubSubscriber[String]      = pubSub
  val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = pubSub.map[SchemaInvalidatedMessage]((str: String) => SchemaInvalidated)
  val invalidationPublisher: PubSubPublisher[String]                     = pubSub
  val functionEnvironment                                                = DevFunctionEnvironment()
  val blockedProjectIds: Vector[String]                                  = Vector.empty
  val endpointResolver                                                   = LocalEndpointResolver()
  val uncachedProjectResolver                                            = UncachedProjectResolver(internalDb)
  val cachedProjectResolver: CachedProjectResolver                       = CachedProjectResolverImpl(uncachedProjectResolver)(system.dispatcher)
  val requestPrefix                                                      = "local"
  val sssEventsPubSub                                                    = InMemoryAkkaPubSub[String]()
  val sssEventsPublisher: PubSubPublisher[String]                        = sssEventsPubSub
  val sssEventsSubscriber: PubSubSubscriber[String]                      = sssEventsPubSub

  // API webhooks -> worker webhooks
  val webhooksQueue: Queue[Webhook] = InMemoryAkkaQueue[Webhook]()

  // Worker LogItems -> String (API "LogItems" - easier in this direction)
  val logsQueue: Queue[LogItem] = InMemoryAkkaQueue[LogItem]()

  // Consumer for worker webhook
  val webhooksWorkerConsumer: QueueConsumer[WorkerWebhook] = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)

  // Log item publisher for APIs (they use strings at the moment)
  val logsPublisher: QueuePublisher[String] = logsQueue.map[String](Converters.string2LogItem)

  // Webhooks publisher for the APIs
  val webhooksPublisher: Queue[Webhook] = webhooksQueue

  val workerServices: WorkerServices = WorkerDevServices(webhooksWorkerConsumer, logsQueue)

  val projectSchemaFetcher: RefreshableProjectFetcher = CachedProjectFetcherImpl(
    projectFetcher = ProjectFetcherImpl(blockedProjectIds, config),
    projectSchemaInvalidationSubscriber = projectSchemaInvalidationSubscriber
  )

  // Websocket deps
  val requestsQueue         = InMemoryAkkaQueue[WebsocketRequest]()
  val requestsQueueConsumer = requestsQueue.map[SubscriptionRequest](Converters.websocketRequest2SubscriptionRequest)
  val responsePubSub        = InMemoryAkkaPubSub[String]()

  val websocketServices = WebsocketDevDependencies(requestsQueue, responsePubSub)

  // Simple subscription deps
  val converterResponse07ToString = (response: SubscriptionSessionResponse) => {
    import cool.graph.subscriptions.protocol.ProtocolV07.SubscriptionResponseWriters._
    Json.toJson(response).toString
  }

  val converterResponse05ToString = (response: SubscriptionSessionResponseV05) => {
    import cool.graph.subscriptions.protocol.ProtocolV05.SubscriptionResponseWriters._
    Json.toJson(response).toString
  }

  val responsePubSubPublisherV05 = responsePubSub.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  val responsePubSubPublisherV07 = responsePubSub.map[SubscriptionSessionResponse](converterResponse07ToString)

  bind[QueueConsumer[SubscriptionRequest]] identifiedBy "subscription-requests-consumer" toNonLazy requestsQueueConsumer
  bind[PubSubPublisher[SubscriptionSessionResponseV05]] identifiedBy "subscription-responses-publisher-05" toNonLazy responsePubSubPublisherV05
  bind[PubSubPublisher[SubscriptionSessionResponse]] identifiedBy "subscription-responses-publisher-07" toNonLazy responsePubSubPublisherV07
  bind[WorkerServices] identifiedBy "worker-services" toNonLazy workerServices
  bind[WebsocketServices] identifiedBy "websocket-services" toNonLazy websocketServices
  bind[PubSubPublisher[String]] identifiedBy "schema-invalidation-publisher" toNonLazy invalidationPublisher
  bind[QueuePublisher[String]] identifiedBy "logsPublisher" toNonLazy logsPublisher
  bind[PubSubSubscriber[SchemaInvalidatedMessage]] identifiedBy "schema-invalidation-subscriber" toNonLazy invalidationSubscriber
  bind[FunctionEnvironment] toNonLazy functionEnvironment
  bind[EndpointResolver] identifiedBy "endpointResolver" toNonLazy endpointResolver
  bind[QueuePublisher[Webhook]] identifiedBy "webhookPublisher" toNonLazy webhooksPublisher
  bind[PubSubPublisher[String]] identifiedBy "sss-events-publisher" toNonLazy sssEventsPublisher
  bind[PubSubSubscriber[String]] identifiedBy "sss-events-subscriber" toNonLazy sssEventsSubscriber
  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
  
  binding identifiedBy "project-schema-fetcher" toNonLazy projectSchemaFetcher
  binding identifiedBy "projectResolver" toNonLazy cachedProjectResolver
  binding identifiedBy "cachedProjectResolver" toNonLazy cachedProjectResolver
  binding identifiedBy "uncachedProjectResolver" toNonLazy uncachedProjectResolver
}
