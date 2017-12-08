package cool.graph.singleserver

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.AmazonS3
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.aws.AwsInitializers
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.client.authorization.ClientAuthImpl
import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.client.{ClientInjectorImpl, FeatureMetricActor}
import cool.graph.messagebus._
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
import cool.graph.shared.ApiMatrixFactory
import cool.graph.shared.database.{GlobalDatabaseManager, InternalDatabase}
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
import cool.graph.system.externalServices._
import cool.graph.system.{SchemaBuilder, SchemaBuilderImpl, SystemInjector}
import cool.graph.webhook.Webhook
import cool.graph.websockets.protocol.{Request => WebsocketRequest}
import cool.graph.worker.payloads.{LogItem, Webhook => WorkerWebhook}
import cool.graph.worker.services.{WorkerDevServices, WorkerServices}
import play.api.libs.json.Json
import scaldi.Module

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SingleServerInjectorImpl(implicit val actorSystem: ActorSystem, actorMaterializer: ActorMaterializer)
    extends ClientInjectorImpl
    with SystemInjector
    with SimpleSubscriptionInjector {

  implicit val singleServerInjectorImpl = this

  override implicit lazy val toScaldi: Module = {
    val outer = this
    new Module {
      binding identifiedBy "kinesis" toNonLazy outer.kinesis
      binding identifiedBy "internal-db" toNonLazy outer.internalDB
      binding identifiedBy "logs-db" toNonLazy outer.logsDB
      binding identifiedBy "export-data-s3" toNonLazy outer.exportDataS3
      binding identifiedBy "config" toNonLazy outer.config
      binding identifiedBy "actorSystem" toNonLazy outer.system destroyWith (_.terminate())
      binding identifiedBy "dispatcher" toNonLazy outer.system.dispatcher
      binding identifiedBy "actorMaterializer" toNonLazy outer.materializer
      binding identifiedBy "master-token" toNonLazy outer.masterToken
      binding identifiedBy "clientResolver" toNonLazy outer.clientResolver
      binding identifiedBy "projectQueries" toNonLazy outer.projectQueries
      binding identifiedBy "environment" toNonLazy outer.environment
      binding identifiedBy "service-name" toNonLazy outer.serviceName
      binding identifiedBy "project-schema-fetcher" toNonLazy outer.projectSchemaFetcher
      binding identifiedBy "api-metrics-middleware" toNonLazy outer.apiMetricsMiddleware
      binding identifiedBy "featureMetricActor" to outer.featureMetricActor
      binding identifiedBy "s3" toNonLazy outer.s3
      binding identifiedBy "s3-fileupload" toNonLazy outer.s3Fileupload
      binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
      binding identifiedBy "featureMetricActor" to featureMetricActor
      binding identifiedBy "project-schema-fetcher" toNonLazy projectSchemaFetcher
      binding identifiedBy "projectResolver" toNonLazy cachedProjectResolver
      binding identifiedBy "cachedProjectResolver" toNonLazy cachedProjectResolver
      binding identifiedBy "uncachedProjectResolver" toNonLazy uncachedProjectResolver
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

      bind[AlgoliaKeyChecker] identifiedBy "algoliaKeyChecker" toNonLazy outer.algoliaKeyChecker
      bind[Auth0Api] toNonLazy outer.auth0Api
      bind[Auth0Extend] toNonLazy outer.auth0Extend
      bind[BugSnagger] toNonLazy outer.bugsnagger
      bind[TestableTime] toNonLazy outer.testableTime
      bind[ApiMatrixFactory] toNonLazy outer.apiMatrixFactory

    }
  }

  override lazy val dispatcher                                                    = system.dispatcher
  override val config: Config                                                     = ConfigFactory.load()
  override lazy val testableTime                                                  = new TestableTimeImplementation
  override lazy val apiMetricsFlushInterval                                       = 10
  override lazy val clientAuth                                                    = ClientAuthImpl()
  override implicit lazy val bugsnagger: BugSnaggerImpl                           = BugSnaggerImpl("")
  lazy val schemaBuilder                                                          = SchemaBuilder(userCtx => new SchemaBuilderImpl(userCtx, globalDatabaseManager, InternalDatabase(internalDB)).build())
  lazy val projectResolver: UncachedProjectResolver                               = uncachedProjectResolver
  lazy val exportDataS3: AmazonS3                                                 = AwsInitializers.createExportDataS3()
  lazy val masterToken: Option[String]                                            = sys.env.get("MASTER_TOKEN")
  lazy val clientResolver: ClientResolver                                         = ClientResolver(internalDB, cachedProjectResolver)(system.dispatcher)
  lazy val projectQueries: ProjectQueries                                         = ProjectQueries()(internalDB, cachedProjectResolver)
  lazy val algoliaKeyChecker: AlgoliaKeyChecker                                   = new AlgoliaKeyCheckerImplementation()(toScaldi)
  lazy val auth0Api: Auth0Api                                                     = new Auth0ApiImplementation()(toScaldi)
  lazy val auth0Extend: Auth0Extend                                               = new Auth0ExtendImplementation()(toScaldi)
  override lazy val environment: String                                           = sys.env.getOrElse("ENVIRONMENT", "local")
  override lazy val serviceName: String                                           = sys.env.getOrElse("SERVICE_NAME", "local")
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
  override lazy val sssEventsPublisher: PubSubPublisher[String]                   = sssEventsPubSub
  lazy val sssEventsSubscriber: PubSubSubscriber[String]                          = sssEventsPubSub
  lazy val snsPublisher                                                           = DummySnsPublisher()
  override lazy val kinesisAlgoliaSyncQueriesPublisher                            = DummyKinesisPublisher()
  override lazy val kinesisApiMetricsPublisher                                    = DummyKinesisPublisher()
  override lazy val featureMetricActor: ActorRef                                  = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
  override lazy val apiMetricsMiddleware                                          = new ApiMetricsMiddleware(testableTime, featureMetricActor)

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

  // API webhooks -> worker webhooks
  lazy val webhooksQueue: Queue[Webhook] = InMemoryAkkaQueue[Webhook]()

  // Worker LogItems -> String (API "LogItems" - easier in this direction)
  lazy val logsQueue: Queue[LogItem] = InMemoryAkkaQueue[LogItem]()

  // Consumer for worker webhook
  lazy val webhooksWorkerConsumer: QueueConsumer[WorkerWebhook] = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)

  // Log item publisher for APIs (they use strings at the moment)
  override lazy val logsPublisher: QueuePublisher[String] = logsQueue.map[String](Converters.string2LogItem)

  // Webhooks publisher for the APIs
  override lazy val webhookPublisher: Queue[Webhook] = webhooksQueue

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
