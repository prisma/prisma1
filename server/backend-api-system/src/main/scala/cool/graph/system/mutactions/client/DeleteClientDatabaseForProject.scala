package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder

import scala.concurrent.Future

case class DeleteClientDatabaseForProject(projectId: String) extends ClientSqlSchemaChangeMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteProjectDatabase(projectId = projectId)))
  }

  override def rollback = Some(CreateClientDatabaseForProject(projectId).execute)
}
