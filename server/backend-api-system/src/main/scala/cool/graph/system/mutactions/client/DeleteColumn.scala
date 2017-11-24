package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.models.{Field, Model}

import scala.concurrent.Future

case class DeleteColumn(projectId: String, model: Model, field: Field) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteColumn(projectId = projectId, tableName = model.name, columnName = field.name)))
  }

  override def rollback = Some(CreateColumn(projectId, model, field).execute)
}
