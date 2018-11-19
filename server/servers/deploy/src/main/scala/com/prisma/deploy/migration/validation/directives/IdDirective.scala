package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.ConnectorCapability.{IntIdCapability, UuidIdCapability}
import com.prisma.shared.models.FieldBehaviour.IdBehaviour
import com.prisma.shared.models.TypeIdentifier.{IdTypeIdentifier, ScalarTypeIdentifier, TypeIdentifier}
import com.prisma.shared.models.{ConnectorCapability, FieldBehaviour, TypeIdentifier}
import sangria.ast._

object IdDirective extends FieldDirective[IdBehaviour] {
  override def name                                                 = "id"
  override def requiredArgs(capabilities: Set[ConnectorCapability]) = Vector.empty
  override def optionalArgs(capabilities: Set[ConnectorCapability]) = Vector(IdStrategyArgument)

  override def validate(
      doc: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    val errors = validatePlacement(typeDef, fieldDef) ++ validateFieldType(doc, typeDef, fieldDef, capabilities)
    errors.toVector
  }

  def validatePlacement(typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition) = {
    typeDef.isEmbedded.toOption {
      DeployError(typeDef, fieldDef, s"The `@$name` directive is not allowed on embedded types.")
    }
  }

  def validateFieldType(doc: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    val supportsUuid   = capabilities.contains(UuidIdCapability)
    val supportsInt    = capabilities.contains(IntIdCapability)
    val validTypes     = Set[TypeIdentifier](TypeIdentifier.Cuid) ++ supportsUuid.toOption(TypeIdentifier.UUID) ++ supportsInt.toOption(TypeIdentifier.Int)
    val hasInvalidType = !validTypes.contains(fieldDef.typeIdentifier(doc))
    val isNotRequired  = !fieldDef.isRequired

    val requiredError = isNotRequired.toOption(DeployError(typeDef, fieldDef, s"Fields that are marked as id must be required."))
    val typeError = hasInvalidType.toOption {
      val validTypesString = validTypes
        .map {
          case t if t == TypeIdentifier.Cuid => s"`ID!`"
          case t                             => s"`${t.code}!`"
        }
        .mkString(",")
      DeployError(typeDef, fieldDef, s"The field `${fieldDef.name}` is marked as id must have one of the following types: $validTypesString.")
    }

    requiredError ++ typeError
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.directive(name).map { directive =>
      val strategy = IdStrategyArgument.value(directive).getOrElse(FieldBehaviour.IdStrategy.Auto)
      IdBehaviour(strategy)
    }
  }
}

object IdStrategyArgument extends DirectiveArgument[FieldBehaviour.IdStrategy] {
  val autoValue           = "AUTO"
  val noneValue           = "NONE"
  val validStrategyValues = Set(autoValue, noneValue)

  override def name = "strategy"

  override def validate(value: Value) = isStrategyValueValid(value)

  override def value(value: Value) = {
    value.asString match {
      case `autoValue` => FieldBehaviour.IdStrategy.Auto
      case `noneValue` => FieldBehaviour.IdStrategy.None
      case x           => sys.error(s"Encountered unknown strategy $x")
    }
  }

  private def isStrategyValueValid(value: sangria.ast.Value): Option[String] = {
    if (validStrategyValues.contains(value.asString)) {
      None
    } else {
      Some(s"Valid values for the strategy argument of `@id` are: ${validStrategyValues.mkString(", ")}.")
    }
  }
}
