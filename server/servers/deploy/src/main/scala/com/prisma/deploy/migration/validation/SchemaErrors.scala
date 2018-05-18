package com.prisma.deploy.migration.validation

import com.prisma.shared.errors.SchemaCheckResult
import sangria.ast.{EnumTypeDefinition, ObjectTypeDefinition, TypeDefinition}

import scala.collection.immutable

case class SchemaError(`type`: String, description: String, field: Option[String]) extends SchemaCheckResult

object SchemaError {
  def apply(`type`: String, field: String, description: String): SchemaError = {
    SchemaError(`type`, description, Some(field))
  }

  def apply(`type`: String, description: String): SchemaError = {
    SchemaError(`type`, description, None)
  }

  def apply(fieldAndType: FieldAndType, description: String): SchemaError = {
    apply(fieldAndType.objectType.name, fieldAndType.fieldDef.name, description)
  }

  def global(description: String): SchemaError = {
    SchemaError("Global", description, None)
  }
}

object SchemaErrors {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  def missingIdField(typeDefinition: TypeDefinition): SchemaError = {
    error(typeDefinition, "All models must specify the `id` field: `id: ID! @unique`")
  }

  def missingUniqueDirective(fieldAndType: FieldAndType): SchemaError = {
    error(fieldAndType, s"""All id fields must specify the `@unique` directive.""")
  }

  def missingRelationDirective(fieldAndType: FieldAndType): SchemaError = {
    error(fieldAndType, s"""The relation field `${fieldAndType.fieldDef.name}` must specify a `@relation` directive: `@relation(name: "MyRelation")`""")
  }

  def relationDirectiveNotAllowedOnScalarFields(fieldAndType: FieldAndType): SchemaError = {
    error(fieldAndType, s"""The field `${fieldAndType.fieldDef.name}` is a scalar field and cannot specify the `@relation` directive.""")
  }

  def ambiguousRelationSinceThereIsOnlyOneRelationDirective(fieldAndType: FieldAndType): SchemaError = {
    val relationName = fieldAndType.fieldDef.previousRelationName.get
    val nameA        = fieldAndType.objectType.name
    val nameB        = fieldAndType.fieldDef.fieldType.namedType.name
    error(
      fieldAndType,
      s"You are trying to set the relation '$relationName' from `$nameA` to `$nameB` and are only providing a relation directive on `$nameA`. " +
        s"Since there is also a relation field without a relation directive on `$nameB` pointing towards `$nameA` that is ambiguous. " +
        s"Please provide the same relation directive on `$nameB` if this is supposed to be the same relation. " +
        s"If you meant to create two separate relations without backrelations please provide a relation directive with a different name on `nameB`."
    )
  }

  def relationDirectiveCannotAppearMoreThanTwice(fieldAndType: FieldAndType): SchemaError = {
    val relationName = fieldAndType.fieldDef.previousRelationName.get
    error(fieldAndType, s"A relation directive cannot appear more than twice. Relation name: '$relationName'")
  }

  def selfRelationMustAppearOneOrTwoTimes(fieldAndType: FieldAndType): SchemaError = {
    val relationName = fieldAndType.fieldDef.previousRelationName.get
    error(fieldAndType, s"A relation directive for a self relation must appear either 1 or 2 times. Relation name: '$relationName'")
  }

  def typesForOppositeRelationFieldsDoNotMatch(fieldAndType: FieldAndType, other: FieldAndType): SchemaError = {
    error(
      fieldAndType,
      s"The relation field `${fieldAndType.fieldDef.name}` has the type `${fieldAndType.fieldDef.typeString}`. But the other directive for this relation appeared on the type `${other.objectType.name}`"
    )
  }

  def missingType(fieldAndType: FieldAndType) = {
    error(
      fieldAndType,
      s"The field `${fieldAndType.fieldDef.name}` has the type `${fieldAndType.fieldDef.typeString}` but there's no type or enum declaration with that name."
    )
  }

  def malformedReservedField(fieldAndType: FieldAndType, requirement: FieldRequirement): SchemaError = {
    error(
      fieldAndType,
      s"The field `${fieldAndType.fieldDef.name}` is reserved and has to have the format: ${requirementMessage(requirement)}."
    )
  }

  def missingReservedField(objectType: ObjectTypeDefinition, fieldName: String, requirement: FieldRequirement): SchemaError = {
    SchemaError(
      objectType.name,
      fieldName,
      s"The required field `$fieldName` is missing and has to have the format: ${requirementMessage(requirement)}."
    )
  }

  // Brain kaputt, todo find a better solution
  def requirementMessage(requirement: FieldRequirement): String = {
    val requiredTypeMessages = requirement.validTypes.map { typeName =>
      requirement match {
        case x @ FieldRequirement(name, _, true, false, false)  => s"$name: $typeName!"
        case x @ FieldRequirement(name, _, true, true, false)   => s"$name: $typeName! @unique"
        case x @ FieldRequirement(name, _, true, true, true)    => s"$name: [$typeName!]! @unique" // is that even possible? Prob. not.
        case x @ FieldRequirement(name, _, true, false, true)   => s"$name: [$typeName!]!"
        case x @ FieldRequirement(name, _, false, true, false)  => s"$name: $typeName @unique"
        case x @ FieldRequirement(name, _, false, true, true)   => s"$name: [$typeName!] @unique"
        case x @ FieldRequirement(name, _, false, false, true)  => s"$name: [$typeName!]"
        case x @ FieldRequirement(name, _, false, false, false) => s"$name: $typeName"
      }
    }
    requiredTypeMessages.mkString(" or ")
  }

  def atNodeIsDeprecated(fieldAndType: FieldAndType) = {
    error(
      fieldAndType,
      s"The model `${fieldAndType.objectType.name}` has the implements Node annotation. This is deprecated, please do not use an annotation."
    )
  }

  def duplicateFieldName(fieldAndType: FieldAndType) = {
    error(
      fieldAndType,
      s"The type `${fieldAndType.objectType.name}` has a duplicate fieldName."
    )
  }

  def duplicateTypeName(fieldAndType: FieldAndType) = {
    error(
      fieldAndType,
      s"The name of the type `${fieldAndType.objectType.name}` occurs more than once."
    )
  }

  def directiveMissesRequiredArgument(fieldAndType: FieldAndType, directive: String, argument: String) = {
    error(
      fieldAndType,
      s"The field `${fieldAndType.fieldDef.name}` specifies the directive `@$directive` but it's missing the required argument `$argument`."
    )
  }

  def directivesMustAppearExactlyOnce(fieldAndType: FieldAndType) = {
    error(fieldAndType, s"The field `${fieldAndType.fieldDef.name}` specifies a directive more than once. Directives must appear exactly once on a field.")
  }

  def manyRelationFieldsMustBeRequired(fieldAndType: FieldAndType) = {
    error(fieldAndType, s"Many relation fields must be marked as required.")
  }

  def listFieldsCantHaveDefaultValues(fieldAndType: FieldAndType) = {
    error(fieldAndType, s"List fields cannot have defaultValues.")
  }

  def invalidEnumValueInDefaultValue(fieldAndType: FieldAndType) = {
    error(fieldAndType, s"The defaultValue contains an invalid enumValue.")
  }

  def invalidTypeForDefaultValue(fieldAndType: FieldAndType) = {
    error(
      fieldAndType,
      s"Invalid value '${fieldAndType.fieldDef.directiveArgumentAsString("default", "value").get}' for type ${fieldAndType.fieldDef.fieldType.namedType.name}."
    )
  }

  def invalidSyntaxForDefaultValue(fieldAndType: FieldAndType) = {
    error(fieldAndType, s"""You are using a '@defaultValue' directive. Prisma uses '@default(value: "Value as String")' to declare default values.""")
  }

  def relationFieldTypeWrong(fieldAndType: FieldAndType): SchemaError = {
    val oppositeType = fieldAndType.fieldDef.fieldType.namedType.name
    error(
      fieldAndType,
      s"""The relation field `${fieldAndType.fieldDef.name}` has the wrong format: `${fieldAndType.fieldDef.typeString}` Possible Formats: `$oppositeType`, `$oppositeType!`, `[$oppositeType!]!`"""
    ) //todo
  }

  def invalidScalarNonListType(fieldAndType: FieldAndType)       = invalidScalarType(fieldAndType, listTypesAllowed = false)
  def invalidScalarListOrNonListType(fieldAndType: FieldAndType) = invalidScalarType(fieldAndType, listTypesAllowed = true)

  private def invalidScalarType(fieldAndType: FieldAndType, listTypesAllowed: Boolean): SchemaError = {
    val scalarType      = fieldAndType.fieldDef.fieldType.namedType.name
    val nonListFormats  = s"`$scalarType`, `$scalarType!`"
    val listFormats     = s", `[$scalarType!]` or `[$scalarType!]!"
    val possibleFormats = if (listTypesAllowed) nonListFormats + listFormats else nonListFormats
    error(
      fieldAndType,
      s"""The scalar field `${fieldAndType.fieldDef.name}` has the wrong format: `${fieldAndType.fieldDef.typeString}` Possible Formats: $possibleFormats"""
    )
  }

  def enumNamesMustBeUnique(enumType: EnumTypeDefinition) = {
    error(enumType, s"The enum type `${enumType.name}` is defined twice in the schema. Enum names must be unique.")
  }

  def enumValuesMustBeginUppercase(enumType: EnumTypeDefinition) = {
    error(enumType, s"The enum type `${enumType.name}` contains invalid enum values. The first character of each value must be an uppercase letter.")
  }

  def enumValuesMustBeValid(enumType: EnumTypeDefinition, enumValues: Seq[String]) = {
    error(enumType, s"The enum type `${enumType.name}` contains invalid enum values. Those are invalid: ${enumValues.map(v => s"`$v`").mkString(", ")}.")
  }

  def systemFieldCannotBeRemoved(theType: String, field: String) = {
    SchemaError(theType, field, s"The field `$field` is a system field and cannot be removed.")
  }

  def schemaFileHeaderIsMissing() = {
    SchemaError.global(s"""The schema must specify the project id and version as a front matter, e.g.:
                          |# projectId: your-project-id
                          |# version: 3
                          |type MyType {
                          |  myfield: String!
                          |}
       """.stripMargin)
  }

  def schemaFileHeaderIsReferencingWrongVersion(expected: Int) = {
    SchemaError.global(s"The schema is referencing the wrong project version. Expected version $expected.")
  }

  def error(fieldAndType: FieldAndType, description: String) = {
    SchemaError(fieldAndType.objectType.name, fieldAndType.fieldDef.name, description)
  }

  def error(typeDef: TypeDefinition, description: String) = {
    SchemaError(typeDef.name, description)
  }

  // note: the cli relies on the string "destructive changes" being present in this error message. Ugly but effective
  def forceArgumentRequired: SchemaError = {
    SchemaError.global(
      "Your migration includes potentially destructive changes. Review using `graphcool deploy --dry-run` and continue using `graphcool deploy --force`.")
  }

  def invalidEnv(message: String) = {
    SchemaError.global(s"""the environment file is invalid: $message""")
  }
}
