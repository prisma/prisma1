package com.prisma.api.connector.mongo.database

import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.api.connector.{MutactionResults, NodeSelector}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.RelationField
import org.mongodb.scala.model.Updates._

import scala.concurrent.ExecutionContext

trait RelationActions {

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

//  def deleteRelationRowByChildId(relationField: RelationField, childId: IdGCValue): DBIO[Unit] = {
//    assert(!relationField.relatedField.isList)
//    val relation  = relationField.relation
//    val condition = relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder)
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
//          }
//        )
//
//      case None =>
//        val query = sql
//          .deleteFrom(relationTable(relation))
//          .where(condition)
//
//        deleteToDBIO(query)(setParams = _.setGcValue(childId))
//    }
//  }
//
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
//
//  def deleteRelationRowByParentId(relationField: RelationField, parentId: IdGCValue): DBIO[Unit] = {
//    assert(!relationField.isList)
//    val relation  = relationField.relation
//    val condition = relationColumn(relation, relationField.relationSide).equal(placeHolder)
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
//            pp.setGcValue(parentId)
//          }
//        )
//
//      case None =>
//        val query = sql
//          .deleteFrom(relationTable(relation))
//          .where(condition)
//
//        deleteToDBIO(query)(setParams = _.setGcValue(parentId))
//    }
//  }
}
