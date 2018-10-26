package com.prisma.prod

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
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.schema.mutations.FunctionValidator
import com.prisma.deploy.server.TelemetryActor
import com.prisma.deploy.server.auth.{AsymmetricManagementAuth, DummyManagementAuth, SymmetricManagementAuth}
import com.prisma.image.{FunctionValidatorImpl, SingleServerProjectFetcher}
import com.prisma.messagebus._
import com.prisma.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import com.prisma.messagebus.queue.rabbit.RabbitQueue
import com.prisma.metrics.MetricsRegistry
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

  val config: PrismaConfig      = ConfigLoader.load()
  private val rabbitUri: String = config.rabbitUri.getOrElse("RabbitMQ URI required but not found in Prisma configuration.")

  override implicit def self: PrismaProdDependencies = this

  override lazy val apiSchemaBuilder = CachedSchemaBuilder(SchemaBuilder(), invalidationPubSub)
  override lazy val projectFetcher: ProjectFetcher = {
    val fetcher = SingleServerProjectFetcher(projectPersistence)
    CachedProjectFetcherImpl(fetcher, invalidationPubSub)
  }

  override lazy val migrator: Migrator = AsyncMigrator(migrationPersistence, projectPersistence, deployConnector)
  override lazy val managementAuth = {
    (config.managementApiSecret, config.legacySecret) match {
      case (Some(jwtSecret), _) if jwtSecret.nonEmpty => SymmetricManagementAuth(jwtSecret)
      case (_, Some(publicKey)) if publicKey.nonEmpty => AsymmetricManagementAuth(publicKey)
      case _                                          => DummyManagementAuth()
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
    RabbitQueue.consumer[WorkerWebhook](rabbitUri, "webhooks")(reporter, JsonConversions.webhookUnmarshaller)

  override lazy val httpClient                           = SimpleHttpClient()
  override lazy val apiAuth                              = AuthImpl
  override lazy val deployConnector: DeployConnector     = ConnectorLoader.loadDeployConnector(config)
  override lazy val functionValidator: FunctionValidator = FunctionValidatorImpl()

  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder

  override lazy val apiConnector                = ConnectorLoader.loadApiConnector(config)
  override lazy val sideEffectMutactionExecutor = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier           = DatabaseMutactionVerifierImpl

  lazy val telemetryActor = system.actorOf(Props(TelemetryActor(deployConnector)))

  override def initialize()(implicit ec: ExecutionContext): Unit = {
    super.initialize()(ec)
    MetricsRegistry.init(deployConnector.cloudSecretPersistence)
  }
}
