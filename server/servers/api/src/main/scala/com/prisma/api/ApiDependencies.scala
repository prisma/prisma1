package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.{ApiConnector, DataResolver, DatabaseMutactionExecutor}
import com.prisma.api.mutactions.{DatabaseMutactionVerifier, SideEffectMutactionExecutor}
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.resolver.DeferredResolverImpl
import com.prisma.api.schema.{ApiUserContext, SchemaBuilder}
import com.prisma.api.server.QueryExecutor
import com.prisma.auth.{Auth, AuthImpl}
import com.prisma.config.PrismaConfig
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.messagebus.{PubSub, PubSubPublisher, PubSubSubscriber, QueuePublisher}
import com.prisma.profiling.JvmProfiler
import com.prisma.shared.messages.SchemaInvalidatedMessage
import com.prisma.shared.models.{ConnectorCapabilities, Project, ProjectIdEncoder}
import com.prisma.subscriptions.Webhook
import com.prisma.utils.await.AwaitUtils

import scala.concurrent.ExecutionContext

trait ApiDependencies extends AwaitUtils {
  implicit def self: ApiDependencies

  implicit val system: ActorSystem
  val materializer: ActorMaterializer
  def config: PrismaConfig
  def projectFetcher: ProjectFetcher
  def apiSchemaBuilder: SchemaBuilder
  def invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage]
  def webhookPublisher: QueuePublisher[Webhook]
  def apiConnector: ApiConnector
  def databaseMutactionExecutor: DatabaseMutactionExecutor = apiConnector.databaseMutactionExecutor
  def sideEffectMutactionExecutor: SideEffectMutactionExecutor
  def mutactionVerifier: DatabaseMutactionVerifier
  def projectIdEncoder: ProjectIdEncoder
  def capabilities: ConnectorCapabilities = apiConnector.capabilities

  implicit lazy val executionContext: ExecutionContext = system.dispatcher
  implicit lazy val reporter: ErrorReporter            = BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))
  lazy val auth: Auth                                  = AuthImpl
  lazy val queryExecutor: QueryExecutor                = QueryExecutor()
  lazy val maxImportExportSize: Int                    = 1000000

  val sssEventsPubSub: PubSub[String]
  lazy val sssEventsPublisher: PubSubPublisher[String] = sssEventsPubSub

  JvmProfiler.schedule(ApiMetrics) // kick off JVM Profiler

  def dataResolver(project: Project): DataResolver       = apiConnector.dataResolver(project)
  def masterDataResolver(project: Project): DataResolver = apiConnector.masterDataResolver(project)
  def deferredResolverProvider(project: Project)         = new DeferredResolverImpl[ApiUserContext](dataResolver(project))

  def destroy = {
    apiConnector.shutdown().await()
    materializer.shutdown()
    system.terminate().await()
  }
}
