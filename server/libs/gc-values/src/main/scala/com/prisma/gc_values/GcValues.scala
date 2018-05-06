package com.prisma.gc_values

import org.joda.time.DateTime
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

sealed trait LeafGCValue                    extends GCValue
object NullGCValue                          extends LeafGCValue
case class StringGCValue(value: String)     extends LeafGCValue
case class IntGCValue(value: Int)           extends LeafGCValue
case class FloatGCValue(value: Double)      extends LeafGCValue
case class BooleanGCValue(value: Boolean)   extends LeafGCValue
case class IdGCValue(value: String)         extends LeafGCValue
case class DateTimeGCValue(value: DateTime) extends LeafGCValue
case class EnumGCValue(value: String)       extends LeafGCValue
case class JsonGCValue(value: JsValue)      extends LeafGCValue

object GCValueExtractor {

  def fromGCValueToString(t: GCValue): String = {
    fromGCValue(t) match {
      case x: Vector[Any] => x.map(_.toString).mkString(start = "[", sep = ",", end = "]")
      case x              => x.toString
    }
  }

  def fromGCValue(t: GCValue): Any = {
    t match {
      case ListGCValue(values)      => values.map(fromGCValue)
      case RootGCValue(_)           => sys.error("RootGCValues not implemented yet in GCValueExtractor")
      case leafGCValue: LeafGCValue => fromLeafGCValue(leafGCValue)
    }
  }

  def fromLeafGCValue(t: LeafGCValue): Any = {
    t match {
      case NullGCValue            => None // todo danger!!!
      case StringGCValue(value)   => value
      case EnumGCValue(value)     => value
      case IdGCValue(value)       => value
      case DateTimeGCValue(value) => value
      case IntGCValue(value)      => value
      case FloatGCValue(value)    => value
      case BooleanGCValue(value)  => value
      case JsonGCValue(value)     => value
    }
  }

  def fromListGCValue(t: ListGCValue): Vector[Any] = t.values.map(fromGCValue)
}
