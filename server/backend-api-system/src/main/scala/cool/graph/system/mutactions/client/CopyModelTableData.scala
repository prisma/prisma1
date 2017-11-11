package cool.graph.system.mutactions.client

import cool.graph.{ClientMutactionNoop, _}
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.models.Model

import scala.concurrent.Future

case class CopyModelTableData(sourceProjectId: String, sourceModel: Model, targetProjectId: String, targetModel: Model) extends ClientSqlSchemaChangeMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val columns = sourceModel.scalarFields.map(_.name)

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder.copyTableData(sourceProjectId, sourceModel.name, columns, targetProjectId, targetModel.name)))
  }

  override def rollback = Some(ClientMutactionNoop().execute) // consider truncating table

}
