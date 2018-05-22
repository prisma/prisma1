package com.prisma.api.connector

import com.prisma.gc_values.ListGCValue
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models._

sealed trait ApiMutaction
sealed trait DatabaseMutaction extends ApiMutaction {
  def project: Project
}
sealed trait SideEffectMutaction extends ApiMutaction

case class AddDataItemToManyRelationByPath(project: Project, path: Path)   extends DatabaseMutaction
case class CascadingDeleteRelationMutactions(project: Project, path: Path) extends DatabaseMutaction
case class CreateDataItem(project: Project, path: Path, nonListArgs: PrismaArgs, listArgs: Vector[(String, ListGCValue)]) extends DatabaseMutaction {
  val model = path.lastModel
  val where = path.edges match {
    case x if x.isEmpty => path.root
    case x              => x.last.asInstanceOf[NodeEdge].childWhere
  }
  val id = where.fieldValueAsString
}

case class PushScalarListsImport(project: Project, tableName: String, id: String, args: ListGCValue)      extends DatabaseMutaction
case class CreateRelationRowsImport(project: Project, relation: Relation, args: Vector[(String, String)]) extends DatabaseMutaction
case class CreateDataItemsImport(project: Project, model: Model, args: Vector[PrismaArgs])                extends DatabaseMutaction
case class DeleteDataItem(project: Project, path: Path, previousValues: PrismaNode) extends DatabaseMutaction {
  val id = previousValues.data.idField.value
}
case class DeleteDataItemNested(project: Project, path: Path)                                    extends DatabaseMutaction
case class DeleteDataItems(project: Project, model: Model, whereFilter: Option[Filter])          extends DatabaseMutaction
case class DeleteManyRelationChecks(project: Project, model: Model, whereFilter: Option[Filter]) extends DatabaseMutaction
case class DeleteRelationCheck(project: Project, path: Path)                                     extends DatabaseMutaction
case class ResetDataMutaction(project: Project, tableNames: Vector[String])                      extends DatabaseMutaction
case class NestedConnectRelation(project: Project, path: Path, topIsCreate: Boolean)             extends DatabaseMutaction
case class NestedCreateRelation(project: Project, path: Path, topIsCreate: Boolean)              extends DatabaseMutaction
case class NestedDisconnectRelation(project: Project, path: Path, topIsCreate: Boolean = false)  extends DatabaseMutaction
case class UpdateDataItem(project: Project, path: Path, nonListArgs: PrismaArgs, listArgs: Vector[(String, ListGCValue)], previousValues: PrismaNode)
    extends DatabaseMutaction
    with UpdateWrapper {
  val namesOfUpdatedFields: Vector[String] = nonListArgs.keys // TODO filter for fields which actually did change
}

sealed trait UpdateWrapper
case class NestedUpdateDataItem(project: Project, path: Path, nonListArgs: PrismaArgs, listArgs: Vector[(String, ListGCValue)])
    extends DatabaseMutaction
    with UpdateWrapper
case class UpdateDataItems(project: Project, model: Model, whereFilter: Option[Filter], updateArgs: PrismaArgs, listArgs: Vector[(String, ListGCValue)])
    extends DatabaseMutaction
case class UpsertDataItem(project: Project,
                          createPath: Path,
                          updatePath: Path,
                          nonListCreateArgs: PrismaArgs,
                          listCreateArgs: Vector[(String, ListGCValue)],
                          nonListUpdateArgs: PrismaArgs,
                          listUpdateArgs: Vector[(String, ListGCValue)])
    extends DatabaseMutaction
case class UpsertDataItemIfInRelationWith(
    project: Project,
    createPath: Path,
    updatePath: Path,
    createListArgs: Vector[(String, ListGCValue)],
    createNonListArgs: PrismaArgs,
    updateListArgs: Vector[(String, ListGCValue)],
    updateNonListArgs: PrismaArgs
) extends DatabaseMutaction
case class VerifyConnection(project: Project, path: Path)     extends DatabaseMutaction
case class VerifyWhere(project: Project, where: NodeSelector) extends DatabaseMutaction

case class PublishSubscriptionEvent(project: Project, value: Map[String, Any], mutationName: String) extends SideEffectMutaction
case class ServerSideSubscription(
    project: Project,
    model: Model,
    mutationType: ModelMutationType,
    function: ServerSideSubscriptionFunction,
    nodeId: Id, //todo
    requestId: String,
    updatedFields: Option[List[String]] = None,
    previousValues: Option[PrismaNode] = None
) extends SideEffectMutaction
