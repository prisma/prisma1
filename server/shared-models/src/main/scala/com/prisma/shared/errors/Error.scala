package com.prisma.shared.errors

//abstract class UserFacingError(message: String, errorCode: Int) extends Exception {
//  val code: Int = errorCode
//}

trait SchemaCheckResult

trait WithSchemaError {
  def schemaError: Option[SchemaError] = None
}

abstract class SystemApiError(message: String, errorCode: Int)                     extends Exception with WithSchemaError
case class SchemaError(`type`: String, description: String, field: Option[String]) extends SchemaCheckResult

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
