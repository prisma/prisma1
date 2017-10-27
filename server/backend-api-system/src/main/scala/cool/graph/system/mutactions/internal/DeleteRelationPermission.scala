package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.errors.SystemErrors
import cool.graph.system.database.tables.{RelationPermissionTable, RelayIdTable}
import cool.graph.shared.models._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteRelationPermission(project: Project, relation: Relation, permission: RelationPermission) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val permissions = TableQuery[RelationPermissionTable]
    val relayIds    = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(sqlAction = DBIO.seq(permissions.filter(_.id === permission.id).delete, relayIds.filter(_.id === permission.id).delete)))
  }

  override def rollback = Some(CreateRelationPermission(project, relation, permission).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    Future.successful(relation.getPermissionById(permission.id) match {
      case None    => Failure(SystemErrors.RelationPermissionNotInModel(relationPermissionId = permission.id, relationName = relation.name))
      case Some(_) => Success(MutactionVerificationSuccess())
    })
  }
}
