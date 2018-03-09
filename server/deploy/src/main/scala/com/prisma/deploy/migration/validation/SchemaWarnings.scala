package com.prisma.deploy.migration.validation

case class SchemaWarning(`type`: String, description: String, field: Option[String])

object SchemaWarning {
  def apply(`type`: String, field: String, description: String): SchemaWarning = {
    SchemaWarning(`type`, description, Some(field))
  }

  def apply(`type`: String, description: String): SchemaWarning = {
    SchemaWarning(`type`, description, None)
  }

  def apply(fieldAndType: FieldAndType, description: String): SchemaWarning = {
    apply(fieldAndType.objectType.name, fieldAndType.fieldDef.name, description)
  }

  def global(description: String): SchemaWarning = {
    SchemaWarning("Global", description, None)
  }
}

object SchemaWarnings {
  def forceArgumentRequired: SchemaWarning = {
    SchemaWarning.global(
      "Your migration includes potentially destructive changes. Review using `prisma deploy --dry-run` and continue using `prisma deploy --force`.")
  }
}
