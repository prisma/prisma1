package cool.graph.fileupload

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.client._
import cool.graph.client.server.{GraphQlRequestHandler, ProjectSchemaBuilder}
import cool.graph.messagebus.{PubSubPublisher, QueuePublisher}
import cool.graph.shared.ApiMatrixFactory
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.externalServices.KinesisPublisher
import cool.graph.shared.functions.{EndpointResolver, FunctionEnvironment}
import cool.graph.webhook.Webhook
import scaldi.Module

case class FileUploadInjector(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ClientInjectorImpl {

  override implicit lazy val injector: FileUploadInjector = this
  implicit lazy val toScaldi: Module = {
    val outer = this
    new Module {
      binding identifiedBy "config" toNonLazy outer.config
      binding identifiedBy "actorSystem" toNonLazy outer.system
      binding identifiedBy "dispatcher" toNonLazy outer.system.dispatcher
      binding identifiedBy "actorMaterializer" toNonLazy outer.materializer
      bind[GraphQlRequestHandler] identifiedBy "simple-gql-request-handler" toNonLazy outer.graphQlRequestHandler
      bind[ProjectSchemaBuilder] identifiedBy "simple-schema-builder" toNonLazy outer.projectSchemaBuilder
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
      bind[GlobalDatabaseManager] toNonLazy outer.globalDatabaseManager
      bind[ApiMatrixFactory] toNonLazy outer.apiMatrixFactory
      bind[FunctionEnvironment] toNonLazy outer.functionEnvironment
    }
  }

  lazy val deferredResolver      = ???
  lazy val projectSchemaBuilder  = ???
  lazy val graphQlRequestHandler = ???
}
//
//class FileuploadServices(implicit _system: ActorRefFactory, system: ActorSystem, implicit val materializer: akka.stream.ActorMaterializer) extends Module {
//
//  override lazy val injector = FileUploadInjector
//
//  lazy val config                  = ConfigFactory.load()
//  lazy val testableTime            = new TestableTimeImplementation
//  lazy val apiMetricsFlushInterval = 10
//  lazy val kinesis                 = createKinesis()
//  lazy val apiMetricsPublisher     = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_API_METRICS"), kinesis)
//  lazy val featureMetricActor      = system.actorOf(Props(new FeatureMetricActor(apiMetricsPublisher, apiMetricsFlushInterval)))
//  lazy val clientAuth              = ClientAuthImpl()
//
//  bind[GlobalDatabaseManager] toNonLazy GlobalDatabaseManager.initializeForSingleRegion(config)
//  bind[GlobalApiEndpointManager] toNonLazy createGlobalApiEndpointManager
//  binding identifiedBy "kinesis" toNonLazy kinesis
//  binding identifiedBy "cloudwatch" toNonLazy CloudwatchImpl()
//  binding identifiedBy "s3-fileupload" toNonLazy createS3()
//  binding identifiedBy "config" toNonLazy config
//  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
//  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
//  binding identifiedBy "actorMaterializer" toNonLazy materializer
//
//  bind[TestableTime] toNonLazy new TestableTimeImplementation
//  bind[BugSnagger] toNonLazy BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))
//
//  bind[KinesisPublisher] identifiedBy "kinesisApiMetricsPublisher" toNonLazy new KinesisPublisherImplementation(
//    streamName = sys.env("KINESIS_STREAM_API_METRICS"),
//    kinesis
//  )
//  bind[ClientAuth] toNonLazy clientAuth
//
//  binding identifiedBy "featureMetricActor" to featureMetricActor
//  binding identifiedBy "api-metrics-middleware" toNonLazy new ApiMetricsMiddleware(testableTime, featureMetricActor)
//  binding identifiedBy "project-schema-fetcher" toNonLazy ProjectFetcherImpl(blockedProjectIds = Vector.empty, config)
//  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
//  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")
//
//  private def createGlobalApiEndpointManager = {
//    GlobalApiEndpointManager(
//      euWest1 = sys.env("API_ENDPOINT_EU_WEST_1"),
//      usWest2 = sys.env("API_ENDPOINT_US_WEST_2"),
//      apNortheast1 = sys.env("API_ENDPOINT_AP_NORTHEAST_1")
//    )
//  }
//
//  private def createS3(): AmazonS3 = {
//    val credentials = new BasicAWSCredentials(
//      sys.env("FILEUPLOAD_S3_AWS_ACCESS_KEY_ID"),
//      sys.env("FILEUPLOAD_S3_AWS_SECRET_ACCESS_KEY")
//    )
//
//    AmazonS3ClientBuilder.standard
//      .withCredentials(new AWSStaticCredentialsProvider(credentials))
//      .withEndpointConfiguration(new EndpointConfiguration(sys.env("FILEUPLOAD_S3_ENDPOINT"), sys.env("FILEUPLOAD_AWS_REGION")))
//      .build
//  }
//
//  private def createKinesis(): AmazonKinesis = {
//    val credentials =
//      new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))
//
//    AmazonKinesisClientBuilder
//      .standard()
//      .withCredentials(new AWSStaticCredentialsProvider(credentials))
//      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
//      .build()
//  }
//}
