package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models._

sealed trait ApiMutaction
sealed trait DatabaseMutaction   extends ApiMutaction
sealed trait SideEffectMutaction extends ApiMutaction

case class AddDataItemToManyRelationByPath(project: Project, path: Path)   extends DatabaseMutaction
case class CascadingDeleteRelationMutactions(project: Project, path: Path) extends DatabaseMutaction
case class CreateDataItem(project: Project, path: Path, args: CoolArgs) extends DatabaseMutaction {
  val model = path.lastModel
  val where = path.edges match {
    case x if x.isEmpty => path.root
    case x              => x.last.asInstanceOf[NodeEdge].childWhere
  }
  val id = where.fieldValueAsString
}
case class CreateDataItemImport(project: Project, model: Model, args: CoolArgs)
case class CreateRelationRow(project: Project, relation: Relation, a: String, b: String)
case class PushScalarListImport(project: Project, tableName: String, id: String, values: Vector[Any])
case class PushScalarListsImport(project: Project, tableName: String, args: Vector[(String, Vector[Any])]) extends DatabaseMutaction
case class CreateRelationRowsImport(project: Project, relation: Relation, args: Vector[(String, String)])  extends DatabaseMutaction
case class CreateDataItemsImport(project: Project, model: Model, args: Vector[CoolArgs])                   extends DatabaseMutaction
case class DeleteDataItem(project: Project, path: Path, previousValues: DataItem, id: String)              extends DatabaseMutaction
case class DeleteDataItemNested(project: Project, path: Path)                                              extends DatabaseMutaction
case class DeleteDataItems(project: Project, model: Model, whereFilter: DataItemFilterCollection)          extends DatabaseMutaction
case class DeleteManyRelationChecks(project: Project, model: Model, filter: DataItemFilterCollection)      extends DatabaseMutaction
case class DeleteRelationCheck(project: Project, path: Path)                                               extends DatabaseMutaction
object DisableForeignKeyConstraintChecks                                                                   extends DatabaseMutaction
object EnableForeignKeyConstraintChecks                                                                    extends DatabaseMutaction
case class NestedConnectRelation(project: Project, path: Path, topIsCreate: Boolean)                       extends DatabaseMutaction
case class NestedCreateRelation(project: Project, path: Path, topIsCreate: Boolean)                        extends DatabaseMutaction
case class NestedDisconnectRelation(project: Project, path: Path, topIsCreate: Boolean = false)            extends DatabaseMutaction
case class SetScalarList(project: Project, path: Path, field: Field, values: Vector[Any])                  extends DatabaseMutaction
case class SetScalarListToEmpty(project: Project, path: Path, field: Field)                                extends DatabaseMutaction
case class TruncateTable(projectId: String, tableName: String)                                             extends DatabaseMutaction
case class UpdateDataItem(project: Project, model: Model, id: Id, args: CoolArgs, previousValues: DataItem) extends DatabaseMutaction {
  // TODO filter for fields which actually did change
  val namesOfUpdatedFields: Vector[String] = args.raw.keys.toVector
}
case class UpdateDataItemByUniqueFieldIfInRelationWith(project: Project, path: Path, args: CoolArgs)                              extends DatabaseMutaction
case class UpdateDataItemIfInRelationWith(project: Project, path: Path, args: CoolArgs)                                           extends DatabaseMutaction
case class UpdateDataItems(project: Project, model: Model, updateArgs: CoolArgs, where: DataItemFilterCollection)                 extends DatabaseMutaction
case class UpsertDataItem(project: Project, path: Path, createWhere: NodeSelector, updatedWhere: NodeSelector, allArgs: CoolArgs) extends DatabaseMutaction
case class UpsertDataItemIfInRelationWith(
    project: Project,
    path: Path,
    createWhere: NodeSelector,
    createArgs: CoolArgs,
    updateArgs: CoolArgs,
    pathForUpdateBranch: Path
) extends DatabaseMutaction
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
    previousValues: Option[DataItem] = None
) extends SideEffectMutaction
