package com.prisma.api.connector

import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models.{Model, Project, ServerSideSubscriptionFunction}
import sangria.relay.Node

sealed trait ApiMutaction
sealed trait DatabaseMutaction   extends ApiMutaction // by default transactionally?
sealed trait SideEffectMutaction extends ApiMutaction

case class AddDataItemToManyRelationByPath(project: Project, path: Path) extends DatabaseMutaction

case class PublishSubscriptionEvent(project: Project, value: Map[String, Any], mutationName: String) extends SideEffectMutaction
case class ServerSideSubscription(
    project: Project,
    model: Model,
    mutationType: ModelMutationType,
    function: ServerSideSubscriptionFunction,
    nodeId: Id,
    requestId: String,
    updatedFields: Option[List[String]] = None,
    previousValues: Option[DataItem] = None
) extends SideEffectMutaction
