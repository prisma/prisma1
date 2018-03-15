package com.prisma.api.mutactions.validation

import com.prisma.api.connector.CoolArgs
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.ClientApiError
import com.prisma.shared.models.{Field, Model, TypeIdentifier}
import spray.json._

import scala.util.{Failure, Success, Try}

object InputValueValidation {

  def validateDataItemInputs(model: Model, args: CoolArgs): Option[ClientApiError] = {
    val fieldsWithValues              = InputValueValidation.scalarFieldsWithValues(model, args)
    val fieldsWithIllegallySizedValue = InputValueValidation.checkValueSize(args, fieldsWithValues)
    () match {
      case _ if fieldsWithIllegallySizedValue.nonEmpty => Some(APIErrors.ValueTooLong(fieldsWithIllegallySizedValue.head.name))
      case _                                           => None
    }
  }

  def validateJson(input: Any): Boolean = {
    Try { input.toString } match {
      case Failure(_) =>
        false

      case Success(string) =>
        Try { string.parseJson } match {
          case Failure(_) =>
            false

          case Success(json) =>
            json match {
              case _: JsArray  => true
              case _: JsObject => true
              case _           => false
            }
        }
    }
  }

  def checkValueSize(args: CoolArgs, updatedFields: List[Field]): List[Field] = {
    updatedFields
      .filter(field => args.hasArgFor(field) && args.getUnwrappedFieldValue(field) != None)
      .filter(field => !isValueSizeValid(args.getUnwrappedFieldValue(field), field))
  }

  private def isValueSizeValid(value: Any, field: Field): Boolean = {
    require(field.isScalarNonList)
    field.typeIdentifier match {
      case TypeIdentifier.String | TypeIdentifier.Json =>
        value.toString.length <= 262144

      case TypeIdentifier.Boolean | TypeIdentifier.Int | TypeIdentifier.DateTime =>
        true

      case TypeIdentifier.GraphQLID =>
        value.toString.length <= 25

      case TypeIdentifier.Enum =>
        value.toString.length <= 191

      case TypeIdentifier.Float =>
        val asDouble = value match {
          case x: Double     => x
          case x: String     => x.toDouble
          case x: BigDecimal => x.toDouble
          case x: Any        => sys.error("Received an invalid type here. Class: " + x.getClass.toString + " value: " + x.toString)
        }
        BigDecimal(asDouble).underlying().toPlainString.length <= 35

      case TypeIdentifier.Relation =>
        sys.error("Relation is not a scalar type. Are you trying to create a db column for a relation?")
    }
  }

  def scalarFieldsWithValues(model: Model, args: CoolArgs): List[Field] = {
    model.scalarFields.filter(field => args.hasArgFor(field)).filter(_.name != "id")
  }
}
