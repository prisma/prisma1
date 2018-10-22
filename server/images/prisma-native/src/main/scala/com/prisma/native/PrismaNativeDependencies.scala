package com.prisma.native

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.SimpleHttpClient
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.ApiConnector
import com.prisma.api.connector.postgres.PostgresApiConnector
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.{CachedProjectFetcherImpl, ProjectFetcher}
import com.prisma.api.schema.{CachedSchemaBuilder, SchemaBuilder}
import com.prisma.config.{ConfigLoader, PrismaConfig}
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.connector.postgres.PostgresDeployConnector
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.server.TelemetryActor
import com.prisma.image.{Converters, FunctionValidatorImpl, SingleServerProjectFetcher}
import com.prisma.jwt.{Algorithm, Auth}
import com.prisma.messagebus.PubSubSubscriber
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.messagebus.queue.inmemory.InMemoryAkkaQueue
import com.prisma.metrics.MetricsRegistry
import com.prisma.shared.messages.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.subscriptions.{SubscriptionDependencies, Webhook}
import com.prisma.workers.dependencies.WorkerDependencies
import com.prisma.workers.payloads.{Webhook => WorkerWebhook}

import scala.concurrent.ExecutionContext

case class PrismaNativeDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends DeployDependencies
    with ApiDependencies
    with WorkerDependencies
    with SubscriptionDependencies {

  override implicit def self                                    = this
  override implicit lazy val executionContext: ExecutionContext = system.dispatcher

  val config: PrismaConfig = ConfigLoader.load()
  val managementSecret     = config.managementApiSecret.getOrElse("")

  MetricsRegistry.init(deployConnector.cloudSecretPersistence)

  override lazy val apiSchemaBuilder = CachedSchemaBuilder(SchemaBuilder(), invalidationPubSub)
  override lazy val projectFetcher: ProjectFetcher = {
    val fetcher = SingleServerProjectFetcher(projectPersistence)
    CachedProjectFetcherImpl(fetcher, invalidationPubSub)(system.dispatcher)
  }

  override lazy val migrator: Migrator = AsyncMigrator(migrationPersistence, projectPersistence, deployConnector)
  override lazy val managementAuth = {
    config.managementApiSecret match {
      case Some(jwtSecret) if jwtSecret.nonEmpty => Auth.jna(Algorithm.HS256)
      case _                                     => println("[Warning] Management authentication is disabled. Enable it in your Prisma config to secure your server."); Auth.none()
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

  override lazy val webhookPublisher = webhooksQueue
  override lazy val webhooksConsumer = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)
  override lazy val httpClient       = SimpleHttpClient()
  override lazy val apiAuth          = Auth.jna(Algorithm.HS256)

  lazy val databaseConfig           = config.databases.head
  override lazy val deployConnector = PostgresDeployConnector(databaseConfig, isActive = databaseConfig.active)
//  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder
  override lazy val apiConnector = PostgresApiConnector(databaseConfig, isActive = databaseConfig.active)

  override def projectIdEncoder: ProjectIdEncoder = ???

  override lazy val functionValidator           = FunctionValidatorImpl()
  override lazy val sideEffectMutactionExecutor = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier           = DatabaseMutactionVerifierImpl

  lazy val telemetryActor = system.actorOf(Props(TelemetryActor(deployConnector)))
}
