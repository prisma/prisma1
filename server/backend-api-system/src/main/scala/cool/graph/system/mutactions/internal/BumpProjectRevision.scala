package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.Project
import cool.graph.system.database.tables.Tables
import scaldi.Injectable
import slick.dbio.Effect.Write
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.Future

// We increase the Project.revision number whenever the project structure is changed

case class BumpProjectRevision(project: Project) extends SystemSqlMutaction with Injectable {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val bumpProjectRevision = setRevisionQuery(project.revision + 1)
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(bumpProjectRevision)))
  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = Some {
    val resetProjectRevision = setRevisionQuery(project.revision)
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(resetProjectRevision)))
  }

  private def setRevisionQuery(revision: Int): FixedSqlAction[Int, NoStream, Write] = {
    val query = for {
      projectRow <- Tables.Projects
      if projectRow.id === project.id
    } yield projectRow.revision
    query.update(revision)
  }
}
