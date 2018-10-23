package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer.whereToBson
import com.prisma.api.connector.mongo.extensions.{DocumentToId, DocumentToRoot}
import com.prisma.gc_values.{CuidGCValue, IdGCValue, ListGCValue}
import com.prisma.shared.models.{Project, RelationField}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.{Document, MongoCollection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.existentials

trait NodeSingleQueries extends FilterConditionBuilder with NodeManyQueries {

  def getModelForGlobalId(project: Project, globalId: CuidGCValue) = SimpleMongoAction { database =>
    val outer = project.models.map { model =>
      database.getCollection(model.dbName).find(Filters.eq("_id", globalId.value)).collect().toFuture.map { results: Seq[Document] =>
        if (results.nonEmpty) Vector(model) else Vector.empty
      }
    }

    Future.sequence(outer).map(_.flatten.headOption)
  }

  def getNodeByWhere(where: NodeSelector): SimpleMongoAction[Option[PrismaNode]] = getNodeByWhere(where, SelectedFields.all(where.model))

  def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields) = SimpleMongoAction { database =>
    database.getCollection(where.model.dbName).find(where).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(where.model, result)
        PrismaNode(root.idField, root, Some(where.model.name))
      }
    }
  }

  def getNodeIdByWhere(where: NodeSelector) = SimpleMongoAction { database =>
    database
      .getCollection(where.model.dbName)
      .find(where)
      .projection(include("_.id"))
      .collect()
      .toFuture
      .map(res => res.headOption.map(DocumentToId.toCUIDGCValue))
  }

  def getNodeIdByParent(parentField: RelationField, parent: NodeAddress): MongoAction[Option[IdGCValue]] = {
    val childModel = parentField.relatedModel_!

    parentField.relationIsInlinedInParent match {
      case true =>
        getNodeByWhere(parent.where).map {
          case None    => None
          case Some(n) => n.getIDAtPath(parentField, parent.path)
        }
      case false =>
        val filter = generateFilterForFieldAndId(parentField.relatedField, parent.idValue)

        getNodeIdsByFilter(childModel, Some(filter)).map(_.headOption)
    }
  }

  def getNodeIdByParentAndWhere(parentField: RelationField, parent: NodeAddress, where: NodeSelector): MongoAction[Option[IdGCValue]] = {
    val childModel = parentField.relatedModel_!

    parentField.relationIsInlinedInParent match {
      case true =>
        getNodeByWhere(parent.where).flatMap {
          case None =>
            noneHelper

          case Some(n) =>
            (parentField.isList, n.data.map(parentField.name)) match {
              case (false, idInParent: CuidGCValue) =>
                getNodeByWhere(where).map {
                  case Some(childForWhere) if idInParent == childForWhere.id => Some(idInParent)
                  case _                                                     => None
                }

              case (true, ListGCValue(values)) =>
                getNodeByWhere(where).map {
                  case Some(childForWhere) if values.contains(childForWhere.id) => Some(childForWhere.id)
                  case _                                                        => None
                }

              case _ =>
                noneHelper
            }
        }
      case false =>
        val parentFilter = generateFilterForFieldAndId(parentField.relatedField, parent.idValue)
        val whereFilter  = ScalarFilter(where.field, Equals(where.fieldGCValue))
        val filter       = Some(AndFilter(Vector(parentFilter, whereFilter)))

        getNodeIdsByFilter(childModel, filter).map(_.headOption)
    }
  }

  def noneHelper = SimpleMongoAction { database =>
    Future(Option.empty[IdGCValue])
  }

  def generateFilterForFieldAndId(relationField: RelationField, id: IdGCValue) = relationField.isList match {
    case true  => ScalarFilter(relationField.model.idField_!.copy(name = relationField.dbName), Contains(id))
    case false => ScalarFilter(relationField.model.idField_!.copy(name = relationField.dbName), Equals(id))
  }

}
