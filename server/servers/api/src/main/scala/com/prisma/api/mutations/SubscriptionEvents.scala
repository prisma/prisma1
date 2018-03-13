package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.PublishSubscriptionEvent
import com.prisma.api.database.mutactions.ClientSqlMutaction
import com.prisma.api.database.mutactions.mutactions.{CreateDataItem, DeleteDataItem, UpdateDataItem}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.Project

import scala.collection.immutable.Seq

//todo this does not handle upsert
object SubscriptionEvents {
  def extractFromSqlMutactions(
      project: Project,
      mutationId: Id,
      mutactions: Seq[ClientSqlMutaction]
  )(implicit apiDependencies: ApiDependencies): Seq[PublishSubscriptionEvent] = {
    mutactions.collect {
      case x: UpdateDataItem => fromUpdateMutaction(project, mutationId, x)
      case x: CreateDataItem => fromCreateMutaction(project, mutationId, x)
      case x: DeleteDataItem => fromDeleteMutaction(project, mutationId, x)
    }
  }

  def fromDeleteMutaction(project: Project, mutationId: Id, mutaction: DeleteDataItem)(implicit apiDependencies: ApiDependencies): PublishSubscriptionEvent = {
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
      value = Map("nodeId" -> mutaction.id, "node" -> nodeData, "modelId" -> mutaction.path.root.model.id, "mutationType" -> "DeleteNode"),
      mutationName = s"delete${mutaction.path.root.model.name}"
    )
  }

  def fromCreateMutaction(project: Project, mutationId: Id, mutaction: CreateDataItem)(implicit apiDependencies: ApiDependencies): PublishSubscriptionEvent = {
    PublishSubscriptionEvent(
      project = project,
      value = Map("nodeId" -> mutaction.id, "modelId" -> mutaction.model.id, "mutationType" -> "CreateNode"),
      mutationName = s"create${mutaction.model.name}"
    )
  }

  def fromUpdateMutaction(project: Project, mutationId: Id, mutaction: UpdateDataItem)(implicit apiDependencies: ApiDependencies): PublishSubscriptionEvent = {
    PublishSubscriptionEvent(
      project = project,
      value = Map(
        "nodeId"        -> mutaction.id,
        "changedFields" -> mutaction.namesOfUpdatedFields.toList, // must be a List as Vector is printed verbatim
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
