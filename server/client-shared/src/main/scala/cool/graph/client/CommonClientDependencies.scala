package cool.graph.client

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSAsyncClientBuilder}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.finder.ProjectFetcher
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.cloudwatch.CloudwatchImpl
import cool.graph.messagebus.PubSubPublisher
import cool.graph.messagebus.{PubSubSubscriber, QueuePublisher}
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, KinesisPublisherImplementation, TestableTime, TestableTimeImplementation}
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment}
import cool.graph.util.ErrorHandlerFactory
import cool.graph.webhook.{Webhook, WebhookCaller, WebhookCallerImplementation}
import scaldi.Module

import scala.util.Try

trait CommonClientDependencies extends Module with LazyLogging {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val bugSnagger = BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))

  lazy val config: Config          = ConfigFactory.load()
  lazy val testableTime            = new TestableTimeImplementation
  lazy val apiMetricsFlushInterval = 10

  lazy val kinesis: AmazonKinesis = {
    val credentials =
      new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonKinesisClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  val projectSchemaInvalidationSubscriber: PubSubSubscriber[String]
  val projectSchemaFetcher: ProjectFetcher
  val functionEnvironment: FunctionEnvironment
  val endpointResolver: EndpointResolver
  val logsPublisher: QueuePublisher[String]
  val webhooksPublisher: QueuePublisher[Webhook]
  val sssEventsPublisher: PubSubPublisher[String]
  val requestPrefix: String

  lazy val clientAuth            = ClientAuthImpl()
  lazy val apiMetricsPublisher   = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
  lazy val featureMetricActor    = system.actorOf(Props(new FeatureMetricActor(apiMetricsPublisher, apiMetricsFlushInterval)))
  lazy val apiMetricsMiddleware  = new ApiMetricsMiddleware(testableTime, featureMetricActor)
  lazy val log                   = (x: String) => logger.info(x)
  lazy val cloudWatch            = CloudwatchImpl()
  lazy val errorHandlerFactory   = ErrorHandlerFactory(log, cloudWatch, bugSnagger)
  lazy val globalDatabaseManager = GlobalDatabaseManager.initializeForSingleRegion(config)
  lazy val globalApiEndpointManager = GlobalApiEndpointManager(
    euWest1 = sys.env("API_ENDPOINT_EU_WEST_1"),
    usWest2 = sys.env("API_ENDPOINT_US_WEST_2"),
    apNortheast1 = sys.env("API_ENDPOINT_AP_NORTHEAST_1")
  )
  lazy val apiMatrixFactory = ApiMatrixFactory(DefaultApiMatrix(_))

  bind[GlobalDatabaseManager] toNonLazy globalDatabaseManager
  bind[GlobalApiEndpointManager] toNonLazy globalApiEndpointManager
  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy apiMetricsPublisher
  bind[WebhookCaller] toNonLazy new WebhookCallerImplementation()
  bind[BugSnagger] toNonLazy bugSnagger
  bind[ClientAuth] toNonLazy clientAuth
  bind[TestableTime] toNonLazy testableTime
  bind[ApiMatrixFactory] toNonLazy apiMatrixFactory

  binding identifiedBy "kinesis" toNonLazy kinesis
  binding identifiedBy "cloudwatch" toNonLazy cloudWatch
  binding identifiedBy "s3" toNonLazy createS3()
  binding identifiedBy "s3-fileupload" toNonLazy createS3Fileupload()
  binding identifiedBy "config" toNonLazy config
  binding identifiedBy "sns" toNonLazy createSystemSns()
  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
  binding identifiedBy "actorMaterializer" toNonLazy materializer
  binding identifiedBy "featureMetricActor" to featureMetricActor
  binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")

  bind[KinesisPublisher] identifiedBy "kinesisAlgoliaSyncQueriesPublisher" toNonLazy new KinesisPublisherImplementation(
    streamName = sys.env("KINESIS_STREAM_ALGOLIA_SYNC_QUERY"),
    kinesis
  )

  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy apiMetricsPublisher

  bind[WebhookCaller] toNonLazy new WebhookCallerImplementation()
  bind[BugSnagger] toNonLazy bugSnagger

  binding identifiedBy "featureMetricActor" to featureMetricActor
  binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)

  private lazy val blockedProjectIds: Vector[String] = Try {
    sys.env("BLOCKED_PROJECT_IDS").split(",").toVector
  }.getOrElse(Vector.empty)

  bind[ClientAuth] toNonLazy clientAuth
  bind[TestableTime] toNonLazy testableTime

  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")

  private def createS3(): AmazonS3 = {
    val credentials = new BasicAWSCredentials(
      sys.env("AWS_ACCESS_KEY_ID"),
      sys.env("AWS_SECRET_ACCESS_KEY")
    )

    AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("FILEUPLOAD_S3_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  // this is still in old SBS AWS account
  private def createS3Fileupload(): AmazonS3 = {
    val credentials = new BasicAWSCredentials(
      sys.env("FILEUPLOAD_S3_AWS_ACCESS_KEY_ID"),
      sys.env("FILEUPLOAD_S3_AWS_SECRET_ACCESS_KEY")
    )

    AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("FILEUPLOAD_S3_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  private def createSystemSns(): AmazonSNS = {
    val credentials =
      new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonSNSAsyncClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("SNS_ENDPOINT_SYSTEM"), sys.env("AWS_REGION")))
      .build
  }
}
