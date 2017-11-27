package cool.graph.shared.database.mutations

import cool.graph.shared.database.{SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.{Field, Model}

import scala.util.Success

case class DeleteColumn(projectId: String, model: Model, field: Field) extends SqlDDLMutaction {

  override def execute = Success(SqlDDL.deleteColumn(projectId = projectId, tableName = model.name, columnName = field.name))

  override def rollback = CreateColumn(projectId, model, field).execute
}
