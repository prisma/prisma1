package com.prisma.deploy.migration.validation

import com.prisma.deploy.validation._
import com.prisma.shared.models.TypeIdentifier
import sangria.ast.{Directive, EnumValue, FieldDefinition, ObjectTypeDefinition}

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

case class DirectiveRequirement(directiveName: String, requiredArguments: Seq[RequiredArg], optionalArguments: Seq[Argument])
case class RequiredArg(name: String, mustBeAString: Boolean)
case class Argument(name: String, isValid: sangria.ast.Value => Boolean)

case class FieldAndType(objectType: ObjectTypeDefinition, fieldDef: FieldDefinition)

case class FieldRequirement(name: String, typeName: String, required: Boolean, unique: Boolean, list: Boolean) {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  def isValid(field: FieldDefinition): Boolean = {
    if (field.name == name) {
      field.fieldType.namedType.name == typeName && field.isRequired == required && field.isUnique == unique && field.isList == list
    } else {
      true
    }
  }
}

object SchemaSyntaxValidator {
  val directiveRequirements = Seq(
    DirectiveRequirement(
      "relation",
      requiredArguments = Seq(RequiredArg("name", mustBeAString = true)),
      optionalArguments = Seq(Argument("onDelete", _.isInstanceOf[EnumValue]))
    ),
    DirectiveRequirement("rename", requiredArguments = Seq(RequiredArg("oldName", mustBeAString = true)), optionalArguments = Seq.empty),
    DirectiveRequirement("default", requiredArguments = Seq(RequiredArg("value", mustBeAString = false)), optionalArguments = Seq.empty),
    DirectiveRequirement("migrationValue", requiredArguments = Seq(RequiredArg("value", mustBeAString = false)), optionalArguments = Seq.empty),
    DirectiveRequirement("unique", requiredArguments = Seq.empty, optionalArguments = Seq.empty)
  )

  val reservedFieldsRequirements = Seq(
    FieldRequirement("id", "ID", required = true, unique = true, list = false),
    FieldRequirement("updatedAt", "DateTime", required = true, unique = false, list = false),
    FieldRequirement("createdAt", "DateTime", required = true, unique = false, list = false)
  )

  def apply(schema: String): SchemaSyntaxValidator = {
    SchemaSyntaxValidator(schema, directiveRequirements, reservedFieldsRequirements)
  }
}

case class SchemaSyntaxValidator(schema: String, directiveRequirements: Seq[DirectiveRequirement], reservedFieldsRequirements: Seq[FieldRequirement]) {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  val result   = SdlSchemaParser.parse(schema)
  lazy val doc = result.get

  def validate(): Seq[SchemaError] = {
    result match {
      case Success(x) => validateInternal()
      case Failure(e) => List(SchemaError.global(s"There's a syntax error in the Schema Definition. ${e.getMessage}"))
    }
  }

  def validateInternal(): Seq[SchemaError] = {
    val allFieldAndTypes: Seq[FieldAndType] = for {
      objectType <- doc.objectTypes
      field      <- objectType.fields
    } yield FieldAndType(objectType, field)

    val reservedFieldsValidations = validateReservedFields(allFieldAndTypes)
    val duplicateTypeValidations  = validateDuplicateTypes(doc.objectTypes, allFieldAndTypes)
    val duplicateFieldValidations = validateDuplicateFields(allFieldAndTypes)
    val missingTypeValidations    = validateMissingTypes(allFieldAndTypes)
    val relationFieldValidations  = validateRelationFields(allFieldAndTypes)
    val scalarFieldValidations    = validateScalarFields(allFieldAndTypes)
    val fieldDirectiveValidations = allFieldAndTypes.flatMap(validateFieldDirectives)

    reservedFieldsValidations ++
      duplicateTypeValidations ++
      duplicateFieldValidations ++
      missingTypeValidations ++
      relationFieldValidations ++
      scalarFieldValidations ++
      fieldDirectiveValidations ++
      validateEnumTypes
  }

  def validateReservedFields(fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    for {
      field        <- fieldAndTypes
      failedChecks = reservedFieldsRequirements.filterNot { _.isValid(field.fieldDef) }
      if failedChecks.nonEmpty
    } yield SchemaErrors.malformedReservedField(field, failedChecks.head)
  }

  def validateDuplicateTypes(objectTypes: Seq[ObjectTypeDefinition], fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    val typeNames          = objectTypes.map(_.name)
    val duplicateTypeNames = typeNames.filter(name => typeNames.count(_ == name) > 1)

    duplicateTypeNames.map(name => SchemaErrors.duplicateTypeName(fieldAndTypes.find(_.objectType.name == name).head)).distinct
  }

  def validateDuplicateFields(fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    for {
      objectType <- fieldAndTypes.map(_.objectType).distinct
      fieldNames = objectType.fields.map(_.name)
      fieldName  <- fieldNames
      if fieldNames.count(_ == fieldName) > 1
    } yield {
      SchemaErrors.duplicateFieldName(fieldAndTypes.find(ft => ft.objectType == objectType & ft.fieldDef.name == fieldName).get)
    }
  }

  def validateMissingTypes(fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    fieldAndTypes
      .filter(!isScalarField(_))
      .collect {
        case fieldAndType if !doc.isObjectOrEnumType(fieldAndType.fieldDef.typeName) =>
          SchemaErrors.missingType(fieldAndType)
      }
  }

  def validateRelationFields(fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    val relationFields = fieldAndTypes.filter(isRelationField)
    val wrongTypeDefinitions = relationFields.collect {
      case fieldAndType if !fieldAndType.fieldDef.isValidRelationType => SchemaErrors.relationFieldTypeWrong(fieldAndType)
    }

    def ambiguousRelationFieldsForType(objectType: ObjectTypeDefinition): Vector[FieldAndType] = {
      val relationFields                                = objectType.fields.filter(isRelationField)
      val grouped: Map[String, Vector[FieldDefinition]] = relationFields.groupBy(_.typeName)
      val ambiguousFields                               = grouped.values.filter(_.size > 1).flatten.toVector
      ambiguousFields.map { field =>
        FieldAndType(objectType, field)
      }
    }
    val ambiguousRelationFields = doc.objectTypes.flatMap(ambiguousRelationFieldsForType)

    val (schemaErrors, _) = partition(ambiguousRelationFields) {
      case fieldAndType if !fieldAndType.fieldDef.hasRelationDirective =>
        Left(SchemaErrors.missingRelationDirective(fieldAndType))

      case fieldAndType if !isSelfRelation(fieldAndType) && relationCount(fieldAndType) != 2 =>
        Left(SchemaErrors.relationNameMustAppear2Times(fieldAndType))

      case fieldAndType if isSelfRelation(fieldAndType) && relationCount(fieldAndType) != 1 && relationCount(fieldAndType) != 2 =>
        Left(SchemaErrors.selfRelationMustAppearOneOrTwoTimes(fieldAndType))

      case fieldAndType =>
        Right(fieldAndType)
    }

    val relationFieldsWithRelationDirective = for {
      objectType <- doc.objectTypes
      field      <- objectType.fields
      if field.hasRelationDirective
      if isRelationField(field)
    } yield FieldAndType(objectType, field)

    /**
      * The validation below must be only applied to fields that specify the relation directive.
      * And it can only occur for relation that specify both sides of a relation.
      */
    val relationFieldsWithNonMatchingTypes = relationFieldsWithRelationDirective
      .groupBy(_.fieldDef.previousRelationName.get)
      .flatMap {
        case (_, fieldAndTypes) if fieldAndTypes.size > 1 =>
          val first  = fieldAndTypes.head
          val second = fieldAndTypes.last
          val firstError = if (first.fieldDef.typeName != second.objectType.name) {
            Option(SchemaErrors.typesForOppositeRelationFieldsDoNotMatch(first, second))
          } else {
            None
          }
          val secondError = if (second.fieldDef.typeName != first.objectType.name) {
            Option(SchemaErrors.typesForOppositeRelationFieldsDoNotMatch(second, first))
          } else {
            None
          }
          firstError ++ secondError
        case _ =>
          Iterable.empty
      }

    wrongTypeDefinitions ++ schemaErrors ++ relationFieldsWithNonMatchingTypes
  }

  def validateScalarFields(fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    val scalarFields = fieldAndTypes.filter(isScalarField)
    scalarFields.collect { case fieldAndType if !fieldAndType.fieldDef.isValidScalarType => SchemaErrors.scalarFieldTypeWrong(fieldAndType) }
  }

  def validateFieldDirectives(fieldAndType: FieldAndType): Seq[SchemaError] = {
    def validateDirectiveRequirements(directive: Directive): Seq[SchemaError] = {
      val optionalArgs = for {
        requirement         <- directiveRequirements if requirement.directiveName == directive.name
        argumentRequirement <- requirement.optionalArguments
        argument            <- directive.argument(argumentRequirement.name)
        schemaError <- if (argumentRequirement.isValid(argument.value)) {
                        None
                      } else {
                        Some(SchemaError(fieldAndType, s"not a valid value for ${argumentRequirement.name}"))
                      }
      } yield schemaError
      val requiredArgs = for {
        requirement <- directiveRequirements if requirement.directiveName == directive.name
        requiredArg <- requirement.requiredArguments
        schemaError <- if (!directive.containsArgument(requiredArg.name, requiredArg.mustBeAString)) {
                        Some(SchemaErrors.directiveMissesRequiredArgument(fieldAndType, requirement.directiveName, requiredArg.name))
                      } else {
                        None
                      }
      } yield schemaError

      requiredArgs ++ optionalArgs
    }

    def ensureDirectivesAreUnique(fieldAndType: FieldAndType): Option[SchemaError] = {
      val directives       = fieldAndType.fieldDef.directives
      val uniqueDirectives = directives.map(_.name).toSet
      if (uniqueDirectives.size != directives.size) {
        Some(SchemaErrors.directivesMustAppearExactlyOnce(fieldAndType))
      } else {
        None
      }
    }

    def ensureRelationDirectivesArePlacedCorrectly(fieldAndType: FieldAndType): Option[SchemaError] = {
      if (!isRelationField(fieldAndType.fieldDef) && fieldAndType.fieldDef.hasRelationDirective) {
        Some(SchemaErrors.relationDirectiveNotAllowedOnScalarFields(fieldAndType))
      } else {
        None
      }
    }

    def ensureNoDefaultValuesOnListFields(fieldAndTypes: FieldAndType): Option[SchemaError] = {
      if (fieldAndType.fieldDef.isList && fieldAndType.fieldDef.hasDefaultValueDirective) {
        Some(SchemaErrors.listFieldsCantHaveDefaultValues(fieldAndType))
      } else {
        None
      }
    }

    fieldAndType.fieldDef.directives.flatMap(validateDirectiveRequirements) ++
      ensureDirectivesAreUnique(fieldAndType) ++
      ensureRelationDirectivesArePlacedCorrectly(fieldAndType) ++
      ensureNoDefaultValuesOnListFields(fieldAndType)
  }

  def validateEnumTypes: Seq[SchemaError] = {
    doc.enumTypes.flatMap { enumType =>
      val invalidEnumValues = enumType.valuesAsStrings.filter(!NameConstraints.isValidEnumValueName(_))

      if (enumType.values.exists(value => value.name.head.isLower)) {
        Some(SchemaErrors.enumValuesMustBeginUppercase(enumType))
      } else if (invalidEnumValues.nonEmpty) {
        Some(SchemaErrors.enumValuesMustBeValid(enumType, invalidEnumValues))
      } else {
        None
      }
    }
  }

  def relationCount(fieldAndType: FieldAndType): Int = {
    def fieldsWithType(objectType: ObjectTypeDefinition, typeName: String): Seq[FieldDefinition] = objectType.fields.filter(_.typeName == typeName)

    val oppositeObjectType = doc.objectType_!(fieldAndType.fieldDef.typeName)
    val fieldsOnTypeA      = fieldsWithType(fieldAndType.objectType, fieldAndType.fieldDef.typeName)
    val fieldsOnTypeB      = fieldsWithType(oppositeObjectType, fieldAndType.objectType.name)

    // TODO: this probably only works if a relation directive appears twice actually in case of ambiguous relations

    isSelfRelation(fieldAndType) match {
      case true  => (fieldsOnTypeB).count(_.relationName == fieldAndType.fieldDef.relationName)
      case false => (fieldsOnTypeA ++ fieldsOnTypeB).count(_.relationName == fieldAndType.fieldDef.relationName)
    }
  }

  def isSelfRelation(fieldAndType: FieldAndType): Boolean  = fieldAndType.fieldDef.typeName == fieldAndType.objectType.name
  def isRelationField(fieldAndType: FieldAndType): Boolean = isRelationField(fieldAndType.fieldDef)
  def isRelationField(fieldDef: FieldDefinition): Boolean  = !isScalarField(fieldDef) && !isEnumField(fieldDef)

  def isScalarField(fieldAndType: FieldAndType): Boolean = isScalarField(fieldAndType.fieldDef)
  def isScalarField(fieldDef: FieldDefinition): Boolean  = TypeIdentifier.withNameOpt(fieldDef.typeName).isDefined

  def isEnumField(fieldDef: FieldDefinition): Boolean = doc.enumType(fieldDef.typeName).isDefined

  def partition[A, B, C](seq: Seq[A])(partitionFn: A => Either[B, C]): (Seq[B], Seq[C]) = {
    val mapped = seq.map(partitionFn)
    val lefts  = mapped.collect { case Left(x) => x }
    val rights = mapped.collect { case Right(x) => x }
    (lefts, rights)
  }
}
