package com.prisma.deploy.migration.validation
import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.shared.models.{ConnectorCapability, TypeIdentifier}
import org.scalactic.{Bad, Good, Or}
import sangria.ast.{EnumTypeDefinition, FieldDefinition, Type}

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
          val behaviour = x.idBehaviour
          ScalarPrismaField(
            name = x.name,
            columnName = x.dbName,
            isList = x.isList,
            isRequired = x.isRequired,
            isUnique = x.isUnique,
            typeIdentifier = typeIdentifierForTypename(x.fieldType),
            defaultValue = None,
            behaviour = behaviour
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
    val allValidations            = Vector(reservedFieldsValidations)

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
