package com.prisma.gc_values

import java.util.UUID

import cool.graph.cuid.Cuid
import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.immutable.SortedMap
import scala.util.Try

/**
  * GCValues should be the sole way to represent data within our system.
  * We will try to use them to get rid of the Any, and get better type safety.
  */
sealed trait GCValue {
  def asRoot: RootGCValue = this.asInstanceOf[RootGCValue]
  def value: Any
}

object RootGCValue {
  def apply(elements: (String, GCValue)*): RootGCValue = RootGCValue(Map(elements: _*))
  def empty: RootGCValue = {
    val empty: Map[String, GCValue] = Map.empty
    RootGCValue(empty)
  }
}

case class RootGCValue(map: Map[String, GCValue]) extends GCValue {

  def idFieldByName(name: String) = map.get(name) match {
    case Some(id) => id.asInstanceOf[IdGCValue]
    case None     => sys.error(s"There was no id field with name '$name'.")
  }

  def filterValues(p: GCValue => Boolean) = copy(map = map.filter(t => p(t._2)))
  def filterKeys(p: String => Boolean)    = copy(map = map.filter(t => p(t._1)))

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

  def hasArgFor(name: String): Boolean   = map.get(name).isDefined
  def add(field: String, value: GCValue) = copy(map = map.updated(field, value))

  def value = sys.error("RootGCValues not implemented yet in GCValueExtractor")
}

object ListGCValue {
  def empty: ListGCValue = ListGCValue(Vector.empty)
}

case class ListGCValue(values: Vector[GCValue]) extends GCValue {
  def isEmpty: Boolean   = values.isEmpty
  def size: Int          = values.size
  def value: Vector[Any] = values.collect { case x if !x.isInstanceOf[RootGCValue] => x.value }

  def ++(other: ListGCValue) = ListGCValue(this.values ++ other.values)
}

sealed trait LeafGCValue extends GCValue

object NullGCValue                          extends LeafGCValue { def value = None }
case class StringGCValue(value: String)     extends LeafGCValue
case class FloatGCValue(value: Double)      extends LeafGCValue
case class BooleanGCValue(value: Boolean)   extends LeafGCValue
case class DateTimeGCValue(value: DateTime) extends LeafGCValue
case class EnumGCValue(value: String)       extends LeafGCValue
case class JsonGCValue(value: JsValue)      extends LeafGCValue

sealed trait IdGCValue                    extends LeafGCValue
case class StringIdGCValue(value: String) extends IdGCValue
case class UuidGCValue(value: UUID)       extends IdGCValue
case class IntGCValue(value: Int)         extends IdGCValue

object UuidGCValue {
  def parse_!(s: String): UuidGCValue    = parse(s).get
  def parse(s: String): Try[UuidGCValue] = Try { UuidGCValue(UUID.fromString(s)) }

  def random: UuidGCValue = UuidGCValue(UUID.randomUUID())
}

object StringIdGCValue {
  def random: StringIdGCValue = StringIdGCValue(Cuid.createCuid())
  def dummy: StringIdGCValue  = StringIdGCValue("")
}
