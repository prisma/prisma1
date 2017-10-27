package cool.graph.system.mutations

import cool.graph.GCDataTypes.GCStringConverter
import cool.graph._
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.client.{OverwriteAllRowsForColumn, OverwriteInvalidEnumForColumnWithMigrationValue}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class MigrateEnumValuesMutation(
    client: Client,
    project: Project,
    args: MigrateEnumValuesInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[MigrateEnumValuesMutationPayload]
    with Injectable {

  val oldEnum: Enum                   = args.oldEnum
  val updatedEnum: Enum               = args.updatedEnum
  val enumFields: List[Field]         = project.allFields.filter(_.enum.contains(oldEnum)).toList
  val removedEnumValues: List[String] = oldEnum.values.toList.filter(!updatedEnum.values.toList.contains(_))

  def migrationValueIsList(value: String): Boolean = {
    (value.startsWith("[") && value.endsWith("]")) || (value.startsWith("\"[") && value.endsWith("]\""))
  }

  def checkEnumValueUsageOnNodes(field: Field): List[InvalidInput] = {
    val model = project.getModelByFieldId_!(field.id)

    field.isList match {
      case true =>
        List(
          InvalidInput(error = UserInputErrors.CantRemoveEnumValueWhenNodesExist(model.name, field.name), isInvalid = clientDbQueries.existsByModel(model))
        )
      case false =>
        List(
          InvalidInput(
            UserInputErrors.EnumValueInUse(),
            isInvalid = Future
              .sequence(removedEnumValues.map(enum => clientDbQueries.itemCountForFieldValue(model, field, enum)))
              .map(_.exists(_ > 0))
          ))
    }
  }

  def changeEnumValuesInDB(field: Field): List[Mutaction with Product with Serializable] = {
    val model = project.getModelByFieldId_!(field.id)

    field.isList match {
      case true =>
        List(
          OverwriteAllRowsForColumn(
            projectId = project.id,
            model = model,
            field = field,
            value = CustomScalarTypes.parseValueFromString(args.migrationValue.get, field.typeIdentifier, field.isList)
          )
        )
      case false =>
        removedEnumValues.map { removedEnum =>
          OverwriteInvalidEnumForColumnWithMigrationValue(
            project.id,
            model = model,
            field = field,
            oldValue = removedEnum,
            migrationValue = args.migrationValue.get
          )
        }
    }
  }

  def validateOnFieldLevel(field: Field): List[Mutaction] = {
    if (removedEnumValues.isEmpty) {
      List.empty
    } else {
      (field.defaultValue, args.migrationValue) match {
        case (Some(dV), _) if !updatedEnum.values.contains(GCStringConverter(field.typeIdentifier, field.isList).fromGCValue(dV)) =>
          List(InvalidInput(UserInputErrors.EnumValueUsedAsDefaultValue(GCStringConverter(field.typeIdentifier, field.isList).fromGCValue(dV), field.name)))

        case (_, Some(_)) =>
          changeEnumValuesInDB(field)

        case (_, None) =>
          checkEnumValueUsageOnNodes(field)
      }
    }
  }

  override def prepareActions(): List[Mutaction] = {
    args.migrationValue match {
      case Some(migrationValue) =>
        enumFields.find(_.isList != migrationValueIsList(migrationValue)) match {
          case Some(invalidField) =>
            List(
              InvalidInput(
                UserInputErrors
                  .InvalidMigrationValueForEnum(project.getModelByFieldId_!(invalidField.id).name, invalidField.name, migrationValue)))

          case None =>
            enumFields.flatMap(validateOnFieldLevel)
        }

      case None =>
        enumFields.flatMap(validateOnFieldLevel)
    }
  }

  override def getReturnValue: Option[MigrateEnumValuesMutationPayload] = {
    Some(MigrateEnumValuesMutationPayload(clientMutationId = args.clientMutationId, enum = updatedEnum, project = project))
  }
}

case class MigrateEnumValuesMutationPayload(clientMutationId: Option[String], enum: Enum, project: models.Project) extends Mutation

case class MigrateEnumValuesInput(clientMutationId: Option[String], oldEnum: Enum, updatedEnum: Enum, migrationValue: Option[String]) extends MutationInput
