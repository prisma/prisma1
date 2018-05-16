package com.prisma.deploy.migration.validation

import com.prisma.deploy.gc_value.GCStringConverter
import com.prisma.deploy.migration.inference.InvalidGCValue
import com.prisma.deploy.validation._
import com.prisma.shared.models.TypeIdentifier
import org.scalactic.{Bad, Good}
import sangria.ast._

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

case class DirectiveRequirement(directiveName: String, requiredArguments: Seq[RequiredArg], optionalArguments: Seq[Argument])
case class RequiredArg(name: String, mustBeAString: Boolean)
case class Argument(name: String, isValid: sangria.ast.Value => Boolean)

case class FieldAndType(objectType: ObjectTypeDefinition, fieldDef: FieldDefinition)

object FieldRequirement {
  def apply(name: String, validType: String, required: Boolean, unique: Boolean, list: Boolean): FieldRequirement = {
    FieldRequirement(name = name, validTypes = Vector(validType), required = required, unique = unique, list = list)
  }
}
case class FieldRequirement(name: String, validTypes: Vector[String], required: Boolean, unique: Boolean, list: Boolean) {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  def isValid(field: FieldDefinition): Boolean = {
    field.name == name match {
      case true  => validTypes.contains(field.typeName) && field.isRequired == required && field.isUnique == unique && field.isList == list
      case false => true
    }
  }
}

object SchemaSyntaxValidator {

  def validOnDeleteEnum(x: sangria.ast.Value): Boolean = {
    val enum = x.isInstanceOf[EnumValue]

    val valid = x.renderPretty match {
      case "CASCADE"  => true
      case "SET_NULL" => true
      case _          => false
    }
    enum && valid
  }

  def validOldName(x: sangria.ast.Value): Boolean = x.isInstanceOf[StringValue]

  val directiveRequirements = Seq(
    DirectiveRequirement(
      "relation",
      requiredArguments = Seq(RequiredArg("name", mustBeAString = true)),
      optionalArguments = Seq(Argument("onDelete", validOnDeleteEnum), Argument("oldName", validOldName))
    ),
    DirectiveRequirement("rename", requiredArguments = Seq(RequiredArg("oldName", mustBeAString = true)), optionalArguments = Seq.empty),
    DirectiveRequirement("default", requiredArguments = Seq(RequiredArg("value", mustBeAString = false)), optionalArguments = Seq.empty),
    DirectiveRequirement("unique", requiredArguments = Seq.empty, optionalArguments = Seq.empty)
  )

  val idFieldRequirementForPassiveConnectors = FieldRequirement("id", Vector("ID", "Int"), required = true, unique = true, list = false)
  val idFieldRequirementForActiveConnectors  = FieldRequirement("id", "ID", required = true, unique = true, list = false)

  val reservedFieldsRequirementsForAllConnectors = Seq(
    FieldRequirement("updatedAt", "DateTime", required = true, unique = false, list = false),
    FieldRequirement("createdAt", "DateTime", required = true, unique = false, list = false)
  )

  val reservedFieldsRequirementsForActiveConnectors  = reservedFieldsRequirementsForAllConnectors ++ Seq(idFieldRequirementForActiveConnectors)
  val reservedFieldsRequirementsForPassiveConnectors = reservedFieldsRequirementsForAllConnectors ++ Seq(idFieldRequirementForPassiveConnectors)

  val requiredReservedFields = Vector(idFieldRequirementForPassiveConnectors)

  def apply(schema: String, isActive: Boolean): SchemaSyntaxValidator = {
    val fieldRequirements         = if (isActive) reservedFieldsRequirementsForActiveConnectors else reservedFieldsRequirementsForPassiveConnectors
    val requiredFieldRequirements = if (isActive) Vector.empty else requiredReservedFields
    SchemaSyntaxValidator(
      schema = schema,
      directiveRequirements = directiveRequirements,
      reservedFieldsRequirements = fieldRequirements,
      requiredReservedFields = requiredFieldRequirements,
      allowScalarLists = isActive
    )
  }
}

case class SchemaSyntaxValidator(
    schema: String,
    directiveRequirements: Seq[DirectiveRequirement],
    reservedFieldsRequirements: Seq[FieldRequirement],
    requiredReservedFields: Seq[FieldRequirement],
    allowScalarLists: Boolean
) {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  val result   = SdlSchemaParser.parse(schema)
  lazy val doc = result.get

  def validate: Seq[SchemaError] = {
    result match {
      case Success(_) => validateInternal
      case Failure(e) => List(SchemaError.global(s"There's a syntax error in the Schema Definition. ${e.getMessage}"))
    }
  }

  def generateSDL: PrismaSdl = {

    val enumTypes: Vector[PrismaSdl => PrismaEnum] = doc.enumNames.map { name =>
      val definition: EnumTypeDefinition = doc.enumType(name).get
      PrismaEnum(name, values = definition.values.map(_.name))(_)
    }

    val prismaTypes: Vector[PrismaSdl => PrismaType] = doc.objectTypes.map { definition =>
      val prismaFields = definition.fields.map {
        case x if isRelationField(x) => RelationalPrismaField(x.name, None, x.isList, x.isRequired, x.typeName, x.relationName, x.onDelete)(_)
        case x if isEnumField(x)     => EnumPrismaField(x.name, None, x.isList, x.isRequired, x.isUnique, x.typeName, getDefaultValueFromField_!(x))(_)
        case x if isScalarField(x) =>
          ScalarPrismaField(x.name, None, x.isList, x.isRequired, x.isUnique, typeIdentifierForTypename(x.fieldType), getDefaultValueFromField_!(x))(_)
      }

      //todo column names here?

      PrismaType(definition.name, None, fieldFn = prismaFields)(_)
    }

    PrismaSdl(typesFn = prismaTypes, enumsFn = enumTypes)
  }

  def validateInternal: Seq[SchemaError] = {

    val allFieldAndTypes: Seq[FieldAndType] = for {
      objectType <- doc.objectTypes
      field      <- objectType.fields
    } yield FieldAndType(objectType, field)

    val reservedFieldsValidations = validateReservedFields(allFieldAndTypes)
    val requiredFieldValidations  = validateRequiredReservedFields(doc.objectTypes)
    val duplicateTypeValidations  = validateDuplicateTypes(doc.objectTypes, allFieldAndTypes)
    val duplicateFieldValidations = validateDuplicateFields(allFieldAndTypes)
    val missingTypeValidations    = validateMissingTypes(allFieldAndTypes)
    val relationFieldValidations  = validateRelationFields(allFieldAndTypes)
    val scalarFieldValidations    = validateScalarFields(allFieldAndTypes)
    val fieldDirectiveValidations = allFieldAndTypes.flatMap(validateFieldDirectives)

    reservedFieldsValidations ++
      requiredFieldValidations ++
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

  def validateRequiredReservedFields(objectTypes: Seq[ObjectTypeDefinition]): Seq[SchemaError] = {
    for {
      objectType   <- objectTypes
      fieldNames   = objectType.fields.map(_.name)
      failedChecks = requiredReservedFields.filterNot(req => fieldNames.contains(req.name))
      if failedChecks.nonEmpty
    } yield SchemaErrors.missingReservedField(objectType, failedChecks.head.name, failedChecks.head)
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
      ambiguousFields.map(field => FieldAndType(objectType, field))
    }

    val ambiguousRelationFields = doc.objectTypes.flatMap(ambiguousRelationFieldsForType)

    val (schemaErrors, _) = partition(ambiguousRelationFields) {
      case fieldAndType if !fieldAndType.fieldDef.hasRelationDirective =>
        Left(SchemaErrors.missingRelationDirective(fieldAndType))

      case fieldAndType if !isSelfRelation(fieldAndType) && relationCount(fieldAndType) > 2 =>
        Left(SchemaErrors.relationDirectiveCannotAppearMoreThanTwice(fieldAndType))

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
      * Check that if a relation is not a self-relation and a relation-directive occurs only once that there is no
      * opposing field without a relationdirective on the other side.
      */
    val allowOnlyOneDirectiveOnlyWhenUnambigous = relationFieldsWithRelationDirective.flatMap {
      case thisType if !isSelfRelation(thisType) && relationCount(thisType) == 1 =>
        val oppositeObjectType               = doc.objectType_!(thisType.fieldDef.typeName)
        val fieldsOnOppositeObjectType       = oppositeObjectType.fields.filter(_.typeName == thisType.objectType.name)
        val relationFieldsWithoutDirective   = fieldsOnOppositeObjectType.filter(f => !f.hasRelationDirective && isRelationField(f))
        val relationFieldsPointingToThisType = relationFieldsWithoutDirective.filter(f => f.typeName == thisType.objectType.name)
        if (relationFieldsPointingToThisType.nonEmpty) Some(SchemaErrors.ambiguousRelationSinceThereIsOnlyOneRelationDirective(thisType)) else None

      case _ =>
        None
    }

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

    wrongTypeDefinitions ++ schemaErrors ++ relationFieldsWithNonMatchingTypes ++ allowOnlyOneDirectiveOnlyWhenUnambigous
  }

  def validateScalarFields(fieldAndTypes: Seq[FieldAndType]): Seq[SchemaError] = {
    val scalarFields = fieldAndTypes.filter(isScalarField)
    if (allowScalarLists) {
      scalarFields.collect {
        case fieldAndType if !fieldAndType.fieldDef.isValidScalarListOrNonListType => SchemaErrors.invalidScalarListOrNonListType(fieldAndType)
      }
    } else {
      scalarFields.collect {
        case fieldAndType if !fieldAndType.fieldDef.isValidScalarNonListType => SchemaErrors.invalidScalarNonListType(fieldAndType)
      }
    }
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

    def ensureNoOldDefaultValueDirectives(fieldAndTypes: FieldAndType): Option[SchemaError] = {
      if (fieldAndType.fieldDef.hasOldDefaultValueDirective) {
        Some(SchemaErrors.invalidSyntaxForDefaultValue(fieldAndType))
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

    def ensureNoInvalidEnumValuesInDefaultValues(fieldAndTypes: FieldAndType): Option[SchemaError] = {
      def containsInvalidEnumValue: Boolean = {
        val enumValues = doc.enumType(fieldAndType.fieldDef.typeName).get.values.map(_.name)
        !enumValues.contains(fieldAndType.fieldDef.directiveArgumentAsString("default", "value").get)
      }

      fieldAndType.fieldDef.hasDefaultValueDirective && isEnumField(fieldAndType.fieldDef) match {
        case true if containsInvalidEnumValue => Some(SchemaErrors.invalidEnumValueInDefaultValue(fieldAndType))
        case _                                => None
      }
    }

    def ensureDefaultValuesHaveCorrectType(fieldAndTypes: FieldAndType): Option[SchemaError] = {
      def hasInvalidType: Boolean = {
        getDefaultValueFromField(fieldAndType.fieldDef) match {
          case Some(Good(gcValue)) => false
          case None                => false
          case Some(Bad(err))      => true
        }
      }

      fieldAndType.fieldDef.hasDefaultValueDirective match {
        case true if hasInvalidType => Some(SchemaErrors.invalidEnumValueInDefaultValue(fieldAndType))
        case _                      => None
      }
    }

    fieldAndType.fieldDef.directives.flatMap(validateDirectiveRequirements) ++
      ensureDirectivesAreUnique(fieldAndType) ++
      ensureNoOldDefaultValueDirectives(fieldAndType) ++
      ensureRelationDirectivesArePlacedCorrectly(fieldAndType) ++
      ensureNoDefaultValuesOnListFields(fieldAndType) ++
      ensureNoInvalidEnumValuesInDefaultValues(fieldAndType) ++
      ensureDefaultValuesHaveCorrectType(fieldAndType)
  }

  private def getDefaultValueFromField(fieldDef: FieldDefinition) = {
    val defaultValue   = fieldDef.directiveArgumentAsString("default", "value")
    val typeIdentifier = typeIdentifierForTypename(fieldDef.fieldType)
    defaultValue.map(value => GCStringConverter(typeIdentifier, fieldDef.isList).toGCValue(value))
  }

  def getDefaultValueFromField_!(fieldDef: FieldDefinition) = getDefaultValueFromField(fieldDef).map(_.get)

  def validateEnumTypes: Seq[SchemaError] = {
    val duplicateNames = doc.enumNames.diff(doc.enumNames.distinct).distinct.flatMap(dupe => Some(SchemaErrors.enumNamesMustBeUnique(doc.enumType(dupe).get)))

    val otherErrors = doc.enumTypes.flatMap { enumType =>
      val invalidEnumValues = enumType.valuesAsStrings.filter(!NameConstraints.isValidEnumValueName(_))

      if (enumType.values.exists(value => value.name.head.isLower)) {
        Some(SchemaErrors.enumValuesMustBeginUppercase(enumType))
      } else if (invalidEnumValues.nonEmpty) {
        Some(SchemaErrors.enumValuesMustBeValid(enumType, invalidEnumValues))
      } else {
        None
      }
    }

    duplicateNames ++ otherErrors
  }

  def relationCount(fieldAndType: FieldAndType): Int = {
    def fieldsWithType(objectType: ObjectTypeDefinition, typeName: String): Seq[FieldDefinition] = objectType.fields.filter(_.typeName == typeName)

    val oppositeObjectType = doc.objectType_!(fieldAndType.fieldDef.typeName)
    val fieldsOnTypeA      = fieldsWithType(fieldAndType.objectType, fieldAndType.fieldDef.typeName)
    val fieldsOnTypeB      = fieldsWithType(oppositeObjectType, fieldAndType.objectType.name)

    isSelfRelation(fieldAndType) match {
      case true  => fieldsOnTypeB.count(_.relationName == fieldAndType.fieldDef.relationName)
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

  def typeIdentifierForTypename(fieldType: Type): TypeIdentifier.Value = {
    val typeName = fieldType.namedType.name

    if (doc.objectType(typeName).isDefined) {
      TypeIdentifier.Relation
    } else if (doc.enumType(typeName).isDefined) {
      TypeIdentifier.Enum
    } else {
      TypeIdentifier.withNameHacked(typeName)
    }
  }
}
