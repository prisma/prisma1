package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.{DeployError, DeployErrors}
import com.prisma.shared.models.ConnectorCapabilities
import com.prisma.shared.models.FieldBehaviour.Sequence
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

object SequenceDirective extends FieldDirective[Sequence] {
  override def name = "sequence"

  val nameArg        = DirectiveArgument("name", validateStringValue, _.asString)
  val initialValue   = DirectiveArgument("initialValue", validateIntValue, _.asInt)
  val allocationSize = DirectiveArgument("allocationSize", validateIntValue, _.asInt)

  override def requiredArgs(capabilities: ConnectorCapabilities) = {
    Vector(
      nameArg,
      initialValue,
      allocationSize
    )
  }

  override def optionalArgs(capabilities: ConnectorCapabilities) = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: ConnectorCapabilities
  ): Vector[DeployError] = {
    val hasIdDirective           = fieldDef.hasDirective(IdDirective.name)
    val hasRightStrategyArgument = fieldDef.directiveArgumentAsString(IdDirective.name, IdStrategyArgument.name).contains(IdStrategyArgument.sequenceValue)
    val hasIntType               = fieldDef.typeName == "Int"
    val invalidPlacement = (!hasIdDirective || !hasRightStrategyArgument || !hasIntType).toOption {
      DeployErrors.sequenceDirectiveMisplaced(typeDef, fieldDef)
    }

    invalidPlacement.toVector
  }

  override def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      capabilities: ConnectorCapabilities
  ) = {
    for {
      directive      <- fieldDef.directive(name)
      name           <- nameArg.value(directive)
      initialValue   <- initialValue.value(directive)
      allocationSize <- allocationSize.value(directive)
    } yield Sequence(name, initialValue, allocationSize)
  }

}
