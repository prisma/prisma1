package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.{Field, Model, Project}

import scala.concurrent.Future

trait DataResolver {
  def project: Project

  def resolveByGlobalId(globalId: String): Future[Option[PrismaNode]]

  def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResultNew[PrismaNode]]

  def resolveByUnique(where: NodeSelector): Future[Option[PrismaNode]]

  def countByModel(model: Model, where: Option[DataItemFilterCollection] = None): Future[Int]

  def batchResolveByUnique(model: Model, key: String, values: Vector[GCValue]): Future[Vector[PrismaNode]]

  def batchResolveScalarList(model: Model, field: Field, nodeIds: Vector[String]): Future[Vector[ScalarListValues]]

  def resolveByRelationManyModels(fromField: Field,
                                  fromModelIds: Vector[String],
                                  args: Option[QueryArguments]): Future[Vector[ResolverResultNew[PrismaNodeWithParent]]]

  def countByRelationManyModels(fromField: Field, fromNodeIds: Vector[String], args: Option[QueryArguments]): Future[Vector[(String, Int)]]

  def loadListRowsForExport(model: Model, field: Field, args: Option[QueryArguments] = None): Future[ResolverResultNew[ScalarListValues]]

  def loadRelationRowsForExport(relationId: String, args: Option[QueryArguments] = None): Future[ResolverResultNew[RelationNode]]

  //  def loadModelRowsForExport(model: Model, args: Option[QueryArguments] = None): Future[ResolverResultNew[PrismaNode]]

//  def existsByModelAndId(model: Model, id: String): Future[Boolean]
//
//  def existsByWhere(where: NodeSelector): Future[Boolean]
//
//  def existsByModel(model: Model): Future[Boolean]

//  def resolveByUniques(model: Model, uniques: Vector[NodeSelector]): Future[Vector[DataItem]]

//  def resolveByUniqueWithoutValidation(model: Model, key: String, value: Any): Future[Option[DataItem]]

//  def batchResolveByUniqueWithoutValidation(model: Model, key: String, values: List[Any]): Future[List[DataItem]]

//  def resolveRelation(relationId: String, aId: String, bId: String): Future[ResolverResult]

//  def resolveByRelation(fromField: Field, fromModelId: String, args: Option[QueryArguments]): Future[ResolverResult]

//  def resolveByModelAndId(model: Model, id: Id): Future[Option[DataItem]]
//
//  def resolveByModelAndIdWithoutValidation(model: Model, id: Id): Future[Option[DataItem]]

//  def itemCountForModel(model: Model): Future[Int]

//  def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean]
//
//  def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean]

//  def itemCountForRelation(relation: Relation): Future[Int]
//
//  def itemCountsForAllModels(project: Project): Future[ModelCounts]
}
