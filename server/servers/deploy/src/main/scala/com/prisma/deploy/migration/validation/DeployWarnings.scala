package com.prisma.deploy.migration.validation

import com.prisma.shared.errors.SchemaCheckResult

case class DeployWarning(`type`: String, description: String, field: Option[String]) extends SchemaCheckResult

object DeployWarnings {
  def apply(`type`: String, field: String, description: String): DeployWarning = {
    DeployWarning(`type`, description, Some(field))
  }

  def apply(`type`: String, description: String): DeployWarning = {
    DeployWarning(`type`, description, None)
  }

  def dataLossModel(`type`: String): DeployWarning = {
    DeployWarning(`type`, "You already have nodes for this model. This change will result in data loss.", None)
  }

  def dataLossRelation(`type`: String): DeployWarning = {
    DeployWarning(`type`, "You already have nodes for this relation. This change will result in data loss.", None)
  }

  def dataLossField(`type`: String, field: String): DeployWarning = {
    DeployWarning(`type`, "You already have nodes for this model. This change may result in data loss.", Some(field))
  }
}
