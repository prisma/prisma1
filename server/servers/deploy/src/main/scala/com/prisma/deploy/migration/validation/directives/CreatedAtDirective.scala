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
    val typeError = (fieldDef.typeName != "DateTime").toOption {
      DeployError(typeDef, fieldDef, s"Fields that are marked as `@$name` must be of type `DateTime!` or `DateTime`.")
    }
    val simultaneousDirectives = fieldDef.directive(UpdatedAtDirective.name).isDefined.toOption {
      DeployError(typeDef, fieldDef, s"Fields cannot be marked simultaneously with `@$name` and `@${UpdatedAtDirective.name}`.")
    }
    typeError.orElse(simultaneousDirectives)
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.directive(name).map { _ =>
      CreatedAtBehaviour
    }
  }
}
