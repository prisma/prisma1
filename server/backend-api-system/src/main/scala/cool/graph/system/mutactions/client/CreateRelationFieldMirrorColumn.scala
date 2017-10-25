package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.models.{Field, Project, Relation}

import scala.concurrent.Future

case class CreateRelationFieldMirrorColumn(project: Project, relation: Relation, field: Field) extends ClientSqlSchemaChangeMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {

    val mirrorColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, field, relation)

    // Note: we don't need unique index or null constraints on mirrored fields

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder.createColumn(
          projectId = project.id,
          tableName = relation.id,
          columnName = mirrorColumnName,
          isRequired = false,
          isUnique = false,
          isList = field.isList,
          typeIdentifier = field.typeIdentifier
        )))
  }

  override def rollback = Some(DeleteRelationFieldMirrorColumn(project, relation, field).execute)
}
