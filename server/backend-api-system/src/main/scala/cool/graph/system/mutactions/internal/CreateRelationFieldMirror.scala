package cool.graph.system.mutactions.internal

import cool.graph.shared.errors.UserInputErrors.ObjectDoesNotExistInCurrentProject
import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.{RelationFieldMirrorTable, RelayIdTable}
import cool.graph.shared.models._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateRelationFieldMirror(project: Project, relationFieldMirror: RelationFieldMirror) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val mirrors  = TableQuery[RelationFieldMirrorTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          mirrors += cool.graph.system.database.tables
            .RelationFieldMirror(id = relationFieldMirror.id, relationId = relationFieldMirror.relationId, fieldId = relationFieldMirror.fieldId),
          relayIds += cool.graph.system.database.tables
            .RelayId(relationFieldMirror.id, "RelationFieldMirror")
        )))
  }

  override def rollback = Some(DeleteRelationFieldMirror(project, relationFieldMirror).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    project.getRelationById(relationFieldMirror.relationId) match {
      case None => Future.successful(Failure(ObjectDoesNotExistInCurrentProject("relationId does not correspond to an existing Relation")))
      case _ =>
        project.getFieldById(relationFieldMirror.fieldId) match {
          case None => Future.successful(Failure(ObjectDoesNotExistInCurrentProject("fieldId does not correspond to an existing Field")))
          case _    => Future.successful(Success(MutactionVerificationSuccess()))
        }
    }
  }
}
