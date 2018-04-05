package com.prisma.gc_values

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

import scala.collection.immutable.SortedMap

/**
  * GCValues should be the sole way to represent data within our system.
  * We will try to use them to get rid of the Any, and get better type safety.
  *
  * thoughts:
  *   - move the spot where we do the validations further back? out of the AddFieldMutation to AddField Input already?
  *   - Where do we need Good/Bad Error handling, where can we call get?
  */
sealed trait GCValue {
  def asRoot: RootGCValue = this.asInstanceOf[RootGCValue]
}

object RootGCValue {
  def apply(elements: (String, GCValue)*): RootGCValue = RootGCValue(SortedMap(elements: _*))
  def empty: RootGCValue = {
    val empty: SortedMap[String, GCValue] = SortedMap.empty
    RootGCValue(empty)
  }
}
case class RootGCValue(map: SortedMap[String, GCValue]) extends GCValue {
  def idField = map.get("id") match {
    case Some(id) => id.asInstanceOf[IdGCValue]
    case None     => sys.error("There was no field with name 'id'.")
  }

  def filterValues(p: GCValue => Boolean) = copy(map = map.filter(t => p(t._2)))

//  def toJson: JsValue = GCValueExtractor.fromGCValueToJson(this)

  def toMapStringAny: Map[String, Any] = map.collect {
    case (key, value) =>
      val convertedValue = value match {
        case NullGCValue    => sys.error("This should not be used on NullGCValues")
        case v: LeafGCValue => GCValueExtractor.fromLeafGCValue(v)
        case v: ListGCValue => GCValueExtractor.fromListGCValue(v).toList
        case v: RootGCValue => sys.error("RootGCValue not handled yet")
      }
      (key, convertedValue)
  }

  def isEmpty = map.isEmpty
}

case class ListGCValue(values: Vector[GCValue]) extends GCValue {
  def getStringVector: Vector[String] = values.asInstanceOf[Vector[StringGCValue]].map(_.value)
  def getEnumVector: Vector[String]   = values.asInstanceOf[Vector[EnumGCValue]].map(_.value)
  def isEmpty: Boolean                = values.isEmpty
  def size: Int                       = values.size
}

sealed trait LeafGCValue                  extends GCValue
object NullGCValue                        extends LeafGCValue
case class StringGCValue(value: String)   extends LeafGCValue
case class IntGCValue(value: Int)         extends LeafGCValue
case class FloatGCValue(value: Double)    extends LeafGCValue
case class BooleanGCValue(value: Boolean) extends LeafGCValue
case class IdGCValue(value: String)       extends LeafGCValue
case class DateTimeGCValue(value: DateTime) extends LeafGCValue {
  //the DateTime value should have ISO 8601 format like so "2017-12-05T12:34:23.000Z"

  //but MySql will not accept this for DateTime fields we need to convert to this to "2017-12-05 12:34:23.000"
//  def toMySqlDateTimeFormat = value.toString.replace("T", " ").replace("Z", "")
}
case class EnumGCValue(value: String)  extends LeafGCValue
case class JsonGCValue(value: JsValue) extends LeafGCValue

object GCValueExtractor {

//  def fromGCValueToJson(t: GCValue): JsValue = {
//
//    val formatter = ISODateTimeFormat.dateHourMinuteSecondFraction()
//
//    t match {
//      case NullGCValue         => JsNull
//      case StringGCValue(x)    => JsString(x)
//      case EnumGCValue(x)      => JsString(x)
//      case IdGCValue(x)        => JsString(x)
//      case DateTimeGCValue(x)  => JsString(formatter.print(x))
//      case IntGCValue(x)       => JsNumber(x)
//      case FloatGCValue(x)     => JsNumber(x)
//      case BooleanGCValue(x)   => JsBoolean(x)
//      case JsonGCValue(x)      => x
//      case ListGCValue(values) => JsArray(values.map(fromGCValueToJson))
//      case RootGCValue(map)    => JsObject(map.map { case (k, v) => (k, fromGCValueToJson(v)) })
//    }
//  }

  def fromListGCValue(t: ListGCValue): Vector[Any] = t.values.map(fromGCValue)

  def fromGCValue(t: GCValue): Any = {
    t match {
      case x: ListGCValue => fromListGCValue(x)
      case x: RootGCValue => sys.error("RootGCValues not implemented yet in GCValueExtractor")
      case x: LeafGCValue => fromLeafGCValue(x)
    }
  }

  def fromLeafGCValue(t: LeafGCValue): Any = {
    t match {
      case NullGCValue        => None // todo danger!!!
      case StringGCValue(x)   => x
      case EnumGCValue(x)     => x
      case IdGCValue(x)       => x
      case DateTimeGCValue(x) => x
      case IntGCValue(x)      => x
      case FloatGCValue(x)    => x
      case BooleanGCValue(x)  => x
      case JsonGCValue(x)     => x
    }
  }
}
