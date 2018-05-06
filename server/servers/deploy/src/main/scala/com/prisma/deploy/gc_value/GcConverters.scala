package com.prisma.deploy.gc_value

import com.prisma.gc_values._
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Field, TypeIdentifier}
import org.apache.commons.lang.StringEscapeUtils
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.parboiled2.{Parser, ParserInput}
import org.scalactic.{Bad, Good, Or}
import play.api.libs.json._
import sangria.ast.{Value => SangriaValue, _}
import sangria.parser._

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
  * We need a bunch of different converters from / to GC values
  *
  * 1.  DBValue       <->  GCValue     for writing into typed value fields in the Client-DB -> Deploy does not need to write GCValues anymore
  * 2.  SangriaValue  <->  GCValue     for transforming the Any we get from Sangria per field back and forth
  * 3.  DBString      <->  GCValue     for writing default values in the System-DB since they are always a String, and JSArray for Lists
  * 4.  Json          <->  GCValue     for SchemaSerialization
  * 5.  SangriaValue  <->  String      for reading and writing default values
  * 6.  InputString   <->  GCValue     chains String -> SangriaValue -> GCValue and back
  */
/**
  * 2. SangriaAST <-> GCValue - This is used to transform Sangria parsed values into GCValue and back
  */
case class GCSangriaValueConverter(typeIdentifier: TypeIdentifier, isList: Boolean) extends GCConverter[SangriaValue] {
  import OtherGCStuff._

  override def toGCValue(t: SangriaValue): Or[GCValue, InvalidValueForScalarType] = {
    try {
      val result = (t, typeIdentifier) match {
        case (_: NullValue, _)                                                                   => NullGCValue
        case (x: StringValue, _) if x.value == "null" && typeIdentifier != TypeIdentifier.String => NullGCValue
        case (x: StringValue, TypeIdentifier.String)                                             => StringGCValue(x.value)
        case (x: BigIntValue, TypeIdentifier.Int)                                                => IntGCValue(x.value.toInt)
        case (x: BigIntValue, TypeIdentifier.Float)                                              => FloatGCValue(x.value.toDouble)
        case (x: BigDecimalValue, TypeIdentifier.Float)                                          => FloatGCValue(x.value.toDouble)
        case (x: FloatValue, TypeIdentifier.Float)                                               => FloatGCValue(x.value)
        case (x: BooleanValue, TypeIdentifier.Boolean)                                           => BooleanGCValue(x.value)
        case (x: StringValue, TypeIdentifier.DateTime)                                           => DateTimeGCValue(new DateTime(x.value, DateTimeZone.UTC))
        case (x: StringValue, TypeIdentifier.GraphQLID)                                          => IdGCValue(x.value)
        case (x: EnumValue, TypeIdentifier.Enum)                                                 => EnumGCValue(x.value)
        case (x: StringValue, TypeIdentifier.Json)                                               => JsonGCValue(Json.parse(x.value))
        case (x: ListValue, _) if isList                                                         => sequence(x.values.map(this.toGCValue)).map(seq => ListGCValue(seq)).get
        case _                                                                                   => sys.error("Error in GCSangriaASTConverter. Value: " + t.renderCompact)
      }

      Good(result)
    } catch {
      case NonFatal(_) => Bad(InvalidValueForScalarType(t.renderCompact, typeIdentifier.toString))
    }
  }

  override def fromGCValue(gcValue: GCValue): SangriaValue = {

    val formatter = ISODateTimeFormat.dateHourMinuteSecondFraction()

    gcValue match {
      case NullGCValue        => NullValue()
      case x: StringGCValue   => StringValue(value = x.value)
      case x: IntGCValue      => BigIntValue(x.value)
      case x: FloatGCValue    => FloatValue(x.value)
      case x: BooleanGCValue  => BooleanValue(x.value)
      case x: IdGCValue       => StringValue(x.value)
      case x: DateTimeGCValue => StringValue(formatter.print(x.value))
      case x: EnumGCValue     => EnumValue(x.value)
      case x: JsonGCValue     => StringValue(Json.prettyPrint(x.value))
      case x: ListGCValue     => ListValue(values = x.values.map(this.fromGCValue))
      case x: RootGCValue     => sys.error("Default Value cannot be a RootGCValue. Value " + x.toString)
    }
  }
}

/**
  * 5. String <-> SangriaAST - This is reads and writes Default values we get/need as String.
  */
class MyQueryParser(val input: ParserInput) extends Parser with Tokens with Ignored with Operations with Fragments with Values with Directives with Types

case class StringSangriaValueConverter(typeIdentifier: TypeIdentifier, isList: Boolean) {
  import OtherGCStuff._

  def from(string: String): Or[SangriaValue, InvalidValueForScalarType] = {

    val escapedIfNecessary = typeIdentifier match {
      case _ if string == "null"               => string
      case TypeIdentifier.DateTime if !isList  => escape(string)
      case TypeIdentifier.String if !isList    => escape(string)
      case TypeIdentifier.GraphQLID if !isList => escape(string)
      case TypeIdentifier.Json                 => escape(string)
      case _                                   => string
    }

    val parser = new MyQueryParser(ParserInput(escapedIfNecessary))

    parser.Value.run() match {
      case Failure(e) => e.printStackTrace(); Bad(InvalidValueForScalarType(string, typeIdentifier.toString))
      case Success(x) => Good(x)
    }
  }

  def fromAbleToHandleJsonLists(string: String): Or[SangriaValue, InvalidValueForScalarType] = {

    if (isList && typeIdentifier == TypeIdentifier.Json) {
      try {
        Json.parse(string) match {
          case JsNull     => Good(NullValue())
          case x: JsArray => sequence(x.value.toVector.map(x => from(x.toString))).map(seq => ListValue(seq))
          case _          => Bad(InvalidValueForScalarType(string, typeIdentifier.toString))
        }
      } catch {
        case e: Exception => Bad(InvalidValueForScalarType(string, typeIdentifier.toString))
      }
    } else {
      from(string)
    }
  }

  def to(sangriaValue: SangriaValue): String = {
    sangriaValue match {
      case _: NullValue                                          => sangriaValue.renderCompact
      case x: StringValue if !isList                             => unescape(sangriaValue.renderCompact)
      case x: ListValue if typeIdentifier == TypeIdentifier.Json => "[" + x.values.map(y => unescape(y.renderCompact)).mkString(",") + "]"
      case _                                                     => sangriaValue.renderCompact
    }
  }

  private def escape(str: String): String   = "\"" + StringEscapeUtils.escapeJava(str) + "\""
  private def unescape(str: String): String = StringEscapeUtils.unescapeJava(str).stripPrefix("\"").stripSuffix("\"")
}

/**
  * 6. String <-> GC Value - This combines the StringSangriaConverter and GCSangriaValueConverter for convenience.
  */
case class GCStringConverter(typeIdentifier: TypeIdentifier, isList: Boolean) extends GCConverter[String] {

  override def toGCValue(t: String): Or[GCValue, InvalidValueForScalarType] = {

    for {
      sangriaValue <- StringSangriaValueConverter(typeIdentifier, isList).fromAbleToHandleJsonLists(t)
      result       <- GCSangriaValueConverter(typeIdentifier, isList).toGCValue(sangriaValue)
    } yield result
  }

  override def fromGCValue(t: GCValue): String = {
    val sangriaValue = GCSangriaValueConverter(typeIdentifier, isList).fromGCValue(t)
    StringSangriaValueConverter(typeIdentifier, isList).to(sangriaValue)
  }

  def fromGCValueToOptionalString(t: GCValue): Option[String] = {
    t match {
      case NullGCValue => None
      case value       => Some(fromGCValue(value))
    }
  }
}

/**
  * This validates a GCValue against the field it is being used on, for example after an UpdateFieldMutation
  */
object OtherGCStuff {
  def isValidGCValueForField(value: GCValue, field: Field): Boolean = {
    (value, field.typeIdentifier) match {
      case (NullGCValue, _)                              => true
      case (_: StringGCValue, TypeIdentifier.String)     => true
      case (_: IdGCValue, TypeIdentifier.GraphQLID)      => true
      case (_: EnumGCValue, TypeIdentifier.Enum)         => true
      case (_: JsonGCValue, TypeIdentifier.Json)         => true
      case (_: DateTimeGCValue, TypeIdentifier.DateTime) => true
      case (_: IntGCValue, TypeIdentifier.Int)           => true
      case (_: FloatGCValue, TypeIdentifier.Float)       => true
      case (_: BooleanGCValue, TypeIdentifier.Boolean)   => true
      case (x: ListGCValue, _) if field.isList           => x.values.map(isValidGCValueForField(_, field)).forall(identity)
      case (_: RootGCValue, _)                           => false
      case (_, _)                                        => false
    }
  }

  /**
    * This helps convert Or listvalues.
    */
  def sequence[A, B](seq: Vector[Or[A, B]]): Or[Vector[A], B] = {
    def recurse(seq: Vector[Or[A, B]])(acc: Vector[A]): Or[Vector[A], B] = {
      if (seq.isEmpty) {
        Good(acc)
      } else {
        seq.head match {
          case Good(x)    => recurse(seq.tail)(acc :+ x)
          case Bad(error) => Bad(error)
        }
      }
    }
    recurse(seq)(Vector.empty)
  }
}
