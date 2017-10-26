package cool.graph.relay

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.client.database.{DeferredResolverProvider, RelayManyModelDeferredResolver, RelayToManyDeferredResolver}
import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.client.server.{GraphQlRequestHandler, GraphQlRequestHandlerImpl, ProjectSchemaBuilder}
import cool.graph.messagebus.Conversions.{ByteUnmarshaller, Unmarshallers}
import cool.graph.messagebus.pubsub.rabbit.{RabbitAkkaPubSub, RabbitAkkaPubSubSubscriber}
import cool.graph.client.{CommonClientDependencies, FeatureMetric, UserContext}
import cool.graph.messagebus.{Conversions, PubSubPublisher, QueuePublisher}
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.relay.schema.RelaySchemaBuilder
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, LiveEndpointResolver}
import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import cool.graph.webhook.Webhook

import scala.util.Try

trait RelayApiClientDependencies extends CommonClientDependencies {
  import system.dispatcher

  val relayDeferredResolver: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new RelayToManyDeferredResolver, new RelayManyModelDeferredResolver)

  val relayProjectSchemaBuilder = ProjectSchemaBuilder(project => new RelaySchemaBuilder(project).build())

  val relayGraphQlRequestHandler = GraphQlRequestHandlerImpl(
    errorHandlerFactory = errorHandlerFactory,
    log = log,
    apiVersionMetric = FeatureMetric.ApiRelay,
    apiMetricsMiddleware = apiMetricsMiddleware,
    deferredResolver = relayDeferredResolver
  )

  bind[GraphQlRequestHandler] identifiedBy "relay-gql-request-handler" toNonLazy relayGraphQlRequestHandler
  bind[ProjectSchemaBuilder] identifiedBy "relay-schema-builder" toNonLazy relayProjectSchemaBuilder
}

case class RelayApiDependencies(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends RelayApiClientDependencies {
  val projectSchemaInvalidationSubscriber: RabbitAkkaPubSubSubscriber[String] = {
    val globalRabbitUri                                 = sys.env("GLOBAL_RABBIT_URI")
    implicit val unmarshaller: ByteUnmarshaller[String] = Unmarshallers.ToString

    RabbitAkkaPubSub.subscriber[String](globalRabbitUri, "project-schema-invalidation", durable = true)
  }

  lazy val blockedProjectIds: Vector[String] = Try {
    sys.env("BLOCKED_PROJECT_IDS").split(",").toVector
  }.getOrElse(Vector.empty)

  val projectSchemaFetcher: RefreshableProjectFetcher = CachedProjectFetcherImpl(
    projectFetcher = ProjectFetcherImpl(blockedProjectIds, config),
    projectSchemaInvalidationSubscriber = projectSchemaInvalidationSubscriber
  )

  val functionEnvironment = LambdaFunctionEnvironment(
    sys.env.getOrElse("LAMBDA_AWS_ACCESS_KEY_ID", "whatever"),
    sys.env.getOrElse("LAMBDA_AWS_SECRET_ACCESS_KEY", "whatever")
  )

  val fromStringMarshaller = Conversions.Marshallers.FromString

  val endpointResolver   = LiveEndpointResolver()
  val logsPublisher      = RabbitQueue.publisher[String](sys.env("RABBITMQ_URI"), "function-logs")(bugSnagger, fromStringMarshaller)
  val webhooksPublisher  = RabbitQueue.publisher(sys.env("RABBITMQ_URI"), "webhooks")(bugSnagger, Webhook.marshaller)
  val sssEventsPublisher = RabbitAkkaPubSub.publisher[String](sys.env("RABBITMQ_URI"), "sss-events", durable = true)(bugSnagger, fromStringMarshaller)
  val requestPrefix = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))
  
  binding identifiedBy "project-schema-fetcher" toNonLazy projectSchemaFetcher

  bind[FunctionEnvironment] toNonLazy functionEnvironment
  bind[EndpointResolver] identifiedBy "endpointResolver" toNonLazy endpointResolver
  bind[QueuePublisher[String]] identifiedBy "logsPublisher" toNonLazy logsPublisher
  bind[QueuePublisher[Webhook]] identifiedBy "webhookPublisher" toNonLazy webhooksPublisher
  bind[PubSubPublisher[String]] identifiedBy "sss-events-publisher" toNonLazy sssEventsPublisher
  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
}
