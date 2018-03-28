package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.gc_values.{GCValue, GraphQLIdGCValue}
import com.prisma.shared.models.{Field, Model, Project}

import scala.concurrent.Future

trait DataResolver {
  def project: Project

  //todo ID's to GCValues

  def resolveByGlobalId(globalId: GraphQLIdGCValue): Future[Option[PrismaNode]]

  def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResultNew[PrismaNode]]

  def resolveByUnique(where: NodeSelector): Future[Option[PrismaNode]]

  def countByModel(model: Model, whereFilter: Option[DataItemFilterCollection] = None): Future[Int]

  def batchResolveByUnique(model: Model, fieldName: String, values: Vector[GCValue]): Future[Vector[PrismaNode]]

  def batchResolveScalarList(model: Model, listField: Field, nodeIds: Vector[GraphQLIdGCValue]): Future[Vector[ScalarListValues]]

  def resolveByRelationManyModels(fromField: Field,
                                  fromNodeIds: Vector[GraphQLIdGCValue],
                                  args: Option[QueryArguments]): Future[Vector[ResolverResultNew[PrismaNodeWithParent]]]

  def countByRelationManyModels(fromField: Field, fromNodeIds: Vector[GraphQLIdGCValue], args: Option[QueryArguments]): Future[Vector[(GraphQLIdGCValue, Int)]]

  def loadListRowsForExport(model: Model, listField: Field, args: Option[QueryArguments] = None): Future[ResolverResultNew[ScalarListValues]]

  def loadRelationRowsForExport(relationId: String, args: Option[QueryArguments] = None): Future[ResolverResultNew[RelationNode]]

}
