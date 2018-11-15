package com.prisma.deploy.migration.validation.directives
import com.prisma.deploy.migration.validation.DeployErrors
import com.prisma.shared.models.ConnectorCapability
import sangria.ast.{Directive, Document, ObjectTypeDefinition}

object DbDirective extends TypeDirective[String] {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  override def name = "db"

  override def validate(document: Document, typeDef: ObjectTypeDefinition, directive: Directive, capabilities: Set[ConnectorCapability]) = {
    typeDef.isEmbedded.toOption {
      DeployErrors.embeddedTypesMustNotSpecifyDbName(typeDef.name)
    }.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, capabilities: Set[ConnectorCapability]) = {
    typeDef.dbName
  }

}
