package cool.graph.system.migration.dataSchema

import cool.graph.GCDataTypes.GCStringConverter
import cool.graph.Types.Id
import cool.graph.shared.TypeInfo
import cool.graph.shared.models.{Project, TypeIdentifier}
import cool.graph.system.database.SystemFields
import cool.graph.system.migration.Diff
import cool.graph.system.mutations._
import sangria.ast.FieldDefinition

import scala.collection.Seq

object SchemaMigrator {
  def apply(project: Project, newSchema: String, clientMutationId: Option[String]): SchemaMigrator = {
    val oldSchema = SchemaExport.renderSchema(project)
    val result    = SchemaDiff(oldSchema, newSchema).get
    SchemaMigrator(result, project, clientMutationId)
  }
}

case class SchemaMigrator(diffResult: SchemaDiff, project: Project, clientMutationId: Option[String]) {
  import DataSchemaAstExtensions._

  def determineActionsForUpdate(): UpdateSchemaActions = {
    UpdateSchemaActions(
      modelsToAdd = modelsToAdd,
      modelsToUpdate = modelsToUpdate,
      modelsToRemove = modelsToRemove,
      enumsToAdd = enumsToAdd,
      enumsToUpdate = enumsToUpdate,
      enumsToRemove = enumsToRemove,
      relationsToAdd = relationsToAdd,
      relationsToRemove = relationsToRemove,
      relationsToUpdate = relationsToRename
    )
  }

  def determineActionsForInit(): InitSchemaActions = {
    // as this is the case for initializing a schema, only system models can be updated at this point. We just ignore updated & removed fields here.
    val systemModelsToUpdate = modelsToUpdate.map { updateModelAction =>
      updateModelAction.copy(removeFields = List.empty, updateFields = List.empty)
    }
    InitSchemaActions(
      modelsToAdd = modelsToAdd,
      modelsToUpdate = systemModelsToUpdate,
      enumsToAdd = enumsToAdd,
      relationsToAdd = relationsToAdd
    )
  }

  lazy val modelsToRemove: Seq[DeleteModelAction] = {
    diffResult.removedTypes
      .map { removedType =>
        project
          .getModelByName(removedType)
          .getOrElse(sys.error(s"Did not find removedType $removedType in the project"))
      }
      .filter(model => !model.isSystem || project.isEjected)
      .map(model =>DeleteModelAction(modelName = model.name, input = DeleteModelInput(clientMutationId, model.id)))
  }

  lazy val modelsToAdd: List[AddModelAction] = diffResult.addedTypes.map { addedType =>
    val objectType = diffResult.newSchema.objectTypes.find(_.name == addedType).get

    val addModel = AddModelInput(
      clientMutationId = clientMutationId,
      projectId = project.id,
      modelName = addedType,
      description = None,
      fieldPositions = None
    )
    val addFields = objectType.nonRelationFields.filter(f => f.name != SystemFields.idFieldName).map { fieldDef =>
      AddFieldAction(getAddFieldInputForFieldDef(fieldDef, addModel.id))
    }

    AddModelAction(addModel, addFields.toList)
  }.toList

  lazy val relationsToAdd: Seq[AddRelationAction] = {
    val addRelationActions =
      diffResult.newSchema.objectTypes.flatMap(objectType => objectType.relationFields.map(rf => objectType.name -> rf)).groupBy(_._2.oldRelationName.get).map {
        case (relationName, modelNameAndFieldList) =>
          require(
            modelNameAndFieldList.size == 2 || modelNameAndFieldList.size == 1,
            s"There must be either 1 or 2 fields with same relation name directive. Relation was $relationName. There were ${modelNameAndFieldList.size} fields instead."
          )
          val (leftModelName, leftField)   = modelNameAndFieldList.head
          val (rightModelName, rightField) = modelNameAndFieldList.last
          val leftModelId                  = findModelIdForName(leftModelName)
          val rightModelId                 = findModelIdForName(rightModelName)

          val input = AddRelationInput(
            clientMutationId = clientMutationId,
            projectId = project.id,
            description = None,
            name = relationName,
            leftModelId = leftModelId,
            rightModelId = rightModelId,
            fieldOnLeftModelName = leftField.name,
            fieldOnRightModelName = rightField.name,
            fieldOnLeftModelIsList = leftField.isList,
            fieldOnRightModelIsList = rightField.isList,
            fieldOnLeftModelIsRequired = leftField.fieldType.isRequired,
            fieldOnRightModelIsRequired = rightField.fieldType.isRequired
          )
          AddRelationAction(input = input, leftModelName = leftModelName, rightModelName = rightModelName)
      }

    val removedModelIds = modelsToRemove.map(_.input).map(_.modelId)
    val removedRelationIds =
      project.relations.filter(relation => removedModelIds.contains(relation.modelAId) || removedModelIds.contains(relation.modelBId)).map(_.id)

    val projectWithoutRemovedRelations = project.copy(relations = project.relations.filter(relation => !removedRelationIds.contains(relation.id)))

    val filteredAddRelationActions = addRelationActions
      .filter(addRelation => !RelationDiff.projectContainsRelation(projectWithoutRemovedRelations, addRelation))
      .toSeq

    filteredAddRelationActions
  }

  lazy val relationsToRemove: Seq[DeleteRelationAction] = {
    for {
      relation <- project.relations
      if !RelationDiff.schemaContainsRelation(project, diffResult.newSchema, relation)
      oneOfTheModelsWasRemoved = modelsToRemove.exists { remove =>
        remove.input.modelId == relation.modelAId || remove.input.modelId == relation.modelBId
      }
      if !oneOfTheModelsWasRemoved
    } yield {
      val input = DeleteRelationInput(
        clientMutationId = clientMutationId,
        relationId = relation.id
      )
      val leftModel  = relation.getModelA_!(project)
      val rightModel = relation.getModelB_!(project)
      DeleteRelationAction(
        input = input,
        relationName = relation.name,
        leftModelName = leftModel.name,
        rightModelName = rightModel.name
      )
    }
  }

  lazy val relationsToRename: Seq[UpdateRelationAction] = {
    val tmp = for {
      objectType      <- diffResult.newSchema.objectTypes
      field           <- objectType.fields
      newRelationName <- field.relationName
      oldRelationName <- field.oldRelationName
      if newRelationName != oldRelationName
      relation <- project.getRelationByName(oldRelationName)
    } yield {
      val leftModel  = relation.getModelA_!(project)
      val rightModel = relation.getModelB_!(project)
      UpdateRelationAction(
        input = UpdateRelationInput(
          clientMutationId = None,
          id = relation.id,
          description = None,
          name = Some(newRelationName),
          leftModelId = None,
          rightModelId = None,
          fieldOnLeftModelName = None,
          fieldOnRightModelName = None,
          fieldOnLeftModelIsList = None,
          fieldOnRightModelIsList = None,
          fieldOnLeftModelIsRequired = None,
          fieldOnRightModelIsRequired = None
        ),
        oldRelationName = oldRelationName,
        newRelationName = newRelationName,
        leftModelName = leftModel.name,
        rightModelName = rightModel.name
      )
    }
    val distinctRenameActions = tmp.groupBy(_.input.name).values.map(_.head).toSeq
    distinctRenameActions
  }

  lazy val modelsToUpdate: Seq[UpdateModelAction] = diffResult.updatedTypes
    .map { updatedType =>
      val model      = project.getModelByName_!(updatedType.oldName)
      val objectType = diffResult.newSchema.objectType(updatedType.name).get

      // FIXME: description is not evaluated yet
      val updateModelInput = {
        val tmp = UpdateModelInput(clientMutationId = clientMutationId,
                                   modelId = model.id,
                                   description = None,
                                   name = Diff.diff(model.name, updatedType.name),
                                   fieldPositions = None)

        if (tmp.isAnyArgumentSet()) Some(tmp) else None
      }

      val fieldsToAdd = updatedType.addedFields.flatMap { addedFieldName =>
        val fieldDef = objectType.field(addedFieldName).get
        if (fieldDef.isNoRelation) {
          val input = getAddFieldInputForFieldDef(fieldDef, model.id)
          Some(AddFieldAction(input))
        } else {
          None
        }
      }

      val fieldsToRemove = updatedType.removedFields
        .map(model.getFieldByName_!)
        .filter(field => (!field.isSystem || (field.isSystem && SystemFields.isDeletableSystemField(field.name))) && !field.isRelation)
        .map { removedField =>
          val input = DeleteFieldInput(clientMutationId = clientMutationId, removedField.id)
          DeleteFieldAction(input = input, fieldName = removedField.name)
        }

      val fieldsToUpdate = updatedType.updatedFields
        .map { updatedField =>
          val newFieldDef       = diffResult.newSchema.objectType(updatedType.name).get.field(updatedField.name).get
          val currentField      = model.getFieldByName_!(updatedField.oldName)
          val newEnumId         = findEnumIdForNameOpt(updatedField.newType)
          val newTypeIdentifier = TypeInfo.extract(newFieldDef, None, diffResult.newSchema.enumTypes, false).typeIdentifier

          val oldDefaultValue = currentField.defaultValue.map(GCStringConverter(currentField.typeIdentifier, currentField.isList).fromGCValue)
          val newDefaultValue = newFieldDef.defaultValue

          val inputDefaultValue = (oldDefaultValue, newDefaultValue) match {
            case (Some(oldDV), None)                          => Some(None)
            case (Some(oldDV), Some(newDV)) if oldDV == newDV => None
            case (_, Some(newDV))                             => Some(Some(newDV))
            case (None, None)                                 => None
          }

          //description cant be reset to null at the moment. it would need a similar behavior to defaultValue

          import Diff._
          val input = UpdateFieldInput(
            clientMutationId = clientMutationId,
            fieldId = model.getFieldByName_!(updatedField.oldName).id,
            defaultValue = inputDefaultValue,
            migrationValue = newFieldDef.migrationValue,
            description = diffOpt(currentField.description, newFieldDef.description),
            name = diff(currentField.name, newFieldDef.name),
            typeIdentifier = diff(currentField.typeIdentifier, newTypeIdentifier).map(_.toString),
            isUnique = diff(currentField.isUnique, newFieldDef.isUnique),
            isRequired = diff(currentField.isRequired, newFieldDef.fieldType.isRequired),
            isList = diff(currentField.isList, newFieldDef.isList),
            enumId = diffOpt(currentField.enum.map(_.id), newEnumId)
          )
          UpdateFieldAction(input = input, fieldName = updatedField.oldName)
        }

      val fieldsWithChangesToUpdate = fieldsToUpdate.filter(updateField => updateField.input.isAnyArgumentSet())

      UpdateModelAction(updatedType.name, model.id, updateModelInput, fieldsToAdd, fieldsToRemove, fieldsWithChangesToUpdate)
    }
    .filter(_.hasChanges)

  lazy val enumsToAdd: Vector[AddEnumAction] = diffResult.addedEnums.map { addedEnum =>
    val enumType = diffResult.newSchema.enumType(addedEnum).get
    val input = AddEnumInput(
      clientMutationId = clientMutationId,
      projectId = project.id,
      name = enumType.name,
      values = enumType.valuesAsStrings
    )
    AddEnumAction(input)
  }

  lazy val enumsToRemove: Vector[DeleteEnumAction] = diffResult.removedEnums.map { removedEnum =>
    val enumId = findEnumIdForName(removedEnum)
    val input  = DeleteEnumInput(clientMutationId = clientMutationId, enumId = enumId)
    DeleteEnumAction(input, name = removedEnum)
  }

  lazy val enumsToUpdate: Vector[UpdateEnumAction] = diffResult.updatedEnums
    .map { updatedEnum =>
      val enumId  = findEnumIdForUpdatedEnum(updatedEnum.oldName)
      val newEnum = diffResult.newSchema.enumType(updatedEnum.name).get
      newEnum.oldName
      val newValues = diffResult.newSchema.enumType(updatedEnum.name).get.valuesAsStrings
      val oldValues = diffResult.oldSchema.enumType(updatedEnum.oldName).get.valuesAsStrings
      val input = UpdateEnumInput(
        clientMutationId = clientMutationId,
        enumId = enumId,
        name = Diff.diff(updatedEnum.oldName, updatedEnum.name),
        values = Diff.diff(oldValues, newValues),
        migrationValue = newEnum.migrationValue
      )
      UpdateEnumAction(input, newName = updatedEnum.name, newValues = newValues) //add migrationValue to output
    }
    .filter(_.input.isAnyArgumentSet())

  def getAddFieldInputForFieldDef(fieldDef: FieldDefinition, modelId: String): AddFieldInput = {
    val typeInfo = TypeInfo.extract(fieldDef, None, diffResult.newSchema.enumTypes, false)
    val enumId = typeInfo.typeIdentifier match {
      case TypeIdentifier.Enum =>
        Some(findEnumIdForName(typeInfo.typename))
      case _ =>
        None
    }
    val isRequired = if (fieldDef.isList && typeInfo.typeIdentifier == TypeIdentifier.Relation) {
      false
    } else {
      typeInfo.isRequired
    }
    AddFieldInput(
      clientMutationId = clientMutationId,
      modelId = modelId,
      name = fieldDef.name,
      typeIdentifier = typeInfo.typeIdentifier,
      isRequired = isRequired,
      isList = typeInfo.isList,
      isUnique = fieldDef.isUnique,
      relationId = None,
      enumId = enumId,
      defaultValue = fieldDef.defaultValue,
      migrationValue = fieldDef.migrationValue,
      description = None
    )
  }

  def findModelIdForName(modelName: String): Id = {
    findModelIdForNameOpt(modelName)
      .getOrElse(sys.error(s"The model $modelName was not found in current project, added models or updated models."))
  }

  def findModelIdForNameOpt(modelName: String): Option[Id] = {
    val inProject: Option[Id]           = project.getModelByName(modelName).map(_.id)
    val inAddedModels: Option[Id]       = modelsToAdd.find(_.addModel.modelName == modelName).map(_.addModel.id)
    val inUpdatedModels: Option[String] = modelsToUpdate.find(_.newName == modelName).map(_.id)
    inProject
      .orElse(inAddedModels)
      .orElse(inUpdatedModels)
  }

  def findEnumIdForUpdatedEnum(enumName: String): Id = {
    project
      .getEnumByName(enumName)
      .map(_.id)
      .getOrElse(sys.error(s"The enum $enumName was not found in current project."))
  }

  def findEnumIdForName(enumName: String): Id = {
    findEnumIdForNameOpt(enumName).getOrElse(sys.error(s"The enum $enumName was not found in current project, added enums or updated enums."))
  }

  def findEnumIdForNameOpt(enumName: String): Option[Id] = {
    val inProject: Option[Id]    = project.getEnumByName(enumName).map(_.id)
    val inAddedEnums: Option[Id] = enumsToAdd.find(_.input.name == enumName).map(_.input.id)
    val inUpdatedEnums           = enumsToUpdate.find(_.input.name == enumName).map(_.input.enumId)
    inProject
      .orElse(inAddedEnums)
      .orElse(inUpdatedEnums)
  }
}
