package cool.graph.client

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.amazonaws.services.s3.AmazonS3
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import cool.graph.aws.AwsInitializers
import cool.graph.aws.cloudwatch.{Cloudwatch, CloudwatchImpl}
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.finder.{CachedProjectFetcherImpl, ProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.messagebus.Conversions.{ByteMarshaller, ByteUnmarshaller, Unmarshallers}
import cool.graph.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.messagebus.{Conversions, PubSubPublisher, PubSubSubscriber, QueuePublisher}
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, KinesisPublisherImplementation, TestableTime, TestableTimeImplementation}
import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment, LiveEndpointResolver}
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
import cool.graph.util.ErrorHandlerFactory
import cool.graph.webhook.{Webhook, WebhookCaller, WebhookCallerImplementation}
import scaldi.Module

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.Try

trait ClientInjector {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val bugsnagger: BugSnagger
  implicit val dispatcher: ExecutionContext
  implicit val injector: ClientInjector
  implicit val toScaldi: Module

  val projectSchemaInvalidationSubscriber: PubSubSubscriber[String]
  val projectSchemaFetcher: RefreshableProjectFetcher
  val functionEnvironment: FunctionEnvironment
  val endpointResolver: EndpointResolver
  val logsPublisher: QueuePublisher[String]
  val webhookPublisher: QueuePublisher[Webhook]
  val webhookCaller: WebhookCaller
  val sssEventsPublisher: PubSubPublisher[String]
  val requestPrefix: String
  val cloudwatch: Cloudwatch
  val globalDatabaseManager: GlobalDatabaseManager
  val kinesisAlgoliaSyncQueriesPublisher: KinesisPublisher
  val kinesisApiMetricsPublisher: KinesisPublisher
  val featureMetricActor: ActorRef
  val apiMetricsMiddleware: ApiMetricsMiddleware
  val config: Config = ConfigFactory.load()
  val testableTime: TestableTime
  val apiMetricsFlushInterval: Int
  val clientAuth: ClientAuth
  val log: String => Unit
  val errorHandlerFactory: ErrorHandlerFactory
  val apiMatrixFactory: ApiMatrixFactory
  val globalApiEndpointManager: GlobalApiEndpointManager
  val s3: AmazonS3
  val s3Fileupload: AmazonS3
}

class ClientInjectorImpl(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ClientInjector with LazyLogging {
  implicit lazy val bugsnagger: BugSnagger               = BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))
  implicit lazy val dispatcher: ExecutionContextExecutor = system.dispatcher

  lazy val globalRabbitUri: String                      = sys.env.getOrElse("GLOBAL_RABBIT_URI", sys.error("GLOBAL_RABBIT_URI required for schema invalidation"))
  lazy val blockedProjectIds: Vector[String]            = Try { sys.env("BLOCKED_PROJECT_IDS").split(",").toVector }.getOrElse(Vector.empty)
  lazy val rabbitMQUri: String                          = sys.env("RABBITMQ_URI")
  lazy val fromStringMarshaller: ByteMarshaller[String] = Conversions.Marshallers.FromString
  lazy val globalDatabaseManager: GlobalDatabaseManager = GlobalDatabaseManager.initializeForSingleRegion(config)
  lazy val endpointResolver: EndpointResolver           = LiveEndpointResolver()
  lazy val logsPublisher: QueuePublisher[String]        = RabbitQueue.publisher[String](rabbitMQUri, "function-logs")(bugsnagger, fromStringMarshaller)
  lazy val requestPrefix: String                        = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))
  lazy val cloudwatch: Cloudwatch                       = CloudwatchImpl()
  lazy val featureMetricActor: ActorRef                 = system.actorOf(Props(new FeatureMetricActor(kinesisApiMetricsPublisher, apiMetricsFlushInterval)))
  lazy val apiMetricsMiddleware: ApiMetricsMiddleware   = new ApiMetricsMiddleware(testableTime, featureMetricActor)
  lazy val testableTime: TestableTime                   = new TestableTimeImplementation
  lazy val apiMetricsFlushInterval: Int                 = 10
  lazy val clientAuth: ClientAuth                       = ClientAuthImpl()
  lazy val log: String => Unit                          = (x: String) => logger.info(x)
  lazy val errorHandlerFactory                          = ErrorHandlerFactory(log, cloudwatch, bugsnagger)
  lazy val apiMatrixFactory                             = ApiMatrixFactory(DefaultApiMatrix)
  lazy val s3: AmazonS3                                 = AwsInitializers.createS3()
  lazy val s3Fileupload: AmazonS3                       = AwsInitializers.createS3Fileupload()
  lazy val webhookCaller: WebhookCaller                 = new WebhookCallerImplementation()
  lazy val webhookPublisher: QueuePublisher[Webhook]    = RabbitQueue.publisher(rabbitMQUri, "webhooks")(bugsnagger, Webhook.marshaller)
  lazy val serviceName: String                          = sys.env.getOrElse("SERVICE_NAME", "local")
  lazy val environment: String                          = sys.env.getOrElse("ENVIRONMENT", "local")

  lazy val projectSchemaInvalidationSubscriber: PubSubSubscriber[String] = {
    implicit val unmarshaller: ByteUnmarshaller[String] = Unmarshallers.ToString
    RabbitAkkaPubSub.subscriber[String](globalRabbitUri, "project-schema-invalidation", durable = true)
  }
  lazy val projectSchemaFetcher: RefreshableProjectFetcher = CachedProjectFetcherImpl(
    projectFetcher = ProjectFetcherImpl(blockedProjectIds, config),
    projectSchemaInvalidationSubscriber = projectSchemaInvalidationSubscriber
  )

  lazy val functionEnvironment: FunctionEnvironment = LambdaFunctionEnvironment(
    sys.env.getOrElse("LAMBDA_AWS_ACCESS_KEY_ID", "whatever"),
    sys.env.getOrElse("LAMBDA_AWS_SECRET_ACCESS_KEY", "whatever")
  )

  lazy val kinesis: AmazonKinesis = {
    val credentials = new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))
    AmazonKinesisClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  lazy val sssEventsPublisher: PubSubPublisher[String] =
    RabbitAkkaPubSub.publisher[String](rabbitMQUri, "sss-events", durable = true)(bugsnagger, fromStringMarshaller)
  lazy val kinesisAlgoliaSyncQueriesPublisher: KinesisPublisher =
    new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_ALGOLIA_SYNC_QUERY"), kinesis)
  lazy val kinesisApiMetricsPublisher: KinesisPublisher = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)

  lazy val globalApiEndpointManager = GlobalApiEndpointManager(
    euWest1 = sys.env("API_ENDPOINT_EU_WEST_1"),
    usWest2 = sys.env("API_ENDPOINT_US_WEST_2"),
    apNortheast1 = sys.env("API_ENDPOINT_AP_NORTHEAST_1")
  )

  implicit lazy val injector: ClientInjector = this

  implicit lazy val toScaldi: Module = {
    val outer = this
    new Module {
      binding identifiedBy "project-schema-fetcher" toNonLazy outer.projectSchemaFetcher
      binding identifiedBy "cloudwatch" toNonLazy outer.cloudwatch
      binding identifiedBy "kinesis" toNonLazy outer.kinesis
      binding identifiedBy "api-metrics-middleware" toNonLazy outer.apiMetricsMiddleware
      binding identifiedBy "featureMetricActor" to outer.featureMetricActor
      binding identifiedBy "s3" toNonLazy outer.s3
      binding identifiedBy "s3-fileupload" toNonLazy outer.s3Fileupload
      bind[EndpointResolver] identifiedBy "endpointResolver" toNonLazy outer.endpointResolver
      bind[QueuePublisher[String]] identifiedBy "logsPublisher" toNonLazy outer.logsPublisher
      bind[QueuePublisher[Webhook]] identifiedBy "webhookPublisher" toNonLazy outer.webhookPublisher
      bind[PubSubPublisher[String]] identifiedBy "sss-events-publisher" toNonLazy outer.sssEventsPublisher
      bind[String] identifiedBy "request-prefix" toNonLazy outer.requestPrefix
      bind[KinesisPublisher] identifiedBy "kinesisAlgoliaSyncQueriesPublisher" toNonLazy outer.kinesisAlgoliaSyncQueriesPublisher
      bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy outer.kinesisApiMetricsPublisher
      bind[FunctionEnvironment] toNonLazy outer.functionEnvironment
      bind[GlobalDatabaseManager] toNonLazy outer.globalDatabaseManager
      bind[BugSnagger] toNonLazy outer.bugsnagger

      bind[ClientAuth] toNonLazy outer.clientAuth
      bind[TestableTime] toNonLazy outer.testableTime
      bind[GlobalApiEndpointManager] toNonLazy outer.globalApiEndpointManager
      bind[WebhookCaller] toNonLazy outer.webhookCaller
      bind[ApiMatrixFactory] toNonLazy apiMatrixFactory

      binding identifiedBy "config" toNonLazy outer.config
      binding identifiedBy "actorSystem" toNonLazy outer.system
      binding identifiedBy "dispatcher" toNonLazy outer.dispatcher
      binding identifiedBy "actorMaterializer" toNonLazy outer.materializer
      binding identifiedBy "environment" toNonLazy outer.serviceName
      binding identifiedBy "service-name" toNonLazy outer.environment
    }
  }
}
