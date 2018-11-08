package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.{DeployError, DeployErrors, FieldAndType, PrismaSdl}
import com.prisma.shared.models.ApiConnectorCapability.MongoRelationsCapability
import com.prisma.shared.models.OnDelete.OnDelete
import com.prisma.shared.models.{ConnectorCapability, RelationStrategy}
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

case class RelationDirectiveData(name: Option[String], onDelete: OnDelete, strategy: RelationStrategy)

object RelationDirective extends FieldDirective[RelationDirectiveData] {
  override def name = "relation"

  override def requiredArgs = Vector.empty

  override def optionalArgs = Vector(
    ArgumentRequirement("name", validateStringValue),
    ArgumentRequirement("onDelete", validateEnumValue(Vector("CASCADE", "SET_NULL"))),
    ArgumentRequirement("strategy", validateEnumValue(Vector("AUTO", "EMBED", "RELATION_TABLE")))
  )

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    fieldDef.hasScalarType.toOption {
      DeployErrors.relationDirectiveNotAllowedOnScalarFields(FieldAndType(typeDef, fieldDef))
    }
  }

  override def postValidate(dataModel: PrismaSdl, capabilities: Set[ConnectorCapability]): Vector[DeployError] = {
    validateIfRequiredStrategyIsProvided(dataModel, capabilities)
  }

//  private def validateBackRelationFields(dataModel: PrismaSdl, capabilities: Set[ConnectorCapability]): Vector[DeployError] = {
//    for {
//      modelType     <- dataModel.modelTypes
//      relationField <- modelType.relationFields
//      relatedType   = relationField.relatedType
//      if capabilities.contains(MongoRelationsCapability)
//      if relationField.relatedField.isEmpty && !relatedType.isEmbedded
//    } yield {
//      DeployErrors.missingBackRelationField(relatedType, relationField)
//    }
//  }

  private def validateIfRequiredStrategyIsProvided(dataModel: PrismaSdl, capabilities: Set[ConnectorCapability]): Vector[DeployError] = {
    val isMongo = capabilities.contains(MongoRelationsCapability)
    for {
      modelType                <- dataModel.modelTypes
      relationField            <- modelType.relationFields
      relatedType              = relationField.relatedType
      relatedField             = relationField.relatedField
      strategies               = Set(relationField.strategy) ++ relatedField.map(_.strategy)
      containsOnlyAutoStrategy = strategies == Set(RelationStrategy.Auto)
      if containsOnlyAutoStrategy
      if isMongo || relationField.hasOneToOneRelation
      if modelType.isNotEmbedded || relatedType.isNotEmbedded
    } yield {
      DeployErrors.missingRelationStrategy(relationField)
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    if (fieldDef.isRelationField(document)) {
      val strategy = fieldDef.directiveArgumentAsString(name, "strategy") match {
        case Some("AUTO")           => RelationStrategy.Auto
        case Some("EMBED")          => RelationStrategy.Embed
        case Some("RELATION_TABLE") => RelationStrategy.RelationTable
        case _                      => RelationStrategy.Auto
      }
      Some(RelationDirectiveData(fieldDef.relationName, fieldDef.onDelete, strategy))
    } else {
      None
    }
  }
}
