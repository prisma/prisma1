package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder

import scala.concurrent.Future

case class DeleteClientDatabaseForProject(projectId: String) extends ClientSqlMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteProjectDatabase(projectId = projectId)))
  }

  override def rollback = Some(CreateClientDatabaseForProject(projectId).execute)
}
