package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.{ApiConnector, DataResolver, DatabaseMutactionExecutor}
import com.prisma.api.mutactions.{DatabaseMutactionVerifier, SideEffectMutactionExecutor}
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.resolver.DeferredResolverImpl
import com.prisma.api.schema.{ApiUserContext, SchemaBuilder}
import com.prisma.api.server._
import com.prisma.cache.factory.CacheFactory
import com.prisma.config.PrismaConfig
import com.prisma.errors.{DummyErrorReporter, ErrorReporter}
import com.prisma.jwt.Auth
import com.prisma.messagebus.{PubSub, PubSubPublisher, PubSubSubscriber, QueuePublisher}
import com.prisma.metrics.MetricsRegistry
import com.prisma.shared.messages.SchemaInvalidatedMessage
import com.prisma.shared.models.{ConnectorCapabilities, Project, ProjectIdEncoder}
import com.prisma.subscriptions.Webhook
import com.prisma.utils.await.AwaitUtils

import scala.concurrent.ExecutionContext

trait ApiDependencies extends AwaitUtils {
  implicit def self: ApiDependencies
  implicit val system: ActorSystem

  implicit lazy val executionContext: ExecutionContext = system.dispatcher
  implicit lazy val reporter: ErrorReporter            = DummyErrorReporter

  val materializer: ActorMaterializer
  val cacheFactory: CacheFactory
  val auth: Auth
  val sssEventsPubSub: PubSub[String]
  val metricsRegistry: MetricsRegistry

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

  def capabilities: ConnectorCapabilities                = apiConnector.capabilities
  def dataResolver(project: Project): DataResolver       = apiConnector.dataResolver(project)
  def masterDataResolver(project: Project): DataResolver = apiConnector.masterDataResolver(project)
  def deferredResolverProvider(project: Project)         = new DeferredResolverImpl[ApiUserContext](dataResolver(project))

  lazy val queryExecutor: QueryExecutor                = QueryExecutor()
  lazy val maxImportExportSize: Int                    = 1000000
  lazy val sssEventsPublisher: PubSubPublisher[String] = sssEventsPubSub

  def initializeApiDependencies(): Unit = {
    ApiMetrics.init(metricsRegistry)
  }

  def destroy = {
    apiConnector.shutdown().await()
    materializer.shutdown()
    system.terminate().await()
  }
}
