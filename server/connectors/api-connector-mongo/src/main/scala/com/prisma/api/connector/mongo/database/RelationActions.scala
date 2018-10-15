package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.RelationField
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Updates._

import scala.concurrent.{ExecutionContext, Future}

trait RelationActions extends FilterConditionBuilder {

  def createRelation(relationField: RelationField, parent: NodeAddress, childId: IdGCValue)(implicit ec: ExecutionContext) =
    SimpleMongoAction { database =>
      val parentModel  = relationField.model
      val childModel   = relationField.relatedModel_!
      val relatedField = relationField.relatedField

      val (collectionName, where, update) = relationField.relationIsInlinedInParent match {
        case true if !relationField.isList => (parentModel.dbName, parent.where, set(relationField.dbName, GCToBson(childId)))
        case true if relationField.isList  => (parentModel.dbName, parent.where, push(relationField.dbName, GCToBson(childId)))
        case false if !relatedField.isList => (childModel.dbName, NodeSelector.forId(childModel, childId), set(relatedField.dbName, GCToBson(parent.idValue)))
        case false if relatedField.isList  => (childModel.dbName, NodeSelector.forId(childModel, childId), push(relatedField.dbName, GCToBson(parent.idValue)))
      }

      database
        .getCollection(collectionName)
        .updateOne(where, update)
        .toFuture()
    }

  def deleteRelationRowByChildId(relationField: RelationField, childId: IdGCValue) = SimpleMongoAction { database =>
    assert(!relationField.relatedField.isList)
    relationField.relationIsInlinedInParent match {
      case true =>
        val filter      = ScalarFilter(relationField.model.idField_!.copy(name = relationField.dbName), Equals(childId))
        val mongoFilter = buildConditionForFilter(Some(filter))
        val update      = unset(relationField.dbName)

        database.getCollection(relationField.model.dbName).updateMany(mongoFilter, update).collect().toFuture()
      case false =>
        Future.successful(())
    }
  }

  def deleteRelationRowByChildIdAndParentId(relationField: RelationField, childId: IdGCValue, parent: NodeAddress) = SimpleMongoAction { database =>
    val parentModel = relationField.model
    val childModel  = relationField.relatedModel_!

    relationField.relationIsInlinedInParent match {
      case true =>
        val filter      = ScalarFilter(parentModel.idField_!.copy(name = relationField.dbName, isList = true), Contains(childId))
        val whereFilter = ScalarFilter(parentModel.idField_!, Equals(parent.idValue))
        val mongoFilter = buildConditionForFilter(Some(AndFilter(Vector(filter, whereFilter))))
        val update      = if (relationField.isList) pull(relationField.dbName, GCToBson(childId)) else unset(relationField.dbName)

        database.getCollection(parentModel.dbName).updateMany(mongoFilter, update).collect().toFuture()

      case false =>
        val mongoFilter = buildConditionForFilter(Some(ScalarFilter(childModel.idField_!, Equals(childId))))
        val update = relationField.relatedField.isList match {
          case false => unset(relationField.relatedField.dbName)
          case true  => pull(relationField.relatedField.dbName, GCToBson(parent.idValue))
        }

        database.getCollection(childModel.dbName).updateOne(mongoFilter, update).collect().toFuture()
    }
  }

  def deleteRelationRowByParent(relationField: RelationField, parent: NodeAddress)(implicit ec: ExecutionContext) =
    SimpleMongoAction { database =>
      val parentModel  = relationField.model
      val childModel   = relationField.relatedModel_!
      val relatedField = relationField.relatedField

      relationField.relationIsInlinedInParent match {
        case true =>
          val update: Bson = if (relationField.isList) pull(relationField.dbName, GCToBson(parent.idValue)) else unset(relationField.dbName)

          database.getCollection(parentModel.dbName).updateOne(parent.where, update).collect().toFuture

        case false =>
          relatedField.isList match {
            case false =>
              val mongoFilter =
                buildConditionForFilter(Some(ScalarFilter(childModel.idField_!.copy(relatedField.dbName, isList = false), Equals(parent.idValue))))
              val update = unset(relatedField.dbName)
              database.getCollection(childModel.dbName).updateOne(mongoFilter, update).collect().toFuture

            case true =>
              val mongoFilter =
                buildConditionForFilter(Some(ScalarFilter(childModel.idField_!.copy(relatedField.dbName, isList = true), Contains(parent.idValue))))
              val update = pull(relatedField.dbName, GCToBson(parent.idValue))
              database.getCollection(childModel.dbName).updateMany(mongoFilter, update).collect().toFuture
          }
      }
    }
}
