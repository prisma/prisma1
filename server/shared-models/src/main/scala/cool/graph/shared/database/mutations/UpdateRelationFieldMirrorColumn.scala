package cool.graph.shared.database.mutations

import cool.graph.shared.database.{RelationFieldMirrorUtils, SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.{Field, Project, Relation}

import scala.util.Success

case class UpdateRelationFieldMirrorColumn(project: Project, relation: Relation, oldField: Field, newField: Field) extends SqlDDLMutaction {

  override def execute =
    Success(
      SqlDDL.updateColumn(
        projectId = project.id,
        tableName = relation.id,
        oldColumnName = RelationFieldMirrorUtils.mirrorColumnName(project, oldField, relation),
        newColumnName = RelationFieldMirrorUtils.mirrorColumnName(project, oldField.copy(name = newField.name), relation),
        newIsRequired = false,
        newIsUnique = false,
        newIsList = newField.isList,
        newTypeIdentifier = newField.typeIdentifier
      ))

  override def rollback =
    Success(
      SqlDDL.updateColumn(
        projectId = project.id,
        tableName = relation.id,
        oldColumnName = RelationFieldMirrorUtils.mirrorColumnName(project, oldField.copy(name = newField.name), relation), // use new name for rollback
        newColumnName = RelationFieldMirrorUtils.mirrorColumnName(project, oldField, relation),
        newIsRequired = false,
        newIsUnique = false,
        newIsList = oldField.isList,
        newTypeIdentifier = oldField.typeIdentifier
      ))
}
