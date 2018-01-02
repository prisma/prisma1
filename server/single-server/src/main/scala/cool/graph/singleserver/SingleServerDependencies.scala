package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.Databases
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder
import cool.graph.deploy.DeployDependencies
import cool.graph.deploy.migration.migrator.{AsyncMigrator, Migrator}
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
import cool.graph.messagebus.{PubSubPublisher, PubSubSubscriber, QueueConsumer}
import cool.graph.subscriptions.SubscriptionDependencies
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import cool.graph.websocket.protocol.{Request => WebsocketRequest}
import cool.graph.websocket.services.WebsocketDevDependencies
import play.api.libs.json.Json

trait SingleServerApiDependencies extends DeployDependencies with ApiDependencies {
  override implicit def self: SingleServerDependencies
}

case class SingleServerDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends SingleServerApiDependencies
    with SubscriptionDependencies {
  override implicit def self = this

  val databases                      = Databases.initialize(config)
  val apiSchemaBuilder               = SchemaBuilder()
  val projectFetcher: ProjectFetcher = ProjectFetcherImpl(Vector.empty, config)
  val migrator: Migrator             = AsyncMigrator(clientDb, migrationPersistence, projectPersistence, migrationApplier)

  lazy val pubSub: InMemoryAkkaPubSub[String]                                 = InMemoryAkkaPubSub[String]()
  lazy val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = pubSub.map[SchemaInvalidatedMessage]((str: String) => SchemaInvalidated)

  lazy val sssEventsPubSub: InMemoryAkkaPubSub[String]   = InMemoryAkkaPubSub[String]()
  lazy val sssEventsSubscriber: PubSubSubscriber[String] = sssEventsPubSub

  lazy val requestsQueue: InMemoryAkkaQueue[WebsocketRequest]        = InMemoryAkkaQueue[WebsocketRequest]()
  lazy val requestsQueueConsumer: QueueConsumer[SubscriptionRequest] = requestsQueue.map[SubscriptionRequest](Converters.websocketRequest2SubscriptionRequest)

  lazy val responsePubSub: InMemoryAkkaPubSub[String] = InMemoryAkkaPubSub[String]()
  lazy val websocketServices                          = WebsocketDevDependencies(requestsQueue, responsePubSub)

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
