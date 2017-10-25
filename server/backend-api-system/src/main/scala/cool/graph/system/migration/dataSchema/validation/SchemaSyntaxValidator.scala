package cool.graph.system.migration.dataSchema.validation

import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.SystemErrors.SchemaError
import cool.graph.shared.models.TypeIdentifier
import cool.graph.system.migration.dataSchema.{DataSchemaAstExtensions, SdlSchemaParser}
import sangria.ast.{Directive, FieldDefinition, ObjectTypeDefinition}

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

case class DirectiveRequirement(directiveName: String, arguments: Seq[RequiredArg])
case class RequiredArg(name: String, mustBeAString: Boolean)

case class FieldAndType(objectType: ObjectTypeDefinition, fieldDef: FieldDefinition)

object SchemaSyntaxValidator {
  def apply(schema: String): SchemaSyntaxValidator = {
    SchemaSyntaxValidator(schema, directiveRequirements)
  }

  val directiveRequirements = Seq(
    DirectiveRequirement("model", Seq.empty),
    DirectiveRequirement("relation", Seq(RequiredArg("name", mustBeAString = true))),
    DirectiveRequirement("rename", Seq(RequiredArg("oldName", mustBeAString = true))),
    DirectiveRequirement("defaultValue", Seq(RequiredArg("value", mustBeAString = false))),
    DirectiveRequirement("migrationValue", Seq(RequiredArg("value", mustBeAString = false))),
    DirectiveRequirement("isUnique", Seq.empty)
  )
}

case class SchemaSyntaxValidator(schema: String, directiveRequirements: Seq[DirectiveRequirement]) {
  import DataSchemaAstExtensions._
  val result   = SdlSchemaParser.parse(schema)
  lazy val doc = result.get

  def validate(): Seq[SchemaError] = {
    result match {
      case Success(x) => validateInternal()
      case Failure(e) => List(SchemaError.global(s"There's a syntax error in the Schema Definition. ${e.getMessage}"))
    }
  }

  def validateInternal(): Seq[SchemaError] = {
    val nonSystemFieldAndTypes: Seq[FieldAndType] = for {
      objectType <- doc.objectTypes
      field      <- objectType.fields
      if field.isNotSystemField
    } yield FieldAndType(objectType, field)

    val allFieldAndTypes: Seq[FieldAndType] = for {
      objectType <- doc.objectTypes
      field      <- objectType.fields
    } yield FieldAndType(objectType, field)

    val missingModelDirectiveValidations    = validateModelDirectiveOnTypes(doc.objectTypes, allFieldAndTypes)
    val deprecatedImplementsNodeValidations = validateNodeInterfaceOnTypes(doc.objectTypes, allFieldAndTypes)
    val duplicateTypeValidations            = validateDuplicateTypes(doc.objectTypes, allFieldAndTypes)
    val duplicateFieldValidations           = validateDuplicateFields(allFieldAndTypes)
    val missingTypeValidations              = validateMissingTypes(nonSystemFieldAndTypes)
    val relationFieldValidations            = validateRelationFields(nonSystemFieldAndTypes)
    val scalarFieldValidations              = validateScalarFields(nonSystemFieldAndTypes)
    val fieldDirectiveValidations           = nonSystemFieldAndTypes.flatMap(validateFieldDirectives)

    missingModelDirectiveValidations ++ deprecatedImplementsNodeValidations ++ validateIdFields ++ duplicateTypeValidations ++ duplicateFieldValidations ++ missingTypeValidations ++ relationFieldValidations ++ scalarFieldValidations ++ fieldDirectiveValidations ++ validateEnumTypes
  }

  def validateIdFields(): Seq[SchemaError] = {
    val missingUniqueDirectives = for {
      objectType <- doc.objectTypes
      field      <- objectType.fields
      if field.isIdField && !field.isUnique
    } yield {
      val fieldAndType = FieldAndType(objectType, field)
      SchemaErrors.missingUniqueDirective(fieldAndType)
    }

    val missingIdFields = for {
      objectType <- doc.objectTypes
      if objectType.hasNoIdField
    } yield {
      SchemaErrors.missingIdField(objectType)
    }
    missingUniqueDirectives ++ missingIdFields
  }

  def validateDuplicateTypes(objectTypes: Seq[ObjectTypeDefinition], fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    val typeNames          = objectTypes.map(_.name)
    val duplicateTypeNames = typeNames.filter(name => typeNames.count(_ == name) > 1)
    duplicateTypeNames.map(name => SchemaErrors.duplicateTypeName(fieldAndTypes.find(_.objectType.name == name).head)).distinct
  }

  def validateModelDirectiveOnTypes(objectTypes: Seq[ObjectTypeDefinition], fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    objectTypes.collect {
      case x if !x.directives.exists(_.name == "model") => SchemaErrors.missingAtModelDirective(fieldAndTypes.find(_.objectType.name == x.name).get)
    }
  }

  def validateNodeInterfaceOnTypes(objectTypes: Seq[ObjectTypeDefinition], fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    objectTypes.collect {
      case x if x.interfaces.exists(_.name == "Node") => SchemaErrors.atNodeIsDeprecated(fieldAndTypes.find(_.objectType.name == x.name).get)
    }
  }

  def validateDuplicateFields(fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    val objectTypes         = fieldAndTypes.map(_.objectType)
    val distinctObjectTypes = objectTypes.distinct
    distinctObjectTypes
      .flatMap(objectType => {
        val fieldNames = objectType.fields.map(_.name)
        fieldNames.map(
          name =>
            if (fieldNames.count(_ == name) > 1)
              Seq(SchemaErrors.duplicateFieldName(fieldAndTypes.find(ft => ft.objectType == objectType & ft.fieldDef.name == name).get))
            else Seq.empty)
      })
      .flatten
      .distinct
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

    val wrongTypeDefinitions = relationFields.collect{case fieldAndType if !fieldAndType.fieldDef.isValidRelationType => SchemaErrors.relationFieldTypeWrong(fieldAndType)}

    val (schemaErrors, validRelationFields) = partition(relationFields) {
      case fieldAndType if !fieldAndType.fieldDef.hasRelationDirective =>
        Left(SchemaErrors.missingRelationDirective(fieldAndType))

      case fieldAndType if !isSelfRelation(fieldAndType) && relationCount(fieldAndType) != 2 =>
        Left(SchemaErrors.relationNameMustAppear2Times(fieldAndType))

      case fieldAndType if isSelfRelation(fieldAndType) && relationCount(fieldAndType) != 1 && relationCount(fieldAndType) != 2 =>
        Left(SchemaErrors.selfRelationMustAppearOneOrTwoTimes(fieldAndType))

      case fieldAndType =>
        Right(fieldAndType)
    }

    val relationFieldsWithNonMatchingTypes = validRelationFields
      .groupBy(_.fieldDef.oldRelationName.get)
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
    scalarFields.collect{case fieldAndType if !fieldAndType.fieldDef.isValidScalarType => SchemaErrors.scalarFieldTypeWrong(fieldAndType)}
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

  def relationCount(fieldAndType: FieldAndType): Int = relationCount(fieldAndType.fieldDef.oldRelationName.get)
  def relationCount(relationName: String): Int = {
    val tmp = for {
      objectType <- doc.objectTypes
      field      <- objectType.relationFields
      if field.oldRelationName.contains(relationName)
    } yield field
    tmp.size
  }

  def isSelfRelation(fieldAndType: FieldAndType): Boolean  = fieldAndType.fieldDef.typeName == fieldAndType.objectType.name
  def isRelationField(fieldAndType: FieldAndType): Boolean = isRelationField(fieldAndType.fieldDef)
  def isRelationField(fieldDef: FieldDefinition): Boolean  = !isScalarField(fieldDef) && !isEnumField(fieldDef)

  def isScalarField(fieldAndType: FieldAndType): Boolean = isScalarField(fieldAndType.fieldDef)
  def isScalarField(fieldDef: FieldDefinition): Boolean  = TypeIdentifier.withNameOpt(fieldDef.typeName).isDefined

  def isEnumField(fieldDef: FieldDefinition): Boolean = doc.enumType(fieldDef.typeName).isDefined

  def partition[A, B, C](seq: Seq[A])(parititionFn: A => Either[B, C]): (Seq[B], Seq[C]) = {
    val mapped = seq.map(parititionFn)
    val lefts  = mapped.collect { case Left(x) => x }
    val rights = mapped.collect { case Right(x) => x }
    (lefts, rights)
  }
}
