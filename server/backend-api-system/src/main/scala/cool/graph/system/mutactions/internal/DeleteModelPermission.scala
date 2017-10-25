package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.errors.SystemErrors
import cool.graph.system.database.tables.{ModelPermissionTable, RelayIdTable}
import cool.graph.shared.models.{Model, ModelPermission, Project}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteModelPermission(project: Project, model: Model, permission: ModelPermission) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val permissions = TableQuery[ModelPermissionTable]
    val relayIds    = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(sqlAction = DBIO.seq(permissions.filter(_.id === permission.id).delete, relayIds.filter(_.id === permission.id).delete)))
  }

  override def rollback = Some(CreateModelPermission(project, model, permission).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    Future.successful(model.getPermissionById(permission.id) match {
      case None    => Failure(SystemErrors.ModelPermissionNotInModel(modelPermissionId = permission.id, modelName = model.name))
      case Some(x) => Success(MutactionVerificationSuccess())
    })
  }
}
