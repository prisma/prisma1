package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteRootToken, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteRootTokenMutation(client: Client,
                                   project: Project,
                                   rootToken: RootToken,
                                   args: DeleteRootTokenInput,
                                   projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteRootTokenMutationPayload] {

  val updatedProject: Project = project.copy(rootTokens = project.rootTokens.filter(_.id != args.rootTokenId))

  override def prepareActions(): List[Mutaction] = {

    actions = List(
      DeleteRootToken(rootToken = rootToken),
      BumpProjectRevision(project = project),
      InvalidateSchema(project = project)
    )
    actions
  }

  override def getReturnValue: Option[DeleteRootTokenMutationPayload] = {
    Some(
      DeleteRootTokenMutationPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        rootToken = rootToken
      ))
  }
}

case class DeleteRootTokenMutationPayload(clientMutationId: Option[String], project: models.Project, rootToken: models.RootToken) extends Mutation

case class DeleteRootTokenInput(clientMutationId: Option[String], rootTokenId: String)
