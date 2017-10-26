package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.models.Model

import scala.concurrent.Future

case class RenameTable(projectId: String, model: Model, name: String) extends ClientSqlSchemaChangeMutaction {

  def setName(oldName: String, newName: String): Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.renameTable(projectId = projectId, name = oldName, newName = newName)))

  override def execute: Future[ClientSqlStatementResult[Any]] = setName(model.name, name)

  override def rollback = Some(setName(name, model.name))
}
