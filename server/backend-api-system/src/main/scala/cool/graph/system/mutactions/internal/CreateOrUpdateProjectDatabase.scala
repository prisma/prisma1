package cool.graph.system.mutactions.internal

import cool.graph.shared.models.ProjectDatabase
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.tables.Tables.ProjectDatabases
import cool.graph.{SystemSqlMutaction, SystemSqlStatementResult}
import slick.dbio.DBIOAction
import slick.dbio.Effect.{Read, Transactional, Write}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class CreateOrUpdateProjectDatabase(projectDatabase: ProjectDatabase) extends SystemSqlMutaction {
  import scala.concurrent.ExecutionContext.Implicits.global

  val insertProjectDatabaseIfNotExists: DBIOAction[Any, NoStream, Read with Write with Transactional] =
    ProjectDatabases
      .filter(_.id === projectDatabase.id)
      .exists
      .result
      .flatMap { exists =>
        if (!exists) {
          ProjectDatabases += ModelToDbMapper.convertProjectDatabase(projectDatabase)
        } else {
          DBIO.successful(None) // no-op
        }
      }
      .transactionally

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    import cool.graph.system.database.tables.Tables._
    Future.successful(
      SystemSqlStatementResult(sqlAction =
        DBIO.seq(insertProjectDatabaseIfNotExists, RelayIds.insertOrUpdate(cool.graph.system.database.tables.RelayId(projectDatabase.id, "ProjectDatabase")))))
  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = Some(DeleteProjectDatabase(projectDatabase).execute)

}
