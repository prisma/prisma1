package com.prisma.api.database.mutactions.validation

import com.prisma.api.database.DatabaseConstraints
import com.prisma.api.mutations.CoolArgs
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.{Field, Model}
import spray.json._

import scala.util.{Failure, Success, Try}

object InputValueValidation {

  def validateDataItemInputs(model: Model, args: CoolArgs): (Try[Unit], List[Field]) = {

    val fieldsWithValues              = InputValueValidation.scalarFieldsWithValues(model, args)
    val fieldsWithIllegallySizedValue = InputValueValidation.checkValueSize(args, fieldsWithValues)
    lazy val extraValues              = args.raw.keys.filter(k => !model.fields.exists(_.name == k) && k != "id").toList
//    lazy val constraintErrors         = checkConstraints(values, fieldsWithValues.filter(_.constraints.nonEmpty))

    val validationResult = () match {
      case _ if extraValues.nonEmpty                   => Failure(APIErrors.ExtraArguments(extraValues, model.name))
      case _ if fieldsWithIllegallySizedValue.nonEmpty => Failure(APIErrors.ValueTooLong(fieldsWithIllegallySizedValue.head.name))
//      case _ if constraintErrors.nonEmpty              => Failure(APIErrors.ConstraintViolated(constraintErrors))
      case _ => Success(())
    }

    (validationResult, fieldsWithValues)
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
