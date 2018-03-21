package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Project, Relation}

import scala.collection.immutable
import scala.concurrent.Future

trait DataResolver {
  def project: Project

  def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResult]

  def countByModel(model: Model, where: DataItemFilterCollection): Future[Int]

  def countByModel(model: Model, where: Option[DataItemFilterCollection] = None): Future[Int]

  def existsByModelAndId(model: Model, id: String): Future[Boolean]

  def existsByWhere(where: NodeSelector): Future[Boolean]

  def existsByModel(model: Model): Future[Boolean]

  def resolveByUnique(where: NodeSelector): Future[Option[DataItem]]

  def resolveByUniques(model: Model, uniques: Vector[NodeSelector]): Future[Vector[DataItem]]

  def resolveByUniqueWithoutValidation(model: Model, key: String, value: Any): Future[Option[DataItem]]

  def loadModelRowsForExport(model: Model, args: Option[QueryArguments] = None): Future[ResolverResultNew]

  def loadListRowsForExport(tableName: String, args: Option[QueryArguments] = None): Future[ResolverResult]

  def loadRelationRowsForExport(relationId: String, args: Option[QueryArguments] = None): Future[ResolverResult]

  def batchResolveByUnique(model: Model, key: String, values: List[Any]): Future[List[DataItem]]

  def batchResolveScalarList(model: Model, field: Field, nodeIds: Vector[String]): Future[Vector[ScalarListValue]]

  def batchResolveByUniqueWithoutValidation(model: Model, key: String, values: List[Any]): Future[List[DataItem]]

  def resolveByGlobalId(globalId: String): Future[Option[DataItem]]

  def resolveRelation(relationId: String, aId: String, bId: String): Future[ResolverResult]

  def resolveByRelation(fromField: Field, fromModelId: String, args: Option[QueryArguments]): Future[ResolverResult]

  def resolveByRelationManyModels(fromField: Field, fromModelIds: List[String], args: Option[QueryArguments]): Future[immutable.Seq[ResolverResult]]

  def resolveByModelAndId(model: Model, id: Id): Future[Option[DataItem]]

  def resolveByModelAndIdWithoutValidation(model: Model, id: Id): Future[Option[DataItem]]

  def countByRelationManyModels(fromField: Field, fromNodeIds: List[String], args: Option[QueryArguments]): Future[List[(String, Int)]]

  def itemCountForModel(model: Model): Future[Int]

  def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean]

  def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean]

  def itemCountForRelation(relation: Relation): Future[Int]

  def itemCountsForAllModels(project: Project): Future[ModelCounts]
}
