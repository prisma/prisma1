package cool.graph.client

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.finder.ProjectFetcher
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.cloudwatch.Cloudwatch
import cool.graph.messagebus.{PubSubPublisher, PubSubSubscriber, QueuePublisher}
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, TestableTime, TestableTimeImplementation}
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment}
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
import cool.graph.util.ErrorHandlerFactory
import cool.graph.webhook.{Webhook, WebhookCaller, WebhookCallerImplementation}
import scaldi.Module

import scala.util.Try

trait CommonClientDependencies extends Module with LazyLogging {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val bugSnagger = BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))

  val projectSchemaInvalidationSubscriber: PubSubSubscriber[String]
  val projectSchemaFetcher: ProjectFetcher
  val functionEnvironment: FunctionEnvironment
  val endpointResolver: EndpointResolver
  val logsPublisher: QueuePublisher[String]
  val webhooksPublisher: QueuePublisher[Webhook]
  val sssEventsPublisher: PubSubPublisher[String]
  val requestPrefix: String
  val cloudwatch: Cloudwatch
  val globalDatabaseManager: GlobalDatabaseManager
  val kinesisAlgoliaSyncQueriesPublisher: KinesisPublisher
  val kinesisApiMetricsPublisher: KinesisPublisher
  val featureMetricActor: ActorRef
  val apiMetricsMiddleware: ApiMetricsMiddleware

  lazy val config: Config          = ConfigFactory.load()
  lazy val testableTime            = new TestableTimeImplementation
  lazy val apiMetricsFlushInterval = 10
  lazy val clientAuth              = ClientAuthImpl()
  lazy val log                     = (x: String) => logger.info(x)
  lazy val errorHandlerFactory     = ErrorHandlerFactory(log, cloudwatch, bugSnagger)
  lazy val apiMatrixFactory        = ApiMatrixFactory(DefaultApiMatrix)

  lazy val globalApiEndpointManager = GlobalApiEndpointManager(
    euWest1 = sys.env("API_ENDPOINT_EU_WEST_1"),
    usWest2 = sys.env("API_ENDPOINT_US_WEST_2"),
    apNortheast1 = sys.env("API_ENDPOINT_AP_NORTHEAST_1")
  )

  bind[ClientAuth] toNonLazy clientAuth
  bind[TestableTime] toNonLazy testableTime
  bind[GlobalApiEndpointManager] toNonLazy globalApiEndpointManager
  bind[WebhookCaller] toNonLazy new WebhookCallerImplementation()
  bind[BugSnagger] toNonLazy bugSnagger
  bind[ClientAuth] toNonLazy clientAuth
  bind[TestableTime] toNonLazy testableTime
  bind[ApiMatrixFactory] toNonLazy apiMatrixFactory
  bind[WebhookCaller] toNonLazy new WebhookCallerImplementation()
  bind[BugSnagger] toNonLazy bugSnagger

  binding identifiedBy "s3" toNonLazy createS3()
  binding identifiedBy "s3-fileupload" toNonLazy createS3Fileupload()
  binding identifiedBy "config" toNonLazy config
  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
  binding identifiedBy "actorMaterializer" toNonLazy materializer
  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")

  private lazy val blockedProjectIds: Vector[String] = Try {
    sys.env("BLOCKED_PROJECT_IDS").split(",").toVector
  }.getOrElse(Vector.empty)

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
}
