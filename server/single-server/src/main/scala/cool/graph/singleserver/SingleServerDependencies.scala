package cool.graph.singleserver

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.AmazonS3
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.aws.cloudwatch.CloudwatchMock
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.client.authorization.ClientAuthImpl
import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.client.{ClientInjectorImpl, FeatureMetricActor}
import cool.graph.messagebus._
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices._
import cool.graph.shared.functions.dev.DevFunctionEnvironment
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, LocalEndpointResolver}
import cool.graph.subscriptions.SimpleSubscriptionInjector
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import cool.graph.subscriptions.websockets.services.{WebsocketDevDependencies, WebsocketServices}
import cool.graph.system.database.Initializers
import cool.graph.system.database.finder.client.ClientResolver
import cool.graph.system.database.finder.{CachedProjectResolver, CachedProjectResolverImpl, ProjectQueries, UncachedProjectResolver}
import cool.graph.system.externalServices.{AlgoliaKeyChecker, Auth0Api, Auth0Extend}
import cool.graph.system.{SchemaBuilder, SystemInjector}
import cool.graph.webhook.Webhook
import cool.graph.websockets.protocol.{Request => WebsocketRequest}
import cool.graph.worker.payloads.{LogItem, Webhook => WorkerWebhook}
import cool.graph.worker.services.{WorkerDevServices, WorkerServices}
import play.api.libs.json.Json
import scaldi.Module

import scala.concurrent.{Await, Future}

class SingleServerInjectorImpl(implicit val actorSystem: ActorSystem, actorMaterializer: ActorMaterializer)
    extends ClientInjectorImpl
    with SystemInjector
    with SimpleSubscriptionInjector {

  override lazy val dispatcher = system.dispatcher
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  override val config: Config                           = ConfigFactory.load()
  override lazy val testableTime                        = new TestableTimeImplementation
  override lazy val apiMetricsFlushInterval             = 10
  override lazy val clientAuth                          = ClientAuthImpl()
  override implicit lazy val bugsnagger: BugSnaggerImpl = BugSnaggerImpl("")

  override implicit lazy val toScaldi: Module = {
    val outer = this
    new Module {
      binding identifiedBy "project-schema-fetcher" toNonLazy outer.projectSchemaFetcher
      binding identifiedBy "cloudwatch" toNonLazy outer.cloudwatch
      binding identifiedBy "api-metrics-middleware" toNonLazy outer.apiMetricsMiddleware
      binding identifiedBy "featureMetricActor" to outer.featureMetricActor
      binding identifiedBy "s3" toNonLazy outer.s3
      binding identifiedBy "s3-fileupload" toNonLazy outer.s3Fileupload
      bind[GlobalDatabaseManager] toNonLazy outer.globalDatabaseManager
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
      bind[QueuePublisher[Webhook]] identifiedBy "webhookPublisher" toNonLazy webhookPublisher
      bind[PubSubPublisher[String]] identifiedBy "sss-events-publisher" toNonLazy sssEventsPublisher
      bind[PubSubSubscriber[String]] identifiedBy "sss-events-subscriber" toNonLazy sssEventsSubscriber
      bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
      bind[SnsPublisher] identifiedBy "seatSnsPublisher" toNonLazy snsPublisher
      bind[KinesisPublisher] identifiedBy "kinesisAlgoliaSyncQueriesPublisher" toNonLazy kinesisAlgoliaSyncQueriesPublisher
      bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy kinesisApiMetricsPublisher
      binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
      binding identifiedBy "featureMetricActor" to featureMetricActor
      binding identifiedBy "cloudwatch" toNonLazy cloudwatch
      binding identifiedBy "project-schema-fetcher" toNonLazy projectSchemaFetcher
      binding identifiedBy "projectResolver" toNonLazy cachedProjectResolver
      binding identifiedBy "cachedProjectResolver" toNonLazy cachedProjectResolver
      binding identifiedBy "uncachedProjectResolver" toNonLazy uncachedProjectResolver

    }
  }
  lazy val schemaBuilder: SchemaBuilder             = ???
  lazy val projectResolver: UncachedProjectResolver = ???
  lazy val exportDataS3: AmazonS3                   = ???
  lazy val masterToken: Option[String]              = ???
  lazy val clientResolver: ClientResolver           = ???
  lazy val projectQueries: ProjectQueries           = ???
  lazy val algoliaKeyChecker: AlgoliaKeyChecker     = ???
  lazy val auth0Api: Auth0Api                       = ???
  lazy val auth0Extend: Auth0Extend                 = ???
  lazy val environment: String                      = ???
  lazy val serviceName: String                      = ???

  override lazy val (globalDatabaseManager, internalDB, logsDB) = {
    val internal = Initializers.setupAndGetInternalDatabase()
    val logs     = Initializers.setupAndGetLogsDatabase()
    val client   = GlobalDatabaseManager.initializeForSingleRegion(config)
    val dbs      = Future.sequence(Seq(internal, logs))

    try {
      val res = Await.result(dbs, 1.minute)
      (client, res.head, res.last)
    } catch {
      case e: Throwable =>
        println(s"Unable to initialize databases: $e")
        sys.exit(-1)
    }
  }

  lazy val pubSub: InMemoryAkkaPubSub[String]                                     = InMemoryAkkaPubSub[String]()
  override lazy val projectSchemaInvalidationSubscriber: PubSubSubscriber[String] = pubSub
  lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]     = pubSub.map[SchemaInvalidatedMessage]((str: String) => SchemaInvalidated)
  lazy val invalidationPublisher: PubSubPublisher[String]                         = pubSub
  override lazy val functionEnvironment                                           = DevFunctionEnvironment()
  override lazy val blockedProjectIds: Vector[String]                             = Vector.empty
  override lazy val endpointResolver                                              = LocalEndpointResolver()
  lazy val uncachedProjectResolver                                                = UncachedProjectResolver(internalDB)
  lazy val cachedProjectResolver: CachedProjectResolver                           = CachedProjectResolverImpl(uncachedProjectResolver)(system.dispatcher)
  override lazy val requestPrefix                                                 = "local"
  lazy val sssEventsPubSub: InMemoryAkkaPubSub[String]                            = InMemoryAkkaPubSub[String]()
  override val sssEventsPublisher: PubSubPublisher[String]                        = sssEventsPubSub
  lazy val sssEventsSubscriber: PubSubSubscriber[String]                          = sssEventsPubSub
  override lazy val cloudwatch: CloudwatchMock.type                               = CloudwatchMock
  lazy val snsPublisher                                                           = DummySnsPublisher()
  override lazy val kinesisAlgoliaSyncQueriesPublisher                            = DummyKinesisPublisher()
  override lazy val kinesisApiMetricsPublisher                                    = DummyKinesisPublisher()
  override lazy val featureMetricActor: ActorRef                                  = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
  override lazy val apiMetricsMiddleware                                          = new ApiMetricsMiddleware(testableTime, featureMetricActor)

  // API webhooks -> worker webhooks
  lazy val webhooksQueue: Queue[Webhook] = InMemoryAkkaQueue[Webhook]()

  // Worker LogItems -> String (API "LogItems" - easier in this direction)
  lazy val logsQueue: Queue[LogItem] = InMemoryAkkaQueue[LogItem]()

  // Consumer for worker webhook
  lazy val webhooksWorkerConsumer: QueueConsumer[WorkerWebhook] = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)

  // Log item publisher for APIs (they use strings at the moment)
  override val logsPublisher: QueuePublisher[String] = logsQueue.map[String](Converters.string2LogItem)

  // Webhooks publisher for the APIs
  override val webhookPublisher: Queue[Webhook] = webhooksQueue

  lazy val workerServices: WorkerServices = WorkerDevServices(webhooksWorkerConsumer, logsQueue, logsDB)

  override lazy val projectSchemaFetcher: RefreshableProjectFetcher = CachedProjectFetcherImpl(
    projectFetcher = ProjectFetcherImpl(blockedProjectIds, config),
    projectSchemaInvalidationSubscriber = projectSchemaInvalidationSubscriber
  )

  // Websocket deps
  lazy val requestsQueue: InMemoryAkkaQueue[WebsocketRequest]        = InMemoryAkkaQueue[WebsocketRequest]()
  lazy val requestsQueueConsumer: QueueConsumer[SubscriptionRequest] = requestsQueue.map[SubscriptionRequest](Converters.websocketRequest2SubscriptionRequest)
  lazy val responsePubSub: InMemoryAkkaPubSub[String]                = InMemoryAkkaPubSub[String]()
  lazy val websocketServices                                         = WebsocketDevDependencies(requestsQueue, responsePubSub)

  // Simple subscription deps
  lazy val converterResponse07ToString: SubscriptionSessionResponse => String = (response: SubscriptionSessionResponse) => {
    import cool.graph.subscriptions.protocol.ProtocolV07.SubscriptionResponseWriters._
    Json.toJson(response).toString
  }

  lazy val converterResponse05ToString: SubscriptionSessionResponseV05 => String = (response: SubscriptionSessionResponseV05) => {
    import cool.graph.subscriptions.protocol.ProtocolV05.SubscriptionResponseWriters._
    Json.toJson(response).toString
  }

  lazy val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05] =
    responsePubSub.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  lazy val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse] =
    responsePubSub.map[SubscriptionSessionResponse](converterResponse07ToString)
}
//trait SingleServerApiDependencies
//    extends SystemApiDependencies
//    with SimpleApiClientDependencies
//    with RelayApiClientDependencies
//    with SchemaManagerApiDependencies
//    with SimpleSubscriptionApiDependencies {
//  override lazy val config: Config = ConfigFactory.load()
//  override lazy val testableTime            = new TestableTimeImplementation
//  override lazy val apiMetricsFlushInterval = 10
//  override lazy val clientAuth              = ClientAuthImpl()
//  override implicit lazy val bugsnagger: BugSnaggerImpl = BugSnaggerImpl("")
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
//  lazy val sssEventsPubSub: InMemoryAkkaPubSub[String] = InMemoryAkkaPubSub[String]()
//  lazy val sssEventsPublisher: PubSubPublisher[String]                        = sssEventsPubSub
//  lazy val sssEventsSubscriber: PubSubSubscriber[String]                      = sssEventsPubSub
//  lazy val cloudwatch: CloudwatchMock.type = CloudwatchMock
//  lazy val snsPublisher                                                       = DummySnsPublisher()
//  lazy val kinesisAlgoliaSyncQueriesPublisher                                 = DummyKinesisPublisher()
//  lazy val kinesisApiMetricsPublisher                                         = DummyKinesisPublisher()
//  lazy val featureMetricActor: ActorRef = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
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
//  lazy val requestsQueue: InMemoryAkkaQueue[WebsocketRequest] = InMemoryAkkaQueue[WebsocketRequest]()
//  lazy val requestsQueueConsumer: QueueConsumer[SubscriptionRequest] = requestsQueue.map[SubscriptionRequest](Converters.websocketRequest2SubscriptionRequest)
//  lazy val responsePubSub: InMemoryAkkaPubSub[String] = InMemoryAkkaPubSub[String]()
//  lazy val websocketServices     = WebsocketDevDependencies(requestsQueue, responsePubSub)
//
//  // Simple subscription deps
//  lazy val converterResponse07ToString: SubscriptionSessionResponse => String = (response: SubscriptionSessionResponse) => {
//    import cool.graph.subscriptions.protocol.ProtocolV07.SubscriptionResponseWriters._
//    Json.toJson(response).toString
//  }
//
//  lazy val converterResponse05ToString: SubscriptionSessionResponseV05 => String = (response: SubscriptionSessionResponseV05) => {
//    import cool.graph.subscriptions.protocol.ProtocolV05.SubscriptionResponseWriters._
//    Json.toJson(response).toString
//  }
//
//  lazy val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05] = responsePubSub.map[SubscriptionSessionResponseV05](converterResponse05ToString)
//  lazy val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse] = responsePubSub.map[SubscriptionSessionResponse](converterResponse07ToString)
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
