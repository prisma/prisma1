package com.prisma.deploy

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.auth.Auth
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.persistence.DeployPersistencePlugin
import com.prisma.deploy.schema.SchemaBuilder
import com.prisma.deploy.server.ClusterAuth
import com.prisma.errors.ErrorReporter
import com.prisma.graphql.GraphQlClient
import com.prisma.messagebus.PubSubPublisher

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable, ExecutionContext}

trait DeployDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val reporter: ErrorReporter
  import system.dispatcher

  implicit def self: DeployDependencies

  def migrator: Migrator
  def clusterAuth: ClusterAuth
  def graphQlClient: GraphQlClient
  def invalidationPublisher: PubSubPublisher[String]
  def apiAuth: Auth
  def deployPersistencePlugin: DeployPersistencePlugin

//  lazy val clientDb             = Database.forConfig("client")
  lazy val projectPersistence   = deployPersistencePlugin.projectPersistence
  lazy val migrationPersistence = deployPersistencePlugin.migrationPersistence
  lazy val clusterSchemaBuilder = SchemaBuilder()

  def initialize()(implicit ec: ExecutionContext): Unit = {
    await(deployPersistencePlugin.initialize())
    system.actorOf(Props(DatabaseSizeReporter(projectPersistence, deployPersistencePlugin)))
  }

  private def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, 30.seconds)
}
