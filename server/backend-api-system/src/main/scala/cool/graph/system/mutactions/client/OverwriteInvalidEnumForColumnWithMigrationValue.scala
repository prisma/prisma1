package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.ClientMutactionNoop
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.{Field, Model}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class OverwriteInvalidEnumForColumnWithMigrationValue(projectId: String, model: Model, field: Field, oldValue: String, migrationValue: String)
    extends ClientSqlSchemaChangeMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder.overwriteInvalidEnumForColumnWithMigrationValue(projectId = projectId,
                                                                                            modelName = model.name,
                                                                                            fieldName = field.name,
                                                                                            oldValue = oldValue,
                                                                                            migrationValue = migrationValue)))
  }

  override def rollback = Some(ClientMutactionNoop().execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    oldValue.isEmpty || migrationValue.isEmpty match {
      case true  => Future.successful(Failure(UserAPIErrors.InvalidValue("MigrationValue")))
      case false => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
