package cool.graph.deploy.validation

object NameConstraints {
  def isValidEnumValueName(name: String): Boolean = name.length <= 191 && name.matches("^[A-Z][a-zA-Z0-9_]*$")

  def isValidDataItemId(id: String): Boolean = id.length <= 25 && id.matches("^[a-zA-Z0-9\\-_]*$")

  def isValidFieldName(name: String): Boolean = name.length <= 64 && name.matches("^[a-z][a-zA-Z0-9]*$")

  def isValidEnumTypeName(name: String): Boolean = name.length <= 64 && name.matches("^[A-Z][a-zA-Z0-9_]*$")

  def isValidModelName(name: String): Boolean = name.length <= 64 && name.matches("^[A-Z][a-zA-Z0-9]*$")

  def isValidRelationName(name: String): Boolean = name.length <= 64 && name.matches("^[A-Z][a-zA-Z0-9]*$")

  def isValidServiceName(name: String): Boolean = name.length <= 140 && isValidName(name)

  def isValidServiceStage(stage: String): Boolean = stage.length <= 30 && isValidName(stage)

  private def isValidName(str: String): Boolean = str.matches("^[a-zA-Z][a-zA-Z0-9\\-_]*$")

  def isValidFunctionName(name: String): Boolean = 1 <= name.length && name.length <= 64 && name.matches("^[a-zA-Z0-9\\-_]*$")
}
