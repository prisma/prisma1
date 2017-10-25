package cool.graph.client.server

import cool.graph.shared.database.GlobalDatabaseManager
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object HealthChecks {
  def checkDatabases(globalDatabaseManager: GlobalDatabaseManager)(implicit ec: ExecutionContext): Future[Unit] = {
    Future
      .sequence {
        globalDatabaseManager.databases.values.map { db =>
          for {
            _ <- db.master.run(sql"SELECT 1".as[Int])
            _ <- db.readOnly.run(sql"SELECT 1".as[Int])
          } yield ()
        }
      }
      .map(_ => ())
  }
}
