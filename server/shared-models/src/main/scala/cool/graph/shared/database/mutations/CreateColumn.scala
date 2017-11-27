package cool.graph.shared.database.mutations

import cool.graph.shared.database.{NameConstraints, SqlDDL, SqlDDLMutaction}
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.{Field, Model}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateColumn(projectId: String, model: Model, field: Field) extends SqlDDLMutaction {

  override def execute =
    Success(
      SqlDDL.createColumn(
        projectId = projectId,
        tableName = model.name,
        columnName = field.name,
        isRequired = field.isRequired,
        isUnique = field.isUnique,
        isList = field.isList,
        typeIdentifier = field.typeIdentifier
      ))

  override def rollback = DeleteColumn(projectId, model, field).execute

  override def verify() = {
    NameConstraints.isValidFieldName(field.name) match {
      case false => Failure(UserInputErrors.InvalidName(name = field.name, entityType = " field"))
      case true  => Success(())
    }
  }
}
