package com.prisma.deploy.gc_value

import com.prisma.gc_values._
import com.prisma.shared.models.TypeIdentifier
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import org.apache.commons.lang.StringEscapeUtils
import org.joda.time.{DateTime, DateTimeZone}
import org.parboiled2.{Parser, ParserInput}
import org.scalactic.{Bad, Good, Or}
import play.api.libs.json._
import sangria.ast.{Value => SangriaValue, _}
import sangria.parser._

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
  * String <-> GC Value - This combines the StringSangriaConverter and GCSangriaValueConverter for convenience.
  */
case class GCStringConverter(typeIdentifier: TypeIdentifier, isList: Boolean) extends GCConverter[String] {

  override def toGCValue(t: String): Or[GCValue, InvalidValueForScalarType] =
    for {
      sangriaValue <- StringSangriaValueConverter(typeIdentifier, isList).fromAbleToHandleJsonLists(t)
      result       <- GCSangriaValueConverter(typeIdentifier, isList).toGCValue(sangriaValue)
    } yield result
}

/**
  * SangriaAST <-> GCValue - This is used to transform Sangria parsed values into GCValue and back
  */
private case class GCSangriaValueConverter(typeIdentifier: TypeIdentifier, isList: Boolean) extends GCConverter[SangriaValue] {
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
        case (x: StringValue, TypeIdentifier.Cuid)                                               => StringIdGCValue(x.value)
        case (x: EnumValue, TypeIdentifier.Enum)                                                 => EnumGCValue(x.value)
        case (x: StringValue, TypeIdentifier.Json)                                               => JsonGCValue(Json.parse(x.value))
        case (x: ListValue, _) if isList                                                         => sequence(x.values.map(this.toGCValue)).map(seq => ListGCValue(seq)).get
        case _                                                                                   => sys.error("Error in GCSangriaASTConverter. Value: " + t.renderCompact)
      }

      Good(result)
    } catch {
      case NonFatal(_) => Bad(InvalidValueForScalarType(t.renderCompact, typeIdentifier.code))
    }
  }
}

/**
  * String <-> SangriaAST - This is reads and writes Default values we get/need as String.
  */
private class MyQueryParser(val input: ParserInput)
    extends Parser
    with Tokens
    with Ignored
    with Operations
    with Fragments
    with Values
    with Directives
    with Types

private case class StringSangriaValueConverter(typeIdentifier: TypeIdentifier, isList: Boolean) {
  import OtherGCStuff._

  private def from(string: String): Or[SangriaValue, InvalidValueForScalarType] = {

    val escapedIfNecessary = typeIdentifier match {
      case _ if string == "null"              => string
      case TypeIdentifier.DateTime if !isList => escape(string)
      case TypeIdentifier.String if !isList   => escape(string)
      case TypeIdentifier.Cuid if !isList     => escape(string)
      case TypeIdentifier.Json                => escape(string)
      case _                                  => string
    }

    val parser = new MyQueryParser(ParserInput(escapedIfNecessary))

    parser.Value.run() match {
      case Failure(e) => Bad(InvalidValueForScalarType(string, typeIdentifier.code))
      case Success(x) => Good(x)
    }
  }

  private def escape(str: String): String = "\"" + StringEscapeUtils.escapeJava(str) + "\""

  def fromAbleToHandleJsonLists(string: String): Or[SangriaValue, InvalidValueForScalarType] = {

    if (isList && typeIdentifier == TypeIdentifier.Json) {
      try {
        Json.parse(string) match {
          case JsNull     => Good(NullValue())
          case x: JsArray => sequence(x.value.toVector.map(x => from(x.toString))).map(seq => ListValue(seq))
          case _          => Bad(InvalidValueForScalarType(string, typeIdentifier.code))
        }
      } catch {
        case e: Exception => Bad(InvalidValueForScalarType(string, typeIdentifier.code))
      }
    } else {
      from(string)
    }
  }

}
