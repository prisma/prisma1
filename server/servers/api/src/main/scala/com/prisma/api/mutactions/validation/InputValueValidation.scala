package com.prisma.api.mutactions.validation

import com.prisma.api.connector.PrismaArgs
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.ClientApiError
import com.prisma.gc_values._
import com.prisma.shared.models.{Model, ScalarField}

object InputValueValidation {

  def validateDataItemInputs(model: Model, args: PrismaArgs): Option[ClientApiError] = {
    val fieldsWithValues              = InputValueValidation.scalarFieldsWithValues(model, args)
    val fieldsWithIllegallySizedValue = InputValueValidation.checkValueSize(args, fieldsWithValues)
    () match {
      case _ if fieldsWithIllegallySizedValue.nonEmpty => Some(APIErrors.ValueTooLong(fieldsWithIllegallySizedValue.head.name))
      case _                                           => None
    }
  }

  def checkValueSize(args: PrismaArgs, updatedFields: List[ScalarField]): List[ScalarField] = {
    updatedFields
      .filter(field => args.hasArgFor(field))
      .filter(field => !isValueSizeValid(args.getFieldValue(field.name).get))
  }

  private def isValueSizeValid(value: GCValue) = value match {
    case x: StringGCValue   => x.value.length <= 262144
    case x: JsonGCValue     => x.value.toString.length <= 262144
    case x: IntGCValue      => true
    case x: BooleanGCValue  => true
    case x: DateTimeGCValue => true
    case x: EnumGCValue     => x.value.length <= 191
    case x: StringIdGCValue => x.value.length <= 25
    case x: UuidGCValue     => true
    case x: FloatGCValue    => BigDecimal(x.value).underlying().toPlainString.length <= 35
    case x: ListGCValue     => sys.error("handle this case")
    case x: RootGCValue     => sys.error("handle this case")
    case NullGCValue        => true
  }

  def scalarFieldsWithValues(model: Model, args: PrismaArgs): List[ScalarField] = {
    model.scalarFields.filter(field => args.getFieldValue(field.name).isDefined).filter(_.name != "id")
  }
}
