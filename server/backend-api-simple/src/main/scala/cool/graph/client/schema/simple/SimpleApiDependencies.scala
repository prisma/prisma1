package cool.graph.client.schema.simple

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import cool.graph.aws.AwsInitializers
import cool.graph.aws.cloudwatch.CloudwatchImpl
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.client.server.{GraphQlRequestHandler, GraphQlRequestHandlerImpl, ProjectSchemaBuilder}
import cool.graph.client._
import cool.graph.messagebus.Conversions.{ByteUnmarshaller, Unmarshallers}
import cool.graph.messagebus.pubsub.rabbit.{RabbitAkkaPubSub, RabbitAkkaPubSubSubscriber}
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.messagebus.{Conversions, PubSubPublisher, QueuePublisher}
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, KinesisPublisherImplementation}
import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, LiveEndpointResolver}
import cool.graph.webhook.Webhook

import scala.util.Try

class SimpleInjector(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ClientInjectorImpl {

  import system.dispatcher

  def toScaldi: CommonClientDependencies                       = SimpleApiDependencies()
  override implicit val commonModule: CommonClientDependencies = this.toScaldi

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
}

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
  lazy val projectSchemaInvalidationSubscriber: RabbitAkkaPubSubSubscriber[String] = {
    val globalRabbitUri                                 = sys.env("GLOBAL_RABBIT_URI")
    implicit val unmarshaller: ByteUnmarshaller[String] = Unmarshallers.ToString

    RabbitAkkaPubSub.subscriber[String](globalRabbitUri, "project-schema-invalidation", durable = true)
  }

  lazy val functionEnvironment = LambdaFunctionEnvironment(
    sys.env.getOrElse("LAMBDA_AWS_ACCESS_KEY_ID", "whatever"),
    sys.env.getOrElse("LAMBDA_AWS_SECRET_ACCESS_KEY", "whatever")
  )

  lazy val blockedProjectIds: Vector[String] = Try {
    sys.env("BLOCKED_PROJECT_IDS").split(",").toVector
  }.getOrElse(Vector.empty)

  lazy val projectSchemaFetcher: RefreshableProjectFetcher = CachedProjectFetcherImpl(
    projectFetcher = ProjectFetcherImpl(blockedProjectIds, config),
    projectSchemaInvalidationSubscriber = projectSchemaInvalidationSubscriber
  )

  lazy val kinesis: AmazonKinesis = {
    val credentials =
      new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonKinesisClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  lazy val clusterLocalRabbitUri              = sys.env("RABBITMQ_URI")
  lazy val fromStringMarshaller               = Conversions.Marshallers.FromString
  lazy val globalDatabaseManager              = GlobalDatabaseManager.initializeForSingleRegion(config)
  lazy val endpointResolver                   = LiveEndpointResolver()
  lazy val logsPublisher                      = RabbitQueue.publisher[String](clusterLocalRabbitUri, "function-logs")(bugSnagger, fromStringMarshaller)
  lazy val webhooksPublisher                  = RabbitQueue.publisher(clusterLocalRabbitUri, "webhooks")(bugSnagger, Webhook.marshaller)
  lazy val sssEventsPublisher                 = RabbitAkkaPubSub.publisher[String](sys.env("RABBITMQ_URI"), "sss-events", durable = true)(bugSnagger, fromStringMarshaller)
  lazy val requestPrefix                      = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))
  lazy val cloudwatch                         = CloudwatchImpl()
  lazy val kinesisAlgoliaSyncQueriesPublisher = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_ALGOLIA_SYNC_QUERY"), kinesis)
  lazy val kinesisApiMetricsPublisher         = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
  lazy val featureMetricActor                 = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
  lazy val apiMetricsMiddleware               = new ApiMetricsMiddleware(testableTime, featureMetricActor)

  binding identifiedBy "project-schema-fetcher" toNonLazy projectSchemaFetcher
  binding identifiedBy "cloudwatch" toNonLazy cloudwatch
  binding identifiedBy "kinesis" toNonLazy kinesis
  binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
  binding identifiedBy "featureMetricActor" to featureMetricActor
  binding identifiedBy "s3" toNonLazy AwsInitializers.createS3()
  binding identifiedBy "s3-fileupload" toNonLazy AwsInitializers.createS3Fileupload()

  bind[FunctionEnvironment] toNonLazy functionEnvironment
  bind[EndpointResolver] identifiedBy "endpointResolver" toNonLazy endpointResolver
  bind[QueuePublisher[String]] identifiedBy "logsPublisher" toNonLazy logsPublisher
  bind[QueuePublisher[Webhook]] identifiedBy "webhookPublisher" toNonLazy webhooksPublisher
  bind[PubSubPublisher[String]] identifiedBy "sss-events-publisher" toNonLazy sssEventsPublisher
  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
  bind[GlobalDatabaseManager] toNonLazy globalDatabaseManager
  bind[KinesisPublisher] identifiedBy "kinesisAlgoliaSyncQueriesPublisher" toNonLazy kinesisAlgoliaSyncQueriesPublisher
  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy kinesisApiMetricsPublisher
}
