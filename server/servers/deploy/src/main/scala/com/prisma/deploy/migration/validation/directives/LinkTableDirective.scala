package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.{DeployError, PrismaSdl}
import com.prisma.shared.models.ConnectorCapabilities
import com.prisma.shared.models.ConnectorCapability.RelationLinkTableCapability
import sangria.ast.{Directive, Document, ObjectTypeDefinition}

object LinkTableDirective extends TypeDirective[Boolean] {
  override def name = "linkTable"

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      directive: Directive,
      capabilities: ConnectorCapabilities
  ) = {
    val doesNotSupportLinkTables = capabilities.hasNot(RelationLinkTableCapability)
    val notSupportedError = doesNotSupportLinkTables.toOption {
      DeployError(typeDef.name, s"The directive `@$name` is not supported by this connector.")
    }

    notSupportedError.toVector
  }

  override def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      capabilities: ConnectorCapabilities
  ) = {
    Some(typeDef.hasDirective(name))
  }

  override def postValidate(dataModel: PrismaSdl, capabilities: ConnectorCapabilities) = {
    // if this error occurs automatically the others occur as well. We therefore only return this one.
    val isReferencedError = ensureLinkTableIsReferenced(dataModel, capabilities)
    if (isReferencedError.nonEmpty) {
      isReferencedError
    } else {
      ensureTheRightTypesAreLinked(dataModel, capabilities)
    }
  }

  def ensureLinkTableIsReferenced(dataModel: PrismaSdl, capabilities: ConnectorCapabilities) = {
    for {
      relationTable  <- dataModel.relationTables
      relationFields = dataModel.modelTypes.flatMap(_.relationFields)
      relationNames  = relationFields.flatMap(_.relationName)
      isReferenced   = relationNames.contains(relationTable.name)
      if !isReferenced
    } yield DeployError(relationTable.name, s"The link table `${relationTable.name}` is not referenced in any relation field.")
  }

  def ensureTheRightTypesAreLinked(dataModel: PrismaSdl, capabilities: ConnectorCapabilities) = {
    for {
      relationTable           <- dataModel.relationTables
      relationFields          = dataModel.modelTypes.flatMap(_.relationFields)
      relationFieldsForTable  = relationFields.filter(_.relationName.contains(relationTable.name))
      typesReferencedInFields = (relationFieldsForTable.map(_.referencesType) ++ relationFieldsForTable.map(_.tpe.name)).toSet
      typesReferencedByTable  = relationTable.relationFields.map(_.referencesType).toSet
      if typesReferencedByTable != typesReferencedInFields
    } yield DeployError(relationTable.name, s"The link table `${relationTable.name}` is not referencing the right types.")
  }
}
