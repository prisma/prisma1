package com.prisma.prod

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.SimpleHttpClient
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.jdbc.JdbcApiMetrics
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.{CachedProjectFetcherImpl, ProjectFetcher}
import com.prisma.api.schema.{CachedSchemaBuilder, SchemaBuilder}
import com.prisma.auth.AuthImpl
import com.prisma.cache.factory.{CacheFactory, CaffeineCacheFactory}
import com.prisma.config.{ConfigLoader, PrismaConfig}
import com.prisma.connectors.utils.{ConnectorLoader, SupportedDrivers}
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.schema.mutations.FunctionValidator
import com.prisma.deploy.server.TelemetryActor
import com.prisma.deploy.server.auth.{DummyManagementAuth, SymmetricManagementAuth}
import com.prisma.image.{FunctionValidatorImpl, SingleServerProjectFetcher}
import com.prisma.messagebus._
import com.prisma.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import com.prisma.messagebus.queue.rabbit.RabbitQueue
import com.prisma.metrics.MetricsRegistry
import com.prisma.metrics.micrometer.MicrometerMetricsRegistry
import com.prisma.shared.messages.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.subscriptions.{SubscriptionDependencies, Webhook}
import com.prisma.workers.dependencies.WorkerDependencies
import com.prisma.workers.payloads.{JsonConversions, Webhook => WorkerWebhook}

import scala.concurrent.ExecutionContext

case class PrismaProdDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends DeployDependencies
    with ApiDependencies
    with SubscriptionDependencies
    with WorkerDependencies {

  implicit val supportedDrivers: SupportedDrivers = SupportedDrivers(
    SupportedDrivers.MYSQL    -> new org.mariadb.jdbc.Driver,
    SupportedDrivers.POSTGRES -> new org.postgresql.Driver,
    SupportedDrivers.SQLITE   -> new org.sqlite.JDBC
  )

  override implicit lazy val executionContext: ExecutionContext = system.dispatcher

  val config: PrismaConfig       = ConfigLoader.load()
  val managementSecret           = config.managementApiSecret.getOrElse("")
  val cacheFactory: CacheFactory = new CaffeineCacheFactory()

  private val rabbitUri: String = config.rabbitUri.getOrElse("RabbitMQ URI required but not found in Prisma configuration.")

  override implicit def self: PrismaProdDependencies = this

  override lazy val apiSchemaBuilder = CachedSchemaBuilder(SchemaBuilder(), invalidationPubSub, cacheFactory)
  override lazy val projectFetcher: ProjectFetcher = {
    val fetcher = SingleServerProjectFetcher(projectPersistence)
    CachedProjectFetcherImpl(fetcher, invalidationPubSub, cacheFactory)
  }

  override lazy val migrator: Migrator = AsyncMigrator(migrationPersistence, projectPersistence, deployConnector, invalidationPublisher)
  override lazy val managementAuth = {
    config.managementApiSecret match {
      case Some(jwtSecret) if jwtSecret.nonEmpty =>
        SymmetricManagementAuth(jwtSecret)

      case _ =>
        println("[Warning] Management authentication is disabled. Enable it in your Prisma config to secure your server.")
        DummyManagementAuth()
    }
  }

  private lazy val invalidationPubSub = {
    implicit val marshaller   = Conversions.Marshallers.FromString
    implicit val unmarshaller = Conversions.Unmarshallers.ToString
    RabbitAkkaPubSub[String](rabbitUri, "project-schema-invalidation", durable = true)
  }
  override lazy val invalidationPublisher                                              = invalidationPubSub
  override lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = invalidationPubSub.map((_: String) => SchemaInvalidated)

  private lazy val theSssEventsPubSub = {
    implicit val marshaller   = Conversions.Marshallers.FromString
    implicit val unmarshaller = Conversions.Unmarshallers.ToString
    RabbitAkkaPubSub[String](rabbitUri, exchangeName = "sss-events", durable = true)
  }
  override lazy val sssEventsPubSub: PubSub[String]               = theSssEventsPubSub
  override lazy val sssEventsSubscriber: PubSubSubscriber[String] = theSssEventsPubSub

  override lazy val keepAliveIntervalSeconds = 10

  override lazy val webhookPublisher: QueuePublisher[Webhook] =
    RabbitQueue.publisher[Webhook](rabbitUri, "webhooks")(reporter, Webhook.marshaller)
  override lazy val webhooksConsumer: QueueConsumer[WorkerWebhook] =
    RabbitQueue.consumer[WorkerWebhook](rabbitUri, "webhooks")(reporter, JsonConversions.webhookUnmarshaller, system)

  override lazy val httpClient                           = SimpleHttpClient()
  override lazy val auth                                 = AuthImpl
  override lazy val deployConnector: DeployConnector     = ConnectorLoader.loadDeployConnector(config)
  override lazy val functionValidator: FunctionValidator = FunctionValidatorImpl()

  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder

  override lazy val apiConnector                = ConnectorLoader.loadApiConnector(config)
  override lazy val sideEffectMutactionExecutor = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier           = DatabaseMutactionVerifierImpl

  lazy val telemetryActor = system.actorOf(Props(TelemetryActor(deployConnector)))

  def initialize()(implicit system: ActorSystem): Unit = {
    JdbcApiMetrics.init(metricsRegistry) // Todo lacking a better init structure for now
    initializeDeployDependencies()
    initializeApiDependencies()
    initializeSubscriptionDependencies()
    telemetryActor
  }

  override val metricsRegistry: MetricsRegistry = MicrometerMetricsRegistry.initialize(deployConnector.cloudSecretPersistence)
}
