package com.prisma.local

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.SimpleHttpClient
import com.prisma.api.ApiDependencies
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.{CachedProjectFetcherImpl, ProjectFetcher}
import com.prisma.api.schema.{CachedSchemaBuilder, SchemaBuilder}
import com.prisma.auth.AuthImpl
import com.prisma.config.{ConfigLoader, PrismaConfig}
import com.prisma.connectors.utils.ConnectorLoader
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.server.TelemetryActor
import com.prisma.deploy.server.auth.{AsymmetricManagementAuth, DummyManagementAuth, SymmetricManagementAuth}
import com.prisma.image.{Converters, FunctionValidatorImpl, SingleServerProjectFetcher}
import com.prisma.messagebus.PubSubSubscriber
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.messagebus.queue.inmemory.InMemoryAkkaQueue
import com.prisma.metrics.MetricsRegistry
import com.prisma.shared.messages.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.subscriptions.{SubscriptionDependencies, Webhook}
import com.prisma.workers.dependencies.WorkerDependencies
import com.prisma.workers.payloads.{Webhook => WorkerWebhook}

import scala.concurrent.{ExecutionContext, Future}

case class PrismaLocalDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends DeployDependencies
    with ApiDependencies
    with WorkerDependencies
    with SubscriptionDependencies {
  override implicit def self = this

  val config: PrismaConfig = ConfigLoader.load()

  override lazy val apiSchemaBuilder = CachedSchemaBuilder(SchemaBuilder(), invalidationPubSub)
  override lazy val projectFetcher: ProjectFetcher = {
    val fetcher = SingleServerProjectFetcher(projectPersistence)
    CachedProjectFetcherImpl(fetcher, invalidationPubSub)(system.dispatcher)
  }

  override lazy val migrator: Migrator = AsyncMigrator(migrationPersistence, projectPersistence, deployConnector)
  override lazy val managementAuth = {
    (config.managementApiSecret, config.legacySecret) match {
      case (Some(jwtSecret), _) if jwtSecret.nonEmpty => SymmetricManagementAuth(jwtSecret)
      case (_, Some(publicKey)) if publicKey.nonEmpty => AsymmetricManagementAuth(publicKey)
      case _                                          => DummyManagementAuth()
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
  override lazy val apiAuth           = AuthImpl
  override lazy val deployConnector   = ConnectorLoader.loadDeployConnector(config)
  override lazy val functionValidator = FunctionValidatorImpl()

  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder
  override lazy val apiConnector                  = ConnectorLoader.loadApiConnector(config)
  override lazy val sideEffectMutactionExecutor   = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier             = DatabaseMutactionVerifierImpl

  lazy val telemetryActor = system.actorOf(Props(TelemetryActor(deployConnector)))

  override def initialize()(implicit ec: ExecutionContext): Future[Unit] = {
    super.initialize()(ec)
    MetricsRegistry.init(deployConnector.cloudSecretPersistence)
  }
}
