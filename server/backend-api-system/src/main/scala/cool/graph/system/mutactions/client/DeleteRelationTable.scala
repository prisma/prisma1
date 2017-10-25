package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.models.{Project, Relation}

import scala.concurrent.Future

case class DeleteRelationTable(project: Project, relation: Relation) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.dropTable(projectId = project.id, tableName = relation.id)))

  override def rollback = Some(CreateRelationTable(project, relation).execute)

}
