package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.FilterConditionBuilder
import com.prisma.api.connector.mongo.extensions.DocumentToRoot
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer.whereToBson
import com.prisma.gc_values._
import com.prisma.shared.models._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoClient, MongoCollection}

import scala.concurrent.{ExecutionContext, Future}

case class MongoDataResolver(project: Project, client: MongoClient)(implicit ec: ExecutionContext) extends DataResolver with FilterConditionBuilder {
  val database = client.getDatabase(project.id)

  override def getModelForGlobalId(globalId: CuidGCValue): Future[Option[Model]] = {
    val outer = project.models.map { model =>
      val collection: MongoCollection[Document] = database.getCollection(model.name)
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

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = {
    database.getCollection(table).countDocuments(buildConditionForFilter(whereFilter)).toFuture.map(_.toInt)
  }

  // Fixme this does not use selected fields
  override def getNodes(model: Model, args: Option[QueryArguments], selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(model.dbName)
    val filter = args match {
      case Some(arg) => arg.filter
      case None      => None
    }

    val mongoFilter = buildConditionForFilter(filter)

    val nodes: Future[Seq[PrismaNode]] = collection.find(mongoFilter).collect().toFuture.map { results: Seq[Document] =>
      results.map { result =>
        val root = DocumentToRoot(model, result)
        PrismaNode(root.idField, root, Some(model.name))
      }
    }
    nodes.map(n => ResolverResult[PrismaNode](n.toVector, false, false, None))
  }

  //these are only used for relations between non-embedded types
  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               args: Option[QueryArguments],
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = ???

  override def getRelationNodes(relationTableName: String, args: Option[QueryArguments]): Future[ResolverResult[RelationNode]] = ???

  // these should never be used and are only in here due to the interface
  override def getScalarListValues(model: Model, listField: ScalarField, args: Option[QueryArguments]): Future[ResolverResult[ScalarListValues]] = ???
  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]]  = ???
}
