package cool.graph.shared.database.mutations

import cool.graph.shared.database.{SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.{Project, Relation}

import scala.util.Success

case class DeleteRelationTable(project: Project, relation: Relation) extends SqlDDLMutaction {

  override def execute = Success(SqlDDL.dropTable(projectId = project.id, tableName = relation.id))

  override def rollback = CreateRelationTable(project, relation).execute

}
