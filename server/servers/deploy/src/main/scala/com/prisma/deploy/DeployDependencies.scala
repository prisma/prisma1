package com.prisma.deploy

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import com.prisma.cache.factory.CacheFactory
import com.prisma.config.PrismaConfig
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.schema.SchemaBuilder
import com.prisma.deploy.schema.mutations.FunctionValidator
import com.prisma.errors.ErrorReporter
import com.prisma.auth.Auth
import com.prisma.deploy.server.auth.ManagementAuth
import com.prisma.messagebus.PubSubPublisher
import com.prisma.metrics.MetricsRegistry
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.utils.await.AwaitUtils

import scala.concurrent.ExecutionContext

trait DeployDependencies extends AwaitUtils {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val reporter: ErrorReporter

  implicit def self: DeployDependencies
  implicit lazy val executionContext: ExecutionContext = system.dispatcher

  def config: PrismaConfig
  def migrator: Migrator
  def managementAuth: ManagementAuth
  def invalidationPublisher: PubSubPublisher[String]
  def deployConnector: DeployConnector
  def functionValidator: FunctionValidator
  def projectIdEncoder: ProjectIdEncoder
  def telemetryActor: ActorRef

  lazy val projectPersistence      = deployConnector.projectPersistence
  lazy val migrationPersistence    = deployConnector.migrationPersistence
  lazy val managementSchemaBuilder = SchemaBuilder()

  val managementSecret: String
  val cacheFactory: CacheFactory
  val auth: Auth
  val metricsRegistry: MetricsRegistry

  def initializeDeployDependencies()(implicit ec: ExecutionContext): Unit = {
    await(deployConnector.initialize(), seconds = 30)
    DeployMetrics.init(metricsRegistry, projectPersistence, deployConnector, system)
  }
}
