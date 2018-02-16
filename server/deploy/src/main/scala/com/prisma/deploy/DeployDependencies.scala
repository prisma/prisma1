package com.prisma.deploy

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.auth.{Auth, AuthImpl}
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.deploy.database.persistence.{MigrationPersistenceImpl, ProjectPersistenceImpl}
import com.prisma.deploy.database.schema.InternalDatabaseSchema
import com.prisma.deploy.migration.migrator.{AsyncMigrator, Migrator}
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

  lazy val internalDb           = setupAndGetInternalDatabase()
  lazy val clientDb             = Database.forConfig("client")
  lazy val projectPersistence   = ProjectPersistenceImpl(internalDb)
  lazy val migrationPersistence = MigrationPersistenceImpl(internalDb)
  lazy val clusterSchemaBuilder = SchemaBuilder()

  def setupAndGetInternalDatabase()(implicit ec: ExecutionContext): MySQLProfile.backend.Database = {
    val rootDb = Database.forConfig(s"internalRoot")
    Await.result(rootDb.run(InternalDatabaseSchema.createSchemaActions(recreate = false)), 30.seconds)
    rootDb.close()

    val db = Database.forConfig("internal")
    await(db.run(InternalDatabaseSeedActions.seedActions()))

    startDatabaseSizeReporting()

    db
  }

  def startDatabaseSizeReporting(): Unit = {
    system.actorOf(Props(DatabaseSizeReporter(projectPersistence, clientDb)))
  }

  private def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)
}

case class DeployDependenciesImpl()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this
  override implicit val reporter                 = BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))
  override lazy val migrator: Migrator           = AsyncMigrator(clientDb, migrationPersistence, projectPersistence)
  override lazy val clusterAuth = {
    sys.env.get("CLUSTER_PUBLIC_KEY") match {
      case Some(publicKey) if publicKey.nonEmpty => ClusterAuthImpl(publicKey)
      case _                                     => DummyClusterAuth()
    }
  }

  override lazy val graphQlClient         = GraphQlClient(sys.env.getOrElse("CLUSTER_ADDRESS", sys.error("env var CLUSTER_ADDRESS is not set")))
  override lazy val invalidationPublisher = ???

  override def apiAuth = AuthImpl
}
