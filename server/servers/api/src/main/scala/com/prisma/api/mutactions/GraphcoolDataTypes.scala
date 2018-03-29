package com.prisma.api.mutactions

import com.prisma.api.schema.APIErrors.ValueNotAValidJson
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Field, TypeIdentifier}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.Try

/**
  * Data can enter Graphcool from several places:
  *  - Sangria (queries and mutations)
  *  - Json (RequestPipelineRunner, Schema Extensions)
  *  - Database (SQL queries)
  *  - Strings (default values, migration values)
  *
  * In all cases we convert to a common data representation.
  *
  * INTERNAL DATA MODEL:
  *
  * UserData: Map[String, Option[Any]]
  * None means an explicit null, omitted input values are also omitted in the map
  *
  * DateTime  => joda.DateTime
  * String    => String
  * Password  => String
  * GraphQLId => String
  * Json      => JsValue
  * Boolean   => Boolean
  * Float     => Double
  * Int       => Int
  * Enum      => String
  *
  * relation  => ?????
  *
  * Scalar lists are immutable.Vector[T] for scalar type T defined above
  *
  *
  * Note: This is still WIP. See https://github.com/graphcool/backend-apis/issues/141
  * In the future we will introduce a case class hierarchy to represent valid internal types
  */
object GraphcoolDataTypes {
  type UserData = Map[String, Option[Any]]

  def fromJson(data: play.api.libs.json.JsObject, fields: List[Field]): UserData = {
    val printedJson = play.api.libs.json.Json.prettyPrint(data)
    val sprayJson   = printedJson.parseJson.asJsObject

    fromJson(sprayJson, fields)
  }

  def fromJson(data: JsObject, fields: List[Field], addNoneValuesForMissingFields: Boolean = false): UserData = {

    def getTypeIdentifier(key: String) = fields.find(_.name == key).map(_.typeIdentifier)
    def isList(key: String)            = fields.find(_.name == key).exists(_.isList)
    def verifyJson(key: String, jsValue: JsValue) = {
      if (!(jsValue.isInstanceOf[JsObject] || jsValue.isInstanceOf[JsArray])) {
        throw ValueNotAValidJson(key, jsValue.prettyPrint)
      }

      jsValue
    }

    // todo: this was only used for request pipeline functions. I didn't have the time to remove the calls yet.
    def handleError[T](fieldName: String, f: () => T): Some[T] = {
      Some(f())
    }

    def isListOfType(key: String, expectedtTypeIdentifier: TypeIdentifier.type => TypeIdentifier) =
      isOfType(key, expectedtTypeIdentifier) && isList(key)
    def isOfType(key: String, expectedtTypeIdentifier: TypeIdentifier.type => TypeIdentifier) =
      getTypeIdentifier(key).contains(expectedtTypeIdentifier(TypeIdentifier))

    def toDateTime(string: String) = DateTime.parse(string, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")).withZone(DateTimeZone.UTC)

    val mappedData = data.fields
      .flatMap({
        // OTHER
        case (key, value) if getTypeIdentifier(key).isEmpty => None
        case (key, value) if value == JsNull                => Some((key, None))

        // SCALAR LISTS
        case (key, value) if isListOfType(key, _.DateTime)  => Some((key, handleError(key, () => value.convertTo[Vector[String]].map(toDateTime))))
        case (key, value) if isListOfType(key, _.String)    => Some((key, handleError(key, () => value.convertTo[Vector[String]])))
        case (key, value) if isListOfType(key, _.GraphQLID) => Some((key, handleError(key, () => value.convertTo[Vector[String]])))
        case (key, value) if isListOfType(key, _.Relation)  => None // consider: recurse
        case (key, value) if isListOfType(key, _.Json)      => Some((key, handleError(key, () => value.convertTo[Vector[JsValue]].map(x => verifyJson(key, x)))))
        case (key, value) if isListOfType(key, _.Boolean)   => Some((key, handleError(key, () => value.convertTo[Vector[Boolean]])))
        case (key, value) if isListOfType(key, _.Float)     => Some((key, handleError(key, () => value.convertTo[Vector[Double]])))
        case (key, value) if isListOfType(key, _.Int)       => Some((key, handleError(key, () => value.convertTo[Vector[Int]])))
        case (key, value) if isListOfType(key, _.Enum)      => Some((key, handleError(key, () => value.convertTo[Vector[String]])))

        // SCALARS
        case (key, value) if isOfType(key, _.DateTime)  => Some((key, handleError(key, () => toDateTime(value.convertTo[String]))))
        case (key, value) if isOfType(key, _.String)    => Some((key, handleError(key, () => value.convertTo[String])))
        case (key, value) if isOfType(key, _.GraphQLID) => Some((key, handleError(key, () => value.convertTo[String])))
        case (key, value) if isOfType(key, _.Relation)  => None // consider: recurse
        case (key, value) if isOfType(key, _.Json)      => Some((key, handleError(key, () => verifyJson(key, value.convertTo[JsValue]))))
        case (key, value) if isOfType(key, _.Boolean)   => Some((key, handleError(key, () => value.convertTo[Boolean])))
        case (key, value) if isOfType(key, _.Float)     => Some((key, handleError(key, () => value.convertTo[Double])))
        case (key, value) if isOfType(key, _.Int)       => Some((key, handleError(key, () => value.convertTo[Int])))
        case (key, value) if isOfType(key, _.Enum)      => Some((key, handleError(key, () => value.convertTo[String])))
      })

    if (addNoneValuesForMissingFields) {
      val missingFields = fields.filter(field => !data.fields.keys.toList.contains(field.name)).map(field => (field.name, None)).toMap

      mappedData ++ missingFields
    } else {
      mappedData
    }
  }

  // todo: tighten this up according to types described above
  // todo: use this in all places and get rid of all AnyJsonFormats
//  def convertToJson(data: UserData): JsObject = {
//    def write(x: Any): JsValue = x match {
//      case m: Map[_, _]   => JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
//      case l: List[Any]   => JsArray(l.map(write).toVector)
//      case l: Vector[Any] => JsArray(l.map(write))
//      case l: Seq[Any]    => JsArray(l.map(write).toVector)
//      case n: Int         => JsNumber(n)
//      case n: Long        => JsNumber(n)
//      case n: BigDecimal  => JsNumber(n)
//      case n: Double      => JsNumber(n)
//      case s: String      => JsString(s)
//      case true           => JsTrue
//      case false          => JsFalse
//      case v: JsValue     => v
//      case null           => JsNull
//      case r              => JsString(r.toString + "00")
//    }
//
//    write(unwrapSomes(data)).asJsObject
//  }

//  def unwrapSomes(map: UserData): Map[String, Any] = {
//    map.map {
//      case (field, Some(value)) => (field, value)
//      case (field, None)        => (field, null)
//    }
//  }
}
