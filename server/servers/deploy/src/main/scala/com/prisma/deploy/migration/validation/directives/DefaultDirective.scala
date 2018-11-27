package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.gc_value.GCStringConverter
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import com.prisma.shared.models.{ConnectorCapabilities, TypeIdentifier}
import sangria.ast._

object DefaultDirective extends FieldDirective[GCValue] {
  val valueArg = DirectiveArgument("value", _ => None, _.asString)

  override def name                                              = "default"
  override def requiredArgs(capabilities: ConnectorCapabilities) = Vector(valueArg)
  override def optionalArgs(capabilities: ConnectorCapabilities) = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: ConnectorCapabilities
  ) = {
    val placementIsInvalid = !document.isEnumType(fieldDef.typeName) && !fieldDef.isValidScalarNonListType
    val placementError = placementIsInvalid.toOption {
      DeployError(typeDef, fieldDef, "The `@default` directive must only be placed on scalar fields that are not lists.")
    }

    val value          = directive.argument_!(valueArg.name).value
    val typeIdentifier = document.typeIdentifierForTypename(fieldDef.fieldType).asInstanceOf[ScalarTypeIdentifier]
    val typeError = (typeIdentifier, value) match {
      case (TypeIdentifier.String, _: StringValue)    => None
      case (TypeIdentifier.Float, _: FloatValue)      => None
      case (TypeIdentifier.Float, _: BigIntValue)     => None
      case (TypeIdentifier.Float, _: BigDecimalValue) => None
      case (TypeIdentifier.Int, _: IntValue)          => None
      case (TypeIdentifier.Int, _: BigIntValue)       => None
      case (TypeIdentifier.Boolean, _: BooleanValue)  => None
      case (TypeIdentifier.Json, _: StringValue)      => None
      case (TypeIdentifier.DateTime, _: StringValue)  => None
      case (TypeIdentifier.Enum, v: EnumValue) => {
        val enumValues = document.enumType(fieldDef.typeName).get.values.map(_.name)
        if (enumValues.contains(v.asString)) {
          None
        } else {
          Some(DeployError(typeDef, fieldDef, s"The default value is invalid for this enum. Valid values are: ${enumValues.mkString(", ")}."))
        }
      }
      case (ti, v) => Some(DeployError(typeDef, fieldDef, s"The value ${v.renderPretty} is not a valid default for fields of type ${ti.code}."))
    }

    (placementError ++ typeError).toVector
  }

  override def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      capabilities: ConnectorCapabilities
  ): Option[GCValue] = {
    fieldDef.directive(name).map { directive =>
      val value          = valueArg.value(directive).get
      val typeIdentifier = document.typeIdentifierForTypename(fieldDef.fieldType)
      GCStringConverter(typeIdentifier, fieldDef.isList).toGCValue(value).get
    }
  }
}
