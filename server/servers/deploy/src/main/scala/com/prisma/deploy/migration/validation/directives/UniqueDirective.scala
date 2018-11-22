package com.prisma.deploy.migration.validation.directives
import com.prisma.deploy.migration.validation.DeployErrors
import com.prisma.shared.models.ConnectorCapabilities
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

object UniqueDirective extends FieldDirective[Boolean] {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  override def name                                              = "unique"
  override def requiredArgs(capabilities: ConnectorCapabilities) = Vector.empty
  override def optionalArgs(capabilities: ConnectorCapabilities) = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: ConnectorCapabilities
  ) = {
    val error = typeDef.isEmbedded.toOption {
      DeployErrors.uniqueDisallowedOnEmbeddedTyps(typeDef, fieldDef)
    }
    error.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: ConnectorCapabilities) = {
    fieldDef.isUnique.toOption {
      fieldDef.isUnique
    }
  }
}
