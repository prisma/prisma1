package cool.graph.shared.database.mutations

import cool.graph.shared.database.{SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.Model

import scala.util.Success

case class RenameTable(projectId: String, model: Model, name: String) extends SqlDDLMutaction {

  def setName(oldName: String, newName: String) =
    Success(SqlDDL.renameTable(projectId = projectId, name = oldName, newName = newName))

  override def execute = setName(oldName = model.name, newName = name)

  override def rollback = setName(name, model.name)

  // todo: verify new name
}
