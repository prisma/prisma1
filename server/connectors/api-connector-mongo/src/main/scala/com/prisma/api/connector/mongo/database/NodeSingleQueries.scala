package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer.whereToBson
import com.prisma.api.connector.mongo.extensions.{DocumentToId, DocumentToRoot}
import com.prisma.gc_values.{CuidGCValue, IdGCValue, ListGCValue}
import com.prisma.shared.models.{Model, RelationField}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.{Document, MongoCollection, MongoDatabase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.existentials

trait NodeSingleQueries extends FilterConditionBuilder {
  def getNodeByWhere2(where: NodeSelector) = SimpleMongoAction { database =>
    getNodeByWhere(where, database)
  }

  def getNodeByWhere(where: NodeSelector, database: MongoDatabase): Future[Option[PrismaNode]] =
    getNodeByWhere(where, SelectedFields.all(where.model), database)

  def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields, database: MongoDatabase): Future[Option[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(where.model.dbName)
    collection.find(where).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(where.model, result)
        PrismaNode(root.idField, root)
      }
    }
  }

  def getNodeByFilter(model: Model, mongoFilter: Bson, database: MongoDatabase) = {
    database.getCollection(model.dbName).find(mongoFilter).toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(model, result)
        PrismaNode(root.idField, root, Some(model.name))
      }
    }
  }

  def getNodeIdByWhere(where: NodeSelector) = SimpleMongoAction { database =>
    val collection: MongoCollection[Document] = database.getCollection(where.model.dbName)
    collection.find(where).projection(include("_.id")).collect().toFuture.map(res => res.headOption.map(DocumentToId.toCUIDGCValue))
  }

  def getNodeIdByParentId(parentField: RelationField, parentId: IdGCValue): SimpleMongoAction[Option[IdGCValue]] =
    SimpleMongoAction { database =>
      val parentModel = parentField.model
      val childModel  = parentField.relatedModel_!

      parentField.relation.inlineManifestation match {
        case Some(m) if m.inTableOfModelId == parentModel.name =>
          val node = getNodeByWhere(NodeSelector.forIdGCValue(parentModel, parentId), database)

          node.map {
            case None => None
            case Some(n) =>
              n.data.map(parentField.name) match {
                case x: CuidGCValue => Some(x)
                case _              => None
              }
          }

        case Some(m) if m.inTableOfModelId == childModel.name =>
          val filter = parentField.relatedField.isList match {
            case false => Some(ScalarFilter(childModel.idField_!.copy(name = parentField.relatedField.dbName), Equals(parentId)))
            case true  => Some(ScalarFilter(childModel.idField_!.copy(name = parentField.relatedField.dbName, isList = true), Contains(parentId)))
          }

          getNodeIdsByFilter(childModel, filter, database).map(_.headOption)

        case _ => sys.error("""""")
      }
    }

  def getNodeIdsByParentIds(parentField: RelationField, parentIds: Vector[IdGCValue]) = ???

  def getNodeIdsByFilter(model: Model, filter: Option[Filter], database: MongoDatabase): Future[Seq[IdGCValue]] = {
    val collection: MongoCollection[Document] = database.getCollection(model.dbName)
    val bsonFilter: Bson                      = buildConditionForFilter(filter)
    collection.find(bsonFilter).projection(include("_.id")).collect().toFuture.map(res => res.map(DocumentToId.toCUIDGCValue))
  }

  def getNodeIdsByFilter2(model: Model, filter: Option[Filter]) = SimpleMongoAction { database =>
    val collection: MongoCollection[Document] = database.getCollection(model.dbName)
    val bsonFilter: Bson                      = buildConditionForFilter(filter)
    collection.find(bsonFilter).projection(include("_.id")).collect().toFuture.map(res => res.map(DocumentToId.toCUIDGCValue))
  }

  def getNodeIdByParentIdAndWhere(parentField: RelationField, parentId: IdGCValue, where: NodeSelector): SimpleMongoAction[Option[IdGCValue]] =
    SimpleMongoAction { database =>
      val parentModel = parentField.model
      val childModel  = parentField.relatedModel_!

      parentField.relation.inlineManifestation match { //parent contains one or more ids, one of them matches the child returned for the where
        case Some(m) if m.inTableOfModelId == parentModel.name =>
          getNodeByWhere(NodeSelector.forIdGCValue(parentModel, parentId), database).flatMap {
            case None =>
              Future(None)

            case Some(n) =>
              (parentField.isList, n.data.map(parentField.name)) match {
                case (false, idInParent: CuidGCValue) =>
                  getNodeByWhere(where, database).map {
                    case Some(childForWhere) if idInParent == childForWhere.id => Some(idInParent)
                    case _                                                     => None
                  }

                case (true, ListGCValue(values)) =>
                  getNodeByWhere(where, database).map {
                    case Some(childForWhere) if values.contains(childForWhere.id) => Some(childForWhere.id)
                    case _                                                        => None
                  }

                case _ =>
                  Future(None)
              }
          }
        case Some(m) if m.inTableOfModelId == childModel.name => //child id that matches the where contains the parent
          val parentFilter = parentField.relatedField.isList match {
            case false => ScalarFilter(childModel.idField_!.copy(name = parentField.relatedField.dbName), Equals(parentId))
            case true  => ScalarFilter(childModel.idField_!.copy(name = parentField.relatedField.dbName, isList = true), Contains(parentId))
          }
          val whereFilter = ScalarFilter(where.field, Equals(where.fieldGCValue))
          val filter      = Some(AndFilter(Vector(parentFilter, whereFilter)))

          getNodeIdsByFilter(childModel, filter, database).map(_.headOption)

        case _ => sys.error("""""")
      }
    }

}
