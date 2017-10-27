package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.{ModelPermissionTable, RelayIdTable}
import cool.graph.shared.models._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Success, Try}

case class CreateModelPermission(project: Project, model: Model, permission: ModelPermission) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val permissions = TableQuery[ModelPermissionTable]
    val relayIds    = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          permissions += cool.graph.system.database.tables
            .ModelPermission(
              permission.id,
              model.id,
              permission.operation,
              permission.userType,
              permission.rule,
              permission.ruleName,
              permission.ruleGraphQuery,
              permission.ruleGraphQueryFilePath,
              permission.ruleWebhookUrl,
              permission.applyToWholeModel,
              permission.description,
              permission.isActive
            ),
          relayIds += cool.graph.system.database.tables
            .RelayId(permission.id, "ModelPermission")
        )))
  }

  override def rollback = Some(DeleteModelPermission(project, model, permission).execute)

}
