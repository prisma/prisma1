package cool.graph.deploy

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import cool.graph.deploy.database.persistence.{MigrationPersistenceImpl, ProjectPersistenceImpl}
import cool.graph.deploy.database.schema.InternalDatabaseSchema
import cool.graph.deploy.migration.MigrationApplierJob
import cool.graph.deploy.schema.SchemaBuilder
import cool.graph.deploy.seed.InternalDatabaseSeedActions
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Awaitable, ExecutionContext}

trait DeployDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  import system.dispatcher

  implicit def self: DeployDependencies

  lazy val internalDb           = setupAndGetInternalDatabase()
  lazy val clientDb             = Database.forConfig("client")
  lazy val projectPersistence   = ProjectPersistenceImpl(internalDb)
  lazy val migrationPersistence = MigrationPersistenceImpl(internalDb)
  lazy val migrationApplierJob  = system.actorOf(Props(MigrationApplierJob(clientDb, migrationPersistence)))
  lazy val deploySchemaBuilder  = SchemaBuilder()

  def setupAndGetInternalDatabase()(implicit ec: ExecutionContext): MySQLProfile.backend.Database = {
    val rootDb = Database.forConfig(s"internalRoot")
    Await.result(rootDb.run(InternalDatabaseSchema.createSchemaActions(recreate = false)), 30.seconds)
    rootDb.close()

    val db = Database.forConfig("internal")
    await(db.run(InternalDatabaseSeedActions.seedActions()))

    db
  }

  private def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def init: Unit
}

case class DeployDependenciesImpl()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this

  def init: Unit = {
    migrationApplierJob
  }
}
