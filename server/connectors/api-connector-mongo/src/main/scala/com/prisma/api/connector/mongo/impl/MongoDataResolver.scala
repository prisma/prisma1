package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{FilterConditionBuilder, MongoAction, MongoActionsBuilder}
import com.prisma.api.connector.mongo.extensions.SlickReplacement
import com.prisma.gc_values._
import com.prisma.shared.models._
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

case class MongoDataResolver(project: Project, client: MongoClient)(implicit ec: ExecutionContext) extends DataResolver with FilterConditionBuilder {

  val queryBuilder = MongoActionsBuilder(project.dbName, client)

  val database: MongoDatabase = client.getDatabase(project.dbName)

  private def runQuery[T](query: MongoAction[T]) = {
    for {
      session <- client.startSession().toFuture()
      result  <- SlickReplacement.run(database, query, session)
    } yield result
  }

  override def getModelForGlobalId(globalId: StringIdGCValue): Future[Option[Model]] = {
    val query = queryBuilder.getModelForGlobalId(project, globalId)
    runQuery(query)
  }

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = {
    val query = queryBuilder.getNodeByWhere(where, selectedFields)
    runQuery(query)
  }

  override def getNodes(model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = {
    val query = queryBuilder.getNodes(model, queryArguments, selectedFields)
    runQuery(query)
  }

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               queryArguments: QueryArguments,
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    val query = queryBuilder.getRelatedNodes(fromField, fromNodeIds, queryArguments, selectedFields)
    runQuery(query)
  }

  override def countByModel(model: Model, queryArguments: QueryArguments): Future[Int] = {
    val query = queryBuilder.countFromModel(model, queryArguments)
    runQuery(query)
  }

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = {
    val query = queryBuilder.countFromTable(table, whereFilter)
    runQuery(query)
  }

  //Export
  override def getRelationNodes(relationTableName: String, queryArguments: QueryArguments): Future[ResolverResult[RelationNode]] = ???

  // these should never be used and are only in here due to the interface
  override def getScalarListValues(model: Model, listField: ScalarField, queryArguments: QueryArguments): Future[ResolverResult[ScalarListValues]] = ???
  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]]    = ???
}
