package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.CustomRule.{apply => _, _}
import cool.graph.shared.models.ModelOperation.{apply => _, _}
import cool.graph.shared.models.UserType._
import cool.graph.shared.models._
import cool.graph.system.database.tables.ModelPermissionTable
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateModelPermission(model: Model, oldPermisison: ModelPermission, permission: ModelPermission) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    implicit val userTypesMapper = MappedColumnType.base[UserType, String](
      e => e.toString,
      s => UserType.withName(s)
    )

    implicit val operationTypesMapper =
      MappedColumnType.base[ModelOperation, String](
        e => e.toString,
        s => ModelOperation.withName(s)
      )

    implicit val customRuleTypesMapper =
      MappedColumnType.base[CustomRule, String](
        e => e.toString,
        s => CustomRule.withName(s)
      )

    val permissions = TableQuery[ModelPermissionTable]
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { p <- permissions if p.id === permission.id } yield
        (p.userType,
         p.operation,
         p.applyToWholeModel,
         p.rule,
         p.ruleGraphQuery,
          p.ruleGraphQueryFilePath,
         p.ruleName,
         p.ruleWebhookUrl,
         p.description,
         p.isActive)
      q.update(
        (permission.userType,
         permission.operation,
         permission.applyToWholeModel,
         permission.rule,
         permission.ruleGraphQuery,
          permission.ruleGraphQueryFilePath,
         permission.ruleName,
         permission.ruleWebhookUrl,
         permission.description,
         permission.isActive))
    })))
  }

  override def rollback = Some(UpdateModelPermission(model = model, oldPermisison = permission, permission = oldPermisison).execute)

}
