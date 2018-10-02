package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models.RelationField
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates._
import scala.collection.JavaConverters._

import scala.concurrent.{ExecutionContext, Future}

trait RelationActions extends FilterConditionBuilder {

  def createRelation(relationField: RelationField, parentId: IdGCValue, childId: IdGCValue)(implicit ec: ExecutionContext) =
    SimpleMongoAction { database =>
      val parentModel = relationField.model
      val childModel  = relationField.relatedModel_!

      val (collection, where, update) = relationField.relation.inlineManifestation match {
        case Some(m) if m.inTableOfModelId == parentModel.name =>
          val parentWhere = NodeSelector.forIdGCValue(parentModel, parentId)
          val collection  = database.getCollection(parentModel.dbName)
          val update = relationField.isList match {
            case false => set(m.referencingColumn, GCValueBsonTransformer(childId))
            case true  => push(m.referencingColumn, GCValueBsonTransformer(childId))
          }
          (collection, parentWhere, update)

        case Some(m) if m.inTableOfModelId == childModel.name =>
          val childWhere = NodeSelector.forIdGCValue(childModel, childId)
          val collection = database.getCollection(childModel.dbName)
          val update = relationField.relatedField.isList match {
            case false => set(m.referencingColumn, GCValueBsonTransformer(parentId))
            case true  => push(m.referencingColumn, GCValueBsonTransformer(parentId))
          }

          (collection, childWhere, update)
        case _ => sys.error("There should always be an inline relation manifestation")
      }

      collection
        .updateOne(where, update)
        .toFuture()
        .map(_ => MutactionResults(Vector.empty))
    }

  def deleteRelationRowByChildId(relationField: RelationField, childId: IdGCValue) = SimpleMongoAction { database =>
    assert(!relationField.relatedField.isList)
    relationField.relation.inlineManifestation match {
      case Some(m) if m.inTableOfModelId == relationField.model.name => //delete the child ID from all inlineRelationFields of old parents
        val collection    = database.getCollection(relationField.model.dbName)
        val filter        = ScalarFilter(relationField.model.idField_!.copy(name = m.referencingColumn), Equals(childId))
        val mongoFilter   = buildConditionForFilter(Some(filter))
        val update        = unset(m.referencingColumn)
        val updateOptions = UpdateOptions().arrayFilters(List.empty.asJava)

        collection.updateMany(mongoFilter, update, updateOptions).collect().toFuture()
      case m => Future.successful(()) //do nothing
    }
  }

  def deleteRelationRowByChildIdAndParentId(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue) = SimpleMongoAction { database =>
    val parentModel = relationField.model
    val childModel  = relationField.relatedModel_!

    relationField.relation.inlineManifestation match {
      case Some(m) if m.inTableOfModelId == parentModel.name => //delete the child ID from all inlineRelationFields of old parents
        val collection  = database.getCollection(parentModel.dbName)
        val filter      = ScalarFilter(parentModel.idField_!.copy(name = m.referencingColumn, isList = true), Contains(childId))
        val whereFilter = ScalarFilter(parentModel.idField_!, Equals(parentId))
        val mongoFilter = buildConditionForFilter(Some(AndFilter(Vector(filter, whereFilter))))

        val update = relationField.isList match {
          case false => unset(m.referencingColumn)
          case true  => pull(m.referencingColumn, GCValueBsonTransformer(childId))
        }

        val updateOptions = UpdateOptions().arrayFilters(List.empty.asJava)

        collection.updateMany(mongoFilter, update, updateOptions).collect().toFuture()

      case Some(m) if m.inTableOfModelId == childModel.name => // remove it from the child inline relation
        val collection  = database.getCollection(childModel.dbName)
        val mongoFilter = buildConditionForFilter(Some(ScalarFilter(childModel.idField_!, Equals(childId))))
        val update = relationField.relatedField.isList match {
          case false => unset(m.referencingColumn)
          case true  => pull(m.referencingColumn, GCValueBsonTransformer(parentId))
        }

        collection.updateOne(mongoFilter, update).collect().toFuture()

      case _ => sys.error("Should not happen")
    }
  }

  def deleteRelationRowByParentId(relationField: RelationField, parentId: IdGCValue)(implicit ec: ExecutionContext) =
    SimpleMongoAction { database =>
      val parentModel = relationField.model
      val childModel  = relationField.relatedModel_!

      relationField.relation.manifestation match {
        case Some(m: InlineRelationManifestation) if m.inTableOfModelId == parentModel.name => // stored on this model, set to null or remove from list
          val collection = database.getCollection(parentModel.dbName)

          val update: Bson = relationField.isList match {
            case true  => pull(m.referencingColumn, GCValueBsonTransformer(parentId))
            case false => unset(m.referencingColumn)
          }

          collection.updateOne(NodeSelector.forIdGCValue(parentModel, parentId), update).collect().toFuture.map(_ => Unit)

        case Some(m: InlineRelationManifestation) if m.inTableOfModelId == childModel.name =>
          val collection = database.getCollection(childModel.dbName)
          relationField.relatedField.isList match {
            case false =>
              val mongoFilter = buildConditionForFilter(Some(ScalarFilter(childModel.idField_!.copy(m.referencingColumn, isList = false), Equals(parentId))))
              val update      = unset(m.referencingColumn)
              collection.updateOne(mongoFilter, update).collect().toFuture()

            case true =>
              val mongoFilter = buildConditionForFilter(Some(ScalarFilter(childModel.idField_!.copy(m.referencingColumn, isList = true), Contains(parentId))))
              val update      = pull(m.referencingColumn, GCValueBsonTransformer(parentId))
              collection.updateMany(mongoFilter, update).collect().toFuture()
          }

        case _ => sys.error("should not happen ")
      }
    }
}
