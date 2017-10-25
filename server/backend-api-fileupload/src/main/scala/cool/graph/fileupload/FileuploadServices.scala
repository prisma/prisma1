package cool.graph.fileupload

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.ConfigFactory
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.client._
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.finder.ProjectFetcherImpl
import cool.graph.client.metrics.ApiMetricsMiddleware
import cool.graph.cloudwatch.CloudwatchImpl
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.{KinesisPublisher, KinesisPublisherImplementation, TestableTime, TestableTimeImplementation}
import scaldi.Module

class FileuploadServices(implicit _system: ActorRefFactory, system: ActorSystem, implicit val materializer: akka.stream.ActorMaterializer) extends Module {
  lazy val config                  = ConfigFactory.load()
  lazy val testableTime            = new TestableTimeImplementation
  lazy val apiMetricsFlushInterval = 10
  lazy val kinesis                 = createKinesis()
  lazy val apiMetricsPublisher     = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
  lazy val featureMetricActor      = system.actorOf(Props(new FeatureMetricActor(apiMetricsPublisher, apiMetricsFlushInterval)))
  lazy val clientAuth              = ClientAuthImpl()

  bind[GlobalDatabaseManager] toNonLazy GlobalDatabaseManager.initializeForSingleRegion(config)
  bind[GlobalApiEndpointManager] toNonLazy createGlobalApiEndpointManager
  binding identifiedBy "kinesis" toNonLazy kinesis
  binding identifiedBy "cloudwatch" toNonLazy CloudwatchImpl()
  binding identifiedBy "s3-fileupload" toNonLazy createS3()
  binding identifiedBy "config" toNonLazy config
  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
  binding identifiedBy "actorMaterializer" toNonLazy materializer

  bind[TestableTime] toNonLazy new TestableTimeImplementation
  bind[BugSnagger] toNonLazy BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))

  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy new KinesisPublisherImplementation(
    streamName = sys.env("KINESIS_STREAM_API_METRICS"),
    kinesis
  )
  bind[ClientAuth] toNonLazy clientAuth

  binding identifiedBy "featureMetricActor" to featureMetricActor
  binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
  binding identifiedBy "project-schema-fetcher" toNonLazy ProjectFetcherImpl(blockedProjectIds = Vector.empty, config)
  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")

  private def createGlobalApiEndpointManager = {
    GlobalApiEndpointManager(
      euWest1 = sys.env("API_ENDPOINT_EU_WEST_1"),
      usWest2 = sys.env("API_ENDPOINT_US_WEST_2"),
      apNortheast1 = sys.env("API_ENDPOINT_AP_NORTHEAST_1")
    )
  }

  private def createS3(): AmazonS3 = {
    val credentials = new BasicAWSCredentials(
      sys.env("FILEUPLOAD_S3_AWS_ACCESS_KEY_ID"),
      sys.env("FILEUPLOAD_S3_AWS_SECRET_ACCESS_KEY")
    )

    AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("FILEUPLOAD_S3_ENDPOINT"), sys.env("FILEUPLOAD_AWS_REGION")))
      .build
  }

  private def createKinesis(): AmazonKinesis = {
    val credentials =
      new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonKinesisClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build()
  }
}
