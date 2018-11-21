package com.prisma.deploy.migration.validation.directives
import com.prisma.deploy.migration.validation.DeployErrors
import com.prisma.shared.models.ConnectorCapability
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}
import com.prisma.deploy.migration.DataSchemaAstExtensions._

object TypeDbDirective extends TypeDirective[String] {

  override def name = "db"

  override def validate(document: Document, typeDef: ObjectTypeDefinition, directive: Directive, capabilities: Set[ConnectorCapability]) = {
    val errors = typeDef.isEmbedded.toOption(DeployErrors.embeddedTypesMustNotSpecifyDbName(typeDef.name))
    errors.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, capabilities: Set[ConnectorCapability]) = {
    typeDef.dbName
  }

}

object FieldDbDirective extends FieldDirective[String] {
  override def name                                                 = TypeDbDirective.name
  override def requiredArgs(capabilities: Set[ConnectorCapability]) = Vector.empty
  override def optionalArgs(capabilities: Set[ConnectorCapability]) = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    val errors = fieldDef.isRelationField(document).toOption(DeployErrors.relationFieldsMustNotSpecifyDbName(typeDef, fieldDef))
    errors.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.dbName
  }

}
