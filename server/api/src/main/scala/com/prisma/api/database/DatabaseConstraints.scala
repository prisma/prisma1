package com.prisma.api.database

import com.prisma.shared.models.Field

object DatabaseConstraints {
  def isValueSizeValid(value: Any, field: Field): Boolean = {

    // we can assume that `value` is already sane checked by the query-layer. we only check size here.
    SqlDDL.sqlTypeForScalarTypeIdentifier(isList = field.isList, typeIdentifier = field.typeIdentifier) match {
      case "char(25)" => value.toString.length <= 25
      // at this level we know by courtesy of the type system that boolean, int and datetime won't be too big for mysql
      case "boolean" | "int" | "datetime(3)" => true
      case "text" | "mediumtext"             => value.toString.length <= 262144
      // plain string is part before decimal point. if part after decimal point is longer than 30 characters, mysql will truncate that without throwing an error, which is fine
      case "Decimal(65,30)" =>
        val asDouble = value match {
          case x: Double     => x
          case x: String     => x.toDouble
          case x: BigDecimal => x.toDouble
          case x: Any        => sys.error("Received an invalid type here. Class: " + x.getClass.toString + " value: " + x.toString)
        }
        BigDecimal(asDouble).underlying().toPlainString.length <= 35
      case "varchar(191)" => value.toString.length <= 191
    }
  }
}
