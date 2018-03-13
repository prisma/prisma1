package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor}
import com.prisma.api.database.{DataResolver, Databases}
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.resolver.DeferredResolverProvider
import com.prisma.api.schema.{ApiUserContext, SchemaBuilder}
import com.prisma.api.server.RequestHandler
import com.prisma.auth.{Auth, AuthImpl}
import com.prisma.client.server.{GraphQlRequestHandler, GraphQlRequestHandlerImpl}
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.messagebus.{PubSub, PubSubPublisher, QueuePublisher}
import com.prisma.shared.models.Project
import com.prisma.subscriptions.Webhook
import com.prisma.utils.await.AwaitUtils
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext

trait ApiDependencies extends AwaitUtils {
  implicit def self: ApiDependencies

  val config: Config = ConfigFactory.load()

  implicit val system: ActorSystem
  val materializer: ActorMaterializer
  def projectFetcher: ProjectFetcher
  def apiSchemaBuilder: SchemaBuilder
  def databases: Databases
  def webhookPublisher: QueuePublisher[Webhook]
  def apiConnector: ApiConnector
  def databaseMutactionExecutor: DatabaseMutactionExecutor = apiConnector.databaseMutactionExecutor

  implicit lazy val executionContext: ExecutionContext  = system.dispatcher
  implicit lazy val reporter: ErrorReporter             = BugsnagErrorReporter(sys.env("BUGSNAG_API_KEY"))
  lazy val graphQlRequestHandler: GraphQlRequestHandler = GraphQlRequestHandlerImpl(println)
  lazy val auth: Auth                                   = AuthImpl
  lazy val requestHandler: RequestHandler               = RequestHandler(projectFetcher, apiSchemaBuilder, graphQlRequestHandler, auth, println)
  lazy val maxImportExportSize: Int                     = 10000000

  val sssEventsPubSub: PubSub[String]
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
