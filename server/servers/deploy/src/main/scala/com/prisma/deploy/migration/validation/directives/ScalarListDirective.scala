package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.FieldBehaviour.{ScalarListBehaviour, ScalarListStrategy}
import com.prisma.shared.models.{ConnectorCapability, FieldBehaviour}
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

object ScalarListDirective extends FieldDirective[ScalarListBehaviour] {
  override def name         = "scalarList"
  override def requiredArgs = Vector.empty
  override def optionalArgs = Vector(ArgumentRequirement("strategy", isValidStrategyArgument))

  val embeddedValue       = "EMBEDDED"
  val relationValue       = "RELATION"
  val validStrategyValues = Set(embeddedValue, relationValue)

  private def isValidStrategyArgument(value: sangria.ast.Value): Option[String] = {
    if (validStrategyValues.contains(value.asString)) {
      None
    } else {
      Some(s"Valid values for the strategy argument of `@scalarList` are: ${validStrategyValues.mkString(", ")}.")
    }
  }

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    if (!fieldDef.isValidScalarListType) {
      Some(DeployError(typeDef, fieldDef, s"Fields that are marked as `@scalarList` must be either of type `[String!]` or `[String!]!`."))
    } else {
      None
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    lazy val behaviour = fieldDef.directive(name) match {
      case Some(directive) =>
        directive.argument_!("strategy").valueAsString match {
          case `relationValue` => ScalarListBehaviour(FieldBehaviour.ScalarListStrategy.Relation)
          case `embeddedValue` => ScalarListBehaviour(FieldBehaviour.ScalarListStrategy.Embedded)
          case x               => sys.error(s"Unknown strategy $x")
        }
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
