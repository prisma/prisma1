package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.{DeployError, PrismaSdl, PrismaType, ScalarPrismaField}
import com.prisma.shared.models.{ConnectorCapabilities, TypeIdentifier}
import com.prisma.shared.models.ConnectorCapability.RelationLinkTableCapability
import com.prisma.shared.models.FieldBehaviour.IdBehaviour
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
    val isReferencedError = ensureLinkTableIsReferenced(dataModel, capabilities)
    // if this error occurs automatically the others occur as well. We therefore only return this one to keep errors concise.
    val error = if (isReferencedError.nonEmpty) {
      isReferencedError
    } else {
      ensureTheRightTypesAreLinked(dataModel, capabilities)
    }
    error ++ validateShapeOfLinkTable(dataModel)
  }

  def validateShapeOfLinkTable(dataModel: PrismaSdl): Vector[DeployError] = {
    dataModel.relationTables.flatMap { relationTable =>
      val wrongNumberOfRelationFields = (relationTable.relationFields.size != 2).toOption {
        DeployError(relationTable.name, "A link table must specify exactly two relation fields.")
      }
      val superfluousFieldErrors = relationTable.nonRelationFields
        .filter {
          case s: ScalarPrismaField if s.isId => false
          case _                              => true
        }
        .map { scalarField =>
          DeployError(scalarField.tpe.name, scalarField.name, "A link table must not specify any additional scalar fields.")
        }

      val idFieldHasIllegalType = relationTable.scalarFields.find(f => f.isId && f.typeIdentifier != TypeIdentifier.Cuid).map { scalarField =>
        DeployError(scalarField.tpe.name, scalarField.name, "The id field of a link table must be of type `ID!`.")
      }

      wrongNumberOfRelationFields ++ superfluousFieldErrors ++ idFieldHasIllegalType
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
