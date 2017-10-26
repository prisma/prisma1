package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.models.{Field, Project, Relation}

import scala.concurrent.Future

case class DeleteRelationFieldMirrorColumn(project: Project, relation: Relation, field: Field) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    val mirrorColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, field, relation)

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder.deleteColumn(projectId = project.id, tableName = relation.id, columnName = mirrorColumnName)))
  }

  override def rollback = Some(CreateRelationFieldMirrorColumn(project, relation, field).execute)
}
