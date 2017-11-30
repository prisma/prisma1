package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder
import cool.graph.shared.models.{Field, Model}

import scala.concurrent.Future

case class DeleteColumn(projectId: String, model: Model, field: Field) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteColumn(projectId = projectId, tableName = model.name, columnName = field.name)))
  }

  override def rollback = Some(CreateColumn(projectId, model, field).execute)
}
