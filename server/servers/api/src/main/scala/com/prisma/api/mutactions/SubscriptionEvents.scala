package com.prisma.api.mutactions

import com.prisma.api.connector._
import com.prisma.gc_values.{NullGCValue, RootGCValue}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.Project

object SubscriptionEvents {
  def extractFromMutactionResults(
      project: Project,
      mutationId: Id,
      mutactionResults: MutactionResults
  ): Vector[PublishSubscriptionEvent] = {
    mutactionResults.results.collect {
      case result: CreateNodeResult => fromCreateResult(project, mutationId, result)
      case result: UpdateNodeResult => fromUpdateResult(project, mutationId, result)
      case result: DeleteNodeResult => fromDeleteResult(project, mutationId, result)
    }
  }

  private def fromCreateResult(project: Project, mutationId: Id, result: CreateNodeResult): PublishSubscriptionEvent = {
    val model = result.mutaction.model
    PublishSubscriptionEvent(
      project = project,
      value = Map(
        "nodeId"       -> result.id.value,
        "modelId"      -> model.name,
        "mutationType" -> "CreateNode"
      ),
      mutationName = s"create${model.name}"
    )
  }

  private def fromUpdateResult(project: Project, mutationId: Id, result: UpdateNodeResult): PublishSubscriptionEvent = {
    val model = result.mutaction.model
    val previousValues: Map[String, Any] = result.previousValues.data
      .filterValues(v => v != NullGCValue && !v.isInstanceOf[RootGCValue])
      .toMapStringAny + (model.idField_!.name -> result.previousValues.id.value)

    PublishSubscriptionEvent(
      project = project,
      value = Map(
        "nodeId"         -> result.id.value,
        "changedFields"  -> result.namesOfUpdatedFields.toList, // must be a List as Vector is printed verbatim
        "previousValues" -> previousValues,
        "modelId"        -> model.name,
        "mutationType"   -> "UpdateNode"
      ),
      mutationName = s"update${model.name}"
    )
  }

  private def fromDeleteResult(project: Project, mutationId: Id, result: DeleteNodeResult): PublishSubscriptionEvent = {
    val model = result.mutaction.model
    val previousValues = result.previousValues.data
      .filterValues(v => v != NullGCValue && !v.isInstanceOf[RootGCValue])
      .toMapStringAny + (model.idField_!.name -> result.previousValues.id.value)

    PublishSubscriptionEvent(
      project = project,
      value = Map(
        "nodeId"       -> result.previousValues.id.value,
        "node"         -> previousValues,
        "modelId"      -> model.name,
        "mutationType" -> "DeleteNode"
      ),
      mutationName = s"delete${model.name}"
    )
  }

}
