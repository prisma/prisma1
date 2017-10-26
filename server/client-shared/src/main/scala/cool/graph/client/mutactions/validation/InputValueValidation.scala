package cool.graph.client.mutactions.validation

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.mutactions.validation.ConstraintValueValidation._
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.errors.UserAPIErrors.ValueTooLong
import cool.graph.shared.errors.UserInputErrors.InvalidValueForScalarType
import cool.graph.shared.models.{Field, Model, TypeIdentifier}
import cool.graph.shared.mutactions.MutationTypes.ArgumentValue
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.shared.{DatabaseConstraints, NameConstraints}
import spray.json.JsonParser.ParsingException
import spray.json._

import scala.util.{Failure, Success, Try}

object InputValueValidation {

  def validateDataItemInputs(model: Model, id: Id, values: List[ArgumentValue]): (Try[MutactionVerificationSuccess], List[Field]) = {

    val fieldsWithValues              = InputValueValidation.fieldsWithValues(model, values)
    val fieldsWithIllegallySizedValue = InputValueValidation.checkValueSize(values, fieldsWithValues)
    lazy val extraValues              = values.filter(v => !model.fields.exists(_.name == v.name) && v.name != "id")
    lazy val constraintErrors         = checkConstraints(values, fieldsWithValues.filter(_.constraints.nonEmpty))

    val validationResult = () match {
      case _ if !NameConstraints.isValidDataItemId(id) => Failure(UserAPIErrors.IdIsInvalid(id))
      case _ if extraValues.nonEmpty                   => Failure(UserAPIErrors.ExtraArguments(extraValues.map(_.name), model.name))
      case _ if fieldsWithIllegallySizedValue.nonEmpty => Failure(UserAPIErrors.ValueTooLong(fieldsWithIllegallySizedValue.head.name))
      case _ if constraintErrors.nonEmpty              => Failure(UserAPIErrors.ConstraintViolated(constraintErrors))
      case _                                           => Success(MutactionVerificationSuccess())
    }

    (validationResult, fieldsWithValues)
  }

  def validateRequiredScalarFieldsHaveValues(model: Model, input: List[ArgumentValue]) = {
    val requiredFieldNames = model.fields
      .filter(_.isRequired)
      .filter(_.isScalar)
      .filter(_.defaultValue.isEmpty)
      .map(_.name)
      .filter(name => name != "createdAt" && name != "updatedAt")

    val missingRequiredFieldNames = requiredFieldNames.filter(name => !input.map(_.name).contains(name))
    missingRequiredFieldNames
  }

  def argumentValueTypeValidation(field: Field, value: Any): Any = {

    def parseOne(value: Any): Boolean = {
      val result = (field.typeIdentifier, value) match {
        case (TypeIdentifier.String, _: String)    => true
        case (TypeIdentifier.Int, x: BigDecimal)   => x.isValidLong
        case (TypeIdentifier.Int, _: Integer)      => true
        case (TypeIdentifier.Float, x: BigDecimal) => x.isDecimalDouble
        case (TypeIdentifier.Float, _: Double)     => true
        case (TypeIdentifier.Float, _: Float)      => true
        case (TypeIdentifier.Boolean, _: Boolean)  => true
        case (TypeIdentifier.Password, _: String)  => true
        case (TypeIdentifier.DateTime, x)          => CustomScalarTypes.parseDate(x.toString).isRight
        case (TypeIdentifier.GraphQLID, x: String) => NameConstraints.isValidDataItemId(x)
        case (TypeIdentifier.Enum, x: String)      => NameConstraints.isValidEnumValueName(x)
        case (TypeIdentifier.Json, x)              => validateJson(x)
        case _                                     => false
        // relations not handled for now
      }
      result
    }

    val validTypeForField = (field.isList, value) match {
      case (_, None)                   => true
      case (true, values: Vector[Any]) => values.map(parseOne).forall(identity)
      case (false, singleValue)        => parseOne(singleValue)
      case _                           => false
    }

    if (!validTypeForField) throw UserAPIErrors.InputInvalid(value.toString, field.name, field.typeIdentifier.toString)

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

  def checkConstraints(values: List[ArgumentValue], updatedFields: List[Field]): String = {
    val constraintErrors = updatedFields
      .filter(field => values.exists(v => v.name == field.name && v.value != None))
      .flatMap(field => checkConstraintsOnField(field, values.filter(_.name == field.name).head.unwrappedValue))

    constraintErrors
      .map { error =>
        s" The inputvalue: '${error.value.toString}' violated the constraint '${error.constraintType}' with value: '${error.arg.toString} "
      }
      .mkString("\n")
  }

  def checkValueSize(values: List[ArgumentValue], updatedFields: List[Field]): List[Field] = {
    updatedFields
      .filter(field => values.exists(v => v.name == field.name && v.value != None))
      .filter(field => !DatabaseConstraints.isValueSizeValid(values.filter(v => v.name == field.name).head.unwrappedValue, field))
  }

  def fieldsWithValues(model: Model, values: List[ArgumentValue]): List[Field] = {
    model.fields.filter(field => values.exists(_.name == field.name)).filter(_.name != "id")
  }

  def transformStringifiedJson(argValues: List[ArgumentValue], model: Model): List[ArgumentValue] = {

    def isJson(arg: ArgumentValue): Boolean = model.fields.exists(field => field.name == arg.name && field.typeIdentifier == TypeIdentifier.Json)

    def transformJson(argValue: ArgumentValue): ArgumentValue = {

      def tryParsingValueAsJson(x: JsString): JsValue = {
        try {
          x.value.parseJson
        } catch {
          case e: ParsingException => throw UserAPIErrors.ValueNotAValidJson(argValue.name, x.prettyPrint)
        }
      }

      def transformSingleJson(single: Any): JsValue = {
        single match {
          case x: JsString => tryParsingValueAsJson(x)
          case x: JsObject => x
          case x: JsArray  => x
          case x           => throw UserAPIErrors.ValueNotAValidJson(argValue.name, x.toString)
        }
      }

      def transformListJson(list: Vector[Any]): Vector[JsValue] = list.map(transformSingleJson)

      val field = model.fields.find(_.name == argValue.name).getOrElse(sys.error("ArgumentValues need to have a field on the Model"))
      val transformedValue = field.isList match {
        case true =>
          argValue.value match {
            case Some(x) => Some(transformListJson(x.asInstanceOf[Vector[Any]]))
            case None    => None
            case x       => Some(transformListJson(x.asInstanceOf[Vector[Any]]))
          }
        case false =>
          argValue.value match {
            case Some(x) => Some(transformSingleJson(x))
            case None    => None
            case x       => Some(transformSingleJson(x))
          }
      }
      argValue.copy(value = transformedValue)
    }

    val argsWithoutJson     = argValues.filter(!isJson(_))
    val argsWithJson        = argValues.filter(isJson)
    val argsWithEscapedJson = argsWithJson.map(transformJson)

    argsWithoutJson ++ argsWithEscapedJson
  }
}
