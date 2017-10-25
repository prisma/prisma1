package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models.Project
import cool.graph.system.mutactions.internal.{BumpProjectRevision, EjectProject, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class EjectProjectMutation(
    projectDbsFn: (Project) => InternalAndProjectDbs,
    project: Project,
    args: EjectProjectInput
)(implicit val inj: Injector)
    extends InternalProjectMutation[EjectProjectMutationPayload] {

  override def prepareActions(): List[Mutaction] = {
    val mutactions = List(EjectProject(project), InvalidateSchema(project), BumpProjectRevision(project))
    actions = actions ++ mutactions
    actions
  }

  override def getReturnValue: Option[EjectProjectMutationPayload] =
    Some(
      EjectProjectMutationPayload(
        clientMutationId = args.clientMutationId,
        project = project.copy(isEjected = true)
      ))
}

case class EjectProjectMutationPayload(clientMutationId: Option[String], project: Project) extends Mutation

case class EjectProjectInput(clientMutationId: Option[String], projectId: String)
