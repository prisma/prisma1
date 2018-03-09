package com.prisma.local

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.SimpleHttpClient
import com.prisma.api.ApiDependencies
import com.prisma.api.database.Databases
import com.prisma.api.project.{CachedProjectFetcherImpl, ProjectFetcher}
import com.prisma.api.schema.{CachedSchemaBuilder, SchemaBuilder}
import com.prisma.auth.AuthImpl
import com.prisma.subscriptions.Webhook
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.mysql.MySqlDeployConnector
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.schema.mutations.FunctionValidator
import com.prisma.deploy.server.{ClusterAuthImpl, DummyClusterAuth}
import com.prisma.graphql.GraphQlClient
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.messagebus.queue.inmemory.InMemoryAkkaQueue
import com.prisma.messagebus.{PubSubPublisher, PubSubSubscriber, QueueConsumer, QueuePublisher}
import com.prisma.subscriptions.{SubscriptionDependencies, Webhook}
import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import com.prisma.subscriptions.protocol.SubscriptionRequest
import com.prisma.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.websocket.protocol.{Request => WebsocketRequest}
import com.prisma.workers.dependencies.WorkerDependencies
import com.prisma.workers.payloads.{Webhook => WorkerWebhook}
import play.api.libs.json.Json

case class PrismaLocalDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends DeployDependencies
    with ApiDependencies
    with WorkerDependencies
    with SubscriptionDependencies {
  override implicit def self = this

  override val databases        = Databases.initialize(config)
  override val apiSchemaBuilder = CachedSchemaBuilder(SchemaBuilder(), invalidationPubSub)
  override val projectFetcher: ProjectFetcher = {
    val fetcher = SingleServerProjectFetcher(projectPersistence)
    CachedProjectFetcherImpl(fetcher, invalidationPubSub)
  }

  override val migrator: Migrator = AsyncMigrator(migrationPersistence, projectPersistence, deployPersistencePlugin)
  override val clusterAuth = {
    sys.env.get("CLUSTER_PUBLIC_KEY") match {
      case Some(publicKey) if publicKey.nonEmpty => ClusterAuthImpl(publicKey)
      case _                                     => DummyClusterAuth()
    }
  }

  lazy val invalidationPubSub: InMemoryAkkaPubSub[String] = InMemoryAkkaPubSub[String]()

  override lazy val invalidationPublisher = invalidationPubSub
  override lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] =
    invalidationPubSub.map[SchemaInvalidatedMessage]((str: String) => SchemaInvalidated)

  override lazy val sssEventsPubSub: InMemoryAkkaPubSub[String]   = InMemoryAkkaPubSub[String]()
  override lazy val sssEventsSubscriber: PubSubSubscriber[String] = sssEventsPubSub

  lazy val requestsQueue: InMemoryAkkaQueue[WebsocketRequest]                = InMemoryAkkaQueue[WebsocketRequest]()
  override lazy val requestsQueuePublisher: QueuePublisher[WebsocketRequest] = requestsQueue
  override lazy val requestsQueueConsumer: QueueConsumer[SubscriptionRequest] =
    requestsQueue.map[SubscriptionRequest](Converters.websocketRequest2SubscriptionRequest)

  lazy val responsePubSub: InMemoryAkkaPubSub[String]                  = InMemoryAkkaPubSub[String]()
  override lazy val responsePubSubSubscriber: PubSubSubscriber[String] = responsePubSub

  lazy val converterResponse07ToString: SubscriptionSessionResponse => String = (response: SubscriptionSessionResponse) => {
    import com.prisma.subscriptions.protocol.ProtocolV07.SubscriptionResponseWriters._
    Json.toJson(response).toString
  }

  lazy val converterResponse05ToString: SubscriptionSessionResponseV05 => String = (response: SubscriptionSessionResponseV05) => {
    import com.prisma.subscriptions.protocol.ProtocolV05.SubscriptionResponseWriters._
    Json.toJson(response).toString
  }

  lazy val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05] =
    responsePubSub.map[SubscriptionSessionResponseV05](converterResponse05ToString)
  lazy val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse] =
    responsePubSub.map[SubscriptionSessionResponse](converterResponse07ToString)

  override val keepAliveIntervalSeconds = 10

  lazy val webhooksQueue = InMemoryAkkaQueue[Webhook]()

  override lazy val webhookPublisher = webhooksQueue
  override lazy val webhooksConsumer = webhooksQueue.map[WorkerWebhook](Converters.apiWebhook2WorkerWebhook)
  override lazy val httpClient       = SimpleHttpClient()

  override def apiAuth = AuthImpl

  override def deployPersistencePlugin: DeployConnector = MySqlDeployConnector(databases.master)(system.dispatcher)
  override def functionValidator: FunctionValidator = FunctionValidatorImpl()
}
