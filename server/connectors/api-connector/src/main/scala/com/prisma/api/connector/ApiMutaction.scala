package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.mutations.CoolArgs
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models.{Model, Project, ServerSideSubscriptionFunction}

sealed trait ApiMutaction
sealed trait DatabaseMutaction   extends ApiMutaction // by default transactionally?
sealed trait SideEffectMutaction extends ApiMutaction

case class AddDataItemToManyRelationByPath(project: Project, path: Path)                              extends DatabaseMutaction
case class CascadingDeleteRelationMutactions(project: Project, path: Path)                            extends DatabaseMutaction
case class CreateDataItem(project: Project, path: Path, args: CoolArgs)                               extends DatabaseMutaction
case class DeleteDataItem(project: Project, path: Path, previousValues: DataItem, id: String)         extends DatabaseMutaction
case class DeleteDataItemNested(project: Project, path: Path)                                         extends DatabaseMutaction
case class DeleteDataItems(project: Project, model: Model, whereFilter: DataItemFilterCollection)     extends DatabaseMutaction
case class DeleteManyRelationChecks(project: Project, model: Model, filter: DataItemFilterCollection) extends DatabaseMutaction
case class DeleteRelationCheck(project: Project, path: Path)                                          extends DatabaseMutaction
object DisableForeignKeyConstraintChecks                                                              extends DatabaseMutaction
object EnableForeignKeyConstraintChecks                                                               extends DatabaseMutaction
case class NestedConnectRelation(project: Project, path: Path, topIsCreate: Boolean)                  extends DatabaseMutaction
case class NestedCreateRelation(project: Project, path: Path, topIsCreate: Boolean)                   extends DatabaseMutaction
case class NestedDisconnectRelation(project: Project, path: Path, topIsCreate: Boolean = false)       extends DatabaseMutaction

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
