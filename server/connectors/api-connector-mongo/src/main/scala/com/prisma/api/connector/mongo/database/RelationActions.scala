package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.ArrayFilter
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.RelationField
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates._
import scala.collection.JavaConverters._

import scala.concurrent.{ExecutionContext, Future}

trait RelationActions extends FilterConditionBuilder {

  def createRelation(relationField: RelationField, parent: NodeAddress, childId: IdGCValue)(implicit ec: ExecutionContext) =
    SimpleMongoAction { database =>
      val childModel         = relationField.relatedModel_!
      val relatedField       = relationField.relatedField
      val parentField        = parent.path.stringForField(relationField.dbName)
      val arrayFilters       = ArrayFilter.arrayFilter(parent.path)
      lazy val childSelector = NodeSelector.forId(childModel, childId)

      val (collectionName, where, update) = relationField.relationIsInlinedInParent match {
        case true if !relationField.isList => (parent.where.model.dbName, parent.where, set(parentField, GCToBson(childId)))
        case true if relationField.isList  => (parent.where.model.dbName, parent.where, addToSet(parentField, GCToBson(childId)))
        case false if !relatedField.isList => (childModel.dbName, childSelector, set(relatedField.dbName, GCToBson(parent.idValue)))
        case false if relatedField.isList  => (childModel.dbName, childSelector, addToSet(relatedField.dbName, GCToBson(parent.idValue)))
      }

      val updateOptions = UpdateOptions().arrayFilters(arrayFilters.toList.asJava)

      database
        .getCollection(collectionName)
        .updateOne(where, update, updateOptions)
        .toFuture()
    }

  def deleteRelationRowByChildId(relationField: RelationField, childId: IdGCValue) = SimpleMongoAction { database =>
    assert(!relationField.relatedField.isList)
    relationField.relationIsInlinedInParent match {
      case true =>
        val mongoFilter = buildConditionForFilter(Some(ScalarFilter(relationField.model.dummyField(relationField), Equals(childId))))
        val update      = unset(relationField.dbName)

        database.getCollection(relationField.model.dbName).updateMany(mongoFilter, update).collect().toFuture()

      case false =>
        Future.successful(())
    }
  }

  def deleteRelationRowByChildIdAndParentId(relationField: RelationField, childId: IdGCValue, parent: NodeAddress, fromDelete: Boolean = false) =
    SimpleMongoAction { database =>
      val parentModel = parent.where.model
      val childModel  = relationField.relatedModel_!

      relationField.relationIsInlinedInParent match {
        case true =>
          val field         = parent.path.stringForField(relationField.dbName)
          val af            = ArrayFilter.arrayFilter(parent.path)
          val updateOptions = UpdateOptions().arrayFilters(af.toList.asJava)
          val mongoFilter   = buildConditionForFilter(Some(ScalarFilter(parentModel.idField_!, Equals(parent.idValue))))
          val update        = if (relationField.isList) pull(field, GCToBson(childId)) else unset(field)

          database.getCollection(parentModel.dbName).updateMany(mongoFilter, update, updateOptions).collect().toFuture()

        case false if !fromDelete =>
          val mongoFilter = buildConditionForFilter(Some(ScalarFilter(childModel.idField_!, Equals(childId))))
          val update = relationField.relatedField.isList match {
            case false => unset(relationField.relatedField.dbName)
            case true  => pull(relationField.relatedField.dbName, GCToBson(parent.idValue))
          }

          database.getCollection(childModel.dbName).updateOne(mongoFilter, update).collect().toFuture()

        case false if fromDelete =>
          Future.successful(())
      }
    }

  def deleteRelationRowByParent(relationField: RelationField, parent: NodeAddress)(implicit ec: ExecutionContext) =
    SimpleMongoAction { database =>
      val childModel   = relationField.relatedModel_!
      val relatedField = relationField.relatedField

      relationField.relationIsInlinedInParent match {
        case true =>
          val field         = parent.path.stringForField(relationField.dbName)
          val af            = ArrayFilter.arrayFilter(parent.path)
          val updateOptions = UpdateOptions().arrayFilters(af.toList.asJava)
          val update: Bson  = if (relationField.isList) pull(field, GCToBson(parent.idValue)) else unset(field)

          database.getCollection(parent.where.model.dbName).updateOne(parent.where, update, updateOptions).collect().toFuture

        case false =>
          relatedField.isList match {
            case false =>
              val mongoFilter = buildConditionForFilter(Some(ScalarFilter(childModel.dummyField(relatedField), Equals(parent.idValue))))
              val update      = unset(relatedField.dbName)
              database.getCollection(childModel.dbName).updateOne(mongoFilter, update).collect().toFuture

            case true =>
              val mongoFilter = buildConditionForFilter(Some(ScalarFilter(childModel.dummyField(relatedField), Contains(parent.idValue))))
              val update      = pull(relatedField.dbName, GCToBson(parent.idValue))
              database.getCollection(childModel.dbName).updateMany(mongoFilter, update).collect().toFuture
          }
      }
    }
}
