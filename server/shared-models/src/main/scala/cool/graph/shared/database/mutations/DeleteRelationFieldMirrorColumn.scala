package cool.graph.shared.database.mutations

import cool.graph.shared.database.{RelationFieldMirrorUtils, SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.{Field, Project, Relation}

import scala.util.Success

case class DeleteRelationFieldMirrorColumn(project: Project, relation: Relation, field: Field) extends SqlDDLMutaction {

  override def execute = {

    val mirrorColumnName = RelationFieldMirrorUtils.mirrorColumnName(project, field, relation)

    Success(SqlDDL.deleteColumn(projectId = project.id, tableName = relation.id, columnName = mirrorColumnName))
  }

  override def rollback = CreateRelationFieldMirrorColumn(project, relation, field).execute
}
