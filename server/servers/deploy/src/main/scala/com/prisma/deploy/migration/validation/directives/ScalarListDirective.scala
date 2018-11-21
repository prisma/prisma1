package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.ConnectorCapability.{EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.{ConnectorCapabilities, FieldBehaviour}
import com.prisma.shared.models.FieldBehaviour.{ScalarListBehaviour, ScalarListStrategy}
import sangria.ast._

object ScalarListDirective extends FieldDirective[ScalarListBehaviour] {
  override def name                                              = "scalarList"
  override def requiredArgs(capabilities: ConnectorCapabilities) = Vector.empty
  override def optionalArgs(capabilities: ConnectorCapabilities) = Vector(ScalarListStrategyArgument(capabilities))

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: ConnectorCapabilities
  ) = {
    val invalidTypeError = (!fieldDef.isList).toOption {
      DeployError(typeDef, fieldDef, s"Fields that are marked as `@scalarList` must be lists, e.g. `[${fieldDef.typeName}]`.")
    }
    invalidTypeError.toVector
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: ConnectorCapabilities) = {
    lazy val behaviour = fieldDef.directive(name) match {
      case Some(directive) =>
        val strategy = ScalarListStrategyArgument(capabilities).value(directive).get
        ScalarListBehaviour(strategy)
      case None =>
        if (capabilities.has(EmbeddedScalarListsCapability)) {
          ScalarListBehaviour(ScalarListStrategy.Embedded)
        } else if (capabilities.has(NonEmbeddedScalarListCapability)) {
          ScalarListBehaviour(ScalarListStrategy.Relation)
        } else {
          sys.error("should not happen")
        }
    }
    fieldDef.isList.toOption {
      behaviour
    }
  }
}

case class ScalarListStrategyArgument(capabilities: ConnectorCapabilities) extends DirectiveArgument[ScalarListStrategy] {
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

  private def isValidStrategyArgument(capabilities: ConnectorCapabilities, value: sangria.ast.Value): Option[String] = {
    val validStrategyValues = (
      capabilities.has(EmbeddedScalarListsCapability).toOption(embeddedValue) ++
        capabilities.has(NonEmbeddedScalarListCapability).toOption(relationValue)
    ).toVector

    if (validStrategyValues.contains(value.asString)) {
      None
    } else {
      Some(s"Valid values for the strategy argument of `@scalarList` are: ${validStrategyValues.mkString(", ")}.")
    }
  }
}
