package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{FilterConditionBuilder, MongoActionsBuilder}
import com.prisma.api.connector.mongo.extensions.SlickReplacement
import com.prisma.gc_values._
import com.prisma.shared.models._
import org.mongodb.scala.MongoClient

import scala.concurrent.{ExecutionContext, Future}

case class MongoDataResolver(project: Project, client: MongoClient)(implicit ec: ExecutionContext) extends DataResolver with FilterConditionBuilder {

  val queryBuilder = MongoActionsBuilder(project.id, client)

  val database = client.getDatabase(project.id)

  override def getModelForGlobalId(globalId: CuidGCValue): Future[Option[Model]] = {
    val query = queryBuilder.getModelForGlobalId(project, globalId)
    SlickReplacement.run(database, query)
  }

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = {
    val query = queryBuilder.getNodeByWhere(where, selectedFields)
    SlickReplacement.run(database, query)
  }

  override def getNodes(model: Model, queryArguments: Option[QueryArguments], selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = {
    val query = queryBuilder.getNodes(model, queryArguments, selectedFields)
    SlickReplacement.run(database, query)
  }

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               queryArguments: Option[QueryArguments],
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    val query = queryBuilder.getRelatedNodes(fromField, fromNodeIds, queryArguments, selectedFields)
    SlickReplacement.run(database, query)
  }

  //Fixme this needs to use all the queryarguments
  override def countByModel(model: Model, queryArguments: Option[QueryArguments]): Future[Int] = {
    database.getCollection(model.dbName).countDocuments(buildConditionForFilter(queryArguments.flatMap(_.filter))).toFuture.map(_.toInt)
  }

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = {
    database.getCollection(table).countDocuments(buildConditionForFilter(whereFilter)).toFuture.map(_.toInt)
  }

  //Export
  override def getRelationNodes(relationTableName: String, queryArguments: Option[QueryArguments]): Future[ResolverResult[RelationNode]] = ???

  // these should never be used and are only in here due to the interface
  override def getScalarListValues(model: Model, listField: ScalarField, queryArguments: Option[QueryArguments]): Future[ResolverResult[ScalarListValues]] = ???
  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]]            = ???
}
