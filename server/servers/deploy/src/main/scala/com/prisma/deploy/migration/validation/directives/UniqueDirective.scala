package com.prisma.deploy.migration.validation.directives
import com.prisma.deploy.migration.validation.DeployErrors
import com.prisma.shared.models.ConnectorCapability
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

object UniqueDirective extends FieldDirective[Boolean] {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  override def name         = "unique"
  override def requiredArgs = Vector.empty
  override def optionalArgs = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    val error = typeDef.isEmbedded.toOption {
      DeployErrors.uniqueDisallowedOnEmbeddedTyps(typeDef, fieldDef)
    }
    error.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.isUnique.toOption {
      fieldDef.isUnique
    }
  }
}
