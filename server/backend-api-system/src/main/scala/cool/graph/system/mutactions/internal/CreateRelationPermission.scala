package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.tables.{RelationPermissionTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class CreateRelationPermission(project: Project, relation: Relation, permission: RelationPermission) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val permissions = TableQuery[RelationPermissionTable]
    val relayIds    = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          permissions += cool.graph.system.database.tables
            .RelationPermission(
              permission.id,
              relation.id,
              permission.connect,
              permission.disconnect,
              permission.userType,
              permission.rule,
              permission.ruleName,
              permission.ruleGraphQuery,
              permission.ruleGraphQueryFilePath,
              permission.ruleWebhookUrl,
              permission.description,
              permission.isActive
            ),
          relayIds += cool.graph.system.database.tables
            .RelayId(permission.id, "RelationPermission")
        )))
  }

  override def rollback = Some(DeleteRelationPermission(project, relation, permission).execute)

}
