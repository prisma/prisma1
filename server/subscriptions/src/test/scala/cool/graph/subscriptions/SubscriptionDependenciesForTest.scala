package cool.graph.subscriptions
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.Databases
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder
import cool.graph.bugsnag.{BugSnagger, BugSnaggerMock}
import cool.graph.messagebus.testkits.{InMemoryPubSubTestKit, InMemoryQueueTestKit}
import cool.graph.messagebus.{PubSubPublisher, PubSubSubscriber, QueueConsumer, QueuePublisher}
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.{Converters, SubscriptionRequest}
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import cool.graph.websocket.protocol.Request

class SubscriptionDependenciesForTest()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SubscriptionDependencies {
  override implicit def self: ApiDependencies = this

  override implicit lazy val bugSnagger: BugSnagger = BugSnaggerMock

  lazy val invalidationTestKit   = InMemoryPubSubTestKit[String]()
  lazy val sssEventsTestKit      = InMemoryPubSubTestKit[String]()
  lazy val responsePubSubTestKit = InMemoryPubSubTestKit[String]()
  lazy val requestsQueueTestKit  = InMemoryQueueTestKit[SubscriptionRequest]()

  override val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = {
    invalidationTestKit.map[SchemaInvalidatedMessage]((_: String) => SchemaInvalidated)
  }

  override lazy val sssEventsPublisher: PubSubPublisher[String] = sssEventsTestKit
  override val sssEventsSubscriber: PubSubSubscriber[String]    = sssEventsTestKit

  override val responsePubSubPublisherV05: PubSubPublisher[SubscriptionSessionResponseV05] = {
    responsePubSubTestKit.map[SubscriptionSessionResponseV05](Converters.converterResponse05ToString)
  }
  override val responsePubSubPublisherV07: PubSubPublisher[SubscriptionSessionResponse] = {
    responsePubSubTestKit.map[SubscriptionSessionResponse](Converters.converterResponse07ToString)
  }
  override def responsePubSubSubscriber: PubSubSubscriber[String] = responsePubSubTestKit

  override def requestsQueuePublisher: QueuePublisher[Request] = requestsQueueTestKit.map[Request] { req: Request =>
    SubscriptionRequest(req.sessionId, req.projectId, req.body)
  }
  override val requestsQueueConsumer: QueueConsumer[SubscriptionRequest] = requestsQueueTestKit

  val projectFetcherPort = 12345
  val projectFetcherPath = "project-fetcher"
  override val projectFetcher: ProjectFetcher = {
    ProjectFetcherImpl(Vector.empty,
                       config,
                       schemaManagerEndpoint = s"http://localhost:${projectFetcherPort}/${projectFetcherPath}",
                       schemaManagerSecret = "empty")
  }
  override lazy val apiSchemaBuilder: SchemaBuilder = ???
  override val databases: Databases                 = Databases.initialize(config)
  override lazy val sssEventsPubSub                 = ???

}
