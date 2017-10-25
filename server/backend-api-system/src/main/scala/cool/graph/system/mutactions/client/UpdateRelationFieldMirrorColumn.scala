package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.models.{Field, Project, Relation}

import scala.concurrent.Future

case class UpdateRelationFieldMirrorColumn(project: Project, relation: Relation, oldField: Field, newField: Field) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val updateColumn = DatabaseMutationBuilder.updateColumn(
      projectId = project.id,
      tableName = relation.id,
      oldColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, oldField, relation),
      newColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, oldField.copy(name = newField.name), relation),
      newIsRequired = false,
      newIsUnique = false,
      newIsList = newField.isList,
      newTypeIdentifier = newField.typeIdentifier
    )

    Future.successful(ClientSqlStatementResult(sqlAction = updateColumn))
  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = {
    val updateColumn = DatabaseMutationBuilder
      .updateColumn(
        projectId = project.id,
        tableName = relation.id,
        oldColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, oldField.copy(name = newField.name), relation), // use new name for rollback
        newColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, oldField, relation),
        newIsRequired = false,
        newIsUnique = false,
        newIsList = oldField.isList,
        newTypeIdentifier = oldField.typeIdentifier
      )

    Some(Future.successful(ClientSqlStatementResult(sqlAction = updateColumn)))
  }
}
