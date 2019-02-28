package com.prisma.native_image

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.SimpleHttpClient
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.jdbc.JdbcApiMetrics
import com.prisma.api.connector.postgres.PostgresApiConnector
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.{CachedProjectFetcherImpl, ProjectFetcher}
import com.prisma.api.schema.{CachedSchemaBuilder, SchemaBuilder}
import com.prisma.cache.SimpleCacheFactory
import com.prisma.cache.factory.CacheFactory
import com.prisma.config.{ConfigLoader, PrismaConfig}
import com.prisma.connectors.utils.SupportedDrivers
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.postgres.PostgresDeployConnector
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.server.TelemetryActor
import com.prisma.image.{Converters, FunctionValidatorImpl, SingleServerProjectFetcher}
import com.prisma.jwt.graal.GraalAuth
import com.prisma.jwt.{Algorithm, NoAuth}
import com.prisma.messagebus.PubSubSubscriber
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.messagebus.queue.inmemory.InMemoryAkkaQueue
import com.prisma.metrics.MetricsRegistry
import com.prisma.metrics.dummy.DummyMetricsRegistry
import com.prisma.native_jdbc.CustomJdbcDriver
import com.prisma.shared.messages.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.subscriptions.{SubscriptionDependencies, Webhook}
import com.prisma.workers.dependencies.WorkerDependencies
import com.prisma.workers.payloads.{Webhook => WorkerWebhook}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

case class PrismaNativeDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends DeployDependencies
    with ApiDependencies
    with WorkerDependencies
    with SubscriptionDependencies {

  // Todo this is a temporary workaround for initializing native dependencies
  GraalAuth.initialize

  val config: PrismaConfig = ConfigLoader.load()
  val logger               = LoggerFactory.getLogger("prisma")

  implicit val supportedDrivers: SupportedDrivers = SupportedDrivers(
    SupportedDrivers.POSTGRES -> CustomJdbcDriver.graal
  )

  override implicit def self                                    = this
  override implicit lazy val executionContext: ExecutionContext = system.dispatcher
  override val managementSecret                                 = config.managementApiSecret.getOrElse("")
  override val cacheFactory: CacheFactory                       = new SimpleCacheFactory()
  override lazy val apiSchemaBuilder                            = CachedSchemaBuilder(SchemaBuilder(), invalidationPubSub, cacheFactory)

  override lazy val projectFetcher: ProjectFetcher = {
    val fetcher = SingleServerProjectFetcher(projectPersistence)
    CachedProjectFetcherImpl(fetcher, invalidationPubSub, cacheFactory)(system.dispatcher)
  }

  override lazy val migrator: Migrator = AsyncMigrator(migrationPersistence, projectPersistence, deployConnector, invalidationPublisher)
  override lazy val managementAuth = {
    config.managementApiSecret match {
      case Some(jwtSecret) if jwtSecret.nonEmpty =>
        GraalAuth(Algorithm.HS256)

      case _ =>
        logger.info("Management authentication is disabled. Enable it in your Prisma config to secure your server.")
        NoAuth
    }
  }

  private lazy val invalidationPubSub: InMemoryAkkaPubSub[String] = InMemoryAkkaPubSub[String]()

  override lazy val invalidationPublisher = invalidationPubSub
  override lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] =
    invalidationPubSub.map[SchemaInvalidatedMessage]((str: String) => SchemaInvalidated)

  override lazy val sssEventsPubSub: InMemoryAkkaPubSub[String]   = InMemoryAkkaPubSub[String]()
  override lazy val sssEventsSubscriber: PubSubSubscriber[String] = sssEventsPubSub
  override lazy val keepAliveIntervalSeconds                      = 10
  private lazy val webhooksQueue                                  = InMemoryAkkaQueue[Webhook]()

  override lazy val webhookPublisher = webhooksQueue
  override lazy val webhooksConsumer = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)
  override lazy val httpClient       = SimpleHttpClient()
  override lazy val auth             = GraalAuth(Algorithm.HS256)

  lazy val databaseConfig                         = config.databases.head
  override lazy val deployConnector               = PostgresDeployConnector(databaseConfig, supportedDrivers(SupportedDrivers.POSTGRES), isActive = true, config.isPrototype)
  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder
  override lazy val apiConnector                  = PostgresApiConnector(databaseConfig, supportedDrivers(SupportedDrivers.POSTGRES), isActive = true, config.isPrototype)

  override lazy val functionValidator                = FunctionValidatorImpl()
  override lazy val sideEffectMutactionExecutor      = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier                = DatabaseMutactionVerifierImpl
  override lazy val metricsRegistry: MetricsRegistry = DummyMetricsRegistry.initialize(deployConnector.cloudSecretPersistence)

  lazy val telemetryActor = system.actorOf(Props(TelemetryActor(deployConnector)))

  def initialize()(implicit system: ActorSystem): Unit = {
    JdbcApiMetrics.init(metricsRegistry) // Todo lacking a better init structure for now
    initializeDeployDependencies()
    initializeApiDependencies()
    initializeSubscriptionDependencies()
    telemetryActor
  }
}
