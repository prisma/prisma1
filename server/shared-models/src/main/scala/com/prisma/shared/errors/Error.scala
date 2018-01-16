package cool.graph.shared.errors

abstract class UserFacingError(message: String, errorCode: Int) extends Exception {
  val code: Int = errorCode
}

trait WithSchemaError {
  def schemaError: Option[SchemaError] = None
}

abstract class SystemApiError(message: String, errorCode: Int) extends UserFacingError(message, errorCode) with WithSchemaError
case class SchemaError(`type`: String, description: String, field: Option[String])

object SchemaError {
  def apply(`type`: String, field: String, description: String): SchemaError = {
    SchemaError(`type`, description, Some(field))
  }

  def apply(`type`: String, description: String): SchemaError = {
    SchemaError(`type`, description, None)
  }

  def global(description: String): SchemaError = {
    SchemaError("Global", description, None)
  }
}
