package com.prisma.deploy.migration.validation

sealed trait DeployResult
case class DeployError(`type`: String, description: String, field: Option[String])   extends DeployResult
case class DeployWarning(`type`: String, description: String, field: Option[String]) extends DeployResult

object DeployError {
  def apply(`type`: String, field: String, description: String): DeployError = {
    DeployError(`type`, description, Some(field))
  }

  def apply(`type`: String, description: String): DeployError = {
    DeployError(`type`, description, None)
  }

  def apply(fieldAndType: FieldAndType, description: String): DeployError = {
    apply(fieldAndType.objectType.name, fieldAndType.fieldDef.name, description)
  }

  def global(description: String): DeployError = {
    DeployError("Global", description, None)
  }
}
