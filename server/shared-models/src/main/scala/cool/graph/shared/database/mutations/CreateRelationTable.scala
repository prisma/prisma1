package cool.graph.shared.database.mutations

import cool.graph.shared.database.{SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.{Project, Relation}

import scala.util.Success

case class CreateRelationTable(project: Project, relation: Relation) extends SqlDDLMutaction {
  override def execute = {

    val aModel = project.getModelById_!(relation.modelAId)
    val bModel = project.getModelById_!(relation.modelBId)

    Success(
      SqlDDL
        .createRelationTable(projectId = project.id, tableName = relation.id, aTableName = aModel.name, bTableName = bModel.name))
  }

  override def rollback = DeleteRelationTable(project, relation).execute

}
