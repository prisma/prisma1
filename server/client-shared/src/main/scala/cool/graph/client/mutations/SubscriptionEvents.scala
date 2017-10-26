package cool.graph.client.mutations

import cool.graph.ClientSqlMutaction
import cool.graph.Types.Id
import cool.graph.client.adapters.GraphcoolDataTypes
import cool.graph.client.mutactions._
import cool.graph.shared.models.Project
import scaldi.Injector

import scala.collection.immutable.Seq

object SubscriptionEvents {
  def extractFromSqlMutactions(project: Project, mutationId: Id, mutactions: Seq[ClientSqlMutaction])(implicit inj: Injector): Seq[PublishSubscriptionEvent] = {
    mutactions.collect {
      case x: UpdateDataItem => fromUpdateMutaction(project, mutationId, x)
      case x: CreateDataItem => fromCreateMutaction(project, mutationId, x)
      case x: DeleteDataItem => fromDeleteMutaction(project, mutationId, x)
    }
  }

  def fromDeleteMutaction(project: Project, mutationId: Id, mutaction: DeleteDataItem)(implicit inj: Injector): PublishSubscriptionEvent = {
    val nodeData: Map[String, Any] = mutaction.previousValues.userData
      .collect {
        case (key, Some(value)) =>
          (key, value match {
            case v: Vector[Any] => v.toList // Spray doesn't like Vector and formats it as string ("Vector(something)")
            case v              => v
          })
      } + ("id" -> mutaction.id)

    PublishSubscriptionEvent(
      project = project,
      value = Map("nodeId" -> mutaction.id, "node" -> nodeData, "modelId" -> mutaction.model.id, "mutationType" -> "DeleteNode"),
      mutationName = s"delete${mutaction.model.name}"
    )
  }

  def fromCreateMutaction(project: Project, mutationId: Id, mutaction: CreateDataItem)(implicit inj: Injector): PublishSubscriptionEvent = {
    PublishSubscriptionEvent(
      project = project,
      value = Map("nodeId" -> mutaction.id, "modelId" -> mutaction.model.id, "mutationType" -> "CreateNode"),
      mutationName = s"create${mutaction.model.name}"
    )
  }

  def fromUpdateMutaction(project: Project, mutationId: Id, mutaction: UpdateDataItem)(implicit inj: Injector): PublishSubscriptionEvent = {
    PublishSubscriptionEvent(
      project = project,
      value = Map(
        "nodeId"        -> mutaction.id,
        "changedFields" -> mutaction.namesOfUpdatedFields,
        "previousValues" -> GraphcoolDataTypes
          .convertToJson(mutaction.previousValues.userData)
          .compactPrint,
        "modelId"      -> mutaction.model.id,
        "mutationType" -> "UpdateNode"
      ),
      mutationName = s"update${mutaction.model.name}"
    )
  }
}
