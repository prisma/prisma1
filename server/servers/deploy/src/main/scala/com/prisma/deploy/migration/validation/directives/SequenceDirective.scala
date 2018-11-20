package com.prisma.deploy.migration.validation.directives

import com.prisma.shared.models.ConnectorCapability
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.{DeployError, DeployErrors}
import com.prisma.shared.models.FieldBehaviour.Sequence

object SequenceDirective extends FieldDirective[Sequence] {
  override def name = "sequence"

  val nameArg        = DirectiveArgument("name", validateStringValue, _.asString)
  val initialValue   = DirectiveArgument("initialValue", validateIntValue, _.asInt)
  val allocationSize = DirectiveArgument("allocationSize", validateIntValue, _.asInt)

  override def requiredArgs(capabilities: Set[ConnectorCapability]) = {
    Vector(
      nameArg,
      initialValue,
      allocationSize
    )
  }

  override def optionalArgs(capabilities: Set[ConnectorCapability]) = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ): Vector[DeployError] = {
    val hasIdDirective           = fieldDef.hasDirective(IdDirective.name)
    val hasRightStrategyArgument = fieldDef.directiveArgumentAsString(IdDirective.name, IdStrategyArgument.name).contains(IdStrategyArgument.sequenceValue)
    val invalidPlacement = (!hasIdDirective || !hasRightStrategyArgument).toOption {
      DeployErrors.sequenceDirectiveMisplaced(typeDef, fieldDef)
    }

    invalidPlacement.toVector
  }

  override def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      capabilities: Set[ConnectorCapability]
  ) = {
    for {
      directive      <- fieldDef.directive(name)
      name           <- nameArg.value(directive)
      initialValue   <- initialValue.value(directive)
      allocationSize <- allocationSize.value(directive)
    } yield Sequence(name, initialValue, allocationSize)
  }

}
