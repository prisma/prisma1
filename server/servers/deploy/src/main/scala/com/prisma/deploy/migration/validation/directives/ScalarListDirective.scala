package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.FieldBehaviour.{ScalarListBehaviour, ScalarListStrategy}
import com.prisma.shared.models.{ConnectorCapability, FieldBehaviour}
import sangria.ast._

object ScalarListDirective extends FieldDirective[ScalarListBehaviour] {
  override def name                                                 = "scalarList"
  override def requiredArgs(capabilities: Set[ConnectorCapability]) = Vector.empty
  override def optionalArgs(capabilities: Set[ConnectorCapability]) = Vector(ScalarListStrategyArgument(capabilities))

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    val invalidTypeError = (!fieldDef.isValidScalarListType).toOption {
      DeployError(typeDef, fieldDef, s"Fields that are marked as `@scalarList` must be either of type `[String!]` or `[String!]!`.")
    }
    invalidTypeError.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    lazy val behaviour = fieldDef.directive(name) match {
      case Some(directive) =>
        val strategy = ScalarListStrategyArgument(capabilities).value(directive).get
        ScalarListBehaviour(strategy)
      case None =>
        if (capabilities.contains(EmbeddedScalarListsCapability)) {
          ScalarListBehaviour(ScalarListStrategy.Embedded)
        } else if (capabilities.contains(NonEmbeddedScalarListCapability)) {
          ScalarListBehaviour(ScalarListStrategy.Relation)
        } else {
          sys.error("should not happen")
        }
    }
    fieldDef.isValidScalarListType.toOption {
      behaviour
    }
  }
}

case class ScalarListStrategyArgument(capabilities: Set[ConnectorCapability]) extends DirectiveArgument[ScalarListStrategy] {
  val embeddedValue = "EMBEDDED"
  val relationValue = "RELATION"

  override def name = "strategy"

  override def validate(value: Value) = isValidStrategyArgument(capabilities, value)

  override def value(value: Value) = {
    value.asString match {
      case `relationValue` => FieldBehaviour.ScalarListStrategy.Relation
      case `embeddedValue` => FieldBehaviour.ScalarListStrategy.Embedded
      case x               => sys.error(s"Unknown strategy $x")
    }
  }

  private def isValidStrategyArgument(capabilities: Set[ConnectorCapability], value: sangria.ast.Value): Option[String] = {
    val validStrategyValues = (
      capabilities.contains(EmbeddedScalarListsCapability).toOption(embeddedValue) ++
        capabilities.contains(NonEmbeddedScalarListCapability).toOption(relationValue)
    ).toVector

    if (validStrategyValues.contains(value.asString)) {
      None
    } else {
      Some(s"Valid values for the strategy argument of `@scalarList` are: ${validStrategyValues.mkString(", ")}.")
    }
  }
}
