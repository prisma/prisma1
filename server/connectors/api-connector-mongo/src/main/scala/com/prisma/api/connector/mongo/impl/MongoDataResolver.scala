package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{FilterConditionBuilder, MongoActionsBuilder}
import com.prisma.api.connector.mongo.extensions.{DocumentToRoot, SlickReplacement}
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer.whereToBson
import com.prisma.gc_values._
import com.prisma.shared.models._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoClient, MongoCollection}

import scala.concurrent.{ExecutionContext, Future}

case class MongoDataResolver(project: Project, client: MongoClient)(implicit ec: ExecutionContext) extends DataResolver with FilterConditionBuilder {

  val queryBuilder = MongoActionsBuilder(project.id, client)

  val database = client.getDatabase(project.id)

  override def getModelForGlobalId(globalId: CuidGCValue): Future[Option[Model]] = {
    val outer = project.models.map { model =>
      val collection: MongoCollection[Document] = database.getCollection(model.dbName)
      collection.find(Filters.eq("_id", globalId.value)).collect().toFuture.map { results: Seq[Document] =>
        if (results.nonEmpty) Vector(model) else Vector.empty
      }
    }

    val sequence: Future[List[Vector[Model]]] = Future.sequence(outer)

    sequence.map(_.flatten.headOption)
  }

  //Fixme this does not use selected fields
  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(where.model.dbName)
    collection.find(where).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(where.model, result)
        PrismaNode(root.idField, root, Some(where.model.name))
      }
    }
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

  override def getRelationNodes(relationTableName: String, queryArguments: Option[QueryArguments]): Future[ResolverResult[RelationNode]] = ???

  //Fixme this needs to use all the queryarguments
  override def countByModel(model: Model, queryArguments: Option[QueryArguments]): Future[Int] = {
    database.getCollection(model.dbName).countDocuments(buildConditionForFilter(queryArguments.flatMap(_.filter))).toFuture.map(_.toInt)
  }

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = {
    database.getCollection(table).countDocuments(buildConditionForFilter(whereFilter)).toFuture.map(_.toInt)
  }

  // these should never be used and are only in here due to the interface
  override def getScalarListValues(model: Model, listField: ScalarField, queryArguments: Option[QueryArguments]): Future[ResolverResult[ScalarListValues]] = ???
  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]]            = ???
}
