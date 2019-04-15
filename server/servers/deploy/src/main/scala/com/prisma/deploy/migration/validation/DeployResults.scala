package com.prisma.deploy.migration.validation

import com.prisma.gc_values.GCValue
import sangria.ast.{EnumTypeDefinition, FieldDefinition, ObjectTypeDefinition, TypeDefinition}

object DeployWarnings {
  def dataLossModel(`type`: String): DeployWarning = {
    DeployWarning(`type`, "You already have nodes for this model. This change will result in data loss.", None)
  }

  def dataLossRelation(`type`: String): DeployWarning = {
    DeployWarning(`type`, "You already have nodes for this relation. This change will result in data loss.", None)
  }

  def dataLossField(`type`: String, field: String): DeployWarning = {
    DeployWarning(`type`, "You already have nodes for this model. This change may result in data loss.", Some(field))
  }

  def migValueUsedOnExistingField(`type`: String, field: String, migValue: GCValue): DeployWarning = {
    DeployWarning(`type`, s"You are changing the type of a required field. The fields will be pre-filled with the value: `${migValue.value}`.", Some(field))
  }

  def migValueUsedOnNewField(`type`: String, field: String, migValue: GCValue): DeployWarning = {
    DeployWarning(
      `type`,
      s"You are creating a required field but there are already nodes present that would violate that constraint." +
        s"The fields will be pre-filled with the value `${migValue.value}`.",
      Some(field)
    )
  }
}

object DeployErrors {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  def uniqueDisallowedOnEmbeddedTypes(objectType: ObjectTypeDefinition, fieldDef: FieldDefinition): DeployError = {
    error(objectType, fieldDef, s"The field `${fieldDef.name}` is marked as unique but its type `${objectType.name}` is embedded. This is disallowed.")
  }

  def creatingUniqueRequiredFieldWithExistingNulls(`type`: String, field: String): DeployError = {
    DeployError(
      `type`,
      field,
      s"You are creating a new required unique field `$field`. There are already nodes for the model `${`type`}` that would violate that constraint."
    )
  }

  def updatingUniqueRequiredFieldWithExistingNulls(`type`: String, field: String): DeployError = {
    DeployError(
      `type`,
      field,
      s"You are updating the field `$field` to be required and unique. But there are already nodes for the model `${`type`}` that would violate that constraint."
    )
  }

  def makingFieldRequired(`type`: String, field: String): DeployError = {
    DeployError(
      `type`,
      field,
      s"You are updating the field `$field` to be required. But there are already nodes for the model `${`type`}` that would violate that constraint."
    )
  }

  def makingFieldUnique(`type`: String, field: String): DeployError = {
    DeployError(
      `type`,
      field,
      s"You are updating the field `$field` to be unique. But there are already nodes for the model `${`type`}` that would violate that constraint."
    )
  }

  def changingTypeOfIdField(`type`: String, field: String): DeployError = {
    DeployError(
      `type`,
      field,
      s"You are changing the type of the id field `$field`. But there are already nodes for this model. This would require to regenerate IDs for all nodes which is not possible. You have to remove all nodes for this type by either running `prisma reset` or through a `deleteMany${`type`}s` mutation."
    )
  }

  def missingRelationDirective(fieldAndType: FieldAndType): DeployError = {
    error(fieldAndType, s"""The relation field `${fieldAndType.fieldDef.name}` must specify a `@relation` directive: `@relation(name: "MyRelation")`""")
  }

  def missingRelationStrategy(relationField: RelationalPrismaField, validModes: Vector[String]): DeployError = {
    DeployError(
      relationField.tpe.name,
      relationField.name,
      s"The field `${relationField.name}` must provide a relation link mode. Either specify it on this field or the opposite field. Valid values are: ${validModes
        .mkString(",")}"
    )
  }
  def moreThanOneRelationStrategy(relationField: RelationalPrismaField): DeployError = {
    DeployError(
      relationField.tpe.name,
      relationField.name,
      s"The `link` argument must be specified only on one side of a relation. The field `${relationField.name}` provides a link mode and the opposite field `${relationField.relatedField.get.name}` as well.}"
    )
  }

  def cascadeUsedWithMongo(relationField: RelationalPrismaField): DeployError = {
    DeployError(
      relationField.tpe.name,
      relationField.name,
      s"The Mongo connector currently does not support Cascading Deletes, but the field `${relationField.name}` defines cascade behaviour. Please remove the onDelete argument.}"
    )
  }

  def disallowedBackRelationFieldOnEmbeddedType(relationField: RelationalPrismaField): DeployError = {
    DeployError(
      relationField.tpe.name,
      relationField.name,
      s"The type `${relationField.tpe.name}` specifies the back relation field `${relationField.name}`, which is disallowed for embedded types."
    )
  }

  def relationDirectiveNotAllowedOnScalarFields(fieldAndType: FieldAndType): DeployError = {
    error(fieldAndType, s"""The field `${fieldAndType.fieldDef.name}` is a scalar field and cannot specify the `@relation` directive.""")
  }

  def relationDirectiveHasInvalidName(fieldAndType: FieldAndType): DeployError = {
    error(
      fieldAndType,
      s"""The field `${fieldAndType.fieldDef.name}` has an invalid name in the `@relation` directive. It can only have up to 54 characters and must have the shape [A-Z][a-zA-Z0-9]*"""
    )
  }

  def relationDirectiveWithNameArgumentMustAppearTwice(fieldAndType: FieldAndType): DeployError = {
    val relationName = fieldAndType.fieldDef.previousRelationName.get
    val nameA        = fieldAndType.objectType.name
    val nameB        = fieldAndType.fieldDef.fieldType.namedType.name
    error(
      fieldAndType,
      s"You are trying to set the relation '$relationName' from `$nameA` to `$nameB` and are only providing a relation directive with a name on `$nameA`. " +
        s"Please also provide the same named relation directive on the relation field on `$nameB` pointing towards `$nameA`."
    )
  }

  def relationDirectiveCannotAppearMoreThanTwice(fieldAndType: FieldAndType): DeployError = {
    val relationName = fieldAndType.fieldDef.previousRelationName.get
    error(fieldAndType, s"A relation directive cannot appear more than twice. Relation name: '$relationName'")
  }

  def selfRelationMustAppearOneOrTwoTimes(fieldAndType: FieldAndType): DeployError = {
    val relationName = fieldAndType.fieldDef.previousRelationName.get
    error(fieldAndType, s"A relation directive for a self relation must appear either 1 or 2 times. Relation name: '$relationName'")
  }

  def typesForOppositeRelationFieldsDoNotMatch(fieldAndType: FieldAndType, other: FieldAndType): DeployError = {
    error(
      fieldAndType,
      s"The relation field `${fieldAndType.fieldDef.name}` has the type `${fieldAndType.fieldDef.typeString}`. But the other directive for this relation appeared on the type `${other.objectType.name}`"
    )
  }

  def missingType(objectType: ObjectTypeDefinition, fieldDef: FieldDefinition): DeployError = {
    error(
      objectType,
      fieldDef,
      s"The field `${fieldDef.name}` has the type `${fieldDef.typeString}` but there's no type or enum declaration with that name."
    )
  }

  def duplicateFieldName(fieldAndType: FieldAndType) = {
    error(
      fieldAndType,
      s"The type `${fieldAndType.objectType.name}` has a duplicate fieldName. The detection of duplicates is performed case insensitive. "
    )
  }

  def reservedTypeName(objectTypeDefinition: ObjectTypeDefinition) = {
    error(
      objectTypeDefinition,
      s"The type `${objectTypeDefinition.objectType.name}` has is using a reserved type name. Please rename it."
    )
  }

  def duplicateTypeName(objectTypeDefinition: ObjectTypeDefinition) = {
    error(
      objectTypeDefinition,
      s"The name of the type `${objectTypeDefinition.name}` occurs more than once. The detection of duplicates is performed case insensitive."
    )
  }

  // todo: do4gr should check whether this is still needed
  def crossRenamedTypeName(objectTypeDefinition: ObjectTypeDefinition) = {
    error(
      objectTypeDefinition,
      s"The type `${objectTypeDefinition.name}` is being renamed. Another type is also being renamed and formerly had `${objectTypeDefinition.name}` new name." +
        s"Please split cases where you do renames like type A -> type B and type B -> type A at the same time into two parts. "
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

  def directivesMustAppearExactlyOnce(objectType: ObjectTypeDefinition) = {
    error(objectType, s"The type `${objectType.name}` specifies a directive more than once. Directives must appear exactly once on a type.")
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

  def embeddedTypesAreNotSupported(typeName: String) = {
    DeployError(typeName, s"The type `$typeName` is marked as embedded but this connector does not support embedded types.")
  }

  def embeddedTypesMustNotSpecifyDbName(typeName: String) = {
    DeployError(typeName, s"The type `$typeName` is specifies the `@db` directive. Embedded types must not specify this directive.")
  }

  def relationFieldsMustNotSpecifyDbName(typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition) = {
    DeployError(typeDef, fieldDef, s"The field `${fieldDef.name}` specifies the `@db` directive. Relation fields must not specify this directive.")
  }

  def sequenceDirectiveMisplaced(typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition) = {
    DeployError(
      typeDef,
      fieldDef,
      s"The directive `@sequence` must only be specified for fields that are marked as id, are of type `Int` and use the sequence strategy. E.g. `id: Int! @id(strategy: SEQUENCE)`."
    )
  }

  def error(fieldAndType: FieldAndType, description: String): DeployError = {
    error(fieldAndType.objectType, fieldAndType.fieldDef, description)
  }

  def error(objectType: ObjectTypeDefinition, fieldDef: FieldDefinition, description: String): DeployError = {
    DeployError(objectType.name, fieldDef.name, description)
  }

  def error(typeDef: TypeDefinition, description: String): DeployError = {
    DeployError(typeDef.name, description)
  }
}
