package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder

import scala.concurrent.Future

case class CreateClientDatabaseForProject(projectId: String) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.createClientDatabaseForProject(projectId = projectId)))

  override def rollback = Some(DeleteClientDatabaseForProject(projectId).execute)
}
