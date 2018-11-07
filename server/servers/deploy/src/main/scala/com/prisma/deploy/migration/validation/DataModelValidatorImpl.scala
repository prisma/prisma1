package com.prisma.deploy.migration.validation
import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation.directives._
import com.prisma.shared.models.ConnectorCapability
import com.prisma.utils.boolean.BooleanUtils
import org.scalactic.{Bad, Good, Or}
import sangria.ast.{Argument => _, _}

import scala.collection.immutable.Seq
import scala.util.{Failure, Success, Try}

object DataModelValidatorImpl extends DataModelValidator {
  override def validate(
      dataModel: String,
      fieldRequirements: FieldRequirementsInterface,
      capabilities: Set[ConnectorCapability]
  ): PrismaSdl Or Vector[DeployError] = {
    DataModelValidatorImpl(dataModel, fieldRequirements, capabilities).validate
  }
}

case class DataModelValidatorImpl(
    dataModel: String,
    fieldRequirements: FieldRequirementsInterface,
    capabilities: Set[ConnectorCapability]
) {
  import com.prisma.deploy.migration.DataSchemaAstExtensions._

  val result   = GraphQlSdlParser.parse(dataModel)
  lazy val doc = result.get

  def validate: PrismaSdl Or Vector[DeployError] = {
    val errors = validateSyntax
    if (errors.isEmpty) {
      Good(generateSDL)
    } else {
      Bad(errors.toVector)
    }
  }

  def generateSDL: PrismaSdl = {
    val enumTypes: Vector[PrismaSdl => PrismaEnum] = doc.enumTypes.map { definition =>
      val enumValues = definition.values.map(_.name)
      PrismaEnum(definition.name, values = enumValues)(_)
    }

    val prismaTypes: Vector[PrismaSdl => PrismaType] = doc.objectTypes.map { typeDef =>
      val prismaFields = typeDef.fields.map {
        case x if isRelationField(x) =>
          val relationDirective = RelationDirective.value(doc, typeDef, x, capabilities).get
          RelationalPrismaField(
            name = x.name,
            relationDbDirective = x.relationDBDirective,
            strategy = relationDirective.strategy,
            isList = x.isList,
            isRequired = x.isRequired,
            referencesType = x.typeName,
            relationName = relationDirective.name,
            cascade = relationDirective.onDelete
          )(_)

        case x if isEnumField(x) =>
          EnumPrismaField(
            name = x.name,
            columnName = x.dbName,
            isList = x.isList,
            isRequired = x.isRequired,
            isUnique = x.isUnique,
            enumName = x.typeName,
            defaultValue = DefaultDirective.value(doc, typeDef, x, capabilities),
            behaviour = FieldDirective.behaviour.flatMap(_.value(doc, typeDef, x, capabilities)).headOption
          )(_)

        case x if isScalarField(x) =>
          ScalarPrismaField(
            name = x.name,
            columnName = x.dbName,
            isList = x.isList,
            isRequired = x.isRequired,
            isUnique = x.isUnique,
            typeIdentifier = doc.typeIdentifierForTypename(x.fieldType),
            defaultValue = DefaultDirective.value(doc, typeDef, x, capabilities),
            behaviour = FieldDirective.behaviour.flatMap(_.value(doc, typeDef, x, capabilities)).headOption
          )(_)
      }

      PrismaType(
        name = typeDef.name,
        tableName = typeDef.dbName,
        isEmbedded = typeDef.isEmbedded,
        isRelationTable = typeDef.isRelationTable,
        fieldFn = prismaFields
      )(_)
    }

    PrismaSdl(typesFn = prismaTypes, enumsFn = enumTypes)
  }

  def validateSyntax: Seq[DeployError] = result match {
    case Success(_) => validateInternal
    case Failure(e) => List(DeployError.global(s"There's a syntax error in the Schema Definition. ${e.getMessage}"))
  }

  lazy val allFieldAndTypes: Seq[FieldAndType] = for {
    objectType <- doc.objectTypes
    field      <- objectType.fields
  } yield FieldAndType(objectType, field)

  def validateInternal: Seq[DeployError] = {
    val reservedFieldsValidations    = tryValidation(validateTypes())
    val fieldDirectiveValidationsNew = tryValidation(validateFieldDirectives())
    val allValidations = Vector(
      reservedFieldsValidations,
      fieldDirectiveValidationsNew
    )

    val validationErrors: Vector[DeployError] = allValidations.collect { case Good(x) => x }.flatten
    val validationFailures: Vector[Throwable] = allValidations.collect { case Bad(e) => e }

    // We don't want to return unhelpful exception messages to the user if there are normal validation errors. It is likely that the exceptions won't occur if those get fixed first.
    val errors = if (validationErrors.nonEmpty) {
      validationErrors
    } else {
      validationFailures.map { throwable =>
        throwable.printStackTrace()
        DeployError.global(s"An unknown error happened: $throwable")
      }
    }

    errors.distinct
  }

  def tryValidation(block: => Seq[DeployError]): Or[Seq[DeployError], Throwable] = Or.from(Try(block))

  def validateTypes(): Seq[DeployError] = {
    doc.objectTypes.flatMap { objectType =>
      val hasIdDirective = objectType.fields.exists(_.hasDirective("id"))
      if (!hasIdDirective && !objectType.isRelationTable) {
        Some(DeployError.apply(objectType.name, s"One field of the type `${objectType.name}` must be marked as the id field with the `@id` directive."))
      } else {
        None
      }
    }
  }

  def validateFieldDirectives(): Seq[DeployError] = {
    for {
      fieldAndType <- allFieldAndTypes
      directive    <- fieldAndType.fieldDef.directives
      validator    <- FieldDirective.all
      if directive.name == validator.name
      argumentErrors  = validateDirectiveArguments(directive, validator, fieldAndType)
      validationError = validator.validate(doc, fieldAndType.objectType, fieldAndType.fieldDef, directive, capabilities)
      error           <- argumentErrors ++ validationError
    } yield {
      error
    }
  }

  def validateDirectiveArguments(directive: Directive, validator: FieldDirective[_], fieldAndType: FieldAndType): Vector[DeployError] = {
    val requiredArgErrors = for {
      argumentRequirement <- validator.requiredArgs
      schemaError <- directive.argument(argumentRequirement.name) match {
                      case None =>
                        Some(DeployErrors.directiveMissesRequiredArgument(fieldAndType, validator.name, argumentRequirement.name))
                      case Some(arg) =>
                        argumentRequirement.validate(arg.value).map(error => DeployError(fieldAndType, error))
                    }
    } yield schemaError

    val optionalArgErrors = for {
      argumentRequirement <- validator.optionalArgs
      argument            <- directive.argument(argumentRequirement.name)
      schemaError <- argumentRequirement.validate(argument.value).map { errorMsg =>
                      DeployError(fieldAndType, errorMsg)
                    }
    } yield schemaError

    requiredArgErrors ++ optionalArgErrors
  }

  private def isSelfRelation(fieldAndType: FieldAndType): Boolean  = fieldAndType.fieldDef.typeName == fieldAndType.objectType.name
  private def isRelationField(fieldAndType: FieldAndType): Boolean = isRelationField(fieldAndType.fieldDef)
  private def isRelationField(fieldDef: FieldDefinition): Boolean  = !isScalarField(fieldDef) && !isEnumField(fieldDef)

  private def isScalarField(fieldAndType: FieldAndType): Boolean = isScalarField(fieldAndType.fieldDef)
  private def isScalarField(fieldDef: FieldDefinition): Boolean  = fieldDef.hasScalarType

  private def isEnumField(fieldDef: FieldDefinition): Boolean = doc.isEnumType(fieldDef.typeName)
}
