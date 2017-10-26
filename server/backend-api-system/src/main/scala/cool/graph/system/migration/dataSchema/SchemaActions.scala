package cool.graph.system.migration.dataSchema

import cool.graph.InternalMutation
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.client.{ClientDbQueries, EmptyClientDbQueries}
import cool.graph.system.mutations._
import scaldi.Injector

import scala.collection.{Seq, mutable}

case class UpdateSchemaActions(
    modelsToAdd: Seq[AddModelAction],
    modelsToUpdate: Seq[UpdateModelAction],
    modelsToRemove: Seq[DeleteModelAction],
    enumsToAdd: Seq[AddEnumAction],
    enumsToUpdate: Seq[UpdateEnumAction],
    enumsToRemove: Seq[DeleteEnumAction],
    relationsToAdd: Seq[AddRelationAction],
    relationsToRemove: Seq[DeleteRelationAction],
    relationsToUpdate: Seq[UpdateRelationAction]
) {
  def verbalDescriptions: Seq[VerbalDescription] = {
    modelsToAdd.map(_.verbalDescription) ++
      modelsToUpdate.map(_.verbalDescription) ++
      modelsToRemove.map(_.verbalDescription) ++
      enumsToAdd.map(_.verbalDescription) ++
      enumsToUpdate.map(_.verbalDescription) ++
      enumsToRemove.map(_.verbalDescription) ++
      relationsToAdd.map(_.verbalDescription) ++
      relationsToRemove.map(_.verbalDescription) ++
      relationsToUpdate.map(_.verbalDescription)
  }

  // will any of the actions potentially delete data
  def isDestructive: Boolean = {
    modelsToRemove.nonEmpty || enumsToRemove.nonEmpty || relationsToRemove.nonEmpty ||
    modelsToUpdate.exists(_.removeFields.nonEmpty)
  }

  def determineMutations(client: Client, project: Project, projectDbsFn: Project => InternalAndProjectDbs, clientDbQueries: ClientDbQueries)(
      implicit inj: Injector): (Seq[InternalMutation[_]], Project) = {
    val mutations      = mutable.Buffer.empty[InternalMutation[_]]
    var currentProject = project

    // ADD ENUMS
    mutations ++= enumsToAdd.map { addEnumAction =>
      val mutation = AddEnumMutation(client, currentProject, addEnumAction.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    // REMOVE MODELS
    mutations ++= modelsToRemove.map { deleteModelAction =>
      val mutation = DeleteModelMutation(client, currentProject, deleteModelAction.input, projectDbsFn, clientDbQueries)
      currentProject = mutation.updatedProject
      mutation
    }

    // ADD MODELS
    mutations ++= modelsToAdd.flatMap { addModelAction =>
      val mutation = AddModelMutation(client, currentProject, addModelAction.addModel, projectDbsFn)
      currentProject = mutation.updatedProject
      List(mutation) ++ addModelAction.addFields.map { addField =>
        val mutation = AddFieldMutation(client, currentProject, addField.input, projectDbsFn, clientDbQueries)
        currentProject = mutation.updatedProject
        mutation
      }
    }

    // UPDATE MODELS
    mutations ++= modelsToUpdate.flatMap { updateModelAction =>
      val updateModelMutation = updateModelAction.updateModel.map { updateModelInput =>
        UpdateModelMutation(client, currentProject, updateModelInput, projectDbsFn)
      }
      currentProject = updateModelMutation.map(_.updatedProject).getOrElse(currentProject)
      updateModelMutation.toSeq ++
        updateModelAction.addFields.map { addFieldAction =>
          val mutation = AddFieldMutation(client, currentProject, addFieldAction.input, projectDbsFn, clientDbQueries)
          currentProject = mutation.updatedProject
          mutation
        } ++
        updateModelAction.removeFields.map { deleteFieldAction =>
          val mutation = DeleteFieldMutation(client, currentProject, deleteFieldAction.input, projectDbsFn, clientDbQueries)
          currentProject = mutation.updatedProject
          mutation
        } ++
        updateModelAction.updateFields.map { updateFieldAction =>
          val mutation = UpdateFieldMutation(client, currentProject, updateFieldAction.input, projectDbsFn, clientDbQueries)
          currentProject = mutation.updatedProject
          mutation
        }
    }

    // REMOVE ENUMS
    mutations ++= enumsToRemove.map { deleteEnumAction =>
      val mutation = DeleteEnumMutation(client, currentProject, deleteEnumAction.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    // UPDATE ENUMS
    mutations ++= enumsToUpdate.map { updateEnumAction =>
      val mutation = UpdateEnumMutation(client, currentProject, updateEnumAction.input, projectDbsFn, clientDbQueries)
      currentProject = mutation.updatedProject
      mutation
    }

    // REMOVE RELATIONS
    mutations ++= relationsToRemove.map { deleteRelationAction =>
      val mutation = DeleteRelationMutation(client, currentProject, deleteRelationAction.input, projectDbsFn, clientDbQueries)
      currentProject = mutation.updatedProject
      mutation
    }

    // ADD RELATIONS
    mutations ++= relationsToAdd.map { addRelationAction =>
      val mutation = AddRelationMutation(client, currentProject, addRelationAction.input, projectDbsFn, clientDbQueries)
      currentProject = mutation.updatedProject
      mutation
    }

    // UPDATE RELATIONS
    mutations ++= relationsToUpdate.map { updateRelationAction =>
      val mutation            = UpdateRelationMutation(client, project, updateRelationAction.input, projectDbsFn, clientDbQueries)
      val (_, _, _, cProject) = mutation.updatedProject
      currentProject = cProject
      mutation
    }

    (mutations, currentProject)
  }
}

case class InitSchemaActions(
    modelsToAdd: Seq[AddModelAction],
    modelsToUpdate: Seq[UpdateModelAction],
    enumsToAdd: Seq[AddEnumAction],
    relationsToAdd: Seq[AddRelationAction]
) {
  def verbalDescriptions: Seq[VerbalDescription] = {
    modelsToAdd.map(_.verbalDescription) ++
      modelsToUpdate.map(_.verbalDescription) ++
      enumsToAdd.map(_.verbalDescription) ++
      relationsToAdd.map(_.verbalDescription)
  }

  def determineMutations(client: Client, project: Project, projectDbsFn: Project => InternalAndProjectDbs)(implicit inj: Injector): Seq[InternalMutation[_]] = {
    val updateActions = UpdateSchemaActions(
      modelsToAdd = modelsToAdd,
      modelsToUpdate = modelsToUpdate,
      modelsToRemove = Seq.empty,
      enumsToAdd = enumsToAdd,
      enumsToUpdate = Seq.empty,
      enumsToRemove = Seq.empty,
      relationsToAdd = relationsToAdd,
      relationsToRemove = Seq.empty,
      relationsToUpdate = Seq.empty
    )
    // because of all those empty sequences we know that the the DbQueries for an empty db won't be a problem in this call. But it's not nice this way.
    val (mutations, currentProject) = updateActions.determineMutations(client, project, projectDbsFn, EmptyClientDbQueries)
    mutations
  }
}

case class VerbalDescription(`type`: String, action: String, name: String, description: String, subDescriptions: Seq[VerbalSubDescription] = Seq.empty)

case class VerbalSubDescription(`type`: String, action: String, name: String, description: String)

/**
  * Action Data Structures
  */
case class AddModelAction(addModel: AddModelInput, addFields: List[AddFieldAction]) {
  def verbalDescription = VerbalDescription(
    `type` = "Type",
    action = "Create",
    name = addModel.modelName,
    description = s"A new type with the name `${addModel.modelName}` is created.",
    subDescriptions = addFields.map(_.verbalDescription)
  )
}

case class UpdateModelAction(newName: String,
                             id: String,
                             updateModel: Option[UpdateModelInput],
                             addFields: List[AddFieldAction],
                             removeFields: List[DeleteFieldAction],
                             updateFields: List[UpdateFieldAction]) {

  def hasChanges: Boolean = addFields.nonEmpty || removeFields.nonEmpty || updateFields.nonEmpty || updateModel.nonEmpty

  lazy val verbalDescription = VerbalDescription(
    `type` = "Type",
    action = "Update",
    name = newName,
    description = s"The type `$newName` is updated.",
    subDescriptions = addFieldDescriptions ++ removeFieldDescriptions ++ updateFieldDescriptions
  )

  val addFieldDescriptions: List[VerbalSubDescription]    = addFields.map(_.verbalDescription)
  val removeFieldDescriptions: List[VerbalSubDescription] = removeFields.map(_.verbalDescription)
  val updateFieldDescriptions: List[VerbalSubDescription] = updateFields.map(_.verbalDescription)
}

case class DeleteModelAction(
    modelName: String,
    input: DeleteModelInput
) {
  def verbalDescription = VerbalDescription(
    `type` = "Type",
    action = "Delete",
    name = modelName,
    description = s"The type `$modelName` is removed. This also removes all its fields and relations."
  )
}

case class AddFieldAction(input: AddFieldInput) {
  val verbalDescription = VerbalSubDescription(
    `type` = "Field",
    action = "Create",
    name = input.name,
    description = {
      val typeString = VerbalDescriptionUtil.typeString(typeName = input.typeIdentifier.toString, isRequired = input.isRequired, isList = input.isList)
      s"A new field with the name `${input.name}` and type `$typeString` is created."
    }
  )
}

case class UpdateFieldAction(input: UpdateFieldInput, fieldName: String) {
  val verbalDescription = VerbalSubDescription(
    `type` = "Field",
    action = "Update",
    name = fieldName,
    description = s"The field `$fieldName` is updated."
  )
}

case class DeleteFieldAction(input: DeleteFieldInput, fieldName: String) {
  val verbalDescription = VerbalSubDescription(
    `type` = "Field",
    action = "Delete",
    name = fieldName,
    description = s"The field `$fieldName` is deleted."
  )
}

case class AddRelationAction(input: AddRelationInput, leftModelName: String, rightModelName: String) {
  def verbalDescription =
    VerbalDescription(
      `type` = "Relation",
      action = "Create",
      name = input.name,
      description = s"The relation `${input.name}` is created. It connects the type `$leftModelName` with the type `$rightModelName`."
    )
}

case class DeleteRelationAction(input: DeleteRelationInput, relationName: String, leftModelName: String, rightModelName: String) {
  def verbalDescription =
    VerbalDescription(
      `type` = "Relation",
      action = "Delete",
      name = relationName,
      description = s"The relation `$relationName` is deleted. It connected the type `$leftModelName` with the type `$rightModelName`."
    )
}

case class UpdateRelationAction(input: UpdateRelationInput, oldRelationName: String, newRelationName: String, leftModelName: String, rightModelName: String) {
  def verbalDescription =
    VerbalDescription(
      `type` = "Relation",
      action = "Update",
      name = oldRelationName,
      description = s"The relation `$oldRelationName` is renamed to `$newRelationName`. It connects the type `$leftModelName` with the type `$rightModelName`."
    )
}

case class AddEnumAction(input: AddEnumInput) {
  def verbalDescription =
    VerbalDescription(`type` = "Enum",
                      action = "Create",
                      name = input.name,
                      description = s"The enum `${input.name}` is created. It has the values: ${input.values.mkString(",")}.")
}

case class UpdateEnumAction(input: UpdateEnumInput, newName: String, newValues: Seq[String]) {
  def verbalDescription =
    VerbalDescription(`type` = "Enum",
                      action = "Update",
                      name = newName,
                      description = s"The enum `$newName` is updated. It has the values: ${newValues.mkString(",")}.")
}

case class DeleteEnumAction(input: DeleteEnumInput, name: String) {
  def verbalDescription =
    VerbalDescription(`type` = "Enum", action = "Delete", name = name, description = s"The enum `$name` is deleted.")
}

object VerbalDescriptionUtil {
  def typeString(typeName: String, isRequired: Boolean, isList: Boolean): String = {
    (isList, isRequired) match {
      case (false, false) => s"$typeName"
      case (false, true)  => s"$typeName!"
      case (true, true)   => s"[$typeName!]!"
      case (true, false)  => s"[$typeName!]"
    }
  }
}
