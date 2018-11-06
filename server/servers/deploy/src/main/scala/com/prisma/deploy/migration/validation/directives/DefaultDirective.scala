package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.gc_value.GCStringConverter
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import com.prisma.shared.models.{ConnectorCapability, TypeIdentifier}
import sangria.ast._

object DefaultDirective extends FieldDirective[GCValue] {
  val valueArg = "value"

  override def name         = "default"
  override def requiredArgs = Vector(ArgumentRequirement("value", _ => None))
  override def optionalArgs = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ): Option[DeployError] = {
    val placementIsInvalid = !document.isEnumType(fieldDef.typeName) && !fieldDef.isValidScalarNonListType
    if (placementIsInvalid) {
      return Some(DeployError(typeDef, fieldDef, "The `@default` directive must only be placed on scalar fields that are not lists."))
    }

    val value          = directive.argument_!(valueArg).value
    val typeIdentifier = document.typeIdentifierForTypename(fieldDef.fieldType).asInstanceOf[ScalarTypeIdentifier]
    (typeIdentifier, value) match {
      case (TypeIdentifier.String, _: StringValue)   => None
      case (TypeIdentifier.Float, _: FloatValue)     => None
      case (TypeIdentifier.Boolean, _: BooleanValue) => None
      case (TypeIdentifier.Json, _: StringValue)     => None
      case (TypeIdentifier.DateTime, _: StringValue) => None
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
  }

  override def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      capabilities: Set[ConnectorCapability]
  ): Option[GCValue] = {
    fieldDef.directive(name).map { directive =>
      val value          = directive.argument_!(valueArg).valueAsString
      val typeIdentifier = document.typeIdentifierForTypename(fieldDef.fieldType)
      GCStringConverter(typeIdentifier, fieldDef.isList).toGCValue(value).get
    }
  }
}
