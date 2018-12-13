package com.prisma.api.connector.mongo.database

import com.prisma.api.connector.SelectedFields
import com.prisma.shared.models.ReservedFields
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Aggregates.project
import org.mongodb.scala.model.Projections._

trait ProjectionBuilder {
  def projectSelected(selectedFields: SelectedFields): conversions.Bson =
    include(selectedFields.relationFields.map(_.dbName).toList ++ selectedFields.scalarFields.filterNot(_.isId).map(_.dbName) :+ "_id": _*)
  def idProjection: conversions.Bson = include(ReservedFields.mongoInternalIdfieldName)
  def idProjectionStage              = project(idProjection)
}
