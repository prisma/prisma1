package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.FieldBehaviour.IdBehaviour
import com.prisma.shared.models.TypeIdentifier.IdTypeIdentifier
import com.prisma.shared.models.{ConnectorCapability, FieldBehaviour}
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
    val errors = validatePlacement(typeDef, fieldDef) ++ validateFieldType(doc, typeDef, fieldDef)
    errors.toVector
  }

  def validatePlacement(typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition) = {
    typeDef.isEmbedded.toOption {
      DeployError(typeDef, fieldDef, s"The `@$name` directive is not allowed on embedded types.")
    }
  }

  def validateFieldType(doc: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition) = {
    // TODO: the valid types are connector dependent
//    val validTypes = Set(TypeIdentifier.Int, TypeIdentifier.Float, TypeIdentifier.UUID, TypeIdentifier.Cuid)
    if (fieldDef.typeIdentifier(doc).isInstanceOf[IdTypeIdentifier]) {
      None
    } else {
      Some(DeployError(typeDef, fieldDef, s"The field `${fieldDef.name}` is marked as id and therefore has to have the type `Int!`, `ID!`, or `UUID!`."))
    }
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
