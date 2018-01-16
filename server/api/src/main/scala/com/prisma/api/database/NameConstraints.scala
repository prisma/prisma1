package cool.graph.api.database

object NameConstraints {
  def isValidEnumValueName(name: String): Boolean = name.length <= 191 && name.matches("^[A-Z][a-zA-Z0-9_]*$")

  def isValidDataItemId(id: String): Boolean = id.length <= 25 && id.matches("^[a-zA-Z0-9\\-_]*$")
}
