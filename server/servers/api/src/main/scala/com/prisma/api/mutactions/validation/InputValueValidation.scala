package com.prisma.api.mutactions.validation

import com.prisma.api.connector.CoolArgs
import com.prisma.api.connector.mysql.database.DatabaseConstraints
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.ClientApiError
import com.prisma.shared.models.{Field, Model}
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
      .filter(field => !DatabaseConstraints.isValueSizeValid(args.getUnwrappedFieldValue(field), field))
  }

  def scalarFieldsWithValues(model: Model, args: CoolArgs): List[Field] = {
    model.scalarFields.filter(field => args.hasArgFor(field)).filter(_.name != "id")
  }
}
