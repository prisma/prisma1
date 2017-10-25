package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.ActionHandlerType._
import cool.graph.shared.models.ActionTriggerType._
import cool.graph.shared.models.{Action, ActionHandlerType, ActionTriggerType, Project}
import cool.graph.system.database.tables.ActionTable
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateAction(project: Project, oldAction: Action, action: Action) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    implicit val ActionHandlerTypeMapper =
      MappedColumnType.base[ActionHandlerType, String](
        e => e.toString,
        s => ActionHandlerType.withName(s)
      )
    implicit val ActionTriggerTypeMapper =
      MappedColumnType.base[ActionTriggerType, String](
        e => e.toString,
        s => ActionTriggerType.withName(s)
      )

    val actions = TableQuery[ActionTable]
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { a <- actions if a.id === action.id } yield (a.description, a.isActive, a.triggerType, a.handlerType)

      q.update((action.description, action.isActive, action.triggerType, action.handlerType))
    })))
  }

  override def rollback = Some(UpdateAction(project, oldAction, oldAction).execute)

}
