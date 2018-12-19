package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.ArrayFilter
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.shared.models.{RelationField, ReservedFields}
import org.mongodb.scala.bson.{BsonDocument, conversions}
import org.mongodb.scala.model.Aggregates.project
import org.mongodb.scala.model.Projections._

trait ProjectionBuilder {
  val idProjection: conversions.Bson = include(ReservedFields.mongoInternalIdfieldName)
  val idProjectionStage              = project(idProjection)

  def projectSelected(selectedFields: SelectedFields): conversions.Bson = {
    val scalarFields              = selectedFields.scalarFields.filterNot(_.isId).map(_.dbName).toVector :+ "_id"
    val embeddedRelationFields    = helper(selectedFields.relationalSelectedFields.filter(x => x.field.relatedModel_!.isEmbedded))
    val nonEmbeddedRelationFields = selectedFields.relationalSelectedFields.filterNot(x => x.field.relatedModel_!.isEmbedded).map(_.field.dbName)

    include(scalarFields ++ embeddedRelationFields ++ nonEmbeddedRelationFields: _*)
  }

  private def helper(fields: Set[SelectedRelationField], prefix2: String = ""): Vector[String] = {
    fields.flatMap { selected =>
      val prefix    = combineTwo(prefix2, selected.field.dbName)
      val scalars   = selected.selectedFields.scalarFields.filterNot(_.isId).map(x => combineTwo(prefix, x.dbName)).toVector :+ combineTwo(prefix, "_id")
      val embeddeds = helper(selected.selectedFields.relationalSelectedFields.filter(x => x.field.relatedModel_!.isEmbedded), prefix)
      val nonEmbeddeds =
        selected.selectedFields.relationalSelectedFields.filterNot(x => x.field.relatedModel_!.isEmbedded).map(x => combineTwo(prefix, x.field.dbName))

      scalars ++ embeddeds ++ nonEmbeddeds
    }.toVector
  }

  def projectPath(path: Path, relationField: RelationField): conversions.Bson = {
    def helper(path: Path, stringPath: String): List[String] = path.segments.headOption match {
      case None                   => List(s""""${combineTwo(stringPath, relationField.dbName)}": 1""")
      case Some(ToOneSegment(rf)) => helper(path.dropFirst, combineTwo(stringPath, rf.dbName))
      case Some(ToManySegment(rf, where)) =>
        helper(path.dropFirst, combineTwo(stringPath, rf.dbName)) :+ s""""${combineTwo(stringPath, s"${rf.dbName}.${ArrayFilter.fieldName(where)}")}" : 1"""
      case Some(ToManyFilterSegment(rf, _)) => helper(path.dropFirst, combineTwo(stringPath, rf.dbName))
    }

    BsonDocument(s"""{${helper(path, "").mkString(",")}}""")
  }

  //Fixme, do we also need a aggregation stage for that?
}
