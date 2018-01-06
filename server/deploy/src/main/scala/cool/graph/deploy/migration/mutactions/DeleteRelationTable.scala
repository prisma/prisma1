package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder
import cool.graph.shared.models.{Relation, Schema}

import scala.concurrent.Future

case class DeleteRelationTable(projectId: String, schema: Schema, relation: Relation) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.dropTable(projectId = projectId, tableName = relation.id))
  }

  override def rollback = Some(CreateRelationTable(projectId, schema, relation).execute)

}
