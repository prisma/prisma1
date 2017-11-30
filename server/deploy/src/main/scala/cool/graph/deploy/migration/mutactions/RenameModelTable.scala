package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder

import scala.concurrent.Future

case class RenameModelTable(projectId: String, oldName: String, newName: String) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = setName(oldName, newName)

  override def rollback = Some(setName(newName, oldName))

  private def setName(oldName: String, newName: String): Future[ClientSqlStatementResult[Any]] = Future.successful {
    ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.renameTable(projectId = projectId, name = oldName, newName = newName))
  }
}
