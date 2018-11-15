package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.{DeployError, DeployErrors, FieldAndType, PrismaSdl}
import com.prisma.shared.models.ApiConnectorCapability.{JoinRelationLinksCapability, RelationLinkListCapability, RelationLinkTableCapability}
import com.prisma.shared.models.OnDelete.OnDelete
import com.prisma.shared.models.{ConnectorCapability, RelationStrategy}
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

case class RelationDirectiveData(name: Option[String], onDelete: OnDelete, strategy: Option[RelationStrategy])

object RelationDirective extends FieldDirective[RelationDirectiveData] {
  override def name = "relation"

  override def requiredArgs(capabilities: Set[ConnectorCapability]) = Vector.empty

  override def optionalArgs(capabilities: Set[ConnectorCapability]) = {
    val validLinkModes = Vector("AUTO", "INLINE") ++ capabilities.contains(RelationLinkTableCapability).toOption("TABLE")
    Vector(
      ArgumentRequirement("name", validateStringValue),
      ArgumentRequirement("onDelete", validateEnumValue("onDelete")(Vector("CASCADE", "SET_NULL"))),
      ArgumentRequirement("link", validateEnumValue("link")(validLinkModes))
    )
  }

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    val placementError = fieldDef.hasScalarType.toOption {
      DeployErrors.relationDirectiveNotAllowedOnScalarFields(FieldAndType(typeDef, fieldDef))
    }
    placementError.toVector
  }

  override def postValidate(dataModel: PrismaSdl, capabilities: Set[ConnectorCapability]): Vector[DeployError] = {
    validateIfRequiredStrategyIsProvided(dataModel, capabilities) ++
      validateBackRelationFields(dataModel, capabilities) ++
      validateStrategyIsProvidedExactlyOnce(dataModel, capabilities)
  }

  private def validateBackRelationFields(dataModel: PrismaSdl, capabilities: Set[ConnectorCapability]): Vector[DeployError] = {
    for {
      modelType     <- dataModel.modelTypes
      relationField <- modelType.relationFields
      relatedField  <- relationField.relatedField
      if relationField.relatedType.isEmbedded
    } yield {
      DeployErrors.disallowedBackRelationFieldOnEmbeddedType(relatedField)
    }
  }

  private def validateStrategyIsProvidedExactlyOnce(dataModel: PrismaSdl, capabilities: Set[ConnectorCapability]): Vector[DeployError] = {
    for {
      modelType     <- dataModel.modelTypes
      relationField <- modelType.relationFields
      relatedField  <- relationField.relatedField
      strategies    = relationField.strategy ++ relatedField.strategy
      if strategies.size > 1
    } yield {
      DeployErrors.moreThanOneRelationStrategy(relationField)
    }
  }

  private def validateIfRequiredStrategyIsProvided(dataModel: PrismaSdl, capabilities: Set[ConnectorCapability]): Vector[DeployError] = {
    val isMongo = capabilities.contains(RelationLinkListCapability)
    for {
      modelType     <- dataModel.modelTypes
      relationField <- modelType.relationFields
      relatedType   = relationField.relatedType
      relatedField  = relationField.relatedField
      strategies    = relationField.strategy ++ relatedField.flatMap(_.strategy)
      if strategies.isEmpty
      if isMongo || relationField.hasOneToOneRelation
      if modelType.isNotEmbedded && relatedType.isNotEmbedded
    } yield {
      val inlineMode = capabilities.contains(JoinRelationLinksCapability).toOption("`@relation(link: INLINE)`")
      val tableMode  = capabilities.contains(RelationLinkTableCapability).toOption("`@relation(link: TABLE)`")
      val validModes = (tableMode ++ inlineMode).toVector
      DeployErrors.missingRelationStrategy(relationField, validModes)
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    if (fieldDef.isRelationField(document)) {
      val strategy = fieldDef.directiveArgumentAsString(name, "link") match {
        case Some("INLINE") => Some(RelationStrategy.Inline)
        case Some("TABLE")  => Some(RelationStrategy.Table)
        case _              => None
      }
      Some(RelationDirectiveData(fieldDef.relationName, fieldDef.onDelete, strategy))
    } else {
      None
    }
  }
}
