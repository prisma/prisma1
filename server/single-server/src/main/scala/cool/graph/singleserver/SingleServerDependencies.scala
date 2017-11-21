//package cool.graph.singleserver
//
//import akka.actor.{ActorRef, ActorSystem, Props}
//import akka.stream.ActorMaterializer
//import com.amazonaws.services.s3.AmazonS3
//import com.typesafe.config.ConfigFactory
//import cool.graph.aws.cloudwatch.{Cloudwatch, CloudwatchMock}
//import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
//import cool.graph.client.{ClientInjector, FeatureMetricActor, GlobalApiEndpointManager, UserContext}
//import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
//import cool.graph.client.database.DeferredResolverProvider
//import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
//import cool.graph.client.metrics.ApiMetricsMiddleware
//import cool.graph.client.server.{GraphQlRequestHandler, ProjectSchemaBuilder}
//import cool.graph.messagebus._
//import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
//import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
//import cool.graph.schemamanager.SchemaManagerApiDependencies
//import cool.graph.shared.ApiMatrixFactory
//import cool.graph.shared.database.GlobalDatabaseManager
//import cool.graph.shared.externalServices._
//import cool.graph.shared.functions.dev.DevFunctionEnvironment
//import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, LocalEndpointResolver}
//import cool.graph.subscriptions.SimpleSubscriptionInjector
//import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
//import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
//import cool.graph.subscriptions.protocol.SubscriptionRequest
//import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
//import cool.graph.subscriptions.websockets.services.{WebsocketDevDependencies, WebsocketServices}
//import cool.graph.system.{SchemaBuilder, SystemApiDependencies, SystemInjector}
//import cool.graph.system.database.Initializers
//import cool.graph.system.database.finder.client.ClientResolver
//import cool.graph.system.database.finder.{CachedProjectResolver, CachedProjectResolverImpl, ProjectQueries, UncachedProjectResolver}
//import cool.graph.system.externalServices.{AlgoliaKeyChecker, Auth0Api, Auth0Extend}
//import cool.graph.util.ErrorHandlerFactory
//import cool.graph.webhook.{Webhook, WebhookCaller}
//import cool.graph.websockets.protocol.{Request => WebsocketRequest}
//import cool.graph.worker.payloads.{LogItem, Webhook => WorkerWebhook}
//import cool.graph.worker.services.{WorkerDevServices, WorkerServices}
//import play.api.libs.json.Json
//import scaldi.Module
//
//import scala.concurrent.{Await, ExecutionContextExecutor, Future}
//
//
//
//
//trait SingleServerInjector extends ClientInjector with SystemInjector with SimpleSubscriptionInjector{
//  override lazy val config                  = ConfigFactory.load()
//  override lazy val testableTime            = new TestableTimeImplementation
//  override lazy val apiMetricsFlushInterval = 10
//  override lazy val clientAuth              = ClientAuthImpl()
//  override implicit lazy val bugsnagger     = BugSnaggerImpl("")
//}
//
//
//case class SingleServerInjectorImpl (implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SingleServerInjector {
//
//  override val environment: String = _
//  override val serviceName: String = _
//  override implicit val injector: ClientInjector = _
//  override val webhookPublisher: QueuePublisher[Webhook] = _
//  override val webhookCaller: WebhookCaller = _
//  override val log: String => Unit = _
//  override val errorHandlerFactory: ErrorHandlerFactory = _
//  override val apiMatrixFactory: ApiMatrixFactory = _
//  override val globalApiEndpointManager: GlobalApiEndpointManager = _
//  override val deferredResolver: DeferredResolverProvider[_, UserContext] = _
//  override val projectSchemaBuilder: ProjectSchemaBuilder = _
//  override val graphQlRequestHandler: GraphQlRequestHandler = _
//  override val s3: AmazonS3 = _
//  override val s3Fileupload: AmazonS3 = _
//  override implicit val toScaldi: Module = _
//  override val schemaBuilder: SchemaBuilder = _
//  override val projectResolver: UncachedProjectResolver = _
//  override val internalDB = _
//  override val logsDB = _
//  override val exportDataS3: AmazonS3 = _
//  override val actorSystem: ActorSystem = _
//  override val dispatcher: ExecutionContextExecutor = _
//  override val actorMaterializer: ActorMaterializer = _
//  override val masterToken: Option[String] = _
//  override val clientResolver: ClientResolver = _
//  override val projectQueries: ProjectQueries = _
//  override val algoliaKeyChecker: AlgoliaKeyChecker = _
//  override val auth0Api: Auth0Api = _
//  override val auth0Extend: Auth0Extend = _
//
//
//  import system.dispatcher
//
//  import scala.concurrent.duration._
//
//  lazy val (globalDatabaseManager, internalDb, logsDb) = {
//    val internal = Initializers.setupAndGetInternalDatabase()
//    val logs     = Initializers.setupAndGetLogsDatabase()
//    val client   = GlobalDatabaseManager.initializeForSingleRegion(config)
//    val dbs      = Future.sequence(Seq(internal, logs))
//
//    try {
//      val res = Await.result(dbs, 1.minute)
//      (client, res.head, res.last)
//    } catch {
//      case e: Throwable =>
//        println(s"Unable to initialize databases: $e")
//        sys.exit(-1)
//    }
//  }
//
//  lazy val pubSub: InMemoryAkkaPubSub[String]                                 = InMemoryAkkaPubSub[String]()
//  lazy val projectSchemaInvalidationSubscriber: PubSubSubscriber[String]      = pubSub
//  lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = pubSub.map[SchemaInvalidatedMessage]((str: String) => SchemaInvalidated)
//  lazy val invalidationPublisher: PubSubPublisher[String]                     = pubSub
//  lazy val functionEnvironment                                                = DevFunctionEnvironment()
//  lazy val blockedProjectIds: Vector[String]                                  = Vector.empty
//  lazy val endpointResolver                                                   = LocalEndpointResolver()
//  lazy val uncachedProjectResolver                                            = UncachedProjectResolver(internalDb)
//  lazy val cachedProjectResolver: CachedProjectResolver                       = CachedProjectResolverImpl(uncachedProjectResolver)(system.dispatcher)
//  lazy val requestPrefix                                                      = "local"
//  lazy val sssEventsPubSub                                                    = InMemoryAkkaPubSub[String]()
//  lazy val sssEventsPublisher: PubSubPublisher[String]                        = sssEventsPubSub
//  lazy val sssEventsSubscriber: PubSubSubscriber[String]                      = sssEventsPubSub
//  lazy val cloudwatch                                                         = CloudwatchMock
//  lazy val snsPublisher                                                       = DummySnsPublisher()
//  lazy val kinesisAlgoliaSyncQueriesPublisher                                 = DummyKinesisPublisher()
//  lazy val kinesisApiMetricsPublisher                                         = DummyKinesisPublisher()
//  lazy val featureMetricActor                                                 = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
//  lazy val apiMetricsMiddleware                                               = new ApiMetricsMiddleware(testableTime, featureMetricActor)
//
//  // API webhooks -> worker webhooks
//  lazy val webhooksQueue: Queue[Webhook] = InMemoryAkkaQueue[Webhook]()
//
//  // Worker LogItems -> String (API "LogItems" - easier in this direction)
//  lazy val logsQueue: Queue[LogItem] = InMemoryAkkaQueue[LogItem]()
//
//  // Consumer for worker webhook
//  lazy val webhooksWorkerConsumer: QueueConsumer[WorkerWebhook] = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)
//
//  // Log item publisher for APIs (they use strings at the moment)
//  lazy val logsPublisher: QueuePublisher[String] = logsQueue.map[String](Converters.string2LogItem)
//
//  // Webhooks publisher for the APIs
//  lazy val webhooksPublisher: Queue[Webhook] = webhooksQueue
//
//  lazy val workerServices: WorkerServices = WorkerDevServices(webhooksWorkerConsumer, logsQueue, logsDb)
//
//  lazy val projectSchemaFetcher: RefreshableProjectFetcher = CachedProjectFetcherImpl(
//    projectFetcher = ProjectFetcherImpl(blockedProjectIds, config),
//    projectSchemaInvalidationSubscriber = projectSchemaInvalidationSubscriber
//  )
//
//  // Websocket deps
//  lazy val requestsQueue         = InMemoryAkkaQueue[WebsocketRequest]()
//  lazy val requestsQueueConsumer = requestsQueue.map[SubscriptionRequest](Converters.websocketRequest2SubscriptionRequest)
//  lazy val responsePubSub        = InMemoryAkkaPubSub[String]()
//  lazy val websocketServices     = WebsocketDevDependencies(requestsQueue, responsePubSub)
//
//  // Simple subscription deps
//  lazy val converterResponse07ToString = (response: SubscriptionSessionResponse) => {
//    import cool.graph.subscriptions.protocol.ProtocolV07.SubscriptionResponseWriters._
//    Json.toJson(response).toString
//  }
//
//  lazy val converterResponse05ToString = (response: SubscriptionSessionResponseV05) => {
//    import cool.graph.subscriptions.protocol.ProtocolV05.SubscriptionResponseWriters._
//    Json.toJson(response).toString
//  }
//
//  lazy val responsePubSubPublisherV05 = responsePubSub.map[SubscriptionSessionResponseV05](converterResponse05ToString)
//  lazy val responsePubSubPublisherV07 = responsePubSub.map[SubscriptionSessionResponse](converterResponse07ToString)
//}
//
//
//
//
//
//
//
//trait SingleServerApiDependencies
//    extends SystemApiDependencies
//    with SimpleApiClientDependencies
//    with RelayApiClientDependencies
//    with SchemaManagerApiDependencies
//    with SimpleSubscriptionApiDependencies {
//  override lazy val config                  = ConfigFactory.load()
//  override lazy val testableTime            = new TestableTimeImplementation
//  override lazy val apiMetricsFlushInterval = 10
//  override lazy val clientAuth              = ClientAuthImpl()
//  override implicit lazy val bugsnagger     = BugSnaggerImpl("")
//}
//
//
//
//case class SingleServerDependencies(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SingleServerApiDependencies {
//  import system.dispatcher
//
//  import scala.concurrent.duration._
//
//  lazy val (globalDatabaseManager, internalDb, logsDb) = {
//    val internal = Initializers.setupAndGetInternalDatabase()
//    val logs     = Initializers.setupAndGetLogsDatabase()
//    val client   = GlobalDatabaseManager.initializeForSingleRegion(config)
//    val dbs      = Future.sequence(Seq(internal, logs))
//
//    try {
//      val res = Await.result(dbs, 1.minute)
//      (client, res.head, res.last)
//    } catch {
//      case e: Throwable =>
//        println(s"Unable to initialize databases: $e")
//        sys.exit(-1)
//    }
//  }
//
//  lazy val pubSub: InMemoryAkkaPubSub[String]                                 = InMemoryAkkaPubSub[String]()
//  lazy val projectSchemaInvalidationSubscriber: PubSubSubscriber[String]      = pubSub
//  lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = pubSub.map[SchemaInvalidatedMessage]((str: String) => SchemaInvalidated)
//  lazy val invalidationPublisher: PubSubPublisher[String]                     = pubSub
//  lazy val functionEnvironment                                                = DevFunctionEnvironment()
//  lazy val blockedProjectIds: Vector[String]                                  = Vector.empty
//  lazy val endpointResolver                                                   = LocalEndpointResolver()
//  lazy val uncachedProjectResolver                                            = UncachedProjectResolver(internalDb)
//  lazy val cachedProjectResolver: CachedProjectResolver                       = CachedProjectResolverImpl(uncachedProjectResolver)(system.dispatcher)
//  lazy val requestPrefix                                                      = "local"
//  lazy val sssEventsPubSub                                                    = InMemoryAkkaPubSub[String]()
//  lazy val sssEventsPublisher: PubSubPublisher[String]                        = sssEventsPubSub
//  lazy val sssEventsSubscriber: PubSubSubscriber[String]                      = sssEventsPubSub
//  lazy val cloudwatch                                                         = CloudwatchMock
//  lazy val snsPublisher                                                       = DummySnsPublisher()
//  lazy val kinesisAlgoliaSyncQueriesPublisher                                 = DummyKinesisPublisher()
//  lazy val kinesisApiMetricsPublisher                                         = DummyKinesisPublisher()
//  lazy val featureMetricActor                                                 = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
//  lazy val apiMetricsMiddleware                                               = new ApiMetricsMiddleware(testableTime, featureMetricActor)
//
//  // API webhooks -> worker webhooks
//  lazy val webhooksQueue: Queue[Webhook] = InMemoryAkkaQueue[Webhook]()
//
//  // Worker LogItems -> String (API "LogItems" - easier in this direction)
//  lazy val logsQueue: Queue[LogItem] = InMemoryAkkaQueue[LogItem]()
//
//  // Consumer for worker webhook
//  lazy val webhooksWorkerConsumer: QueueConsumer[WorkerWebhook] = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)
//
//  // Log item publisher for APIs (they use strings at the moment)
//  lazy val logsPublisher: QueuePublisher[String] = logsQueue.map[String](Converters.string2LogItem)
//
//  // Webhooks publisher for the APIs
//  lazy val webhooksPublisher: Queue[Webhook] = webhooksQueue
//
//  lazy val workerServices: WorkerServices = WorkerDevServices(webhooksWorkerConsumer, logsQueue, logsDb)
//
//  lazy val projectSchemaFetcher: RefreshableProjectFetcher = CachedProjectFetcherImpl(
//    projectFetcher = ProjectFetcherImpl(blockedProjectIds, config),
//    projectSchemaInvalidationSubscriber = projectSchemaInvalidationSubscriber
//  )
//
//  // Websocket deps
//  lazy val requestsQueue         = InMemoryAkkaQueue[WebsocketRequest]()
//  lazy val requestsQueueConsumer = requestsQueue.map[SubscriptionRequest](Converters.websocketRequest2SubscriptionRequest)
//  lazy val responsePubSub        = InMemoryAkkaPubSub[String]()
//  lazy val websocketServices     = WebsocketDevDependencies(requestsQueue, responsePubSub)
//
//  // Simple subscription deps
//  lazy val converterResponse07ToString = (response: SubscriptionSessionResponse) => {
//    import cool.graph.subscriptions.protocol.ProtocolV07.SubscriptionResponseWriters._
//    Json.toJson(response).toString
//  }
//
//  lazy val converterResponse05ToString = (response: SubscriptionSessionResponseV05) => {
//    import cool.graph.subscriptions.protocol.ProtocolV05.SubscriptionResponseWriters._
//    Json.toJson(response).toString
//  }
//
//  lazy val responsePubSubPublisherV05 = responsePubSub.map[SubscriptionSessionResponseV05](converterResponse05ToString)
//  lazy val responsePubSubPublisherV07 = responsePubSub.map[SubscriptionSessionResponse](converterResponse07ToString)
//
//  bind[QueueConsumer[SubscriptionRequest]] identifiedBy "subscription-requests-consumer" toNonLazy requestsQueueConsumer
//  bind[PubSubPublisher[SubscriptionSessionResponseV05]] identifiedBy "subscription-responses-publisher-05" toNonLazy responsePubSubPublisherV05
//  bind[PubSubPublisher[SubscriptionSessionResponse]] identifiedBy "subscription-responses-publisher-07" toNonLazy responsePubSubPublisherV07
//  bind[WorkerServices] identifiedBy "worker-services" toNonLazy workerServices
//  bind[WebsocketServices] identifiedBy "websocket-services" toNonLazy websocketServices
//  bind[PubSubPublisher[String]] identifiedBy "schema-invalidation-publisher" toNonLazy invalidationPublisher
//  bind[QueuePublisher[String]] identifiedBy "logsPublisher" toNonLazy logsPublisher
//  bind[PubSubSubscriber[SchemaInvalidatedMessage]] identifiedBy "schema-invalidation-subscriber" toNonLazy invalidationSubscriber
//  bind[FunctionEnvironment] toNonLazy functionEnvironment
//  bind[EndpointResolver] identifiedBy "endpointResolver" toNonLazy endpointResolver
//  bind[QueuePublisher[Webhook]] identifiedBy "webhookPublisher" toNonLazy webhooksPublisher
//  bind[PubSubPublisher[String]] identifiedBy "sss-events-publisher" toNonLazy sssEventsPublisher
//  bind[PubSubSubscriber[String]] identifiedBy "sss-events-subscriber" toNonLazy sssEventsSubscriber
//  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
//  bind[GlobalDatabaseManager] toNonLazy globalDatabaseManager
//  bind[SnsPublisher] identifiedBy "seatSnsPublisher" toNonLazy snsPublisher
//  bind[KinesisPublisher] identifiedBy "kinesisAlgoliaSyncQueriesPublisher" toNonLazy kinesisAlgoliaSyncQueriesPublisher
//  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy kinesisApiMetricsPublisher
//
//  binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
//  binding identifiedBy "featureMetricActor" to featureMetricActor
//  binding identifiedBy "cloudwatch" toNonLazy cloudwatch
//  binding identifiedBy "project-schema-fetcher" toNonLazy projectSchemaFetcher
//  binding identifiedBy "projectResolver" toNonLazy cachedProjectResolver
//  binding identifiedBy "cachedProjectResolver" toNonLazy cachedProjectResolver
//  binding identifiedBy "uncachedProjectResolver" toNonLazy uncachedProjectResolver
//}
