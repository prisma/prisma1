package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.FieldBehaviour.IdBehaviour
import com.prisma.shared.models.{ConnectorCapability, FieldBehaviour}
import sangria.ast.{Directive, Document, FieldDefinition, ObjectTypeDefinition}

object IdDirective extends FieldDirective[IdBehaviour] {
  val autoValue           = "AUTO"
  val noneValue           = "NONE"
  val validStrategyValues = Set(autoValue, noneValue)

  override def name         = "id"
  override def requiredArgs = Vector.empty
  override def optionalArgs = Vector(ArgumentRequirement("strategy", isStrategyValueValid))

  private def isStrategyValueValid(value: sangria.ast.Value): Option[String] = {
    if (validStrategyValues.contains(value.asString)) {
      None
    } else {
      Some(s"Valid values for the strategy argument of `@$name` are: ${validStrategyValues.mkString(", ")}.")
    }
  }

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    if (typeDef.isEmbedded) {
      Some(DeployError(typeDef, fieldDef, s"The `@$name` directive is not allowed on embedded types."))
    } else {
      None
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.directive(name).map { directive =>
      directive.argumentValueAsString("strategy").getOrElse(autoValue) match {
        case `autoValue` => IdBehaviour(FieldBehaviour.IdStrategy.Auto)
        case `noneValue` => IdBehaviour(FieldBehaviour.IdStrategy.None)
        case x           => sys.error(s"Encountered unknown strategy $x")
      }
    }
  }
}
