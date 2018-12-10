package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.ConnectorCapabilities
import com.prisma.shared.models.FieldBehaviour.UpdatedAtBehaviour
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

object UpdatedAtDirective extends FieldDirective[UpdatedAtBehaviour.type] {
  override def name                                              = "updatedAt"
  override def requiredArgs(capabilities: ConnectorCapabilities) = Vector.empty
  override def optionalArgs(capabilities: ConnectorCapabilities) = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: ConnectorCapabilities
  ) = {
    val typeError = (fieldDef.typeName != "DateTime").toOption {
      DeployError(typeDef, fieldDef, s"Fields that are marked as @updatedAt must be of type `DateTime!` or `DateTime`.")
    }
    typeError.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: ConnectorCapabilities) = {
    fieldDef.directive(name).map { _ =>
      UpdatedAtBehaviour
    }
  }
}
