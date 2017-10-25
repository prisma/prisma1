package cool.graph.shared

import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.models.Field

object NameConstraints {
  def isValidEnumValueName(name: String): Boolean = name.length <= 191 && name.matches("^[A-Z][a-zA-Z0-9_]*$")

  def isValidDataItemId(id: String): Boolean = id.length <= 25 && id.matches("^[a-zA-Z0-9\\-_]*$")

  def isValidFieldName(name: String): Boolean = name.length <= 64 && name.matches("^[a-z][a-zA-Z0-9]*$")

  def isValidEnumTypeName(name: String): Boolean = name.length <= 64 && name.matches("^[A-Z][a-zA-Z0-9_]*$")

  def isValidModelName(name: String): Boolean = name.length <= 64 && name.matches("^[A-Z][a-zA-Z0-9]*$")

  def isValidRelationName(name: String): Boolean = name.length <= 64 && name.matches("^[A-Z][a-zA-Z0-9]*$")

  def isValidProjectName(name: String): Boolean = name.length <= 64 && name.matches("^[a-zA-Z][a-zA-Z0-9\\-_ ]*$")

  def isValidProjectAlias(alias: String): Boolean =
    alias.length <= 64 && alias.matches("^[a-zA-Z0-9\\-_]*$") // we are abusing "" in UpdateProject as replacement for null

  def isValidFunctionName(name: String): Boolean = 1 <= name.length && name.length <= 64 && name.matches("^[a-zA-Z0-9\\-_]*$")
}

object DatabaseConstraints {
  def isValueSizeValid(value: Any, field: Field): Boolean = {

    // we can assume that `value` is already sane checked by the query-layer. we only check size here.
    DatabaseMutationBuilder
      .sqlTypeForScalarTypeIdentifier(isList = field.isList, typeIdentifier = field.typeIdentifier) match {
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
