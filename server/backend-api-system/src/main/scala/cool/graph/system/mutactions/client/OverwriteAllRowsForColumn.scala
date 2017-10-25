package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.ClientMutactionNoop
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.{Field, Model}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class OverwriteAllRowsForColumn(projectId: String, model: Model, field: Field, value: Option[Any]) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(
      sqlAction = DatabaseMutationBuilder.overwriteAllRowsForColumn(projectId = projectId, modelName = model.name, fieldName = field.name, value = value.get)))
  }

  override def rollback = Some(ClientMutactionNoop().execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    value.isEmpty match {
      case true  => Future.successful(Failure(UserAPIErrors.InvalidValue("OverrideValue")))
      case false => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
