package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.models._
import cool.graph.system.database.tables.{ModelPermissionFieldTable, RelayIdTable}
import scaldi.Injector
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class CreateModelPermissionField(project: Project, model: Model, permission: ModelPermission, fieldId: String)(implicit inj: Injector)
    extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val newId = Cuid.createCuid()

    val permissionFields = TableQuery[ModelPermissionFieldTable]
    val relayIds         = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          permissionFields += cool.graph.system.database.tables
            .ModelPermissionField(
              id = newId,
              modelPermissionId = permission.id,
              fieldId = fieldId
            ),
          relayIds += cool.graph.system.database.tables
            .RelayId(newId, "ModelPermissionField")
        )))
  }

  override def rollback = Some(DeleteModelPermissionField(project, model, permission, fieldId).execute)

}
