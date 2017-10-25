package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.CustomRule.{apply => _, _}
import cool.graph.shared.models.ModelOperation.{apply => _}
import cool.graph.shared.models.UserType._
import cool.graph.shared.models._
import cool.graph.system.database.tables.RelationPermissionTable
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateRelationPermission(relation: Relation,
                                    oldPermission: RelationPermission,
                                    permission: RelationPermission)
    extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    implicit val userTypesMapper = MappedColumnType.base[UserType, String](
      e => e.toString,
      s => UserType.withName(s)
    )

    implicit val customRuleTypesMapper =
      MappedColumnType.base[CustomRule, String](
        e => e.toString,
        s => CustomRule.withName(s)
      )

    val permissions = TableQuery[RelationPermissionTable]
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { p <- permissions if p.id === permission.id } yield
        (p.userType,
         p.connect,
         p.disconnect,
         p.rule,
         p.ruleGraphQuery,
          p.ruleGraphQueryFilePath,
         p.ruleName,
         p.ruleWebhookUrl,
         p.description,
         p.isActive)
      q.update(
        (permission.userType,
         permission.connect,
         permission.disconnect,
         permission.rule,
         permission.ruleGraphQuery,
          permission.ruleGraphQueryFilePath,
         permission.ruleName,
         permission.ruleWebhookUrl,
         permission.description,
         permission.isActive))
    })))
  }

  override def rollback = Some(UpdateRelationPermission(relation = relation, oldPermission = permission, permission = oldPermission).execute)

}
