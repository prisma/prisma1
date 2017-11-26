package cool.graph.deploy
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.deploy.database.persistence.{DbToModelMapper, ProjectPersistence, ProjectPersistenceImpl}
import cool.graph.deploy.database.tables.Tables
import cool.graph.deploy.migration.MigrationApplierJob
import cool.graph.deploy.schema.SchemaBuilder
import cool.graph.deploy.seed.InternalDatabaseSeedActions
import cool.graph.deploy.server.DeployServer
import cool.graph.shared.models.Client
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration.Duration

object DeployMain extends App {
  implicit val system       = ActorSystem("deploy-main")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val internalDb         = Database.forConfig("internal")
  val clientDb           = Database.forConfig("client")
  val projectPersistence = ProjectPersistenceImpl(internalDb)

  val client = seedDatabase()

  val migrationApplierJob = system.actorOf(Props(MigrationApplierJob(clientDb, projectPersistence)))
  val schemaBuilder       = SchemaBuilder(internalDb, projectPersistence)
  val server              = DeployServer(schemaBuilder = schemaBuilder, dummyClient = client)
  ServerExecutor(8081, server).startBlocking()

  private def seedDatabase(): Client = {
    await(internalDb.run(InternalDatabaseSeedActions.seedActions()))

    val query = for {
      client <- Tables.Clients
    } yield client

    val dbRow = await(internalDb.run(query.result.headOption))
    DbToModelMapper.convert(dbRow.getOrElse(sys.error("could not find the default client")))
  }

  private def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)
}
