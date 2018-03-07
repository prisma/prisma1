package com.prisma.deploy

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.auth.{Auth, AuthImpl}
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.deploy.database.persistence.ProjectPersistenceImpl
import com.prisma.deploy.database.schema.InternalDatabaseSchema
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
import com.prisma.deploy.persistence.DeployPersistencePlugin
import com.prisma.deploy.schema.SchemaBuilder
import com.prisma.deploy.seed.InternalDatabaseSeedActions
import com.prisma.deploy.server.{ClusterAuth, ClusterAuthImpl, DummyClusterAuth}
import com.prisma.graphql.GraphQlClient
import com.prisma.messagebus.PubSubPublisher
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.{Duration, _}
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
  def persistencePlugin: DeployPersistencePlugin

  setupAndGetInternalDatabase()
  lazy val clientDb             = Database.forConfig("client")
  lazy val projectPersistence   = persistencePlugin.projectPersistence
  lazy val migrationPersistence = persistencePlugin.migrationPersistence
  lazy val clusterSchemaBuilder = SchemaBuilder()

  def setupAndGetInternalDatabase()(implicit ec: ExecutionContext): MySQLProfile.backend.Database = {
//    val rootDb = Database.forConfig(s"internalRoot")
//    await(rootDb.run(InternalDatabaseSchema.createSchemaActions(recreate = false)))
//    rootDb.close()
    await(persistencePlugin.createInternalSchema())

//    val db = Database.forConfig("internal")
//    await(db.run(InternalDatabaseSeedActions.seedActions()))

    startDatabaseSizeReporting()

    null
  }

  def startDatabaseSizeReporting(): Unit = {
    system.actorOf(Props(DatabaseSizeReporter(projectPersistence, persistencePlugin)))
  }

  private def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, 30.seconds)
}
