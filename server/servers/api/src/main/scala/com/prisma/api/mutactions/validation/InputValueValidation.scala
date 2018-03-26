package com.prisma.api.mutactions.validation

import com.prisma.api.connector.{CoolArgs, ReallyCoolArgs}
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.ClientApiError
import com.prisma.gc_values._
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

  def validateDataItemInputsGC(model: Model, args: ReallyCoolArgs): Option[ClientApiError] = {
    val fieldsWithValues              = InputValueValidation.scalarFieldsWithValuesGC(model, args)
    val fieldsWithIllegallySizedValue = InputValueValidation.checkValueSizeGC(args, fieldsWithValues)
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

  def checkValueSizeGC(args: ReallyCoolArgs, updatedFields: List[Field]): List[Field] = {
    updatedFields
      .filter(field => args.raw.asRoot.map.get(field.name).isDefined && args.getFieldValue(field.name).isDefined)
      .filter(field => !isValueSizeValidGC(args.getFieldValue(field.name).get))
  }

  private def isValueSizeValid(value: Any, field: Field): Boolean = {
    if (field.isScalarList) {
      return true
    }
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

  private def isValueSizeValidGC(value: GCValue) = value match {
    case x: StringGCValue    => x.value.length <= 262144
    case x: JsonGCValue      => x.value.toString.length <= 262144
    case x: IntGCValue       => true
    case x: BooleanGCValue   => true
    case x: DateTimeGCValue  => true
    case x: EnumGCValue      => x.value.length <= 191
    case x: GraphQLIdGCValue => x.value.length <= 25
    case x: FloatGCValue     => BigDecimal(x.value).underlying().toPlainString.length <= 35
    case x: ListGCValue      => sys.error("handle this case")
    case x: RootGCValue      => sys.error("handle this case")
    case NullGCValue         => true
  }

  def scalarFieldsWithValues(model: Model, args: CoolArgs): List[Field] = {
    model.scalarFields.filter(field => args.hasArgFor(field)).filter(_.name != "id")
  }

  def scalarFieldsWithValuesGC(model: Model, args: ReallyCoolArgs): List[Field] = {
    model.scalarFields.filter(field => args.raw.asRoot.map.get(field.name).isDefined).filter(_.name != "id")
  }
}
