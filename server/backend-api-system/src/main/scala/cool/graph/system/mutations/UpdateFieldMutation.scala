package cool.graph.system.mutations

import cool.graph.GCDataTypes.{GCStringConverter, GCValue, NullGCValue}
import cool.graph._
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.{SystemErrors, UserAPIErrors, UserInputErrors}
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.client._
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateField}
import org.scalactic.{Bad, Good, Or}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Try}

case class UpdateFieldMutation(
    client: Client,
    project: Project,
    args: UpdateFieldInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateFieldMutationPayload]
    with Injectable {

  val oldField: Field = project.getFieldById_!(args.fieldId)

  val model: Model = project.getModelByFieldId_!(args.fieldId)

  val updatedField: Field = mergeInputValuesToField(oldField)
  val newModel: Model     = model.copy(fields = model.fields.filter(_.id != oldField.id) :+ updatedField)
  val updatedProject: Project = project.copy(models = project.models.map {
    case oldModel if oldModel.id == newModel.id => newModel
    case oldModel                               => oldModel
  })

  def mergeInputValuesToField(existingField: Field): Field = {
    val newTypeIdentifier = args.typeIdentifier.map(CustomScalarTypes.parseTypeIdentifier).getOrElse(existingField.typeIdentifier)
    val newIsList         = args.isList.getOrElse(existingField.isList)

    val oldDefaultValue: Option[GCValue] =
      (newTypeIdentifier != oldField.typeIdentifier) || args.isList.exists(_ != oldField.isList) match {
        case true  => None
        case false => oldField.defaultValue
      }

    val newDefaultValue: Option[GCValue] = args.defaultValue match {
      case None          => None
      case Some(None)    => Some(NullGCValue())
      case Some(Some(x)) => GCStringConverter(newTypeIdentifier, newIsList).toGCValue(x).toOption
    }

    val defaultValueMerged = newDefaultValue.orElse(oldDefaultValue)

    val newEnum = if (newTypeIdentifier == TypeIdentifier.Enum) {
      args.enumId match {
        case Some(enumId) => Some(project.getEnumById_!(enumId))
        case None         => existingField.enum
      }
    } else None

    existingField.copy(
      defaultValue = defaultValueMerged,
      description = args.description.orElse(existingField.description),
      name = args.name.getOrElse(existingField.name),
      typeIdentifier = newTypeIdentifier,
      isUnique = args.isUnique.getOrElse(existingField.isUnique),
      isRequired = args.isRequired.getOrElse(existingField.isRequired),
      isList = newIsList,
      enum = newEnum
    )
  }

  def removedEnumValues: List[String] = {
    oldField.enum match {
      case Some(oldEnum) =>
        updatedField.enum match {
          case Some(newEnum) => oldEnum.values.filter(!newEnum.values.contains(_)).toList
          case None          => List.empty
        }

      case None => List.empty
    }
  }

  def shouldUpdateClientDbColumn(oldField: Field, updatedField: Field): Boolean = {
    if (oldField.isScalar)
      oldField.isRequired != updatedField.isRequired ||
      oldField.name != updatedField.name ||
      oldField.typeIdentifier != updatedField.typeIdentifier ||
      oldField.isList != updatedField.isList ||
      oldField.isUnique != updatedField.isUnique
    else false
  }

  object MigrationType extends Enumeration {
    type MigrationType = Value
    val UniqueViolation                = Value("UNIQUE_VIOLATION")
    val AllFields                      = Value("ALL_FIELDS")
    val RemovedEnumFieldsAndNullFields = Value("REMOVED_ENUM_FIELDS_AND_NULL_FIELDS")
    val RemovedEnumFields              = Value("REMOVED_ENUM_FIELDS")
    val NullFields                     = Value("NULL_FIELDS")
    val NoMigrationValue               = Value("NO_MIGRATION_VALUE")
    val VoluntaryMigrationValue        = Value("UNNECESSARY_MIGRATION_VALUE")
  }

  def scalarValueMigrationType(): MigrationType.Value = {
    if (args.migrationValue.isEmpty)
      MigrationType.NoMigrationValue
    else if (updatedField.isUnique)
      MigrationType.UniqueViolation
    else if (UpdateField.typeChangeRequiresMigration(oldField, updatedField))
      MigrationType.AllFields
    else if (oldField.isList != updatedField.isList)
      MigrationType.AllFields
    else if (updatedField.isList && removedEnumValues.nonEmpty)
      MigrationType.AllFields
    else if (!updatedField.isList && removedEnumValues.nonEmpty && updatedField.isRequired && !oldField.isRequired)
      MigrationType.RemovedEnumFieldsAndNullFields
    else if (!updatedField.isList && removedEnumValues.nonEmpty)
      MigrationType.RemovedEnumFields
    else if (updatedField.isRequired && !oldField.isRequired)
      MigrationType.NullFields
    else
      MigrationType.VoluntaryMigrationValue
  }

  def violatedFieldConstraints: List[FieldConstraint] = {
    val listConstraints  = oldField.constraints.filter(_.constraintType == FieldConstraintType.LIST)
    val otherConstraints = oldField.constraints.filter(_.constraintType != FieldConstraintType.LIST)
    val newType          = updatedField.typeIdentifier

    () match {
      case _ if listConstraints.nonEmpty && !updatedField.isList =>
        listConstraints

      case _ if otherConstraints.nonEmpty && !oldField.isList && updatedField.isList =>
        otherConstraints

      case _ if otherConstraints.nonEmpty =>
        otherConstraints.head.constraintType match {
          case FieldConstraintType.STRING if newType != TypeIdentifier.String                                 => otherConstraints
          case FieldConstraintType.BOOLEAN if newType != TypeIdentifier.Boolean                               => otherConstraints
          case FieldConstraintType.NUMBER if newType != TypeIdentifier.Float && newType != TypeIdentifier.Int => otherConstraints
          case _                                                                                              => List.empty
        }

      case _ =>
        List.empty
    }
  }

  override def prepareActions(): List[Mutaction] = {

    () match {
      case _ if verifyDefaultValue.nonEmpty =>
        actions = List(InvalidInput(verifyDefaultValue.head))

      case _ if (!oldField.isScalar || !updatedField.isScalar) && args.isAnyArgumentSet(List("isRequired", "name")) =>
        actions = List(InvalidInput(SystemErrors.IsNotScalar(args.typeIdentifier.getOrElse(oldField.relatedModel(project).get.name))))

      case _ if violatedFieldConstraints.nonEmpty =>
        actions = List(
          InvalidInput(SystemErrors.UpdatingTheFieldWouldViolateConstraint(fieldId = oldField.id, constraintId = violatedFieldConstraints.head.id)))

      case _ if scalarValueMigrationType == MigrationType.UniqueViolation =>
        actions = List(InvalidInput(UserAPIErrors.UniqueConstraintViolation(model.name, "Field = " + oldField.name + " Value = " + args.migrationValue.get)))

      case _ =>
        createActions

    }
    actions
  }

  private def createActions = {
    if (removedEnumValues.nonEmpty && args.migrationValue.isEmpty) {
      if (oldField.isList) {
        actions :+= InvalidInput(UserInputErrors.CantRemoveEnumValueWhenNodesExist(model.name, updatedField.name),
                                 isInvalid = clientDbQueries.itemCountForModel(model).map(_ > 0))
      } else {
        actions :+= InvalidInput(
          UserInputErrors.EnumValueInUse(),
          isInvalid = Future
            .sequence(removedEnumValues.map(enum => clientDbQueries.itemCountForFieldValue(model, oldField, enum)))
            .map(_.exists(_ > 0))
        )
      }
    }

    actions :+= UpdateField(
      model = model,
      oldField = oldField,
      field = updatedField,
      migrationValue = args.migrationValue,
      clientDbQueries = clientDbQueries
    )

    actions ++= (scalarValueMigrationType() match {
      case MigrationType.AllFields =>
        replaceAllRowsWithMigValue

      case MigrationType.VoluntaryMigrationValue =>
        replaceAllRowsWithMigValue

      case MigrationType.RemovedEnumFieldsAndNullFields =>
        removedEnumValues.map(removedEnum => overWriteInvalidEnumsForColumn(removedEnum)) :+ populateNullRowsForColumn(args.migrationValue)

      case MigrationType.RemovedEnumFields =>
        removedEnumValues.map(removedEnum => overWriteInvalidEnumsForColumn(removedEnum))

      case MigrationType.NullFields =>
        List(populateNullRowsForColumn(CustomScalarTypes.parseValueFromString(args.migrationValue.get, updatedField.typeIdentifier, updatedField.isList)))

      case _ =>
        List.empty
    })

    if (shouldUpdateClientDbColumn(oldField, updatedField)) {
      actions :+= UpdateColumn(projectId = project.id, model = model, oldField = oldField, newField = updatedField)
      actions ++= project
        .getRelationFieldMirrorsByFieldId(oldField.id)
        .map(mirror => UpdateRelationFieldMirrorColumn(project, project.getRelationById_!(mirror.relationId), oldField, updatedField))
    }

    actions ++= (scalarValueMigrationType() match {
      case MigrationType.NoMigrationValue =>
        List.empty

      case _ =>
        project
          .getRelationFieldMirrorsByFieldId(oldField.id)
          .map(mirror => PopulateRelationFieldMirrorColumn(project, project.getRelationById_!(mirror.relationId), oldField))
    })

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  private def populateNullRowsForColumn(value: Option[Any]) = {
    PopulateNullRowsForColumn(
      projectId = project.id,
      model = model,
      field = updatedField,
      value = value
    )
  }

  private def overWriteInvalidEnumsForColumn(removedEnum: String) = {
    OverwriteInvalidEnumForColumnWithMigrationValue(projectId = project.id,
                                                    model = model,
                                                    field = updatedField,
                                                    oldValue = removedEnum,
                                                    migrationValue = args.migrationValue.get)
  }

  private def replaceAllRowsWithMigValue = {
    val createColumnField = updatedField.copy(name = oldField.name, isRequired = false)
    List(
      DeleteColumn(projectId = project.id, model = model, field = oldField),
      CreateColumn(projectId = project.id, model = model, field = createColumnField),
      OverwriteAllRowsForColumn(
        projectId = project.id,
        model = model,
        field = createColumnField,
        value = CustomScalarTypes.parseValueFromString(args.migrationValue.get, createColumnField.typeIdentifier, createColumnField.isList)
      )
    ) ++
      project
        .getRelationFieldMirrorsByFieldId(oldField.id)
        .flatMap(mirror =>
          List(
            DeleteRelationFieldMirrorColumn(project, project.getRelationById_!(mirror.relationId), oldField),
            CreateRelationFieldMirrorColumn(project, project.getRelationById_!(mirror.relationId), createColumnField)
        ))
  }

  override def getReturnValue: Option[UpdateFieldMutationPayload] = {

    Some(
      UpdateFieldMutationPayload(
        clientMutationId = args.clientMutationId,
        field = updatedField,
        model = newModel,
        project = updatedProject
      ))
  }

  val verifyDefaultValue: List[UserInputErrors.InvalidValueForScalarType] = {
    val x = args.defaultValue match {
      case None              => None
      case Some(None)        => Some(Good(NullGCValue()))
      case Some(Some(value)) => Some(GCStringConverter(updatedField.typeIdentifier, updatedField.isList).toGCValue(value))
    }

    x match {
      case Some(Good(_))    => List.empty
      case Some(Bad(error)) => List(error)
      case None             => List.empty
    }
  }

}

case class UpdateFieldMutationPayload(clientMutationId: Option[String], model: models.Model, field: models.Field, project: models.Project) extends Mutation

case class UpdateFieldInput(clientMutationId: Option[String],
                            fieldId: String,
                            defaultValue: Option[Option[String]],
                            migrationValue: Option[String],
                            description: Option[String],
                            name: Option[String],
                            typeIdentifier: Option[String],
                            isUnique: Option[Boolean],
                            isRequired: Option[Boolean],
                            isList: Option[Boolean],
                            enumId: Option[String])
    extends MutationInput
