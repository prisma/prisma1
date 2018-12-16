package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.shared.models.ReservedFields
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Aggregates.project
import org.mongodb.scala.model.Projections._

trait ProjectionBuilder {
  def projectSelected(selectedFields: SelectedFields): conversions.Bson =
    include(selectedFields.relationFields.map(_.dbName).toList ++ selectedFields.scalarFields.filterNot(_.isId).map(_.dbName) :+ "_id": _*)
  def idProjection: conversions.Bson = include(ReservedFields.mongoInternalIdfieldName)
  def idProjectionStage              = project(idProjection)

  //Fixme project along the path and only return the needed subfields
  // should mirror getNode at path
  //need to check whether the conversion to prisma node fails if fields are missing
  //also how much overhead does it save to insert NullGCValues instead of converting the data
  //general thought: we're mixing dot notation (field.subfield) and nesting (field: {subfield})
  def projectPath(path: Path): conversions.Bson = {
//    val doc = path.segments.headOption match {
//      case None                                       => 1
//      case Some(ToOneSegment(rf))                     => Document(rf.dbName -> projectPath(path.dropFirst))
//      case Some(ToManySegment(rf, where))             => Document(rf.dbName -> projectPath(path.dropFirst))
//      case Some(ToManyFilterSegment(rf, whereFilter)) => Document(rf.dbName -> projectPath(path.dropFirst))
//    }
//
//    include(doc)
    include("")
  }
}
