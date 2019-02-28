package com.prisma.local

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.SimpleHttpClient
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.jdbc.JdbcApiMetrics
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.{CachedProjectFetcherImpl, ProjectFetcher}
import com.prisma.api.schema.{CachedSchemaBuilder, SchemaBuilder}
import com.prisma.cache.factory.{CacheFactory, CaffeineCacheFactory}
import com.prisma.config.{ConfigLoader, PrismaConfig}
import com.prisma.connectors.utils.{ConnectorLoader, SupportedDrivers}
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.server.TelemetryActor
import com.prisma.image.{Converters, FunctionValidatorImpl, SingleServerProjectFetcher}
import com.prisma.jwt.jna.JnaAuth
import com.prisma.jwt.{Algorithm, NoAuth}
import com.prisma.messagebus.PubSubSubscriber
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.messagebus.queue.inmemory.InMemoryAkkaQueue
import com.prisma.metrics.MetricsRegistry
import com.prisma.metrics.micrometer.MicrometerMetricsRegistry
import com.prisma.shared.messages.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.subscriptions.{SubscriptionDependencies, Webhook}
import com.prisma.workers.dependencies.WorkerDependencies
import com.prisma.workers.payloads.{Webhook => WorkerWebhook}

import scala.concurrent.ExecutionContext

case class PrismaLocalDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends DeployDependencies
    with ApiDependencies
    with WorkerDependencies
    with SubscriptionDependencies {

  implicit val supportedDrivers: SupportedDrivers = SupportedDrivers(
    SupportedDrivers.MYSQL    -> new org.mariadb.jdbc.Driver,
    SupportedDrivers.POSTGRES -> new org.postgresql.Driver,
  )

  override implicit def self                                    = this
  override implicit lazy val executionContext: ExecutionContext = system.dispatcher

  val config: PrismaConfig       = ConfigLoader.load()
  val managementSecret           = config.managementApiSecret.getOrElse("")
  val cacheFactory: CacheFactory = new CaffeineCacheFactory()

  override lazy val apiSchemaBuilder = CachedSchemaBuilder(SchemaBuilder(), invalidationPubSub, cacheFactory)
  override lazy val projectFetcher: ProjectFetcher = {
    val fetcher = SingleServerProjectFetcher(projectPersistence)
    CachedProjectFetcherImpl(fetcher, invalidationPubSub, cacheFactory)(system.dispatcher)
  }

  override lazy val migrator: Migrator = AsyncMigrator(migrationPersistence, projectPersistence, deployConnector, invalidationPublisher)
  override lazy val managementAuth = {
    config.managementApiSecret match {
      case Some(jwtSecret) if jwtSecret.nonEmpty => JnaAuth(Algorithm.HS256)
      case _                                     => println("[Warning] Management authentication is disabled. Enable it in your Prisma config to secure your server."); NoAuth
    }
  }

  private lazy val invalidationPubSub: InMemoryAkkaPubSub[String] = InMemoryAkkaPubSub[String]()

  override lazy val invalidationPublisher = invalidationPubSub
  override lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] =
    invalidationPubSub.map[SchemaInvalidatedMessage]((str: String) => SchemaInvalidated)

  override lazy val sssEventsPubSub: InMemoryAkkaPubSub[String]   = InMemoryAkkaPubSub[String]()
  override lazy val sssEventsSubscriber: PubSubSubscriber[String] = sssEventsPubSub

  override lazy val keepAliveIntervalSeconds = 10

  private lazy val webhooksQueue = InMemoryAkkaQueue[Webhook]()

  override lazy val webhookPublisher  = webhooksQueue
  override lazy val webhooksConsumer  = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)
  override lazy val httpClient        = SimpleHttpClient()
  override lazy val auth              = JnaAuth(Algorithm.HS256)
  override lazy val deployConnector   = ConnectorLoader.loadDeployConnector(config)
  override lazy val functionValidator = FunctionValidatorImpl()

  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder
  override lazy val apiConnector                  = ConnectorLoader.loadApiConnector(config)
  override lazy val sideEffectMutactionExecutor   = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier             = DatabaseMutactionVerifierImpl
  override val metricsRegistry: MetricsRegistry   = MicrometerMetricsRegistry.initialize(deployConnector.cloudSecretPersistence)

  lazy val telemetryActor = system.actorOf(Props(TelemetryActor(deployConnector)))

  def initialize()(implicit system: ActorSystem): Unit = {
    JdbcApiMetrics.init(metricsRegistry) // Todo lacking a better init structure for now
    initializeDeployDependencies()
    initializeApiDependencies()
    initializeSubscriptionDependencies()
    telemetryActor
  }
}
