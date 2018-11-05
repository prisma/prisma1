package com.prisma.deploy.migration.validation
import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.shared.models.FieldBehaviour._
import com.prisma.shared.models.{ConnectorCapability, FieldBehaviour, TypeIdentifier}
import org.scalactic.{Bad, Good, Or}
import sangria.ast._
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability}

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
            typeIdentifier = typeIdentifierForTypename(x.fieldType),
            defaultValue = None,
            behaviour = FieldDirectiveThingy.all.flatMap(_.getValue(x, capabilities)).headOption
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
    val reservedFieldsValidations = tryValidation(validateTypes())
    val fieldDirectiveValidations = tryValidation(validateFieldDirectives())
    val allValidations = Vector(
      reservedFieldsValidations,
      fieldDirectiveValidations
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

  def validateFieldDirectives(): Seq[DeployError] = {
    for {
      fieldAndType <- allFieldAndTypes
      directive    <- fieldAndType.fieldDef.directives
      validator    <- FieldDirectiveThingy.all
      error        = validator.isValid(fieldAndType.objectType, fieldAndType.fieldDef, directive, capabilities)
      if error.isDefined
    } yield {
      DeployError(fieldAndType, error.get)
    }
  }

  private def isSelfRelation(fieldAndType: FieldAndType): Boolean  = fieldAndType.fieldDef.typeName == fieldAndType.objectType.name
  private def isRelationField(fieldAndType: FieldAndType): Boolean = isRelationField(fieldAndType.fieldDef)
  private def isRelationField(fieldDef: FieldDefinition): Boolean  = !isScalarField(fieldDef) && !isEnumField(fieldDef)

  private def isScalarField(fieldAndType: FieldAndType): Boolean = isScalarField(fieldAndType.fieldDef)
  private def isScalarField(fieldDef: FieldDefinition): Boolean  = fieldDef.hasScalarType

  private def isEnumField(fieldDef: FieldDefinition): Boolean = doc.enumType(fieldDef.typeName).isDefined

  private def typeIdentifierForTypename(fieldType: Type): TypeIdentifier.Value = {
    val typeName = fieldType.namedType.name

    if (doc.objectType(typeName).isDefined) {
      TypeIdentifier.Relation
    } else if (doc.enumType(typeName).isDefined) {
      TypeIdentifier.Enum
    } else {
      TypeIdentifier.withName(typeName)
    }
  }
}

trait FieldDirectiveThingy[T] {
  def name: String

  def isValid(
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: sangria.ast.Directive,
      capabilities: Set[ConnectorCapability]
  ): Option[String] = {
    if (directive.name == name) {
      isValidOnType(typeDef, capabilities)
        .orElse(isValidOnField(fieldDef, capabilities))
        .orElse(hasValidArguments(directive, capabilities))
    } else {
      None
    }
  }

  def isValidOnType(typeDef: ObjectTypeDefinition, capabilities: Set[ConnectorCapability]): Option[String]

  def isValidOnField(fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]): Option[String]

  def hasValidArguments(directive: sangria.ast.Directive, capabilities: Set[ConnectorCapability]): Option[String]

  def getValue(fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]): Option[T] = {
    fieldDef.directive(name).map { directive =>
      getValue(directive, capabilities)
    }
  }

  protected def getValue(directive: sangria.ast.Directive, capabilities: Set[ConnectorCapability]): T
}

object FieldDirectiveThingy {
  val all = Vector(IdDirective, CreatedAtDirective, UpdatedAtDirective, ScalarListDirective)
}

object IdDirective extends FieldDirectiveThingy[IdBehaviour] {
  val autoValue           = "AUTO"
  val noneValue           = "NONE"
  val validStrategyValues = Set(autoValue, noneValue)

  override def name = "id"

  override def isValidOnType(typeDef: ObjectTypeDefinition, capabilities: Set[ConnectorCapability]) = {
    if (typeDef.isEmbedded) {
      Some(s"The `@$name` directive is not allowed on embedded types.")
    } else {
      None
    }
  }

  override def isValidOnField(fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    // FIXME: introduce capabilities for database sequences. Int is only valid for connectors that support sequences.
    None
  }

  override def hasValidArguments(directive: Directive, capabilities: Set[ConnectorCapability]) = {
    directive.argumentValueAsString("strategy") match {
      case Some(strat) =>
        if (validStrategyValues.contains(strat)) {
          None
        } else {
          Some(s"Valid values for the strategy argument of `@$name` are: ${validStrategyValues.mkString(", ")}.")
        }
      case None => None
    }
  }

  override def getValue(directive: Directive, capabilities: Set[ConnectorCapability]) = {
    directive.argumentValueAsString("strategy").getOrElse(autoValue) match {
      case `autoValue` => IdBehaviour(FieldBehaviour.IdStrategy.Auto)
      case `noneValue` => IdBehaviour(FieldBehaviour.IdStrategy.None)
      case x           => sys.error(s"Encountered unknown strategy $x")
    }
  }
}

object CreatedAtDirective extends FieldDirectiveThingy[CreatedAtBehaviour.type] {
  override def name = "createdAt"

  override def isValidOnType(typeDef: ObjectTypeDefinition, capabilities: Set[ConnectorCapability]) = None

  override def isValidOnField(fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    if (fieldDef.typeName == "DateTime" && fieldDef.isRequired) {
      None
    } else {
      Some(s"Fields that are marked as @createdAt must be of type `DateTime!`.")
    }
  }

  override def hasValidArguments(directive: Directive, capabilities: Set[ConnectorCapability]) = None

  override protected def getValue(directive: Directive, capabilities: Set[ConnectorCapability]) = CreatedAtBehaviour
}

object UpdatedAtDirective extends FieldDirectiveThingy[UpdatedAtBehaviour.type] {
  override def name = "updatedAt"

  override def isValidOnType(typeDef: ObjectTypeDefinition, capabilities: Set[ConnectorCapability]) = None

  override def isValidOnField(fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    if (fieldDef.typeName == "DateTime" && fieldDef.isRequired) {
      None
    } else {
      Some(s"Fields that are marked as @updatedAt must be of type `DateTime!`.")
    }
  }

  override def hasValidArguments(directive: Directive, capabilities: Set[ConnectorCapability]) = None

  override protected def getValue(directive: Directive, capabilities: Set[ConnectorCapability]) = UpdatedAtBehaviour
}

object ScalarListDirective extends FieldDirectiveThingy[ScalarListBehaviour] {
  val embeddedValue       = "EMBEDDED"
  val relationValue       = "RELATION"
  val validStrategyValues = Set(embeddedValue, relationValue)

  override def name = "scalarList"

  override def isValidOnType(typeDef: ObjectTypeDefinition, capabilities: Set[ConnectorCapability]) = None

  override def isValidOnField(fieldDef: FieldDefinition, capabilities: Set[ConnectorCapability]) = {
    if (!fieldDef.isValidScalarListType) Some(s"Fields that are marked as `@scalarList` must be either of type `[String!]` or `[String!]!`.")
    else None
  }

  override def hasValidArguments(directive: Directive, capabilities: Set[ConnectorCapability]) = {
    val value = directive.argument("strategy").map(_.valueAsString)
    value match {
      case None => None
      case Some(v) =>
        if (validStrategyValues.contains(v)) {
          None
        } else {
          Some(s"Valid values for the strategy argument of `@scalarList` are: ${validStrategyValues.mkString(", ")}.")
        }
    }
  }

  override protected def getValue(directive: Directive, capabilities: Set[ConnectorCapability]): ScalarListBehaviour = {
    val defaultBehaviour = if (capabilities.contains(EmbeddedScalarListsCapability)) {
      ScalarListBehaviour(ScalarListStrategy.Embedded)
    } else if (capabilities.contains(NonEmbeddedScalarListCapability)) {
      ScalarListBehaviour(ScalarListStrategy.Relation)
    } else {
      sys.error("should not happen")
    }

    val configuredBehaviour = directive.argumentValueAsString("strategy").map {
      case `relationValue` => ScalarListBehaviour(FieldBehaviour.ScalarListStrategy.Relation)
      case `embeddedValue` => ScalarListBehaviour(FieldBehaviour.ScalarListStrategy.Embedded)
      case x               => sys.error(s"Unknown strategy $x")
    }

    configuredBehaviour.getOrElse(defaultBehaviour)
  }
}
