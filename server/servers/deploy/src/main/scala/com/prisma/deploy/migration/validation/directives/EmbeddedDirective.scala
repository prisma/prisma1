package com.prisma.deploy.migration.validation.directives
import com.prisma.shared.models.ConnectorCapability
import sangria.ast.{Directive, Document, ObjectTypeDefinition}
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployErrors
import com.prisma.shared.models.ApiConnectorCapability.EmbeddedTypesCapability

object EmbeddedDirective extends TypeDirective[Boolean] {
  override def name = "embedded"

  override def validate(document: Document, typeDef: ObjectTypeDefinition, directive: Directive, capabilities: Set[ConnectorCapability]) = {
    val embeddedTypesAreSupported = capabilities.contains(EmbeddedTypesCapability)
    val errors                    = (typeDef.isEmbedded && !embeddedTypesAreSupported).toOption(DeployErrors.embeddedTypesAreNotSupported(typeDef.name))
    errors.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, capabilities: Set[ConnectorCapability]) = {
    Some(typeDef.isEmbedded)
  }

}
