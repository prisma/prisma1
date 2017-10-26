package cool.graph.system.migration

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import cool.graph.InternalMutation
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.migration.dataSchema.VerbalDescription
import cool.graph.system.mutations._
import scaldi.Injector

import scala.collection.{Seq, mutable}

trait ModuleActions {
  def verbalDescriptions: Vector[VerbalDescription]
  def determineMutations(client: Client, project: Project, projectDbsFn: Project => InternalAndProjectDbs)(
      implicit inj: Injector,
      actorSystem: ActorSystem,
      actorMaterializer: ActorMaterializer): (Seq[InternalMutation[_]], Project)
}

case class RemoveModuleActions(
    subscriptionFunctionsToRemove: Vector[RemoveSubscriptionFunctionAction],
    schemaExtensionFunctionsToRemove: Vector[RemoveSchemaExtensionFunctionAction],
    operationFunctionsToRemove: Vector[RemoveOperationFunctionAction],
    modelPermissionsToRemove: Vector[RemoveModelPermissionAction],
    relationPermissionsToRemove: Vector[RemoveRelationPermissionAction],
    rootTokensToRemove: Vector[RemoveRootTokenAction]
) extends ModuleActions {
  override def verbalDescriptions: Vector[VerbalDescription] = {
    subscriptionFunctionsToRemove.map(_.verbalDescription) ++
      schemaExtensionFunctionsToRemove.map(_.verbalDescription) ++
      operationFunctionsToRemove.map(_.verbalDescription) ++
      modelPermissionsToRemove.map(_.verbalDescription) ++
      relationPermissionsToRemove.map(_.verbalDescription) ++
      rootTokensToRemove.map(_.verbalDescription)
  }

  def determineMutations(client: Client, project: Project, projectDbsFn: Project => InternalAndProjectDbs)(
      implicit inj: Injector,
      actorSystem: ActorSystem,
      actorMaterializer: ActorMaterializer): (Seq[InternalMutation[_]], Project) = {
    val mutations      = mutable.Buffer.empty[InternalMutation[_]]
    var currentProject = project

    // REMOVE FUNCTIONS
    mutations ++= subscriptionFunctionsToRemove.map { x =>
      val mutation = DeleteFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    mutations ++= schemaExtensionFunctionsToRemove.map { x =>
      val mutation = DeleteFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    mutations ++= operationFunctionsToRemove.map { x =>
      val mutation = DeleteFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    // REMOVE PERMISSIONS
    mutations ++= modelPermissionsToRemove.map { x =>
      val model      = project.getModelByModelPermissionId_!(x.input.modelPermissionId)
      val permission = project.getModelPermissionById_!(x.input.modelPermissionId)
      val mutation   = DeleteModelPermissionMutation(client, currentProject, model, permission, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    mutations ++= relationPermissionsToRemove.map { x =>
      val relation   = project.getRelationByRelationPermissionId_!(x.input.relationPermissionId)
      val permission = project.getRelationPermissionById_!(x.input.relationPermissionId)
      val mutation   = DeleteRelationPermissionMutation(client, currentProject, relation, permission, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    // REMOVE ROOTTOKENS
    mutations ++= rootTokensToRemove.map { x =>
      val rootToken = project.getRootTokenById_!(x.input.rootTokenId)
      val mutation  = DeleteRootTokenMutation(client, currentProject, rootToken, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    (mutations, currentProject)
  }
}

case class AddModuleActions(subscriptionFunctionsToAdd: Vector[AddServerSideSubscriptionFunctionAction],
                            schemaExtensionFunctionsToAdd: Vector[AddSchemaExtensionFunctionAction],
                            operationFunctionsToAdd: Vector[AddOperationFunctionAction],
                            modelPermissionsToAdd: Vector[AddModelPermissionAction],
                            relationPermissionsToAdd: Vector[AddRelationPermissionAction],
                            rootTokensToCreate: Vector[CreateRootTokenAction])
    extends ModuleActions {
  def verbalDescriptions: Vector[VerbalDescription] = {
    subscriptionFunctionsToAdd.map(_.verbalDescription) ++
      schemaExtensionFunctionsToAdd.map(_.verbalDescription) ++
      operationFunctionsToAdd.map(_.verbalDescription) ++
      modelPermissionsToAdd.map(_.verbalDescription) ++
      relationPermissionsToAdd.map(_.verbalDescription) ++
      rootTokensToCreate.map(_.verbalDescription)
  }

  def determineMutations(client: Client, project: Project, projectDbsFn: Project => InternalAndProjectDbs)(
      implicit inj: Injector,
      actorSystem: ActorSystem,
      actorMaterializer: ActorMaterializer): (Seq[InternalMutation[_]], Project) = {
    val mutations      = mutable.Buffer.empty[InternalMutation[_]]
    var currentProject = project

    // ADD FUNCTIONS
    mutations ++= subscriptionFunctionsToAdd.map { x =>
      val mutation = AddServerSideSubscriptionFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    mutations ++= schemaExtensionFunctionsToAdd.map { x =>
      val mutation = AddSchemaExtensionFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    mutations ++= operationFunctionsToAdd.map { x =>
      val mutation = AddRequestPipelineMutationFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    // ADD PERMISSIONS
    mutations ++= modelPermissionsToAdd.map { x =>
      val model    = project.getModelById_!(x.input.modelId)
      val mutation = AddModelPermissionMutation(client, currentProject, model, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    mutations ++= relationPermissionsToAdd.map { x =>
      val relation = project.getRelationById_!(x.input.relationId)
      val mutation = AddRelationPermissionMutation(client, currentProject, relation, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    // ADD ROOTTOKENS
    mutations ++= rootTokensToCreate.map { x =>
      val mutation = CreateRootTokenMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    (mutations, currentProject)
  }
}

case class UpdateModuleActions(subscriptionFunctionsToUpdate: Vector[UpdateServerSideSubscriptionFunctionAction],
                               schemaExtensionFunctionsToUpdate: Vector[UpdateSchemaExtensionFunctionAction],
                               operationFunctionsToUpdate: Vector[UpdateOperationFunctionAction])
    extends ModuleActions {
  def verbalDescriptions: Vector[VerbalDescription] = {
    subscriptionFunctionsToUpdate.map(_.verbalDescription) ++
      schemaExtensionFunctionsToUpdate.map(_.verbalDescription) ++
      operationFunctionsToUpdate.map(_.verbalDescription)
  }

  def determineMutations(client: Client, project: Project, projectDbsFn: Project => InternalAndProjectDbs)(
      implicit inj: Injector,
      actorSystem: ActorSystem,
      actorMaterializer: ActorMaterializer): (Seq[InternalMutation[_]], Project) = {
    val mutations      = mutable.Buffer.empty[InternalMutation[_]]
    var currentProject = project

    // ADD FUNCTIONS
    mutations ++= subscriptionFunctionsToUpdate.map { x =>
      val mutation = UpdateServerSideSubscriptionFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    mutations ++= schemaExtensionFunctionsToUpdate.map { x =>
      val mutation = UpdateSchemaExtensionFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }

    mutations ++= operationFunctionsToUpdate.map { x =>
      val mutation = UpdateRequestPipelineMutationFunctionMutation(client, currentProject, x.input, projectDbsFn)
      currentProject = mutation.updatedProject
      mutation
    }
    (mutations, currentProject)
  }
}

case class AddServerSideSubscriptionFunctionAction(input: AddServerSideSubscriptionFunctionInput) {
  def verbalDescription = VerbalDescription(
    `type` = "subscription function",
    action = "Create",
    name = input.name,
    description = s"A new subscription with the name `${input.name}` is created."
  )
}

case class AddOperationFunctionAction(input: AddRequestPipelineMutationFunctionInput) {
  def verbalDescription = VerbalDescription(
    `type` = "operation function",
    action = "Create",
    name = input.name,
    description = s"A new operation function with the name `${input.name}` is created."
  )
}

case class AddSchemaExtensionFunctionAction(input: AddSchemaExtensionFunctionInput) {
  def verbalDescription = VerbalDescription(
    `type` = "resolver function",
    action = "Create",
    name = input.name,
    description = s"A new resolver function with the name `${input.name}` is created."
  )
}

case class UpdateServerSideSubscriptionFunctionAction(input: UpdateServerSideSubscriptionFunctionInput) {
  private val functionName = input.name.get

  def verbalDescription = VerbalDescription(
    `type` = "subscription function",
    action = "Update",
    name = functionName,
    description = s"A subscription with the name `$functionName` is updated."
  )
}

case class UpdateOperationFunctionAction(input: UpdateRequestPipelineMutationFunctionInput) {
  private val functionName = input.name.get
  def verbalDescription = VerbalDescription(
    `type` = "operation function",
    action = "Update",
    name = functionName,
    description = s"An operation function with the name `$functionName` is updated."
  )
}

case class UpdateSchemaExtensionFunctionAction(input: UpdateSchemaExtensionFunctionInput) {
  private val functionName = input.name.get
  def verbalDescription = VerbalDescription(
    `type` = "resolver function",
    action = "Update",
    name = functionName,
    description = s"A resolver function with the name `$functionName` is updated."
  )
}

case class RemoveSubscriptionFunctionAction(input: DeleteFunctionInput, name: String) {
  def verbalDescription = VerbalDescription(
    `type` = "subscription function",
    action = "Delete",
    name = name,
    description = s"A subscription with the name `$name` is deleted."
  )
}

case class RemoveOperationFunctionAction(input: DeleteFunctionInput, name: String) {
  def verbalDescription = VerbalDescription(
    `type` = "operation function",
    action = "Delete",
    name = name,
    description = s"An operation function with the name `$name` is deleted."
  )
}

case class RemoveSchemaExtensionFunctionAction(input: DeleteFunctionInput, name: String) {
  def verbalDescription = VerbalDescription(
    `type` = "resolver function",
    action = "Delete",
    name = name,
    description = s"A resolver function with the name `$name` is deleted."
  )
}

case class AddModelPermissionAction(input: AddModelPermissionInput, modelPermissionName: String) {
  def verbalDescription = VerbalDescription(
    `type` = "model permission",
    action = "Create",
    name = modelPermissionName,
    description = s"A permission for the operation `${input.operation}` is created."
  )
}

case class AddRelationPermissionAction(input: AddRelationPermissionInput, relationPermissionName: String, operation: String) {
  def verbalDescription = VerbalDescription(
    `type` = "model permission",
    action = "Create",
    name = relationPermissionName,
    description = s"A permission for the operation `$operation` is created."
  )
}

case class RemoveModelPermissionAction(input: DeleteModelPermissionInput, modelPermissionName: String, operation: String) {
  def verbalDescription = VerbalDescription(
    `type` = "model permission",
    action = "Delete",
    name = modelPermissionName,
    description = s"A permission for the operation `$operation` is deleted."
  )
}

case class RemoveRelationPermissionAction(input: DeleteRelationPermissionInput, relationPermissionName: String, operation: String) {
  def verbalDescription = VerbalDescription(
    `type` = "model permission",
    action = "Delete",
    name = relationPermissionName,
    description = s"A permission for the operation `$operation` is deleted."
  )
}

case class RemoveRootTokenAction(input: DeleteRootTokenInput, rootTokenName: String) {
  def verbalDescription = VerbalDescription(
    `type` = "rootToken",
    action = "Delete",
    name = rootTokenName,
    description = s"A rootToken with the name `$rootTokenName` is deleted."
  )
}

case class CreateRootTokenAction(input: CreateRootTokenInput, rootTokenName: String) {
  def verbalDescription = VerbalDescription(
    `type` = "rootToken",
    action = "Create",
    name = rootTokenName,
    description = s"A rootToken with the name `$rootTokenName` is created."
  )
}
