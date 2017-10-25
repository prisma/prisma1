package cool.graph.system.mutactions.internal.validations

import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors.{InvalidName, TypeAlreadyExists}
import cool.graph.shared.models.Project

import scala.util.{Failure, Success, Try}

object TypeNameValidation {

  def validateModelName(project: Project, modelName: String): Try[Unit] = {
    // we intentionally just validate against the enum names because CreateModel needs the model name validation to not happen
    validateAgainstEnumNames(project, modelName, NameConstraints.isValidModelName)
  }

  def validateEnumName(project: Project, modelName: String): Try[Unit] = {
    validateTypeName(project, modelName, NameConstraints.isValidEnumTypeName)
  }

  def validateTypeName(project: Project, typeName: String, validateName: String => Boolean): Try[Unit] = {
    val modelWithNameExists = project.getModelByName(typeName).isDefined
    if (modelWithNameExists) {
      Failure(TypeAlreadyExists(typeName))
    } else {
      validateAgainstEnumNames(project, typeName, validateName)
    }
  }

  def validateAgainstEnumNames(project: Project, typeName: String, validateName: String => Boolean): Try[Unit] = {
    val enumWithNameExists = project.getEnumByName(typeName).isDefined
    val isValidTypeName    = validateName(typeName)
    if (!isValidTypeName) {
      Failure(InvalidName(typeName))
    } else if (enumWithNameExists) {
      Failure(TypeAlreadyExists(typeName))
    } else {
      Success(())
    }
  }
}
