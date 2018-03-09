package com.prisma.api.database.mutactions.validation

import com.prisma.api.database.DatabaseConstraints
import com.prisma.api.database.mutactions.MutactionVerificationSuccess
import com.prisma.api.mutations.CoolArgs
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.{Field, Model}
import spray.json._

import scala.util.{Failure, Success, Try}

object InputValueValidation {

  def validateDataItemInputs(model: Model, args: CoolArgs): (Try[MutactionVerificationSuccess], List[Field]) = {

    val fieldsWithValues              = InputValueValidation.scalarFieldsWithValues(model, args)
    val fieldsWithIllegallySizedValue = InputValueValidation.checkValueSize(args, fieldsWithValues)
    lazy val extraValues              = args.raw.keys.filter(k => !model.fields.exists(_.name == k) && k != "id").toList
//    lazy val constraintErrors         = checkConstraints(values, fieldsWithValues.filter(_.constraints.nonEmpty))

    val validationResult = () match {
      case _ if extraValues.nonEmpty                   => Failure(APIErrors.ExtraArguments(extraValues, model.name))
      case _ if fieldsWithIllegallySizedValue.nonEmpty => Failure(APIErrors.ValueTooLong(fieldsWithIllegallySizedValue.head.name))
//      case _ if constraintErrors.nonEmpty              => Failure(APIErrors.ConstraintViolated(constraintErrors))
      case _ => Success(MutactionVerificationSuccess())
    }

    (validationResult, fieldsWithValues)
  }

//  def validateRequiredScalarFieldsHaveValues(model: Model, input: List[ArgumentValue]) = {
//    val requiredFieldNames = model.scalarFields
//      .filter(_.isRequired)
//      .filter(_.defaultValue.isEmpty)
//      .map(_.name)
//      .filter(name => name != "createdAt" && name != "updatedAt")
//
//    val missingRequiredFieldNames = requiredFieldNames.filter(name => !input.map(_.name).contains(name))
//    missingRequiredFieldNames
//  }

//  def argumentValueTypeValidation(field: Field, value: Any): Any = {
//
//    def parseOne(value: Any): Boolean = {
//      val result = (field.typeIdentifier, value) match {
//        case (TypeIdentifier.String, _: String)    => true
//        case (TypeIdentifier.Int, x: BigDecimal)   => x.isValidLong
//        case (TypeIdentifier.Int, _: Integer)      => true
//        case (TypeIdentifier.Float, x: BigDecimal) => x.isDecimalDouble
//        case (TypeIdentifier.Float, _: Double)     => true
//        case (TypeIdentifier.Float, _: Float)      => true
//        case (TypeIdentifier.Boolean, _: Boolean)  => true
//        case (TypeIdentifier.DateTime, x)          => CustomScalarTypes.parseDate(x.toString).isRight
//        case (TypeIdentifier.GraphQLID, x: String) => NameConstraints.isValidDataItemId(x)
//        case (TypeIdentifier.Enum, x: String)      => NameConstraints.isValidEnumValueName(x)
//        case (TypeIdentifier.Json, x)              => validateJson(x)
//        case _                                     => false
//        // relations not handled for now
//      }
//      result
//    }
//
//    val validTypeForField = (field.isList, value) match {
//      case (_, None)                   => true
//      case (true, values: Vector[Any]) => values.map(parseOne).forall(identity)
//      case (false, singleValue)        => parseOne(singleValue)
//      case _                           => false
//    }
//
//    if (!validTypeForField) throw APIErrors.InputInvalid(value.toString, field.name, field.typeIdentifier.toString)
//
//  }

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
//
//  def checkConstraints(values: List[ArgumentValue], updatedFields: List[Field]): String = {
//    val constraintErrors = updatedFields
//      .filter(field => values.exists(v => v.name == field.name && v.value != None))
//      .flatMap(field => checkConstraintsOnField(field, values.filter(_.name == field.name).head.unwrappedValue))
//
//    constraintErrors
//      .map { error =>
//        s" The inputvalue: '${error.value.toString}' violated the constraint '${error.constraintType}' with value: '${error.arg.toString} "
//      }
//      .mkString("\n")
//  }

  def checkValueSize(args: CoolArgs, updatedFields: List[Field]): List[Field] = {
    updatedFields
      .filter(field => args.hasArgFor(field) && args.getUnwrappedFieldValue(field) != None)
      .filter(field => !DatabaseConstraints.isValueSizeValid(args.getUnwrappedFieldValue(field), field))
  }

  def scalarFieldsWithValues(model: Model, args: CoolArgs): List[Field] = {
    model.scalarFields.filter(field => args.hasArgFor(field)).filter(_.name != "id")
  }

//  def transformStringifiedJson(argValues: List[ArgumentValue], model: Model): List[ArgumentValue] = {
//
//    def isJson(arg: ArgumentValue): Boolean = model.fields.exists(field => field.name == arg.name && field.typeIdentifier == TypeIdentifier.Json)
//
//    def transformJson(argValue: ArgumentValue): ArgumentValue = {
//
//      def tryParsingValueAsJson(x: JsString): JsValue = {
//        try {
//          x.value.parseJson
//        } catch {
//          case e: ParsingException => throw APIErrors.ValueNotAValidJson(argValue.name, x.prettyPrint)
//        }
//      }
//
//      def transformSingleJson(single: Any): JsValue = {
//        single match {
//          case x: JsString => tryParsingValueAsJson(x)
//          case x: JsObject => x
//          case x: JsArray  => x
//          case x           => throw APIErrors.ValueNotAValidJson(argValue.name, x.toString)
//        }
//      }
//
//      def transformListJson(list: Vector[Any]): Vector[JsValue] = list.map(transformSingleJson)
//
//      val field = model.fields.find(_.name == argValue.name).getOrElse(sys.error("ArgumentValues need to have a field on the Model"))
//      val transformedValue = field.isList match {
//        case true =>
//          argValue.value match {
//            case Some(x) => Some(transformListJson(x.asInstanceOf[Vector[Any]]))
//            case None    => None
//            case x       => Some(transformListJson(x.asInstanceOf[Vector[Any]]))
//          }
//        case false =>
//          argValue.value match {
//            case Some(x) => Some(transformSingleJson(x))
//            case None    => None
//            case x       => Some(transformSingleJson(x))
//          }
//      }
//      argValue.copy(value = transformedValue)
//    }
//
//    val argsWithoutJson     = argValues.filter(!isJson(_))
//    val argsWithJson        = argValues.filter(isJson)
//    val argsWithEscapedJson = argsWithJson.map(transformJson)
//
//    argsWithoutJson ++ argsWithEscapedJson
//  }
}
