package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.{Field, Model}
import cool.graph.system.mutactions.internal.SystemMutactionNoop

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class OverwriteAllRowsForColumn(projectId: String, model: Model, field: Field, value: Option[Any]) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    Future.successful(SystemSqlStatementResult(
      sqlAction = DatabaseMutationBuilder.overwriteAllRowsForColumn(projectId = projectId, modelName = model.name, fieldName = field.name, value = value.get)))
  }

  override def rollback = Some(SystemMutactionNoop().execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    value.isEmpty match {
      case true  => Future.successful(Failure(UserAPIErrors.InvalidValue("OverrideValue")))
      case false => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
