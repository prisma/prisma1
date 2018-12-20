package com.prisma.deploy.migration.validation.directives
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployErrors
import com.prisma.shared.models.ConnectorCapabilities
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import sangria.ast.{Directive, Document, ObjectTypeDefinition}

object EmbeddedDirective extends TypeDirective[Boolean] {
  override def name = "embedded"

  override def validate(document: Document, typeDef: ObjectTypeDefinition, directive: Directive, capabilities: ConnectorCapabilities) = {
    val embeddedTypesAreSupported = capabilities.has(EmbeddedTypesCapability)
    val errors                    = (typeDef.isEmbedded && !embeddedTypesAreSupported).toOption(DeployErrors.embeddedTypesAreNotSupported(typeDef.name))
    errors.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, capabilities: ConnectorCapabilities) = {
    Some(typeDef.isEmbedded)
  }

}
