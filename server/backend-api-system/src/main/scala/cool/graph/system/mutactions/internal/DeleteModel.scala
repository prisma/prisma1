package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.models.{Model, Project}
import cool.graph.system.database.tables.{ModelTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteModel(project: Project, model: Model) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val models   = TableQuery[ModelTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(models.filter(_.id === model.id).delete, relayIds.filter(_.id === model.id).delete)))
  }

  override def rollback = Some(CreateModel(project, model).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    if (model.isSystem && !project.isEjected) {
      Future.successful(Failure(SystemErrors.SystemModelCannotBeRemoved(model.name)))
    } else {
      Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
