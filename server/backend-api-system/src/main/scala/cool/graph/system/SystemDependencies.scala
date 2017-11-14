package cool.graph.system

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.aws.AwsInitializers
import cool.graph.aws.cloudwatch.{Cloudwatch, CloudwatchImpl}
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.messagebus.Conversions.ByteMarshaller
import cool.graph.messagebus.pubsub.rabbit.{RabbitAkkaPubSub, RabbitAkkaPubSubPublisher}
import cool.graph.messagebus.{Conversions, PubSubPublisher}
import cool.graph.shared.database.{GlobalDatabaseManager, InternalDatabase}
import cool.graph.shared.externalServices._
import cool.graph.shared.functions.FunctionEnvironment
import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
import cool.graph.system.database.Initializers
import cool.graph.system.database.finder.client.ClientResolver
import cool.graph.system.database.finder.{CachedProjectResolver, CachedProjectResolverImpl, ProjectQueries, UncachedProjectResolver}
import cool.graph.system.externalServices._
import cool.graph.system.metrics.SystemMetrics
import scaldi.Module
import slick.jdbc
import slick.jdbc.MySQLProfile

import scala.concurrent.{Await, ExecutionContextExecutor, Future}

trait SystemInjector {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  val functionEnvironment: FunctionEnvironment
  val uncachedProjectResolver: UncachedProjectResolver
  val cachedProjectResolver: CachedProjectResolver
  val invalidationPublisher: PubSubPublisher[String]
  val cloudwatch: Cloudwatch
  val internalDb: MySQLProfile.backend.Database
  val logsDb: MySQLProfile.backend.Database
  val globalDatabaseManager: GlobalDatabaseManager
  val snsPublisher: SnsPublisher
  val kinesisAlgoliaSyncQueriesPublisher: KinesisPublisher
  val schemaBuilder: SchemaBuilder

  def requestPrefix: String
  def projectResolver: UncachedProjectResolver
  def internalDB: MySQLProfile.backend.Database
  def logsDB: MySQLProfile.backend.Database
  def exportDataS3: AmazonS3
  def config: Config = ConfigFactory.load()
  def actorSystem: ActorSystem
  def dispatcher: ExecutionContextExecutor
  def actorMaterializer: ActorMaterializer
  def masterToken: Option[String]
  def clientResolver: ClientResolver
  def projectQueries: ProjectQueries
  def environment: String
  def serviceName: String
  def algoliaKeyChecker: AlgoliaKeyChecker
  def auth0Api: Auth0Api
  def auth0Extend: Auth0Extend
  def bugsnagger: BugSnaggerImpl
  def testableTime: TestableTime

  //temporarily be able to pass old injector
  def toScaldi: Module
}

class SystemInjectorImpl(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SystemInjector {
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  SystemMetrics.init()

  implicit val marshaller: ByteMarshaller[String]     = Conversions.Marshallers.FromString
  implicit val systemInjectorImpl: SystemInjectorImpl = this
  override val toScaldi: SystemDependencies           = SystemDependencies()
  implicit val bugsnaggerImp: BugSnaggerImpl          = BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))

  lazy val schemaBuilder = SchemaBuilder(userCtx => new SchemaBuilderImpl(userCtx, globalDatabaseManager, InternalDatabase(internalDB)).build())

  val dbs: Seq[jdbc.MySQLProfile.backend.DatabaseDef] = {
    val internal = Initializers.setupAndGetInternalDatabase()
    val logs     = Initializers.setupAndGetLogsDatabase()
    val dbs      = Future.sequence(Seq(internal, logs))

    try {
      Await.result(dbs, 15.seconds)
    } catch {
      case e: Throwable =>
        println(s"Unable to initialize databases: $e")
        sys.exit(-1)
    }
  }

  lazy val internalDb                                   = dbs.head
  lazy val logsDb                                       = dbs.last
  lazy val globalDatabaseManager: GlobalDatabaseManager = GlobalDatabaseManager.initializeForMultipleRegions(config)
  lazy val globalRabbitUri: String                      = sys.env.getOrElse("GLOBAL_RABBIT_URI", sys.error("GLOBAL_RABBIT_URI required for schema invalidation"))
  lazy val uncachedProjectResolver                      = UncachedProjectResolver(internalDb)
  lazy val cachedProjectResolver: CachedProjectResolver = CachedProjectResolverImpl(uncachedProjectResolver)(system.dispatcher)
  lazy val apiMatrixFactory: ApiMatrixFactory           = ApiMatrixFactory(DefaultApiMatrix)
  lazy val requestPrefix: String                        = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))
  lazy val cloudwatch                                   = CloudwatchImpl()
  lazy val snsPublisher                                 = new SnsPublisherImplementation(topic = sys.env("SNS_SEAT"))(toScaldi)
  lazy val kinesis: AmazonKinesis                       = AwsInitializers.createKinesis()
  lazy val kinesisAlgoliaSyncQueriesPublisher           = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_ALGOLIA_SYNC_QUERY"), kinesis)
  lazy val invalidationPublisher: RabbitAkkaPubSubPublisher[String] =
    RabbitAkkaPubSub.publisher[String](globalRabbitUri, "project-schema-invalidation", durable = true)
  lazy val functionEnvironment = LambdaFunctionEnvironment(
    sys.env.getOrElse("LAMBDA_AWS_ACCESS_KEY_ID", "whatever"),
    sys.env.getOrElse("LAMBDA_AWS_SECRET_ACCESS_KEY", "whatever")
  )

  def projectResolver: UncachedProjectResolver  = uncachedProjectResolver
  def internalDB: MySQLProfile.backend.Database = internalDb
  def logsDB: MySQLProfile.backend.Database     = logsDb
  def exportDataS3: AmazonS3                    = AwsInitializers.createExportDataS3()
  def actorSystem: ActorSystem                  = system
  def dispatcher: ExecutionContextExecutor      = system.dispatcher
  def actorMaterializer: ActorMaterializer      = materializer
  def masterToken: Option[String]               = sys.env.get("MASTER_TOKEN")
  def clientResolver: ClientResolver            = ClientResolver(internalDb, cachedProjectResolver)(system.dispatcher)
  def projectQueries                            = ProjectQueries()(internalDb, cachedProjectResolver)
  def environment: String                       = sys.env.getOrElse("ENVIRONMENT", "local")
  def serviceName: String                       = sys.env.getOrElse("SERVICE_NAME", "local")
  def algoliaKeyChecker: AlgoliaKeyChecker      = new AlgoliaKeyCheckerImplementation()(toScaldi)
  def auth0Api: Auth0Api                        = new Auth0ApiImplementation()(toScaldi)
  def auth0Extend: Auth0Extend                  = new Auth0ExtendImplementation()(toScaldi)
  def bugsnagger: BugSnaggerImpl                = bugsnaggerImp
  override def testableTime: TestableTime       = new TestableTimeImplementation
}

trait SystemApiDependencies extends Module {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val systemInjector: SystemInjector

  def config: Config = ConfigFactory.load()

  val functionEnvironment: FunctionEnvironment
  val uncachedProjectResolver: UncachedProjectResolver
  val cachedProjectResolver: CachedProjectResolver
  val invalidationPublisher: PubSubPublisher[String]
  val requestPrefix: String
  val cloudwatch: Cloudwatch
  val internalDb: MySQLProfile.backend.Database
  val logsDb: MySQLProfile.backend.Database
  val globalDatabaseManager: GlobalDatabaseManager
  val snsPublisher: SnsPublisher
  val kinesisAlgoliaSyncQueriesPublisher: KinesisPublisher

  lazy val clientResolver: ClientResolver      = ClientResolver(internalDb, cachedProjectResolver)(system.dispatcher)
  lazy val schemaBuilder                       = SchemaBuilder(userCtx => new SchemaBuilderImpl(userCtx, globalDatabaseManager, InternalDatabase(internalDb)).build())
  implicit lazy val bugsnagger: BugSnaggerImpl = BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))

  binding identifiedBy "internal-db" toNonLazy internalDb
  binding identifiedBy "logs-db" toNonLazy logsDb
  binding identifiedBy "export-data-s3" toNonLazy AwsInitializers.createExportDataS3()
  binding identifiedBy "config" toNonLazy config
  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
  binding identifiedBy "actorMaterializer" toNonLazy materializer
  binding identifiedBy "master-token" toNonLazy sys.env.get("MASTER_TOKEN")
  binding identifiedBy "clientResolver" toNonLazy clientResolver
  binding identifiedBy "projectQueries" toNonLazy ProjectQueries()(internalDb, cachedProjectResolver)
  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")

  bind[AlgoliaKeyChecker] identifiedBy "algoliaKeyChecker" toNonLazy new AlgoliaKeyCheckerImplementation()
  bind[Auth0Api] toNonLazy new Auth0ApiImplementation
  bind[Auth0Extend] toNonLazy new Auth0ExtendImplementation()
  bind[BugSnagger] toNonLazy bugsnagger
  bind[TestableTime] toNonLazy new TestableTimeImplementation
}

case class SystemDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer, val systemInjector: SystemInjector)
    extends SystemApiDependencies {
  import system.dispatcher

  import scala.concurrent.duration._

  SystemMetrics.init()

  implicit val marshaller: ByteMarshaller[String] = Conversions.Marshallers.FromString

  val dbs: Seq[jdbc.MySQLProfile.backend.DatabaseDef] = {
    val internal = Initializers.setupAndGetInternalDatabase()
    val logs     = Initializers.setupAndGetLogsDatabase()
    val dbs      = Future.sequence(Seq(internal, logs))

    try {
      Await.result(dbs, 15.seconds)
    } catch {
      case e: Throwable =>
        println(s"Unable to initialize databases: $e")
        sys.exit(-1)
    }
  }

  lazy val internalDb                                   = dbs.head
  lazy val logsDb                                       = dbs.last
  lazy val globalDatabaseManager: GlobalDatabaseManager = GlobalDatabaseManager.initializeForMultipleRegions(config)
  lazy val globalRabbitUri: String                      = sys.env.getOrElse("GLOBAL_RABBIT_URI", sys.error("GLOBAL_RABBIT_URI required for schema invalidation"))
  lazy val invalidationPublisher: RabbitAkkaPubSubPublisher[String] =
    RabbitAkkaPubSub.publisher[String](globalRabbitUri, "project-schema-invalidation", durable = true)
  lazy val uncachedProjectResolver                      = UncachedProjectResolver(internalDb)
  lazy val cachedProjectResolver: CachedProjectResolver = CachedProjectResolverImpl(uncachedProjectResolver)(system.dispatcher)
  lazy val apiMatrixFactory: ApiMatrixFactory           = ApiMatrixFactory(DefaultApiMatrix)
  lazy val requestPrefix: String                        = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))
  lazy val cloudwatch                                   = CloudwatchImpl()
  lazy val snsPublisher                                 = new SnsPublisherImplementation(topic = sys.env("SNS_SEAT"))
  lazy val kinesis: AmazonKinesis                       = AwsInitializers.createKinesis()
  lazy val kinesisAlgoliaSyncQueriesPublisher           = new KinesisPublisherImplementation(streamName = sys.env("KINESIS_STREAM_ALGOLIA_SYNC_QUERY"), kinesis)

  lazy val functionEnvironment = LambdaFunctionEnvironment(
    sys.env.getOrElse("LAMBDA_AWS_ACCESS_KEY_ID", "whatever"),
    sys.env.getOrElse("LAMBDA_AWS_SECRET_ACCESS_KEY", "whatever")
  )

  bind[PubSubPublisher[String]] identifiedBy "schema-invalidation-publisher" toNonLazy invalidationPublisher
  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
  bind[FunctionEnvironment] toNonLazy functionEnvironment
  bind[ApiMatrixFactory] toNonLazy apiMatrixFactory
  bind[GlobalDatabaseManager] toNonLazy globalDatabaseManager
  bind[AmazonSNS] identifiedBy "sns" toNonLazy AwsInitializers.createSns()
  bind[SnsPublisher] identifiedBy "seatSnsPublisher" toNonLazy snsPublisher
  bind[KinesisPublisher] identifiedBy "kinesisAlgoliaSyncQueriesPublisher" toNonLazy kinesisAlgoliaSyncQueriesPublisher

  binding identifiedBy "kinesis" toNonLazy kinesis
  binding identifiedBy "cloudwatch" toNonLazy cloudwatch
  binding identifiedBy "projectResolver" toNonLazy cachedProjectResolver
  binding identifiedBy "cachedProjectResolver" toNonLazy cachedProjectResolver
  binding identifiedBy "uncachedProjectResolver" toNonLazy uncachedProjectResolver
}
