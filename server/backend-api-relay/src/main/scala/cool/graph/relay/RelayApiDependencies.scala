package cool.graph.relay

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import cool.graph.aws.AwsInitializers
import cool.graph.aws.cloudwatch.CloudwatchImpl
import cool.graph.client.database.{DeferredResolverProvider, RelayManyModelDeferredResolver, RelayToManyDeferredResolver}
import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.client.server.{GraphQlRequestHandler, GraphQlRequestHandlerImpl, ProjectSchemaBuilder}
import cool.graph.client.{CommonClientDependencies, FeatureMetric, FeatureMetricActor, UserContext}
import cool.graph.messagebus.Conversions.{ByteUnmarshaller, Unmarshallers}
import cool.graph.messagebus.pubsub.rabbit.{RabbitAkkaPubSub, RabbitAkkaPubSubSubscriber}
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.messagebus.{Conversions, PubSubPublisher, QueuePublisher}
import cool.graph.relay.schema.RelaySchemaBuilder
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, KinesisPublisherImplementation}
import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, LiveEndpointResolver}
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

  val kinesis: AmazonKinesis = {
    val credentials =
      new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonKinesisClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  val clusterLocalRabbitUri              = sys.env("RABBITMQ_URI")
  val globalDatabaseManager              = GlobalDatabaseManager.initializeForSingleRegion(config)
  val fromStringMarshaller               = Conversions.Marshallers.FromString
  val endpointResolver                   = LiveEndpointResolver()
  val logsPublisher                      = RabbitQueue.publisher[String](clusterLocalRabbitUri, "function-logs")(bugSnagger, fromStringMarshaller)
  val webhooksPublisher                  = RabbitQueue.publisher(clusterLocalRabbitUri, "webhooks")(bugSnagger, Webhook.marshaller)
  val sssEventsPublisher                 = RabbitAkkaPubSub.publisher[String](clusterLocalRabbitUri, "sss-events", durable = true)(bugSnagger, fromStringMarshaller)
  val requestPrefix                      = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))
  val cloudwatch                         = CloudwatchImpl()
  val kinesisAlgoliaSyncQueriesPublisher = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_ALGOLIA_SYNC_QUERY"), kinesis)
  val kinesisApiMetricsPublisher         = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
  val featureMetricActor                 = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
  val apiMetricsMiddleware               = new ApiMetricsMiddleware(testableTime, featureMetricActor)

  binding identifiedBy "project-schema-fetcher" toNonLazy projectSchemaFetcher
  binding identifiedBy "cloudwatch" toNonLazy cloudwatch
  binding identifiedBy "kinesis" toNonLazy kinesis
  binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
  binding identifiedBy "featureMetricActor" to featureMetricActor
  binding identifiedBy "s3" toNonLazy AwsInitializers.createS3()
  binding identifiedBy "s3-fileupload" toNonLazy AwsInitializers.createS3Fileupload()

  bind[GlobalDatabaseManager] toNonLazy globalDatabaseManager
  bind[FunctionEnvironment] toNonLazy functionEnvironment
  bind[EndpointResolver] identifiedBy "endpointResolver" toNonLazy endpointResolver
  bind[QueuePublisher[String]] identifiedBy "logsPublisher" toNonLazy logsPublisher
  bind[QueuePublisher[Webhook]] identifiedBy "webhookPublisher" toNonLazy webhooksPublisher
  bind[PubSubPublisher[String]] identifiedBy "sss-events-publisher" toNonLazy sssEventsPublisher
  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
  bind[KinesisPublisher] identifiedBy "kinesisAlgoliaSyncQueriesPublisher" toNonLazy kinesisAlgoliaSyncQueriesPublisher
  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy kinesisApiMetricsPublisher

}
