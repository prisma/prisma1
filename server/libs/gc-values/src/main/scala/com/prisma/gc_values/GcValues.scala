package com.prisma.gc_values

import org.joda.time.DateTime
import play.api.libs.json.JsValue

/**
  * GCValues should be the sole way to represent data within our system.
  * We will try to use them to get rid of the Any, and get better type safety.
  *
  * thoughts:
  *   - move the spot where we do the validations further back? out of the AddFieldMutation to AddField Input already?
  *   - Where do we need Good/Bad Error handling, where can we call get?
  */
sealed trait GCValue

case class RootGCValue(map: Map[String, GCValue]) extends GCValue

case class ListGCValue(values: Vector[GCValue]) extends GCValue {
  def getStringVector: Vector[String] = values.asInstanceOf[Vector[StringGCValue]].map(_.value)
  def getEnumVector: Vector[String]   = values.asInstanceOf[Vector[EnumGCValue]].map(_.value)
}

sealed trait LeafGCValue                   extends GCValue
object NullGCValue                         extends LeafGCValue
case class StringGCValue(value: String)    extends LeafGCValue
case class IntGCValue(value: Int)          extends LeafGCValue
case class FloatGCValue(value: Double)     extends LeafGCValue
case class BooleanGCValue(value: Boolean)  extends LeafGCValue
case class GraphQLIdGCValue(value: String) extends LeafGCValue
case class DateTimeGCValue(value: DateTime) extends LeafGCValue {
  //the DateTime value should have ISO 8601 format like so "2017-12-05T12:34:23.000Z"

  //but MySql will not accept this for DateTime fields we need to convert to this to "2017-12-05 12:34:23.000"
  def toMySqlDateTimeFormat = value.toString.replace("T", " ").replace("Z", "")
}
case class EnumGCValue(value: String)  extends LeafGCValue
case class JsonGCValue(value: JsValue) extends LeafGCValue
