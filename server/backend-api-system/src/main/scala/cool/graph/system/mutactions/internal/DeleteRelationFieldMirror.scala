package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.tables.{RelationFieldMirrorTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteRelationFieldMirror(project: Project, relationFieldMirror: RelationFieldMirror) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val mirrors  = TableQuery[RelationFieldMirrorTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(mirrors.filter(_.id === relationFieldMirror.id).delete, relayIds.filter(_.id === relationFieldMirror.id).delete)))
  }

  override def rollback = Some(CreateRelationFieldMirror(project, relationFieldMirror).execute)

}
