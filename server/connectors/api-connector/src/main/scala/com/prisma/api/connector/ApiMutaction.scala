package com.prisma.api.connector

import com.prisma.gc_values.ListGCValue
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models._

sealed trait ApiMutaction

// DATABASE MUTACTIONS
sealed trait DatabaseMutaction extends ApiMutaction {
  def project: Project
  def allMutactions: Vector[DatabaseMutaction]
}

sealed trait TopLevelDatabaseMutaction extends DatabaseMutaction

sealed trait FurtherNestedMutaction extends DatabaseMutaction {
  def nestedCreates: Vector[NestedCreateDataItem]         = Vector.empty
  def nestedUpdates: Vector[NestedUpdateDataItem]         = Vector.empty
  def nestedUpserts: Vector[NestedUpsertDataItem]         = Vector.empty
  def nestedDeletes: Vector[NestedDeleteDataItem]         = Vector.empty
  def nestedConnects: Vector[NestedConnectRelation]       = Vector.empty
  def nestedDisconnects: Vector[NestedDisconnectRelation] = Vector.empty

  def allMutactions: Vector[DatabaseMutaction] = nestedCreates ++ nestedUpdates ++ nestedUpserts ++ nestedDeletes ++ nestedConnects ++ nestedDisconnects
}

sealed trait FinalMutaction extends DatabaseMutaction {
  override def allMutactions = Vector.empty
}

// TOP LEVEL - SINGLE
case class CreateDataItem(
    project: Project,
    model: Model,
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)],
    override val nestedCreates: Vector[NestedCreateDataItem],
    override val nestedConnects: Vector[NestedConnectRelation]
) extends FurtherNestedMutaction
    with TopLevelDatabaseMutaction

case class UpdateDataItem(
    project: Project,
    where: NodeSelector,
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)],
    previousValues: PrismaNode,
    override val nestedCreates: Vector[NestedCreateDataItem],
    override val nestedUpdates: Vector[NestedUpdateDataItem],
    override val nestedUpserts: Vector[NestedUpsertDataItem],
    override val nestedDeletes: Vector[NestedDeleteDataItem],
    override val nestedConnects: Vector[NestedConnectRelation],
    override val nestedDisconnects: Vector[NestedDisconnectRelation]
) extends FurtherNestedMutaction
    with TopLevelDatabaseMutaction

case class UpsertDataItem(
    project: Project,
    where: NodeSelector,
    create: CreateDataItem,
    update: UpdateDataItem
) extends FurtherNestedMutaction
    with TopLevelDatabaseMutaction

case class DeleteDataItem(project: Project, where: NodeSelector, previousValues: PrismaNode)
    extends DatabaseMutaction
    with TopLevelDatabaseMutaction
    with FinalMutaction

// TOP LEVEL - MANY
case class DeleteDataItems(project: Project, model: Model, whereFilter: Option[Filter])
    extends DatabaseMutaction
    with TopLevelDatabaseMutaction
    with FinalMutaction
case class ResetDataMutaction(project: Project, tableNames: Vector[String]) extends DatabaseMutaction with TopLevelDatabaseMutaction with FinalMutaction
case class UpdateDataItems(project: Project, model: Model, whereFilter: Option[Filter], updateArgs: PrismaArgs, listArgs: Vector[(String, ListGCValue)])
    extends DatabaseMutaction
    with TopLevelDatabaseMutaction
    with FinalMutaction

// NESTED
sealed trait NestedDatabaseMutaction extends DatabaseMutaction {
  def relationField: RelationField
}

case class NestedCreateDataItem(
    project: Project,
    relationField: RelationField,
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)],
    override val nestedCreates: Vector[NestedCreateDataItem],
    override val nestedConnects: Vector[NestedConnectRelation],
    topIsCreate: Boolean
) extends NestedDatabaseMutaction
    with FurtherNestedMutaction

case class NestedUpdateDataItem(
    project: Project,
    relationField: RelationField,
    where: Option[NodeSelector],
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)],
    override val nestedCreates: Vector[NestedCreateDataItem],
    override val nestedUpdates: Vector[NestedUpdateDataItem],
    override val nestedUpserts: Vector[NestedUpsertDataItem],
    override val nestedDeletes: Vector[NestedDeleteDataItem],
    override val nestedConnects: Vector[NestedConnectRelation],
    override val nestedDisconnects: Vector[NestedDisconnectRelation]
) extends NestedDatabaseMutaction
    with FurtherNestedMutaction

case class NestedUpsertDataItem(
    project: Project,
    relationField: RelationField,
    where: Option[NodeSelector],
    create: NestedCreateDataItem,
    update: NestedUpdateDataItem
) extends NestedDatabaseMutaction
    with FurtherNestedMutaction

case class NestedDeleteDataItem(project: Project, relationField: RelationField, where: Option[NodeSelector]) extends NestedDatabaseMutaction with FinalMutaction
case class NestedConnectRelation(project: Project, relationField: RelationField, where: NodeSelector, topIsCreate: Boolean)
    extends NestedDatabaseMutaction
    with FinalMutaction
case class NestedDisconnectRelation(project: Project, relationField: RelationField, where: Option[NodeSelector])
    extends NestedDatabaseMutaction
    with FinalMutaction

// IMPORT
case class PushScalarListsImport(project: Project, tableName: String, id: String, args: ListGCValue)      extends DatabaseMutaction with FinalMutaction
case class CreateRelationRowsImport(project: Project, relation: Relation, args: Vector[(String, String)]) extends DatabaseMutaction with FinalMutaction
case class CreateDataItemsImport(project: Project, model: Model, args: Vector[PrismaArgs])                extends DatabaseMutaction with FinalMutaction

// OBSOLETE ??
case class AddDataItemToManyRelationByPath(project: Project, path: Path)
case class VerifyConnection(project: Project, path: Path)
case class VerifyWhere(project: Project, where: NodeSelector)
case class CascadingDeleteRelationMutactions(project: Project, path: Path)
case class DeleteManyRelationChecks(project: Project, model: Model, whereFilter: Option[Filter])
case class DeleteRelationCheck(project: Project, path: Path)

// SIDE EFFECT MUTACTIONS
sealed trait SideEffectMutaction                                                                     extends ApiMutaction
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
