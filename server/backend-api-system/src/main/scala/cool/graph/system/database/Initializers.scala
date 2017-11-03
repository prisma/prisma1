package cool.graph.system.database

import cool.graph.system.database.schema.{InternalDatabaseSchema, LogDatabaseSchema}
import cool.graph.system.database.seed.InternalDatabaseSeedActions
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object Initializers {
  def setupAndGetInternalDatabase()(implicit ec: ExecutionContext): Future[MySQLProfile.backend.Database] = {
    Future {
      val rootDb = Database.forConfig(s"internalRoot")
      Await.result(rootDb.run(InternalDatabaseSchema.createSchemaActions(recreate = false)), 30.seconds)
      rootDb.close()

      val db = Database.forConfig("internal")
      Await.result(db.run(InternalDatabaseSeedActions.seedActions(sys.env.get("MASTER_TOKEN"))), 5.seconds)
      db
    }
  }

  def setupAndGetLogsDatabase()(implicit ec: ExecutionContext): Future[MySQLProfile.backend.Database] = {
    Future {
      val rootDb = Database.forConfig(s"logsRoot")
      Await.result(rootDb.run(LogDatabaseSchema.createSchemaActions(recreate = false)), 30.seconds)
      rootDb.close()

      Database.forConfig("logs")
    }
  }
}
