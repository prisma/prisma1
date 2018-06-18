package com.prisma.api.mutactions

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.Project

object SubscriptionEvents {
  def extractFromSqlMutactions(
      project: Project,
      mutationId: Id,
      mutactions: Vector[DatabaseMutaction]
  )(implicit apiDependencies: ApiDependencies): Vector[PublishSubscriptionEvent] = {
    mutactions.collect {
      case x: UpdateDataItem => fromUpdateMutaction(project, mutationId, x)
      case x: CreateDataItem => fromCreateMutaction(project, mutationId, x)
      case x: DeleteDataItem => fromDeleteMutaction(project, mutationId, x)
    }
  }

  def fromDeleteMutaction(project: Project, mutationId: Id, mutaction: DeleteDataItem)(implicit apiDependencies: ApiDependencies): PublishSubscriptionEvent = {
    val previousValues = mutaction.previousValues.data.filterValues(_ != NullGCValue).toMapStringAny + ("id" -> mutaction.id)

    PublishSubscriptionEvent(
      project = project,
      value = Map("nodeId" -> mutaction.id, "node" -> previousValues, "modelId" -> mutaction.path.root.model.name, "mutationType" -> "DeleteNode"),
      mutationName = s"delete${mutaction.path.root.model.name}"
    )
  }

  def fromCreateMutaction(project: Project, mutationId: Id, mutaction: CreateDataItem)(implicit apiDependencies: ApiDependencies): PublishSubscriptionEvent = {
    PublishSubscriptionEvent(
      project = project,
      value = Map("nodeId" -> mutaction.id, "modelId" -> mutaction.model.name, "mutationType" -> "CreateNode"),
      mutationName = s"create${mutaction.model.name}"
    )
  }

  def fromUpdateMutaction(project: Project, mutationId: Id, mutaction: UpdateDataItem)(implicit apiDependencies: ApiDependencies): PublishSubscriptionEvent = {
    val previousValues: Map[String, Any] = mutaction.previousValues.data
      .filterValues(_ != NullGCValue)
      .toMapStringAny + ("id" -> mutaction.previousValues.id.value)

    PublishSubscriptionEvent(
      project = project,
      value = Map(
        "nodeId"         -> previousValues("id"),
        "changedFields"  -> mutaction.namesOfUpdatedFields.toList, // must be a List as Vector is printed verbatim
        "previousValues" -> previousValues,
        "modelId"        -> mutaction.path.lastModel.name,
        "mutationType"   -> "UpdateNode"
      ),
      mutationName = s"update${mutaction.path.lastModel.name}"
    )
  }
}
