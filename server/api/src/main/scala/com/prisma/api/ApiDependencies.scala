package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.api.database.deferreds.DeferredResolverProvider
import cool.graph.api.database.{DataResolver, Databases}
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.{ApiUserContext, SchemaBuilder}
import cool.graph.api.server.RequestHandler
import cool.graph.api.subscriptions.Webhook
import cool.graph.auth.{Auth, AuthImpl}
import cool.graph.client.server.{GraphQlRequestHandler, GraphQlRequestHandlerImpl}
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.messagebus.queue.inmemory.InMemoryAkkaQueue
import cool.graph.messagebus.{PubSubPublisher, Queue}
import cool.graph.shared.models.Project
import cool.graph.utils.await.AwaitUtils

import scala.concurrent.ExecutionContext

trait ApiDependencies extends AwaitUtils {
  implicit def self: ApiDependencies

  val config: Config = ConfigFactory.load()

  implicit val system: ActorSystem
  val materializer: ActorMaterializer
  def projectFetcher: ProjectFetcher
  def apiSchemaBuilder: SchemaBuilder
  def databases: Databases
  def webhookPublisher: Queue[Webhook]

  implicit lazy val executionContext: ExecutionContext  = system.dispatcher
  implicit lazy val reporter: ErrorReporter             = BugsnagErrorReporter(sys.env("BUGSNAG_API_KEY"))
  lazy val log: String => Unit                          = println
  lazy val graphQlRequestHandler: GraphQlRequestHandler = GraphQlRequestHandlerImpl(log)
  lazy val auth: Auth                                   = AuthImpl
  lazy val requestHandler: RequestHandler               = RequestHandler(projectFetcher, apiSchemaBuilder, graphQlRequestHandler, auth, log)
  lazy val maxImportExportSize: Int                     = 10000000

  val sssEventsPubSub: InMemoryAkkaPubSub[String]
  lazy val sssEventsPublisher: PubSubPublisher[String] = sssEventsPubSub

  def dataResolver(project: Project): DataResolver       = DataResolver(project)
  def masterDataResolver(project: Project): DataResolver = DataResolver(project, useMasterDatabaseOnly = true)
  def deferredResolverProvider(project: Project)         = new DeferredResolverProvider[ApiUserContext](dataResolver(project))

  def destroy = {
    println("ApiDependencies [DESTROY]")
    databases.master.shutdown.await()
    databases.readOnly.shutdown.await()
    materializer.shutdown()
    system.terminate().await()
  }
}

case class ApiDependenciesImpl(sssEventsPubSub: InMemoryAkkaPubSub[String])(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends ApiDependencies {
  override implicit def self: ApiDependencies = this

  val databases        = Databases.initialize(config)
  val apiSchemaBuilder = SchemaBuilder()(system, this)
  val projectFetcher: ProjectFetcher = {
    val schemaManagerEndpoint = config.getString("schemaManagerEndpoint")
    val schemaManagerSecret   = config.getString("schemaManagerSecret")
    ProjectFetcherImpl(Vector.empty, config, schemaManagerEndpoint = schemaManagerEndpoint, schemaManagerSecret = schemaManagerSecret)
  }
  override val webhookPublisher = InMemoryAkkaQueue[Webhook]()
}
