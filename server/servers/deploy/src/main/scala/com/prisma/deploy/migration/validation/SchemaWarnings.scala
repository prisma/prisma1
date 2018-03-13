package com.prisma.deploy.migration.validation

import com.prisma.shared.errors.SchemaCheckResult

case class SchemaWarning(`type`: String, description: String, field: Option[String]) extends SchemaCheckResult

object SchemaWarning {
  def apply(`type`: String, field: String, description: String): SchemaWarning = {
    SchemaWarning(`type`, description, Some(field))
  }

  def apply(`type`: String, description: String): SchemaWarning = {
    SchemaWarning(`type`, description, None)
  }

  def dataLossModel(`type`: String): SchemaWarning = {
    SchemaWarning(`type`, "You already have nodes for this model. This change will result in data loss.", None)
  }

  def dataLossField(`type`: String, field: String): SchemaWarning = {
    SchemaWarning(`type`, "You already have nodes for this model. This change may result in data loss.", Some(field))
  }
}
