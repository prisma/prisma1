package com.prisma.deploy.migration.validation.directives
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployErrors
import com.prisma.shared.models.ConnectorCapability.RelationLinkListCapability
import com.prisma.shared.models.{ConnectorCapabilities, RelationStrategy}
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

object TypeDbDirective extends TypeDirective[String] {

  override def name = "db"

  override def validate(document: Document, typeDef: ObjectTypeDefinition, directive: Directive, capabilities: ConnectorCapabilities) = {
    val errors = typeDef.isEmbedded.toOption(DeployErrors.embeddedTypesMustNotSpecifyDbName(typeDef.name))
    errors.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, capabilities: ConnectorCapabilities) = {
    typeDef.dbName
  }

}

object FieldDbDirective extends FieldDirective[String] {
  override def name                                              = TypeDbDirective.name
  override def requiredArgs(capabilities: ConnectorCapabilities) = Vector.empty
  override def optionalArgs(capabilities: ConnectorCapabilities) = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: ConnectorCapabilities
  ) = {
    val isInlineExplicitly    = RelationDirective.value(document, typeDef, fieldDef, capabilities).map(_.strategy).contains(Some(RelationStrategy.Inline))
    val isInlineAutomatically = capabilities.hasNot(RelationLinkListCapability) && !fieldDef.isList
    val isInline              = isInlineExplicitly || isInlineAutomatically
    val errors = (fieldDef.isRelationField(document) && !isInline).toOption {
      DeployErrors.relationFieldsMustNotSpecifyDbName(typeDef, fieldDef)
    }
    errors.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: ConnectorCapabilities) = {
    fieldDef.dbName
  }

}
