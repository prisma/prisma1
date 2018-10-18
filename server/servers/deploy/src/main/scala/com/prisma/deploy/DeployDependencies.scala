package com.prisma.deploy

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.config.PrismaConfig
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.schema.SchemaBuilder
import com.prisma.deploy.schema.mutations.FunctionValidator
import com.prisma.errors.ErrorReporter
import com.prisma.jwt.Auth
import com.prisma.messagebus.PubSubPublisher
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
  def managementAuth: Auth
  def invalidationPublisher: PubSubPublisher[String]
  def apiAuth: Auth
  def deployConnector: DeployConnector
  def functionValidator: FunctionValidator
  def projectIdEncoder: ProjectIdEncoder
  def telemetryActor: ActorRef

  lazy val projectPersistence      = deployConnector.projectPersistence
  lazy val migrationPersistence    = deployConnector.migrationPersistence
  lazy val managementSchemaBuilder = SchemaBuilder()

  val managementSecret: String

  def initialize()(implicit ec: ExecutionContext): Unit = {
    await(deployConnector.initialize(), seconds = 30)
    system.actorOf(Props(DatabaseSizeReporter(projectPersistence, deployConnector)))
  }
}
