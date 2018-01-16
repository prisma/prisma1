package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder
import cool.graph.deploy.validation.NameConstraints
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.{Field, Model}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateColumn(projectId: String, model: Model, field: Field) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder.createColumn(
          projectId = projectId,
          tableName = model.name,
          columnName = field.name,
          isRequired = field.isRequired,
          isUnique = field.isUnique,
          isList = field.isList,
          typeIdentifier = field.typeIdentifier
        )))
  }

  override def rollback = Some(DeleteColumn(projectId, model, field).execute)

  override def verify(): Future[Try[Unit]] = {
    NameConstraints.isValidFieldName(field.name) match {
      case false => Future.successful(Failure(UserInputErrors.InvalidName(name = field.name, entityType = " field")))
      case true  => Future.successful(Success(()))
    }
  }
}
