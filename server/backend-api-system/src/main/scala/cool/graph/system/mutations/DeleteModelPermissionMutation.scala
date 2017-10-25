package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteModelPermission, DeleteModelPermissionField, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteModelPermissionMutation(client: Client,
                                         project: Project,
                                         model: Model,
                                         modelPermission: ModelPermission,
                                         args: DeleteModelPermissionInput,
                                         projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteModelPermissionMutationPayload] {

  val newModel: Model = model.copy(permissions = model.permissions.filter(_.id != modelPermission.id))
  val updatedProject: Project = project.copy(models = project.models.map {
    case x if x.id == newModel.id => newModel
    case x                        => x
  })

  override def prepareActions(): List[Mutaction] = {

    actions ++= modelPermission.fieldIds.map(fieldId =>
      DeleteModelPermissionField(project = project, model = model, permission = modelPermission, fieldId = fieldId))

    actions :+= DeleteModelPermission(project, model = model, permission = modelPermission)

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[DeleteModelPermissionMutationPayload] = {

    Some(
      DeleteModelPermissionMutationPayload(
        clientMutationId = args.clientMutationId,
        model = newModel,
        modelPermission = modelPermission,
        project = updatedProject
      ))
  }
}

case class DeleteModelPermissionMutationPayload(clientMutationId: Option[String], model: models.Model, modelPermission: ModelPermission, project: Project)
    extends Mutation

case class DeleteModelPermissionInput(clientMutationId: Option[String], modelPermissionId: String)
