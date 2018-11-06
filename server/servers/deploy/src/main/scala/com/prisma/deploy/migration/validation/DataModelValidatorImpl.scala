package com.prisma.deploy.migration.validation
import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.deploy.gc_value.GCStringConverter
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.shared.models.FieldBehaviour._
import com.prisma.shared.models.{ConnectorCapability, FieldBehaviour, TypeIdentifier}
import org.scalactic.{Bad, Good, Or}
import sangria.ast.{Argument => _, _}
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import com.prisma.utils.boolean.BooleanUtils

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

//    val enumTypes: Vector[PrismaSdl => PrismaEnum] = doc.enumNames.map { name =>
//      val definition: EnumTypeDefinition = doc.enumType(name).get
//      val enumValues                     = definition.values.map(_.name)
//      PrismaEnum(name, values = enumValues)(_)
//    }
    val enumTypes = Vector.empty

    val prismaTypes: Vector[PrismaSdl => PrismaType] = doc.objectTypes.map { definition =>
      val prismaFields = definition.fields.map {
        case x if isRelationField(x) =>
          RelationalPrismaField(
            name = x.name,
            relationDbDirective = x.relationDBDirective,
            isList = x.isList,
            isRequired = x.isRequired,
            referencesType = x.typeName,
            relationName = x.relationName,
            cascade = x.onDelete
          )(_)

        case x if isEnumField(x) =>
          EnumPrismaField(
            name = x.name,
            columnName = x.dbName,
            isList = x.isList,
            isRequired = x.isRequired,
            isUnique = x.isUnique,
            enumName = x.typeName,
            defaultValue = None
          )(_)

        case x if isScalarField(x) =>
          ScalarPrismaField(
            name = x.name,
            columnName = x.dbName,
            isList = x.isList,
            isRequired = x.isRequired,
            isUnique = x.isUnique,
            typeIdentifier = doc.typeIdentifierForTypename(x.fieldType),
            defaultValue = DefaultDirective.value(doc, definition, x, capabilities),
            behaviour = FieldDirectiveNew.behaviour.flatMap(_.value(doc, definition, x, capabilities)).headOption
          )(_)
      }

      PrismaType(definition.name, definition.tableNameDirective, definition.isEmbedded, prismaFields)(_)
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
    val fieldDirectiveValidationsNew = tryValidation(validateFieldDirectivesNew())
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
      if (hasIdDirective) {
        None
      } else {
        Some(DeployError.apply(objectType.name, s"One field of the type `${objectType.name}` must be marked as the id field with the `@id` directive."))
      }
    }
  }

  def validateFieldDirectivesNew(): Seq[DeployError] = {
    for {
      fieldAndType <- allFieldAndTypes
      directive    <- fieldAndType.fieldDef.directives
      validator    <- FieldDirectiveNew.all
      if directive.name == validator.name
      argumentErrors  = validateArguments(directive, validator, fieldAndType)
      validationError = validator.validate(doc, fieldAndType.objectType, fieldAndType.fieldDef, directive, capabilities)
      error           <- argumentErrors ++ validationError
    } yield {
      error
    }
  }

  def validateArguments(directive: Directive, validator: FieldDirectiveNew[_], fieldAndType: FieldAndType): Vector[DeployError] = {
    val requiredArgErrors = for {
      argumentRequirement <- validator.optionalArgs
      schemaError <- if (!directive.containsArgument(argumentRequirement.name)) {
                      Some(DeployErrors.directiveMissesRequiredArgument(fieldAndType, validator.name, argumentRequirement.name))
                    } else {
                      None
                    }
    } yield schemaError

    val optionalArgErrors = for {
      argumentRequirement <- validator.optionalArgs
      argument            <- directive.argument(argumentRequirement.name)
      schemaError <- argumentRequirement.isValid(argument.value).map { errorMsg =>
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

  private def isEnumField(fieldDef: FieldDefinition): Boolean = doc.enumType(fieldDef.typeName).isDefined
}

trait ValidatorShared {}

trait FieldDirectiveNew[T] extends BooleanUtils { // could introduce a new interface for type level directives
  def name: String
  def requiredArgs: Vector[ArgumentNew]
  def optionalArgs: Vector[ArgumentNew]

  // gets called if the directive was found. Can return an error message
  def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: sangria.ast.Directive,
      capabilities: Set[ConnectorCapability]
  ): Option[DeployError]

  def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      capabilities: Set[ConnectorCapability]
  ): Option[T]
}

object FieldDirectiveNew {
  val behaviour = Vector(IdDirectiveNew, CreatedAtDirectiveNew, UpdatedAtDirectiveNew, ScalarListDirectiveNew)
  val all       = Vector(DefaultDirective) ++ behaviour

}

case class ArgumentNew(name: String, isValid: sangria.ast.Value => Option[String])

object DefaultDirective extends FieldDirectiveNew[GCValue] {
  val valueArg = "value"

  override def name         = "default"
  override def requiredArgs = Vector(ArgumentNew("value", _ => None))
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
      case (TypeIdentifier.Enum, _)                  => None
      case (TypeIdentifier.String, _: StringValue)   => None
      case (TypeIdentifier.Float, _: FloatValue)     => None
      case (TypeIdentifier.Boolean, _: BooleanValue) => None
      case (TypeIdentifier.Json, _: StringValue)     => None
      case (TypeIdentifier.DateTime, _: StringValue) => None
      case (ti, v)                                   => Some(DeployError(typeDef, fieldDef, s"The value ${v.renderPretty} is not a valid default for fields of type ${ti.code}."))
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

object IdDirectiveNew extends FieldDirectiveNew[IdBehaviour] {
  val autoValue           = "AUTO"
  val noneValue           = "NONE"
  val validStrategyValues = Set(autoValue, noneValue)

  override def name         = "id"
  override def requiredArgs = Vector(ArgumentNew("strategy", isStrategyValueValid))
  override def optionalArgs = Vector.empty

  private def isStrategyValueValid(value: sangria.ast.Value): Option[String] = {
    if (validStrategyValues.contains(value.asString)) {
      None
    } else {
      Some(s"Valid values for the strategy argument of `@$name` are: ${validStrategyValues.mkString(", ")}.")
    }
  }

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    if (typeDef.isEmbedded) {
      Some(DeployError(typeDef, fieldDef, s"The `@$name` directive is not allowed on embedded types."))
    } else {
      None
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.directive(name).map { directive =>
      directive.argumentValueAsString("strategy").getOrElse(autoValue) match {
        case `autoValue` => IdBehaviour(FieldBehaviour.IdStrategy.Auto)
        case `noneValue` => IdBehaviour(FieldBehaviour.IdStrategy.None)
        case x           => sys.error(s"Encountered unknown strategy $x")
      }
    }
  }
}

object CreatedAtDirectiveNew extends FieldDirectiveNew[CreatedAtBehaviour.type] {
  override def name         = "createdAt"
  override def requiredArgs = Vector.empty
  override def optionalArgs = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    if (fieldDef.typeName == "DateTime" && fieldDef.isRequired) {
      None
    } else {
      Some(DeployError(typeDef, fieldDef, s"Fields that are marked as @createdAt must be of type `DateTime!`."))
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.directive(name).map { _ =>
      CreatedAtBehaviour
    }
  }
}

object UpdatedAtDirectiveNew extends FieldDirectiveNew[UpdatedAtBehaviour.type] {
  override def name         = "updatedAt"
  override def requiredArgs = Vector.empty
  override def optionalArgs = Vector.empty

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    if (fieldDef.typeName == "DateTime" && fieldDef.isRequired) {
      None
    } else {
      Some(DeployError(typeDef, fieldDef, s"Fields that are marked as @updatedAt must be of type `DateTime!`."))
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    fieldDef.directive(name).map { _ =>
      UpdatedAtBehaviour
    }
  }
}

object ScalarListDirectiveNew extends FieldDirectiveNew[ScalarListBehaviour] {
  override def name         = "scalarList"
  override def requiredArgs = Vector.empty
  override def optionalArgs = Vector(ArgumentNew("strategy", isValidStrategyArgument))

  val embeddedValue       = "EMBEDDED"
  val relationValue       = "RELATION"
  val validStrategyValues = Set(embeddedValue, relationValue)

  private def isValidStrategyArgument(value: sangria.ast.Value): Option[String] = {
    if (validStrategyValues.contains(value.asString)) {
      None
    } else {
      Some(s"Valid values for the strategy argument of `@scalarList` are: ${validStrategyValues.mkString(", ")}.")
    }
  }

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: Set[ConnectorCapability]
  ) = {
    if (!fieldDef.isValidScalarListType) {
      Some(DeployError(typeDef, fieldDef, s"Fields that are marked as `@scalarList` must be either of type `[String!]` or `[String!]!`."))
    } else {
      None
    }
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    lazy val behaviour = fieldDef.directive(name) match {
      case Some(directive) =>
        directive.argument_!("strategy").valueAsString match {
          case `relationValue` => ScalarListBehaviour(FieldBehaviour.ScalarListStrategy.Relation)
          case `embeddedValue` => ScalarListBehaviour(FieldBehaviour.ScalarListStrategy.Embedded)
          case x               => sys.error(s"Unknown strategy $x")
        }
      case None =>
        if (capabilities.contains(EmbeddedScalarListsCapability)) {
          ScalarListBehaviour(ScalarListStrategy.Embedded)
        } else if (capabilities.contains(NonEmbeddedScalarListCapability)) {
          ScalarListBehaviour(ScalarListStrategy.Relation)
        } else {
          sys.error("should not happen")
        }
    }
    fieldDef.isValidScalarListType.toOption {
      behaviour
    }
  }
}
