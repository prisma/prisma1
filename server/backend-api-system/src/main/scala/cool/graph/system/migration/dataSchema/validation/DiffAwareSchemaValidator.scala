package cool.graph.system.migration.dataSchema.validation

import cool.graph.shared.errors.SystemErrors.SchemaError
import cool.graph.shared.models.Project
import cool.graph.system.database.SystemFields
import cool.graph.system.migration.dataSchema.SchemaDiff

import scala.collection.immutable.Seq
import scala.util.{Failure, Success, Try}

case class DiffAwareSchemaValidator(diffResultTry: Try[SchemaDiff], project: Project) {

  def validate(): Seq[SchemaError] = {
    diffResultTry match {
      case Success(schemaDiff) => validateInternal(schemaDiff)
      case Failure(e)          => List.empty // the Syntax Validator already returns an error for this case
    }
  }

  def validateInternal(schemaDiff: SchemaDiff): Seq[SchemaError] = {
    validateRemovedFields(schemaDiff) ++ validateRemovedTypes(schemaDiff)
  }

  def validateRemovedTypes(schemaDiff: SchemaDiff): Seq[SchemaError] = {
    for {
      removedType <- schemaDiff.removedTypes
      model       = project.getModelByName_!(removedType)
      if model.isSystem && !project.isEjected
    } yield SchemaErrors.systemTypeCannotBeRemoved(model.name)
  }

  def validateRemovedFields(schemaDiff: SchemaDiff): Seq[SchemaError] = {
    for {
      updatedType  <- schemaDiff.updatedTypes
      model        = project.getModelByName_!(updatedType.oldName)
      removedField <- updatedType.removedFields
      field        = model.getFieldByName_!(removedField)
      if field.isSystem && !SystemFields.isDeletableSystemField(field.name)
    } yield SchemaErrors.systemFieldCannotBeRemoved(updatedType.name, field.name)
  }
}
