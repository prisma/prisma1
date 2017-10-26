package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{Function, Project}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteFunction, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteFunctionMutation(client: models.Client,
                                  project: models.Project,
                                  args: DeleteFunctionInput,
                                  projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteFunctionMutationPayload] {

  val function: Function = project.getFunctionById_!(args.functionId)

  val updatedProject: Project = project.copy(functions = project.functions.filter(_.id != args.functionId))

  override def prepareActions(): List[Mutaction] = {
    this.actions = List(DeleteFunction(project, function), BumpProjectRevision(project = project), InvalidateSchema(project))
    this.actions
  }

  override def getReturnValue: Option[DeleteFunctionMutationPayload] =
    Some(DeleteFunctionMutationPayload(args.clientMutationId, project, function))
}

case class DeleteFunctionMutationPayload(clientMutationId: Option[String], project: models.Project, function: models.Function) extends Mutation

case class DeleteFunctionInput(clientMutationId: Option[String], functionId: String)
