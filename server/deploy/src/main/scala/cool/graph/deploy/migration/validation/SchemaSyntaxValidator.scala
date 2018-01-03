package cool.graph.deploy.migration.validation

import cool.graph.deploy.validation._
import cool.graph.shared.models.TypeIdentifier
import sangria.ast.{Directive, FieldDefinition, ObjectTypeDefinition}

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

case class DirectiveRequirement(directiveName: String, arguments: Seq[RequiredArg])
case class RequiredArg(name: String, mustBeAString: Boolean)

case class FieldAndType(objectType: ObjectTypeDefinition, fieldDef: FieldDefinition)

case class FieldRequirement(name: String, typeName: String, required: Boolean, unique: Boolean, list: Boolean) {
  import cool.graph.deploy.migration.DataSchemaAstExtensions._

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
    DirectiveRequirement("relation", Seq(RequiredArg("name", mustBeAString = true))),
    DirectiveRequirement("rename", Seq(RequiredArg("oldName", mustBeAString = true))),
    DirectiveRequirement("default", Seq(RequiredArg("value", mustBeAString = false))),
    DirectiveRequirement("migrationValue", Seq(RequiredArg("value", mustBeAString = false))),
    DirectiveRequirement("unique", Seq.empty)
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
  import cool.graph.deploy.migration.DataSchemaAstExtensions._

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

    /**
      * group relation fields by the types it is connecting? Map[ConnectedTypes, Fields]
      * ambiguous if map.get(types).get.count > 2
      */
    // TODO: should this be only performed for ambiguous relations?
    val (schemaErrors, validRelationFields) = partition(relationFields) {
      case fieldAndType if !fieldAndType.fieldDef.hasRelationDirective =>
        // TODO: only error if relation would be ambiguous
        Left(SchemaErrors.missingRelationDirective(fieldAndType))

      case fieldAndType if !isSelfRelation(fieldAndType) && relationCount(fieldAndType) != 2 =>
        // TODO: only error if relation would be ambiguous
        Left(SchemaErrors.relationNameMustAppear2Times(fieldAndType))

      case fieldAndType if isSelfRelation(fieldAndType) && relationCount(fieldAndType) != 1 && relationCount(fieldAndType) != 2 =>
        // TODO: only error if relation would be ambiguous
        Left(SchemaErrors.selfRelationMustAppearOneOrTwoTimes(fieldAndType))

      case fieldAndType =>
        Right(fieldAndType)
    }

    // TODO: we can't rely on the relation directive anymore
    val relationFieldsWithNonMatchingTypes = validRelationFields
      .groupBy(_.fieldDef.previousRelationName.get)
      .flatMap {
        case (_, fieldAndTypes) =>
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
      }

    wrongTypeDefinitions ++ schemaErrors ++ relationFieldsWithNonMatchingTypes
  }

  def validateScalarFields(fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    val scalarFields = fieldAndTypes.filter(isScalarField)
    scalarFields.collect { case fieldAndType if !fieldAndType.fieldDef.isValidScalarType => SchemaErrors.scalarFieldTypeWrong(fieldAndType) }
  }

  def validateFieldDirectives(fieldAndType: FieldAndType): Seq[SchemaError] = {
    def validateDirectiveRequirements(directive: Directive): Seq[SchemaError] = {
      for {
        requirement <- directiveRequirements if requirement.directiveName == directive.name
        requiredArg <- requirement.arguments
        schemaError <- if (!directive.containsArgument(requiredArg.name, requiredArg.mustBeAString)) {
                        Some(SchemaErrors.directiveMissesRequiredArgument(fieldAndType, requirement.directiveName, requiredArg.name))
                      } else {
                        None
                      }
      } yield schemaError
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

    fieldAndType.fieldDef.directives.flatMap(validateDirectiveRequirements) ++
      ensureDirectivesAreUnique(fieldAndType) ++
      ensureRelationDirectivesArePlacedCorrectly(fieldAndType)
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

  def relationCount(fieldAndType: FieldAndType): Int = relationCount(fieldAndType.fieldDef.previousRelationName.get)
  def relationCount(relationName: String): Int = {
    // FIXME: this relies on the relation directive
    val tmp = for {
      objectType <- doc.objectTypes
      field      <- objectType.relationFields
      if field.previousRelationName.contains(relationName)
    } yield field
    tmp.size
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
