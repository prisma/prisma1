package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.gc_values.{ListGCValue, RootGCValue}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models._
import com.prisma.util.gc_value.GCValueExtractor

sealed trait ApiMutaction
sealed trait DatabaseMutaction   extends ApiMutaction
sealed trait SideEffectMutaction extends ApiMutaction

case class AddDataItemToManyRelationByPath(project: Project, path: Path)   extends DatabaseMutaction
case class CascadingDeleteRelationMutactions(project: Project, path: Path) extends DatabaseMutaction
case class CreateDataItem(project: Project, path: Path, nonListArgs: ReallyCoolArgs, listArgs: Vector[(String, ListGCValue)]) extends DatabaseMutaction {
  val model = path.lastModel
  val where = path.edges match {
    case x if x.isEmpty => path.root
    case x              => x.last.asInstanceOf[NodeEdge].childWhere
  }
  val id = where.fieldValueAsString
}

case class PushScalarListsImport(project: Project, tableName: String, id: String, args: ListGCValue)      extends DatabaseMutaction
case class CreateRelationRowsImport(project: Project, relation: Relation, args: Vector[(String, String)]) extends DatabaseMutaction
case class CreateDataItemsImport(project: Project, model: Model, args: Vector[ReallyCoolArgs])            extends DatabaseMutaction
case class DeleteDataItem(project: Project, path: Path, previousValues: PrismaNode) extends DatabaseMutaction {
  val id = GCValueExtractor.fromGCValueToString(previousValues.data.map("id"))
}
case class DeleteDataItemNested(project: Project, path: Path)                                              extends DatabaseMutaction
case class DeleteDataItems(project: Project, model: Model, whereFilter: DataItemFilterCollection)          extends DatabaseMutaction
case class DeleteManyRelationChecks(project: Project, model: Model, whereFilter: DataItemFilterCollection) extends DatabaseMutaction
case class DeleteRelationCheck(project: Project, path: Path)                                               extends DatabaseMutaction
object DisableForeignKeyConstraintChecks                                                                   extends DatabaseMutaction //combine
object EnableForeignKeyConstraintChecks                                                                    extends DatabaseMutaction //combine
case class TruncateTable(projectId: String, tableName: String)                                             extends DatabaseMutaction //combine
case class NestedConnectRelation(project: Project, path: Path, topIsCreate: Boolean)                       extends DatabaseMutaction
case class NestedCreateRelation(project: Project, path: Path, topIsCreate: Boolean)                        extends DatabaseMutaction
case class NestedDisconnectRelation(project: Project, path: Path, topIsCreate: Boolean = false)            extends DatabaseMutaction
case class SetScalarList(project: Project, path: Path, field: Field, listGCValue: ListGCValue)             extends DatabaseMutaction
case class SetScalarListToEmpty(project: Project, path: Path, field: Field)                                extends DatabaseMutaction
case class UpdateDataItem(project: Project, path: Path, nonListArgs: ReallyCoolArgs, listArgs: Vector[(String, ListGCValue)], previousValues: PrismaNode)
    extends DatabaseMutaction {
  // TODO filter for fields which actually did change
  val namesOfUpdatedFields: Vector[String] = nonListArgs.raw.asRoot.map.keys.toVector
}
case class NestedUpdateDataItem(project: Project, path: Path, args: ReallyCoolArgs, listArgs: Vector[(String, ListGCValue)]) extends DatabaseMutaction
case class UpdateDataItems(project: Project, model: Model, updateArgs: CoolArgs, where: DataItemFilterCollection)            extends DatabaseMutaction //todo
case class UpsertDataItem(project: Project,
                          createPath: Path,
                          updatePath: Path,
                          nonListCreateArgs: ReallyCoolArgs,
                          listCreateArgs: Vector[(String, ListGCValue)],
                          nonListUpdateArgs: ReallyCoolArgs,
                          listUpdateArgs: Vector[(String, ListGCValue)])
    extends DatabaseMutaction //todo
case class UpsertDataItemIfInRelationWith(
    project: Project,
    createPath: Path,
    updatePath: Path,
    createListArgs: Vector[(String, ListGCValue)],
    createNonListArgs: ReallyCoolArgs,
    updateListArgs: Vector[(String, ListGCValue)],
    updateNonListArgs: ReallyCoolArgs
) extends DatabaseMutaction //todo
case class VerifyConnection(project: Project, path: Path)     extends DatabaseMutaction
case class VerifyWhere(project: Project, where: NodeSelector) extends DatabaseMutaction

case class PublishSubscriptionEvent(project: Project, value: Map[String, Any], mutationName: String) extends SideEffectMutaction
case class ServerSideSubscription(
    project: Project,
    model: Model,
    mutationType: ModelMutationType,
    function: ServerSideSubscriptionFunction,
    nodeId: Id,
    requestId: String,
    updatedFields: Option[List[String]] = None,
    previousValues: Option[PrismaNode] = None
) extends SideEffectMutaction //todo
