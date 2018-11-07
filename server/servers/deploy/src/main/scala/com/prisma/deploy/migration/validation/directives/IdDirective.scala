package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.FieldBehaviour.IdBehaviour
import com.prisma.shared.models.TypeIdentifier.IdTypeIdentifier
import com.prisma.shared.models.{ConnectorCapability, FieldBehaviour, TypeIdentifier}
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
      doc: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    validatePlacement(typeDef, fieldDef).orElse(
      validateFieldType(doc, typeDef, fieldDef)
    )
  }

  def validatePlacement(typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition) = {
    typeDef.isEmbedded.toOption {
      DeployError(typeDef, fieldDef, s"The `@$name` directive is not allowed on embedded types.")
    }
  }

  def validateFieldType(doc: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition) = {
    // TODO: the valid types are connector dependenct
//    val validTypes = Set(TypeIdentifier.Int, TypeIdentifier.Float, TypeIdentifier.UUID, TypeIdentifier.Cuid)
    if (fieldDef.typeIdentifier(doc).isInstanceOf[IdTypeIdentifier]) {
      None
    } else {
      Some(DeployError(typeDef, fieldDef, s"The field `${fieldDef.name}` is marked as id and therefore has to have the type `Int!`, `ID!`, or `UUID!`."))
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
