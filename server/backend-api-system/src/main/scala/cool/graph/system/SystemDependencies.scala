package cool.graph.system

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSAsyncClientBuilder}
import com.typesafe.config.ConfigFactory
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.cloudwatch.CloudwatchImpl
import cool.graph.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import cool.graph.messagebus.{Conversions, PubSubPublisher}
import cool.graph.shared.database.{GlobalDatabaseManager, InternalDatabase}
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
import cool.graph.shared.externalServices._
import cool.graph.shared.functions.FunctionEnvironment
import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import cool.graph.system.database.finder.client.ClientResolver
import cool.graph.system.database.finder.{CachedProjectResolver, CachedProjectResolverImpl, ProjectQueries, UncachedProjectResolver}
import cool.graph.system.database.schema.{InternalDatabaseSchema, LogDatabaseSchema}
import cool.graph.system.database.seed.InternalDatabaseSeedActions
import cool.graph.system.externalServices._
import cool.graph.system.metrics.SystemMetrics
import scaldi.Module
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait SystemApiDependencies extends Module {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  SystemMetrics.init()

  def config = ConfigFactory.load()

  val functionEnvironment: FunctionEnvironment
  val uncachedProjectResolver: UncachedProjectResolver
  val cachedProjectResolver: CachedProjectResolver
  val invalidationPublisher: PubSubPublisher[String]
  val requestPrefix: String

  lazy val internalDb            = setupAndGetInternalDatabase()
  lazy val logsDb                = setupAndGetLogsDatabase()
  lazy val clientResolver        = ClientResolver(internalDb, cachedProjectResolver)(system.dispatcher)
  lazy val kinesis               = createKinesis()
  lazy val globalDatabaseManager = GlobalDatabaseManager.initializeForMultipleRegions(config)
  lazy val schemaBuilder         = SchemaBuilder(userCtx => new SchemaBuilderImpl(userCtx, globalDatabaseManager, InternalDatabase(internalDb)).build())
  implicit lazy val bugsnagger   = BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))

  bind[GlobalDatabaseManager] toNonLazy globalDatabaseManager
  binding identifiedBy "internal-db" toNonLazy internalDb
  binding identifiedBy "logs-db" toNonLazy logsDb
  binding identifiedBy "kinesis" toNonLazy kinesis
  binding identifiedBy "sns" toNonLazy createSns()
  binding identifiedBy "cloudwatch" toNonLazy CloudwatchImpl()
  binding identifiedBy "export-data-s3" toNonLazy createExportDataS3()
  binding identifiedBy "config" toNonLazy config
  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
  binding identifiedBy "actorMaterializer" toNonLazy materializer
  binding identifiedBy "master-token" toNonLazy sys.env.get("MASTER_TOKEN")
  binding identifiedBy "clientResolver" toNonLazy clientResolver
  binding identifiedBy "projectQueries" toNonLazy ProjectQueries()(internalDb, cachedProjectResolver)
  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")

  bind[GlobalDatabaseManager] toNonLazy GlobalDatabaseManager.initializeForMultipleRegions(config)
  bind[AlgoliaKeyChecker] identifiedBy "algoliaKeyChecker" toNonLazy new AlgoliaKeyCheckerImplementation()
  bind[Auth0Api] toNonLazy new Auth0ApiImplementation
  bind[Auth0Extend] toNonLazy new Auth0ExtendImplementation()
  bind[BugSnagger] toNonLazy bugsnagger
  bind[SnsPublisher] identifiedBy "seatSnsPublisher" toNonLazy new SnsPublisherImplementation(topic = sys.env("SNS_SEAT"))
  bind[TestableTime] toNonLazy new TestableTimeImplementation
  bind[KinesisPublisher] identifiedBy "kinesisAlgoliaSyncQueriesPublisher" toNonLazy new KinesisPublisherImplementation(
    streamName = sys.env("KINESIS_STREAM_ALGOLIA_SYNC_QUERY"),
    kinesis)

  protected def createKinesis(): AmazonKinesis = {
    val credentials =
      new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonKinesisClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build()
  }

  protected def createSns(): AmazonSNS = {
    val credentials =
      new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

    AmazonSNSAsyncClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("SNS_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  protected def createExportDataS3(): AmazonS3 = {
    val credentials = new BasicAWSCredentials(
      sys.env.getOrElse("AWS_ACCESS_KEY_ID", ""),
      sys.env.getOrElse("AWS_SECRET_ACCESS_KEY", "")
    )

    AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("DATA_EXPORT_S3_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  protected def setupAndGetInternalDatabase(): MySQLProfile.backend.Database = {
    Try {
      val rootDb = Database.forConfig(s"internalRoot")
      Await.result(rootDb.run(InternalDatabaseSchema.createSchemaActions(recreate = false)), 30.seconds)
      rootDb.close()

      val db = Database.forConfig("internal")
      Await.result(db.run(InternalDatabaseSeedActions.seedActions(sys.env.get("MASTER_TOKEN"))), 5.seconds)
      db
    } match {
      case Success(database) => database
      case Failure(e)        => println(s"[FATAL] Unable to init database: $e"); sys.exit(-1)
    }
  }

  protected def setupAndGetLogsDatabase(): MySQLProfile.backend.Database = {
    Try {
      val rootDb = Database.forConfig(s"logsRoot")
      Await.result(rootDb.run(LogDatabaseSchema.createSchemaActions(recreate = false)), 30.seconds)
      rootDb.close()

      Database.forConfig("logs")
    } match {
      case Success(database) => database
      case Failure(e)        => println(s"[FATAL] Unable to init logs database: $e"); sys.exit(-1)
    }
  }
}

case class SystemDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SystemApiDependencies {
  implicit val marshaller = Conversions.Marshallers.FromString

  val globalRabbitUri                              = sys.env.getOrElse("GLOBAL_RABBIT_URI", sys.error("GLOBAL_RABBIT_URI required for schema invalidation"))
  val invalidationPublisher                        = RabbitAkkaPubSub.publisher[String](globalRabbitUri, "project-schema-invalidation", durable = true)
  val uncachedProjectResolver                      = UncachedProjectResolver(internalDb)
  val cachedProjectResolver: CachedProjectResolver = CachedProjectResolverImpl(uncachedProjectResolver)(system.dispatcher)
  val apiMatrixFactory: ApiMatrixFactory           = ApiMatrixFactory(DefaultApiMatrix(_))
  val requestPrefix                                = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))

  val functionEnvironment = LambdaFunctionEnvironment(
    sys.env.getOrElse("LAMBDA_AWS_ACCESS_KEY_ID", "whatever"),
    sys.env.getOrElse("LAMBDA_AWS_SECRET_ACCESS_KEY", "whatever")
  )

  bind[PubSubPublisher[String]] identifiedBy "schema-invalidation-publisher" toNonLazy invalidationPublisher
  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
  bind[FunctionEnvironment] toNonLazy functionEnvironment
  bind[ApiMatrixFactory] toNonLazy apiMatrixFactory

  binding identifiedBy "projectResolver" toNonLazy cachedProjectResolver
  binding identifiedBy "cachedProjectResolver" toNonLazy cachedProjectResolver
  binding identifiedBy "uncachedProjectResolver" toNonLazy uncachedProjectResolver
}
