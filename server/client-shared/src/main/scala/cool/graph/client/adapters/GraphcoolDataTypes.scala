package cool.graph.client.adapters

import cool.graph.shared.errors.RequestPipelineErrors.JsonObjectDoesNotMatchGraphQLType
import cool.graph.Types.UserData
import cool.graph.shared.errors.UserAPIErrors.ValueNotAValidJson
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models.{Field, TypeIdentifier}
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

    // todo: this error handling assumes this is only used by functions.
    // this will probably change in the future
    def handleError[T](fieldName: String, f: () => T): Some[T] = {
      try {
        Some(f())
      } catch {
        case e: DeserializationException =>
          val typeIdentifier = getTypeIdentifier(fieldName).getOrElse("UNKNOWN")
          val typeString = if (isList(fieldName)) {
            s"[$typeIdentifier]"
          } else {
            typeIdentifier
          }
          throw JsonObjectDoesNotMatchGraphQLType(fieldName, typeString.toString, data.prettyPrint)
      }
    }

    def isListOfType(key: String, expectedtTypeIdentifier: TypeIdentifier.type => TypeIdentifier) =
      isOfType(key, expectedtTypeIdentifier) && isList(key)
    def isOfType(key: String, expectedtTypeIdentifier: TypeIdentifier.type => TypeIdentifier) =
      getTypeIdentifier(key).contains(expectedtTypeIdentifier(TypeIdentifier))

    def toDateTime(string: String) = new DateTime(string, DateTimeZone.UTC)

    val mappedData = data.fields
      .flatMap({
        // OTHER
        case (key, value) if getTypeIdentifier(key).isEmpty => None
        case (key, value) if value == JsNull                => Some((key, None))

        // SCALAR LISTS
        case (key, value) if isListOfType(key, _.DateTime)  => Some((key, handleError(key, () => value.convertTo[Vector[String]].map(toDateTime))))
        case (key, value) if isListOfType(key, _.String)    => Some((key, handleError(key, () => value.convertTo[Vector[String]])))
        case (key, value) if isListOfType(key, _.Password)  => Some((key, handleError(key, () => value.convertTo[Vector[String]])))
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
        case (key, value) if isOfType(key, _.Password)  => Some((key, handleError(key, () => value.convertTo[String])))
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
  def convertToJson(data: UserData): JsObject = {
    def write(x: Any): JsValue = x match {
      case m: Map[_, _]   => JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
      case l: List[Any]   => JsArray(l.map(write).toVector)
      case l: Vector[Any] => JsArray(l.map(write))
      case l: Seq[Any]    => JsArray(l.map(write).toVector)
      case n: Int         => JsNumber(n)
      case n: Long        => JsNumber(n)
      case n: BigDecimal  => JsNumber(n)
      case n: Double      => JsNumber(n)
      case s: String      => JsString(s)
      case true           => JsTrue
      case false          => JsFalse
      case v: JsValue     => v
      case null           => JsNull
      case r              => JsString(r.toString)
    }

    write(unwrapSomes(data)).asJsObject
  }

  // todo: This should be used as close to db as possible
  // todo: this should replace DataResolver.mapDataItem
  def fromSql(data: UserData, fields: List[Field]): UserData = {

    def typeIdentifier(key: String): Option[TypeIdentifier] = fields.find(_.name == key).map(_.typeIdentifier)
    def isList(key: String): Boolean                        = fields.find(_.name == key).exists(_.isList)
    def verifyIsTopLevelJsonValue(key: String, jsValue: JsValue): JsValue = {
      if (!(jsValue.isInstanceOf[JsObject] || jsValue.isInstanceOf[JsArray])) {
        throw ValueNotAValidJson(key, jsValue.prettyPrint)
      }
      jsValue
    }
    def mapTo[T](value: Any, convert: JsValue => T): Seq[T] = {
      value match {
        case x: String =>
          Try {
            x.parseJson
              .asInstanceOf[JsArray]
              .elements
              .map(convert)
          }.getOrElse(List.empty)
        case x: Vector[_] => x.map(_.asInstanceOf[T])
      }
    }

    try {
      data
        .flatMap({
          // OTHER
          case (key, Some(value)) if typeIdentifier(key).isEmpty => None
          case (key, None)                                       => Some((key, None))

          // SCALAR LISTS
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.DateTime) && isList(key) =>
            Some((key, Some(mapTo(value, x => new DateTime(x.convertTo[JsValue], DateTimeZone.UTC)))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.String) && isList(key) => Some((key, Some(mapTo(value, _.convertTo[String]))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Password) && isList(key) =>
            Some((key, Some(mapTo(value, _.convertTo[String]))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.GraphQLID) && isList(key) =>
            Some((key, Some(mapTo(value, _.convertTo[String]))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Relation) && isList(key) => None // consider: recurse
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Json) && isList(key) =>
            Some((key, Some(mapTo(value, x => verifyIsTopLevelJsonValue(key, x.convertTo[JsValue])))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Boolean) && isList(key) =>
            Some((key, Some(mapTo(value, _.convertTo[Boolean]))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Float) && isList(key) => Some((key, Some(mapTo(value, _.convertTo[Double]))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Int) && isList(key)   => Some((key, Some(mapTo(value, _.convertTo[Int]))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Enum) && isList(key)  => Some((key, Some(mapTo(value, _.convertTo[String]))))

          // SCALARS
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.DateTime) =>
            Some(
              (key, Some(DateTime.parse(value.asInstanceOf[java.sql.Timestamp].toString, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withZoneUTC()))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.String)    => Some((key, Some(value.asInstanceOf[String])))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Password)  => Some((key, Some(value.asInstanceOf[String])))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.GraphQLID) => Some((key, Some(value.asInstanceOf[String])))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Relation)  => None
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Json) =>
            Some((key, Some(verifyIsTopLevelJsonValue(key, value.asInstanceOf[JsValue]))))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Boolean) => Some((key, Some(value.asInstanceOf[Boolean])))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Float)   => Some((key, Some(value.asInstanceOf[Double])))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Int)     => Some((key, Some(value.asInstanceOf[Int])))
          case (key, Some(value)) if typeIdentifier(key).contains(TypeIdentifier.Enum)    => Some((key, Some(value.asInstanceOf[String])))
        })
    } catch {
      case e: DeserializationException => sys.error(s" parsing DataItem from SQL failed: ${e.getMessage}")
    }
  }

  def unwrapSomes(map: UserData): Map[String, Any] = {
    map.map {
      case (field, Some(value)) => (field, value)
      case (field, None)        => (field, null)
    }
  }

  def wrapSomes(map: Map[String, Any]): UserData = {
    map.map {
      case (field, Some(value)) => (field, Some(value))
      case (field, None)        => (field, None)
      case (field, value)       => (field, Some(value))
    }
  }
}
