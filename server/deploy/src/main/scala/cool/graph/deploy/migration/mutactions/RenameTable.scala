package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder

import scala.concurrent.Future

case class RenameTable(projectId: String, previousName: String, nextName: String) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = setName(previousName, nextName)

  override def rollback = Some(setName(nextName, previousName))

  private def setName(previousName: String, nextName: String): Future[ClientSqlStatementResult[Any]] = Future.successful {
    ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.renameTable(projectId = projectId, name = previousName, newName = nextName))
  }
}
