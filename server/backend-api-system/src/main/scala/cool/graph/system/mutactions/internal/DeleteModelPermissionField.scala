package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{Model, ModelPermission, Project}
import cool.graph.system.database.tables._
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DeleteModelPermissionField(project: Project, model: Model, permission: ModelPermission, fieldId: String)(implicit inj: Injector)
    extends SystemSqlMutaction
    with Injectable {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val internalDatabase = inject[DatabaseDef](identified by "internal-db")
    val permissionFields = TableQuery[ModelPermissionFieldTable]
    val relayIds         = TableQuery[RelayIdTable]

    val permissionField = permissionFields.filter(pf => pf.modelPermissionId === permission.id && pf.fieldId === fieldId)

    val modelPermissionFields: Future[Seq[ModelPermissionField]] = internalDatabase.run(permissionField.result)

    val sqlStatementResults: Future[SystemSqlStatementResult[Any]] = modelPermissionFields.map { modelPermissionFieldList =>
      val firstModelPermissionField: Option[ModelPermissionField] = modelPermissionFieldList.headOption

      val result: Option[SystemSqlStatementResult[Any]] = firstModelPermissionField.map { existingModelPermissionField =>
        SystemSqlStatementResult[Any](sqlAction =
          DBIO.seq(permissionFields.filter(_.id === existingModelPermissionField.id).delete, relayIds.filter(_.id === existingModelPermissionField.id).delete))
      }

      result match {
        case Some(x) =>
          x
        case None =>
          sys.error(
            "DeleteModelPermissionField_None.get \n"
              + "ModelId: " + model.id + "\n"
              + "FieldId: " + fieldId + "\n"
              + "Permission: " + permission + "\n"
              + "-----------------------------\n"
              + "permissionFields: " + permissionFields + "\n"
              + "relayIds: " + relayIds + "\n"
              + "permissionField: " + permissionField + "\n"
              + "modelPermissionFields: " + modelPermissionFields + "\n"
              + "ModelPermissionFieldList: " + modelPermissionFieldList + "\n"
              + "result: " + result + "\n")
      }
    }
    sqlStatementResults
  }

  override def rollback = Some(CreateModelPermission(project, model, permission).execute)

}
