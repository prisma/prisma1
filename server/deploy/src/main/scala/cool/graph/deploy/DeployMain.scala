package cool.graph.deploy
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.deploy.database.persistence.DbToModelMapper
import cool.graph.deploy.database.tables.Tables
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
  val internalDb            = Database.forConfig("internal")

  val client = seedDatabase()

  val schemaBuilder = SchemaBuilder(internalDb)
  val server        = DeployServer(schemaBuilder = schemaBuilder, dummyClient = client)
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
