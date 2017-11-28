package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder
import cool.graph.deploy.schema.InvalidName
import cool.graph.deploy.validation.NameConstraints
import cool.graph.shared.models.Model

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateModelTable(projectId: String, model: String) extends ClientSqlMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.createTable(projectId = projectId, name = model)))
  }

  override def rollback = Some(DeleteModelTable(projectId, model).execute)

  override def verify(): Future[Try[Unit]] = {
    val validationResult = if (NameConstraints.isValidModelName(model)) {
      Success(())
    } else {
      Failure(InvalidName(name = model, entityType = " model"))
    }

    Future.successful(validationResult)
  }
}
