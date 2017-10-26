package cool.graph.system.migration.dataSchema.validation

import cool.graph.shared.errors.SystemErrors.SchemaError
import cool.graph.shared.models.Project
import cool.graph.system.migration.dataSchema.{SchemaDiff, SchemaExport, SchemaFileHeader}

import scala.collection.immutable.Seq

case class SchemaValidator(schemaSyntaxValidator: SchemaSyntaxValidator, diffAwareSchemaValidator: DiffAwareSchemaValidator) {
  def validate(): Seq[SchemaError] = {
    schemaSyntaxValidator.validate() ++ diffAwareSchemaValidator.validate()
  }
}

object SchemaValidator {
  def apply(project: Project, newSchema: String, schemaFileHeader: SchemaFileHeader): SchemaValidator = {
    val oldSchema = SchemaExport.renderSchema(project)
    SchemaValidator(
      SchemaSyntaxValidator(newSchema),
      DiffAwareSchemaValidator(SchemaDiff(oldSchema, newSchema), project)
    )
  }
}
