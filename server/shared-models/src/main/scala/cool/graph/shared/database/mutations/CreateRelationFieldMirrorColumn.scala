package cool.graph.shared.database.mutations

import cool.graph.shared.database.{RelationFieldMirrorUtils, SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.{Field, Project, Relation}

import scala.util.Success

case class CreateRelationFieldMirrorColumn(project: Project, relation: Relation, field: Field) extends SqlDDLMutaction {
  override def execute = {

    val mirrorColumnName = RelationFieldMirrorUtils.mirrorColumnName(project, field, relation)

    // Note: we don't need unique index or null constraints on mirrored fields

    Success(
      SqlDDL.createColumn(
        projectId = project.id,
        tableName = relation.id,
        columnName = mirrorColumnName,
        isRequired = false,
        isUnique = false,
        isList = field.isList,
        typeIdentifier = field.typeIdentifier
      ))
  }

  override def rollback = DeleteRelationFieldMirrorColumn(project, relation, field).execute
}
