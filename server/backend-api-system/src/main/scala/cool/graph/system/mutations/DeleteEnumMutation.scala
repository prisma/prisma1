package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{Enum, Project}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteEnum, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteEnumMutation(
    client: models.Client,
    project: models.Project,
    args: DeleteEnumInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteEnumMutationPayload] {

  val enum: Enum              = project.getEnumById_!(args.enumId)
  val updatedProject: Project = project.copy(enums = project.enums.filter(_.id != args.enumId))

  override def prepareActions(): List[Mutaction] = {
    this.actions = List(DeleteEnum(project, enum), BumpProjectRevision(project = project), InvalidateSchema(project))
    this.actions
  }

  override def getReturnValue: Option[DeleteEnumMutationPayload] = Some(DeleteEnumMutationPayload(args.clientMutationId, updatedProject, enum))
}

case class DeleteEnumMutationPayload(clientMutationId: Option[String], project: models.Project, enum: models.Enum) extends Mutation

case class DeleteEnumInput(clientMutationId: Option[String], enumId: String)
