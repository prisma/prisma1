package cool.graph.client.schema.simple

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.client.server.{GraphQlRequestHandler, GraphQlRequestHandlerImpl, ProjectSchemaBuilder}
import cool.graph.client.{CommonClientDependencies, FeatureMetric, UserContext}
import cool.graph.messagebus.Conversions.{ByteUnmarshaller, Unmarshallers}
import cool.graph.messagebus.{Conversions, PubSubPublisher, QueuePublisher}
import cool.graph.messagebus.pubsub.rabbit.{RabbitAkkaPubSub, RabbitAkkaPubSubSubscriber}
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, LiveEndpointResolver}
import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import cool.graph.webhook.Webhook

import scala.util.Try

trait SimpleApiClientDependencies extends CommonClientDependencies {
  import system.dispatcher

  val simpleDeferredResolver: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new SimpleToManyDeferredResolver, new SimpleManyModelDeferredResolver)

  val simpleProjectSchemaBuilder = ProjectSchemaBuilder(project => new SimpleSchemaBuilder(project).build())

  val simpleGraphQlRequestHandler = GraphQlRequestHandlerImpl(
    errorHandlerFactory = errorHandlerFactory,
    log = log,
    apiVersionMetric = FeatureMetric.ApiSimple,
    apiMetricsMiddleware = apiMetricsMiddleware,
    deferredResolver = simpleDeferredResolver
  )

  bind[GraphQlRequestHandler] identifiedBy "simple-gql-request-handler" toNonLazy simpleGraphQlRequestHandler
  bind[ProjectSchemaBuilder] identifiedBy "simple-schema-builder" toNonLazy simpleProjectSchemaBuilder
}

case class SimpleApiDependencies(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SimpleApiClientDependencies {
  val projectSchemaInvalidationSubscriber: RabbitAkkaPubSubSubscriber[String] = {
    val globalRabbitUri                                 = sys.env("GLOBAL_RABBIT_URI")
    implicit val unmarshaller: ByteUnmarshaller[String] = Unmarshallers.ToString

    RabbitAkkaPubSub.subscriber[String](globalRabbitUri, "project-schema-invalidation", durable = true)
  }

  val functionEnvironment = LambdaFunctionEnvironment(
    sys.env.getOrElse("LAMBDA_AWS_ACCESS_KEY_ID", "whatever"),
    sys.env.getOrElse("LAMBDA_AWS_SECRET_ACCESS_KEY", "whatever")
  )

  val blockedProjectIds: Vector[String] = Try {
    sys.env("BLOCKED_PROJECT_IDS").split(",").toVector
  }.getOrElse(Vector.empty)

  val projectSchemaFetcher: RefreshableProjectFetcher = CachedProjectFetcherImpl(
    projectFetcher = ProjectFetcherImpl(blockedProjectIds, config),
    projectSchemaInvalidationSubscriber = projectSchemaInvalidationSubscriber
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
