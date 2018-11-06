package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.FieldBehaviour.CreatedAtBehaviour
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

object CreatedAtDirective extends FieldDirective[CreatedAtBehaviour.type] {
  override def name         = "createdAt"
  override def requiredArgs = Vector.empty
  override def optionalArgs = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    if (fieldDef.typeName == "DateTime" && fieldDef.isRequired) {
      None
    } else {
      Some(DeployError(typeDef, fieldDef, s"Fields that are marked as @createdAt must be of type `DateTime!`."))
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.directive(name).map { _ =>
      CreatedAtBehaviour
    }
  }
}
