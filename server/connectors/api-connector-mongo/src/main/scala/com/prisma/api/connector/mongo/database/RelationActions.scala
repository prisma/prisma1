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

  def createRelation(relationField: RelationField, parentId: IdGCValue, childId: IdGCValue)(
      implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val (collection, where, update) = relationField.relation.inlineManifestation match {
        case Some(m) if m.inTableOfModelId == relationField.model.name =>
          val parentWhere = NodeSelector.forIdGCValue(relationField.model, parentId)
          val collection  = database.getCollection(relationField.model.dbName)
          val update = relationField.isList match {
            case false => set(m.referencingColumn, GCValueBsonTransformer(childId))
            case true  => push(m.referencingColumn, GCValueBsonTransformer(childId))
          }
          (collection, parentWhere, update)

        case Some(m) if m.inTableOfModelId == relationField.relatedModel_!.name =>
          val childWhere = NodeSelector.forIdGCValue(relationField.relatedModel_!, childId)
          val collection = database.getCollection(relationField.relatedModel_!.dbName)
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
    val model    = relationField.model
    val relation = relationField.relation

    val manifestation = relation.inlineManifestation.get

    manifestation match {
      case m if m.inTableOfModelId == model.name => // either delete the child ID from all inlineRelationFields of old parents
        val collection    = database.getCollection(model.dbName)
        val filter        = ScalarFilter(model.idField_!.copy(name = m.referencingColumn), Equals(childId))
        val mongoFilter   = buildConditionForFilter(Some(filter))
        val update        = unset(m.referencingColumn)
        val updateOptions = UpdateOptions().arrayFilters(List.empty.asJava)
        collection.updateMany(mongoFilter, update, updateOptions).collect().toFuture()
      case m => Future.successful(()) // or if it is on the child id do nothing
    }
  }

  def deleteRelationRowByChildIdAndParentId(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue) = SimpleMongoAction { database =>
//    val relation = relationField.relation
//    val condition = relationColumn(relation, relationField.oppositeRelationSide)
//      .equal(placeHolder)
//      .and(relationColumn(relation, relationField.relationSide).equal(placeHolder))
//
//    relation.inlineManifestation match {
//      case Some(manifestation) =>
//        val query = sql
//          .update(relationTable(relation))
//          .set(inlineRelationColumn(relation, manifestation), placeHolder)
//          .where(condition)
//
//        updateToDBIO(query)(
//          setParams = { pp =>
//            pp.setGcValue(NullGCValue)
//            pp.setGcValue(childId)
//            pp.setGcValue(parentId)
//          }
//        )
//
//      case None =>
//        val query = sql
//          .deleteFrom(relationTable(relation))
//          .where(condition)
//
//        deleteToDBIO(query)(setParams = { pp =>
//          pp.setGcValue(childId)
//          pp.setGcValue(parentId)
//        })
//    }

    ???
  }

  def deleteRelationRowByParentId(relationField: RelationField, parentId: IdGCValue) =
    SimpleMongoAction { database =>
      val model = relationField.model
      relationField.relation.manifestation match {
        case Some(m: InlineRelationManifestation) if m.inTableOfModelId != model.name =>
          //stored on the other model
          //find the other item that is storing the reference to this parent id
          //update it to remove the parentId

          val collection = database.getCollection(relationField.relatedModel_!.dbName)

          val update: Bson = relationField.isList match {
            case true  => pull(m.referencingColumn, GCValueBsonTransformer(parentId))
            case false => unset(m.referencingColumn)
          }

//          collection.updateOne(NodeSelector.forIdGCValue(model, parentId), ).collect().toFuture
          Future.successful(())
        case Some(m: InlineRelationManifestation) =>
          val x = 1
          // stored on this model, set to null or remove f
          Future.successful(())
        case _ => sys.error("should not happen ")
      }
    }
  //    val condition = relationColumn(relation, relationField.relationSide).equal(placeHolder)
//
//
//        val query = sql
//          .update(relationTable(relation))
//          .set(inlineRelationColumn(relation, manifestation), placeHolder)
//          .where(condition)
//
//        updateToDBIO(query)(
//          setParams = { pp =>
//            pp.setGcValue(NullGCValue)
//            pp.setGcValue(parentId)
//          }
//        )
//  }
//  SimpleMongoAction { database =>
//    assert(!relationField.isList)
//    val relation = relationField.relation
//
//    val inlineManifestation = relation.inlineManifestation.get
//    val parentModel         = relationField.model
//    val parentWhere         = NodeSelector.forIdGCValue(parentModel, parentId)
//
//    val collection = database.getCollection(parentModel.dbName)
//
//    val update = relationField.isList match {
//      case false => set(inlineManifestation.referencingColumn, GCValueBsonTransformer(childId))
//      case true  => push(inlineManifestation.referencingColumn, GCValueBsonTransformer(childId))
//    }
//
//    collection
//      .updateOne(parentWhere, update)
//      .toFuture()
//      .map(_ => MutactionResults(Vector.empty))

}
