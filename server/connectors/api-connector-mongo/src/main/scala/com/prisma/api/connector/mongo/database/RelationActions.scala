package com.prisma.api.connector.mongo.database

import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models.RelationField
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates._

import scala.concurrent.{ExecutionContext, Future}

trait RelationActions extends FilterConditionBuilder {

  def createRelation(relationField: RelationField, parentId: IdGCValue, childId: IdGCValue)(
      implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val relation = relationField.relation

      val inlineManifestation = relation.inlineManifestation.get
      val parentModel         = relationField.model
      val parentWhere         = NodeSelector.forIdGCValue(parentModel, parentId)

      val collection = database.getCollection(parentModel.dbName)

      val update = relationField.isList match {
        case false => set(inlineManifestation.referencingColumn, GCValueBsonTransformer(childId))
        case true  => push(inlineManifestation.referencingColumn, GCValueBsonTransformer(childId))
      }

      collection
        .updateOne(parentWhere, update)
        .toFuture()
        .map(_ => MutactionResults(Vector.empty))
    }

//  def deleteRelationRowByChildId(relationField: RelationField, childId: IdGCValue) = {
//    assert(!relationField.relatedField.isList)
//    val relation  = relationField.relation
//    val condition = relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder)
//
//
//    sql
//          .update(relationTable(relation))
//          .set(inlineRelationColumn(relation, manifestation), placeHolder)
//          .where(condition)
//
//        updateToDBIO(query)(
//          setParams = { pp =>
//            pp.setGcValue(NullGCValue)
//            pp.setGcValue(childId)
//          }
//        )
//
//
//  }

//  def deleteRelationRowByChildIdAndParentId(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue): DBIO[Unit] = {
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
//  }

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
