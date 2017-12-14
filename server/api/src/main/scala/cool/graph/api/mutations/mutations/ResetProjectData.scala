package cool.graph.api.mutations.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.mutactions.MutactionGroup
import cool.graph.api.database.mutactions.mutactions.{DeleteAllDataItems, DeleteAllRelations, DeleteAllRelayIds}
import cool.graph.api.mutations.{ClientMutation, ReallyNoReturnValue, ReturnValueResult}
import cool.graph.shared.models._

import scala.concurrent.Future

case class ResetProjectData(project: Project, dataResolver: DataResolver)(implicit apiDependencies: ApiDependencies)
  extends ClientMutation {

  override def prepareMutactions(): Future[List[MutactionGroup]] = {

    val removeRelations = MutactionGroup(project.relations.map(relation => DeleteAllRelations(projectId = project.id, relation = relation)), true)
    val removeDataItems = MutactionGroup(project.models.map(model => DeleteAllDataItems(projectId = project.id, model = model)), true)
    val removeRelayIds = MutactionGroup(List(DeleteAllRelayIds(projectId = project.id)),true)

    Future.successful(List(removeRelations, removeDataItems, removeRelayIds))
  }

  override def getReturnValue: Future[ReturnValueResult] = Future.successful(ReallyNoReturnValue()) // is this the correct return value??
}