package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.models.Relation

import scala.concurrent.Future

case class DeleteAllRelations(projectId: String, relation: Relation) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteAllDataItems(projectId, relation.id)))

}
