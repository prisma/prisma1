package cool.graph.shared.gc_values

import org.joda.time.DateTime
import org.scalactic.Or
import play.api.libs.json.JsValue
import _root_.cool.graph.shared.models.TypeIdentifier

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

sealed trait LeafGCValue                    extends GCValue
object NullGCValue                          extends LeafGCValue
case class StringGCValue(value: String)     extends LeafGCValue
case class IntGCValue(value: Int)           extends LeafGCValue
case class FloatGCValue(value: Double)      extends LeafGCValue
case class BooleanGCValue(value: Boolean)   extends LeafGCValue
case class PasswordGCValue(value: String)   extends LeafGCValue
case class GraphQLIdGCValue(value: String)  extends LeafGCValue
case class DateTimeGCValue(value: DateTime) extends LeafGCValue
case class EnumGCValue(value: String)       extends LeafGCValue
case class JsonGCValue(value: JsValue)      extends LeafGCValue

/**
  * We need a bunch of different converters from / to GC values
  *
  * 1.  DBValue       <->  GCValue     for writing into typed value fields in the Client-DB
  * 2.  SangriaValue  <->  GCValue     for transforming the Any we get from Sangria per field back and forth
  * 3.  DBString      <->  GCValue     for writing defaultValues in the System-DB since they are always a String, and JSArray for Lists
  * 4.  Json          <->  GCValue     for SchemaSerialization
  * 5.  SangriaValue  <->  String      for reading and writing default and migrationValues
  * 6.  InputString   <->  GCValue     chains String -> SangriaValue -> GCValue and back
  */
trait GCConverter[T] {
  def toGCValue(t: T): Or[GCValue, InvalidValueForScalarType]
  def fromGCValue(gcValue: GCValue): T
}

case class InvalidValueForScalarType(value: String, typeIdentifier: TypeIdentifier.Value)
