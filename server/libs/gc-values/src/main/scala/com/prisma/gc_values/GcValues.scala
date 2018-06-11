package com.prisma.gc_values

import org.joda.time.DateTime
import play.api.libs.json._
import scala.collection.immutable.SortedMap

/**
  * GCValues should be the sole way to represent data within our system.
  * We will try to use them to get rid of the Any, and get better type safety.
  */
sealed trait GCValue {
  def asRoot: RootGCValue = this.asInstanceOf[RootGCValue]
  def value: Any
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
        case v: LeafGCValue => v.value
        case v: ListGCValue => v.value.toList
        case v: RootGCValue => sys.error("RootGCValue not handled yet")
      }
      (key, convertedValue)
  }

  def hasArgFor(name: String): Boolean = map.get(name).isDefined

  def value = sys.error("RootGCValues not implemented yet in GCValueExtractor")
}

case class ListGCValue(values: Vector[GCValue]) extends GCValue {
  def isEmpty: Boolean   = values.isEmpty
  def size: Int          = values.size
  def value: Vector[Any] = values.map(_.value)
}

sealed trait LeafGCValue extends GCValue
object NullGCValue extends LeafGCValue {
  def value = None
}
case class StringGCValue(value: String)     extends LeafGCValue
case class IntGCValue(value: Int)           extends LeafGCValue
case class FloatGCValue(value: Double)      extends LeafGCValue
case class BooleanGCValue(value: Boolean)   extends LeafGCValue
case class IdGCValue(value: String)         extends LeafGCValue
case class DateTimeGCValue(value: DateTime) extends LeafGCValue
case class EnumGCValue(value: String)       extends LeafGCValue
case class JsonGCValue(value: JsValue)      extends LeafGCValue
