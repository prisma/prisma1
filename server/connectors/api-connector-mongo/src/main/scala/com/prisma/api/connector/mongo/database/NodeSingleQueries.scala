package com.prisma.api.connector.mongo.database

import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer.whereToBson
import com.prisma.api.connector.{SelectedFields, _}
import com.prisma.gc_values.{IdGCValue, ListGCValue, StringIdGCValue}
import com.prisma.shared.models.{Project, RelationField}
import org.mongodb.scala.Document
import org.mongodb.scala.model.Filters

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.existentials

trait NodeSingleQueries extends FilterConditionBuilder with NodeManyQueries with ProjectionBuilder {

  def getModelForGlobalId(project: Project, globalId: StringIdGCValue) = SimpleMongoAction { (database, session) =>
    val outer = project.models.map { model =>
      database.getCollection(model.dbName).find(session, Filters.eq("_id", GCToBson(globalId))).projection(idProjection).collect().toFuture.map {
        results: Seq[Document] =>
          if (results.nonEmpty) Vector(model) else Vector.empty
      }
    }

    Future.sequence(outer).map(_.flatten.headOption)
  }

  def getNodeByWhereComplete(where: NodeSelector): SimpleMongoAction[Option[PrismaNode]] = SimpleMongoAction { (database, session) =>
    database.getCollection(where.model.dbName).find(session, where).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map(readsCompletePrismaNode(_, where.model))
    }
  }

  def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields) = SimpleMongoAction { (database, session) =>
    database.getCollection(where.model.dbName).find(session, where).projection(projectSelected(selectedFields)).collect().toFuture.map {
      results: Seq[Document] =>
        results.headOption.map(readsPrismaNode(_, where.model, selectedFields))
    }
  }

  def getNodeIdByWhere(where: NodeSelector): SimpleMongoAction[Option[IdGCValue]] = SimpleMongoAction { (database, session) =>
    database
      .getCollection(where.model.dbName)
      .find(session, where)
      .projection(idProjection)
      .collect()
      .toFuture
      .map(_.headOption.map(readsId))
  }

  def getNodeIdByParent(parentField: RelationField, parent: NodeAddress): MongoAction[Option[IdGCValue]] = {
    val childModel = parentField.relatedModel_!

    parentField.relationIsInlinedInParent match {
      case true =>
        getNodeByWhere(parent.where, SelectedFields.byFieldAndNodeAddress(parentField, parent))
          .map {
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
        getNodeByWhere(parent.where, SelectedFields.byFieldAndNodeAddress(parentField, parent)).flatMap {
          case None =>
            noneHelper
          case Some(n) =>
            PrismaNode.getNodeAtPath(Some(n), parent.path.segments) match {
              case None =>
                noneHelper

              case Some(node) =>
                (parentField.isList, node.data.map(parentField.name)) match {
                  case (false, idInParent: StringIdGCValue) =>
                    getNodeIdByWhere(where).map {
                      case Some(childForWhere) if idInParent == childForWhere => Some(idInParent)
                      case _                                                  => None
                    }

                  case (true, ListGCValue(values)) =>
                    getNodeIdByWhere(where).map {
                      case Some(childForWhere) if values.contains(childForWhere) => Some(childForWhere)
                      case _                                                     => None
                    }

                  case _ =>
                    noneHelper
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

  def noneHelper = SimpleMongoAction { (database, session) =>
    Future(Option.empty[IdGCValue])
  }

  def generateFilterForFieldAndId(relationField: RelationField, id: IdGCValue) = relationField.isList match {
    case true  => ScalarListFilter(relationField.model.dummyField(relationField), ListContains(id))
    case false => ScalarFilter(relationField.model.dummyField(relationField), Equals(id))
  }

}
