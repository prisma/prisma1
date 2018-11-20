package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.{DeployError, DeployErrors, FieldAndType, PrismaSdl}
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, RelationLinkListCapability, RelationLinkTableCapability}
import com.prisma.shared.models.OnDelete.OnDelete
import com.prisma.shared.models.{ConnectorCapability, OnDelete, RelationStrategy}
import sangria.ast._

case class RelationDirectiveData(name: Option[String], onDelete: OnDelete, strategy: Option[RelationStrategy])

object RelationDirective extends FieldDirective[RelationDirectiveData] {
  override def name = "relation"

  override def requiredArgs(capabilities: Set[ConnectorCapability]) = Vector.empty

  val nameArgument = DirectiveArgument("name", validateStringValue, _.asString)

  override def optionalArgs(capabilities: Set[ConnectorCapability]) = {
    Vector(
      nameArgument,
      OnDeleteArgument,
      RelationLinkArgument(capabilities)
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
      val relationName = fieldDef.directive(name).flatMap(nameArgument.value)
      val onDelete     = fieldDef.directive(name).flatMap(OnDeleteArgument.value).getOrElse(OnDelete.SetNull)
      val linkMode     = fieldDef.directive(name).flatMap(RelationLinkArgument(capabilities).value)

      Some(RelationDirectiveData(relationName, onDelete, linkMode))
    } else {
      None
    }
  }
}

case class RelationLinkArgument(capabilities: Set[ConnectorCapability]) extends DirectiveArgument[RelationStrategy] {
  val (inlineMode, tableMode) = ("INLINE", "TABLE")

  override def name = "link"

  override def value(value: Value) = value.asString match {
    case `inlineMode` => RelationStrategy.Inline
    case `tableMode`  => RelationStrategy.Table
    case x            => sys.error(s"cannot happen: $x")
  }

  override def validate(value: Value) = {
    val validLinkModes = Vector(inlineMode) ++ capabilities.contains(RelationLinkTableCapability).toOption(tableMode)
    validateEnumValue(name, validLinkModes)(value)
  }
}

object OnDeleteArgument extends DirectiveArgument[OnDelete.Value] {
  val (cascade, setNull) = ("CASCADE", "SET_NULL")

  override def name = "onDelete"

  override def validate(value: Value) = validateEnumValue(name, Vector(cascade, setNull))(value)

  override def value(value: Value) = {
    value.asString match {
      case `setNull` => OnDelete.SetNull
      case `cascade` => OnDelete.Cascade
      case x         => sys.error(s"The SchemaSyntaxvalidator should catch this already: $x")
    }
  }
}
