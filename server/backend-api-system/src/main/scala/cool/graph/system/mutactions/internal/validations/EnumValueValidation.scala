package cool.graph.system.mutactions.internal.validations

import cool.graph.GCDataTypes.{EnumGCValue, ListGCValue}
import cool.graph.MutactionVerificationSuccess
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.errors.UserInputErrors.{DefaultValueIsNotValidEnum, MigrationValueIsNotValidEnum, NoEnumSelectedAlthoughSetToEnumType}
import cool.graph.shared.models.{Field, TypeIdentifier}
import cool.graph.util.json.SprayJsonExtensions

import scala.util.{Failure, Success, Try}

object EnumValueValidation extends SprayJsonExtensions {

  def validateEnumField(migrationValue: Option[String], field: Field): Try[MutactionVerificationSuccess] = {

    field.typeIdentifier match {
      case TypeIdentifier.Enum if field.enum.isEmpty =>
        Failure(NoEnumSelectedAlthoughSetToEnumType(field.name))

      case TypeIdentifier.Enum =>
        val enum = field.enum.get
        (field.isList, field.defaultValue, migrationValue) match {
          case (false, Some(dV), _) if !dV.isInstanceOf[EnumGCValue] =>
            Failure(DefaultValueIsNotValidEnum(dV.toString))
          case (false, Some(dV: EnumGCValue), _) if !enum.values.contains(dV.value) =>
            Failure(DefaultValueIsNotValidEnum(dV.value))
          case (false, _, Some(mV)) if !enum.values.contains(mV) =>
            Failure(MigrationValueIsNotValidEnum(mV))
          case (true, Some(dV), _) if !dV.isInstanceOf[ListGCValue] =>
            Failure(DefaultValueIsNotValidEnum(dV.toString))
          case (true, Some(dV: ListGCValue), _) if newValidateEnumListInput(dV.getEnumVector, field).nonEmpty =>
            Failure(DefaultValueIsNotValidEnum(validateEnumListInput(dV.toString, field).mkString(",")))

          case (true, _, Some(mV)) if validateEnumListInput(mV, field).nonEmpty =>
            Failure(MigrationValueIsNotValidEnum(validateEnumListInput(mV, field).mkString(",")))

          case _ =>
            Success(MutactionVerificationSuccess())
        }

      case _ =>
        Success(MutactionVerificationSuccess())
    }
  }

  def validateEnumValues(enumValues: Seq[String]): Try[MutactionVerificationSuccess] = {
    lazy val invalidEnumValueNames = enumValues.filter(!NameConstraints.isValidEnumValueName(_))

    () match {
      case _ if enumValues.isEmpty             => Failure(UserInputErrors.MissingEnumValues())
      case _ if invalidEnumValueNames.nonEmpty => Failure(UserInputErrors.InvalidNameMustStartUppercase(invalidEnumValueNames.mkString(",")))
      case _                                   => Success(MutactionVerificationSuccess())
    }
  }

  def validateEnumListInput(input: String, field: Field): Seq[String] = {
    val inputWithoutWhitespace = input.replaceAll(" ", "")

    inputWithoutWhitespace match {
      case "[]" =>
        Seq.empty

      case _ =>
        val values = inputWithoutWhitespace.stripPrefix("[").stripSuffix("]").split(",")
        values.collect { case value if !field.enum.get.values.contains(value) => value }
    }
  }

  def newValidateEnumListInput(input: Vector[String], field: Field): Vector[String] = {
    input.collect { case value if !field.enum.get.values.contains(value) => value }
  }

}
