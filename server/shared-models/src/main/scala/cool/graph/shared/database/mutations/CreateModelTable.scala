package cool.graph.shared.database.mutations

import cool.graph.shared.database.{NameConstraints, SqlDDL, SqlDDLMutaction}
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.Model

import scala.util.{Failure, Success}

case class CreateModelTable(projectId: String, model: Model) extends SqlDDLMutaction {
  override def execute = Success(SqlDDL.createTable(projectId = projectId, name = model.name))

  override def rollback = DeleteModelTable(projectId, model).execute

  override def verify() =
    if (NameConstraints.isValidModelName(model.name)) {
      Success(())
    } else {
      Failure(UserInputErrors.InvalidName(name = model.name, entityType = " model"))
    }
}
