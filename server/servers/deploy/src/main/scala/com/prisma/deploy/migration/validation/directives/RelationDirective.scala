package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.{DeployError, DeployErrors, FieldAndType, PrismaSdl}
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, RelationLinkListCapability, RelationLinkTableCapability}
import com.prisma.shared.models.OnDelete.OnDelete
import com.prisma.shared.models.{ConnectorCapabilities, OnDelete, RelationStrategy}
import sangria.ast._

case class RelationDirectiveData(name: Option[String], onDelete: OnDelete, strategy: Option[RelationStrategy])

object RelationDirective extends FieldDirective[RelationDirectiveData] {
  override def name = "relation"

  override def requiredArgs(capabilities: ConnectorCapabilities) = Vector.empty

  val nameArgument = DirectiveArgument("name", validateStringValue, _.asString)

  override def optionalArgs(capabilities: ConnectorCapabilities) = {
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
      capabilities: ConnectorCapabilities
  ) = {
    val placementError = fieldDef.hasScalarType.toOption {
      DeployErrors.relationDirectiveNotAllowedOnScalarFields(FieldAndType(typeDef, fieldDef))
    }
    placementError.toVector
  }

  override def postValidate(dataModel: PrismaSdl, capabilities: ConnectorCapabilities): Vector[DeployError] = {
    validateIfRequiredStrategyIsProvided(dataModel, capabilities) ++
      validateBackRelationFields(dataModel, capabilities) ++
      validateStrategyIsProvidedExactlyOnce(dataModel, capabilities) ++
      validateIfProvidedStrategiesAreSupported(dataModel, capabilities) ++
      validateIfOnDeleteIsUsedByMongo(dataModel, capabilities)
  }

  private def validateBackRelationFields(dataModel: PrismaSdl, capabilities: ConnectorCapabilities): Vector[DeployError] = {
    for {
      modelType     <- dataModel.modelTypes
      relationField <- modelType.relationFields
      relatedField  <- relationField.relatedField
      if relationField.relatedType.isEmbedded
    } yield {
      DeployErrors.disallowedBackRelationFieldOnEmbeddedType(relatedField)
    }
  }

  private def validateStrategyIsProvidedExactlyOnce(dataModel: PrismaSdl, capabilities: ConnectorCapabilities): Vector[DeployError] = {
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

  private def validateIfRequiredStrategyIsProvided(dataModel: PrismaSdl, capabilities: ConnectorCapabilities): Vector[DeployError] = {
    for {
      modelType     <- dataModel.modelTypes
      relationField <- modelType.relationFields
      relatedType   = relationField.relatedType
      relatedField  = relationField.relatedField
      strategies    = relationField.strategy ++ relatedField.flatMap(_.strategy)
      if strategies.isEmpty
      if capabilities.has(RelationLinkListCapability) || relationField.hasOneToOneRelation
      if modelType.isNotEmbedded && relatedType.isNotEmbedded
    } yield {
      val inlineMode = capabilities.has(JoinRelationLinksCapability).toOption("`@relation(link: INLINE)`")
      val tableMode  = capabilities.has(RelationLinkTableCapability).toOption("`@relation(link: TABLE)`")
      val validModes = (tableMode ++ inlineMode).toVector
      DeployErrors.missingRelationStrategy(relationField, validModes)
    }
  }

  private def validateIfOnDeleteIsUsedByMongo(dataModel: PrismaSdl, capabilities: ConnectorCapabilities): Vector[DeployError] = {
    for {
      modelType     <- dataModel.modelTypes
      relationField <- modelType.relationFields
      cascade       = relationField.cascade
      if capabilities.isMongo && (cascade == OnDelete.Cascade || cascade == OnDelete.SetNull)
    } yield {
      DeployErrors.cascadeUsedWithMongo(relationField)
    }
  }

  private def validateIfProvidedStrategiesAreSupported(dataModel: PrismaSdl, capabilities: ConnectorCapabilities): Vector[DeployError] = {
    for {
      modelType     <- dataModel.modelTypes
      relationField <- modelType.relationFields
      if relationField.isList
      if relationField.strategy.contains(RelationStrategy.Inline)
      if capabilities.hasNot(RelationLinkListCapability)
    } yield {
      val hint = relationField.relatedField match {
        case Some(relatedField) if relatedField.isOne =>
          s"You could fix this by putting `link: INLINE` on the opposite field `${relatedField.name}` in the model `${relatedField.tpe.name}`."
        case _ =>
          // currently a connector either supports Relation Link Lists or Link Tables. So we know it is available in this case.
          require(capabilities.has(RelationLinkTableCapability))
          s"You could fix this by using the strategy `TABLE` instead."
      }
      DeployError(modelType.name, relationField.name, s"This connector does not support the `INLINE` strategy for list relation fields. $hint")
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: ConnectorCapabilities) = {
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

case class RelationLinkArgument(capabilities: ConnectorCapabilities) extends DirectiveArgument[RelationStrategy] {
  val (inlineMode, tableMode) = ("INLINE", "TABLE")

  override def name = "link"

  override def value(value: Value) = value.asString match {
    case `inlineMode` => RelationStrategy.Inline
    case `tableMode`  => RelationStrategy.Table
    case x            => sys.error(s"cannot happen: $x")
  }

  override def validate(value: Value) = {
    val validLinkModes = Vector(inlineMode) ++ capabilities.has(RelationLinkTableCapability).toOption(tableMode)
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
