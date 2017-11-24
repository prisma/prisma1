package cool.graph.deploy.migration.validation

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
