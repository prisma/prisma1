package cool.graph.system.mutactions.internal.validations

import cool.graph.GCDataTypes.{GCStringConverter, NullGCValue}
import cool.graph.MutactionVerificationSuccess
import cool.graph.shared.DatabaseConstraints
import cool.graph.shared.errors.UserAPIErrors.ValueTooLong
import cool.graph.shared.errors.UserInputErrors.InvalidValueForScalarType
import cool.graph.shared.models.Field
import cool.graph.shared.schema.CustomScalarTypes.isValidScalarType
import cool.graph.GCDataTypes.OtherGCStuff.isValidGCValueForField

import scala.util.{Failure, Success, Try}

object MigrationAndDefaultValueValidation {

  def validateDefaultValue(field: Field): Try[MutactionVerificationSuccess] = {
    field.defaultValue match {
      case Some(defValue) if !isValidGCValueForField(defValue, field) => Failure(InvalidValueForScalarType(defValue.toString, field.typeIdentifier))
      case Some(defValue)
          if !defValue.isInstanceOf[NullGCValue] && !DatabaseConstraints
            .isValueSizeValid(GCStringConverter(field.typeIdentifier, field.isList).fromGCValue(defValue), field) =>
        Failure(ValueTooLong("DefaultValue"))
      case _ => Success(MutactionVerificationSuccess())
    }
  }

  def validateMigrationValue(migrationValue: Option[String], field: Field): Try[MutactionVerificationSuccess] = {
    migrationValue match {
      case Some(migValue) if !isValidScalarType(migValue, field)                    => Failure(InvalidValueForScalarType(migValue, field.typeIdentifier))
      case Some(migValue) if !DatabaseConstraints.isValueSizeValid(migValue, field) => Failure(ValueTooLong("MigrationValue"))
      case _                                                                        => Success(MutactionVerificationSuccess())
    }
  }

  def validateMigrationAndDefaultValue(migrationValue: Option[String], field: Field): Try[MutactionVerificationSuccess] = {

    lazy val defaultValueValidationResult   = validateDefaultValue(field)
    lazy val migrationValueValidationResult = validateMigrationValue(migrationValue, field)

    field.isScalar match {
      case true if defaultValueValidationResult.isFailure   => defaultValueValidationResult
      case true if migrationValueValidationResult.isFailure => migrationValueValidationResult
      case _                                                => Success(MutactionVerificationSuccess())
    }
  }

}
