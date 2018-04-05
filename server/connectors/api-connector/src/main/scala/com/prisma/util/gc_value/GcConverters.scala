package com.prisma.util.gc_value

import com.prisma.api.connector.{NodeSelector, PrismaArgs}
import com.prisma.gc_values._
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Field, Model, TypeIdentifier}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.scalactic.{Bad, Good, Or}
import play.api.libs.json.{JsValue, _}
import sangria.ast._
import scala.util.control.NonFatal

/**
  * 0. This gets us a GCValue as String or Any without requiring context like field it therefore only works from GCValue
  * Can be made a singleton
  */
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

  def fromGCValueToJson(t: GCValue): JsValue = {

    val formatter = ISODateTimeFormat.dateHourMinuteSecondFraction()

    t match {
      case NullGCValue         => JsNull
      case StringGCValue(x)    => JsString(x)
      case EnumGCValue(x)      => JsString(x)
      case IdGCValue(x)        => JsString(x)
      case DateTimeGCValue(x)  => JsString(formatter.print(x))
      case IntGCValue(x)       => JsNumber(x)
      case FloatGCValue(x)     => JsNumber(x)
      case BooleanGCValue(x)   => JsBoolean(x)
      case JsonGCValue(x)      => x
      case ListGCValue(values) => JsArray(values.map(fromGCValueToJson))
      case RootGCValue(map)    => JsObject(map.map { case (k, v) => (k, fromGCValueToJson(v)) })
    }
  }

  def fromListGCValue(t: ListGCValue): Vector[Any] = t.values.map(fromGCValue)
}

/**
  * 7. Any <-> GCValue - This is used to transform Sangria arguments
  */
case class GCAnyConverter(typeIdentifier: TypeIdentifier, isList: Boolean) extends GCConverter[Any] {
  import OtherGCStuff._
  import play.api.libs.json.{JsArray => PlayJsArray, JsObject => PlayJsObject}
  import spray.json.{JsArray => SprayJsArray, JsObject => SprayJsObject}

  override def toGCValue(t: Any): Or[GCValue, InvalidValueForScalarType] = {
    try {
      val result = (t, typeIdentifier) match {
        case (_: NullValue, _)                                                        => NullGCValue
        case (x: String, _) if x == "null" && typeIdentifier != TypeIdentifier.String => NullGCValue
        case (x: String, TypeIdentifier.String)                                       => StringGCValue(x)
        case (x: Int, TypeIdentifier.Int)                                             => IntGCValue(x.toInt)
        case (x: BigInt, TypeIdentifier.Int)                                          => IntGCValue(x.toInt)
        case (x: BigInt, TypeIdentifier.Float)                                        => FloatGCValue(x.toDouble)
        case (x: BigDecimal, TypeIdentifier.Float)                                    => FloatGCValue(x.toDouble)
        case (x: Float, TypeIdentifier.Float)                                         => FloatGCValue(x)
        case (x: Double, TypeIdentifier.Float)                                        => FloatGCValue(x)
        case (x: Boolean, TypeIdentifier.Boolean)                                     => BooleanGCValue(x)
        case (x: String, TypeIdentifier.DateTime)                                     => DateTimeGCValue(new DateTime(x))
        case (x: DateTime, TypeIdentifier.DateTime)                                   => DateTimeGCValue(x)
        case (x: String, TypeIdentifier.GraphQLID)                                    => IdGCValue(x)
        case (x: String, TypeIdentifier.Enum)                                         => EnumGCValue(x)
        case (x: PlayJsObject, TypeIdentifier.Json)                                   => JsonGCValue(x)
        case (x: SprayJsObject, TypeIdentifier.Json)                                  => JsonGCValue(Json.parse(x.compactPrint))
        case (x: String, TypeIdentifier.Json)                                         => JsonGCValue(Json.parse(x))
        case (x: SprayJsArray, TypeIdentifier.Json)                                   => JsonGCValue(Json.parse(x.compactPrint))
        case (x: PlayJsArray, TypeIdentifier.Json)                                    => JsonGCValue(x)
        case (x: List[Any], _) if isList                                              => sequence(x.map(this.toGCValue).toVector).map(seq => ListGCValue(seq)).get
        case _                                                                        => sys.error("Error in toGCValue. Value: " + t)
      }

      Good(result)
    } catch {
      case NonFatal(_) => Bad(InvalidValueForScalarType(t.toString, typeIdentifier.toString))
    }
  }

  override def fromGCValue(t: GCValue): Any = GCValueExtractor.fromGCValue(t)
}

/**
  * 7. CoolArgs <-> ReallyCoolArgs - This is used to transform from Coolargs for create on a model to typed ReallyCoolArgs
  */
case class GCCreateReallyCoolArgsConverter(model: Model) {

  def toReallyCoolArgs(raw: Map[String, Any]): PrismaArgs = {

    val res = model.scalarNonListFields.map { field =>
      val converter = GCAnyConverter(field.typeIdentifier, false)

      val gCValue = raw.get(field.name) match {
        case Some(Some(x)) => converter.toGCValue(x).get
        case Some(None)    => NullGCValue
        case Some(x)       => converter.toGCValue(x).get
        case None          => NullGCValue
      }
      field.name -> gCValue
    }
    PrismaArgs(RootGCValue(res: _*))
  }

  def toReallyCoolArgsFromJson(json: JsValue): PrismaArgs = {

    def fromSingleJsValue(jsValue: JsValue, field: Field): GCValue = jsValue match {
      case JsString(x)                                                    => StringGCValue(x)
      case JsNumber(x) if field.typeIdentifier == TypeIdentifier.Int      => IntGCValue(x.toInt)
      case JsNumber(x) if field.typeIdentifier == TypeIdentifier.Float    => FloatGCValue(x.toDouble)
      case JsBoolean(x) if field.typeIdentifier == TypeIdentifier.Boolean => BooleanGCValue(x)
      case _                                                              => sys.error("Unhandled JsValue")
    }

    val res = model.scalarNonListFields.map { field =>
      val gCValue: JsLookupResult = json \ field.name
      val asOption                = gCValue.toOption
      val converted = asOption match {
        case None                                                              => NullGCValue
        case Some(JsNull)                                                      => NullGCValue
        case Some(JsString(x))                                                 => StringGCValue(x)
        case Some(JsNumber(x)) if field.typeIdentifier == TypeIdentifier.Int   => IntGCValue(x.toInt)
        case Some(JsNumber(x)) if field.typeIdentifier == TypeIdentifier.Float => FloatGCValue(x.toDouble)
        case Some(JsBoolean(x))                                                => BooleanGCValue(x)
        case Some(JsArray(x)) if field.isList                                  => ListGCValue(x.map(v => fromSingleJsValue(v, field)).toVector)
        case Some(x: JsValue) if field.typeIdentifier == TypeIdentifier.Json   => JsonGCValue(x)
        case x                                                                 => sys.error("Not implemented yet: " + x)

      }
      field.name -> converted
    }
    PrismaArgs(RootGCValue(res: _*))
  }
}

object OtherGCStuff {

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

  /**
    * This is used to parse SQL exceptions for references of specific GCValues
    */
  def parameterString(where: NodeSelector) = where.fieldValue match {
    case StringGCValue(x)      => s"parameters ['$x',"
    case IntGCValue(x)         => s"parameters [$x,"
    case FloatGCValue(x)       => s"parameters [$x,"
    case BooleanGCValue(false) => s"parameters [0,"
    case BooleanGCValue(true)  => s"parameters [1,"
    case IdGCValue(x)          => s"parameters ['$x',"
    case EnumGCValue(x)        => s"parameters ['$x',"
    case DateTimeGCValue(x)    => s"parameters ['${dateTimeFromISO8601(x)}'," // Todo
    case JsonGCValue(x)        => s"parameters ['$x'," // Todo
    case ListGCValue(_)        => sys.error("Not an acceptable Where")
    case RootGCValue(_)        => sys.error("Not an acceptable Where")
    case NullGCValue           => sys.error("Not an acceptable Where")
  }

  private def dateTimeFromISO8601(v: Any) = {
    val string = v.toString
    //"2017-12-05T12:34:23.000Z" to "2017-12-05T12:34:23.000" which MySQL will accept
    string.replace("Z", "")
  }

}
