package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.Model

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateModelTable(projectId: String, model: Model) extends ClientSqlSchemaChangeMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.createTable(projectId = projectId, name = model.name)))
  }

  override def rollback = Some(DeleteModelTable(projectId, model).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val validationResult = if (NameConstraints.isValidModelName(model.name)) {
      Success(MutactionVerificationSuccess())
    } else {
      Failure(UserInputErrors.InvalidName(name = model.name))
    }

    Future.successful(validationResult)
  }
}
