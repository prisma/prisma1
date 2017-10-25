package cool.graph.system.migration.dataSchema.validation

import cool.graph.shared.errors.SystemErrors.SchemaError
import cool.graph.system.migration.dataSchema.DataSchemaAstExtensions
import sangria.ast.{EnumTypeDefinition, TypeDefinition}

object SchemaErrors {

  import DataSchemaAstExtensions._

  def missingIdField(typeDefinition: TypeDefinition): SchemaError = {
    error(typeDefinition, "All models must specify the `id` field: `id: ID! @isUnique`")
  }

  def missingUniqueDirective(fieldAndType: FieldAndType): SchemaError = {
    error(fieldAndType, s"""All id fields must specify the `@isUnique` directive.""")
  }

  def missingRelationDirective(fieldAndType: FieldAndType): SchemaError = {
    error(fieldAndType, s"""The relation field `${fieldAndType.fieldDef.name}` must specify a `@relation` directive: `@relation(name: "MyRelation")`""")
  }

  def relationDirectiveNotAllowedOnScalarFields(fieldAndType: FieldAndType): SchemaError = {
    error(fieldAndType, s"""The field `${fieldAndType.fieldDef.name}` is a scalar field and cannot specify the `@relation` directive.""")
  }

  def relationNameMustAppear2Times(fieldAndType: FieldAndType): SchemaError = {
    error(fieldAndType, "A relation directive with a name must appear exactly 2 times.")
  }

  def selfRelationMustAppearOneOrTwoTimes(fieldAndType: FieldAndType): SchemaError = {
    error(fieldAndType, "A relation directive for a many to many or one to one self relation must appear either 1 or 2 times.")
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

  def missingAtModelDirective(fieldAndType: FieldAndType) = {
    error(
      fieldAndType,
      s"The model `${fieldAndType.objectType.name}` is missing the @model directive. Please add it. See: https://github.com/graphcool/graphcool/issues/817"
    )
  }

  def atNodeIsDeprecated(fieldAndType: FieldAndType) = {
    error(
      fieldAndType,
      s"The model `${fieldAndType.objectType.name}` has the implements Node annotation. This is deprecated. Please use '@model' instead. See: https://github.com/graphcool/graphcool/issues/817"
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

  def relationFieldTypeWrong(fieldAndType: FieldAndType): SchemaError = {
    val oppositeType = fieldAndType.fieldDef.fieldType.namedType.name
    error(fieldAndType, s"""The relation field `${fieldAndType.fieldDef.name}` has the wrong format: `${fieldAndType.fieldDef.typeString}` Possible Formats: `$oppositeType`, `$oppositeType!`, `[$oppositeType!]!`""")    //todo
  }

  def scalarFieldTypeWrong(fieldAndType: FieldAndType): SchemaError = {
    val scalarType = fieldAndType.fieldDef.fieldType.namedType.name
    error(fieldAndType, s"""The scalar field `${fieldAndType.fieldDef.name}` has the wrong format: `${fieldAndType.fieldDef.typeString}` Possible Formats: `$scalarType`, `$scalarType!`, `[$scalarType!]` or `[$scalarType!]!`""")
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

  def systemTypeCannotBeRemoved(theType: String) = {
    SchemaError(theType, s"The type `$theType` is a system type and cannot be removed.")
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
    SchemaError.global("Your migration includes potentially destructive changes. Review using `graphcool diff` and continue using `graphcool deploy --force`.")
  }

  def invalidEnv(message: String) = {
    SchemaError.global(s"""the environment file is invalid: $message""")
  }
}
