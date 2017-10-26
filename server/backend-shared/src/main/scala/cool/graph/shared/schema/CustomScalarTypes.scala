package cool.graph.shared.schema

import cool.graph.shared.models.{Field, TypeIdentifier}
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.util.crypto.Crypto
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import sangria.ast
import sangria.schema._
import sangria.validation.{StringCoercionViolation, ValueCoercionViolation}
import spray.json._

import scala.util.{Failure, Success, Try}

object CustomScalarTypes {

  val PasswordType = ScalarType[String](
    "Password",
    description = Some("Values of type password are stored safely."),
    coerceOutput = valueOutput,
    coerceUserInput = {
      case s: String ⇒ Right(Crypto.hash(s))
      case _         ⇒ Left(StringCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _) ⇒ Right(Crypto.hash(s))
      case _                        ⇒ Left(StringCoercionViolation)
    }
  )

  case object DateCoercionViolation extends ValueCoercionViolation("Date value expected")

  def parseDate(s: String) = Try(new DateTime(s, DateTimeZone.UTC)) match {
    case Success(date) ⇒ Right(date)
    case Failure(_)    ⇒ Left(DateCoercionViolation)
  }

  val DateTimeType =
    ScalarType[DateTime](
      "DateTime",
      coerceOutput = (d, caps) => {
        d.toDateTime
      },
      coerceUserInput = {
        case s: String ⇒ parseDate(s)
        case _         ⇒ Left(DateCoercionViolation)
      },
      coerceInput = {
        case ast.StringValue(s, _, _) ⇒ parseDate(s)
        case _                        ⇒ Left(DateCoercionViolation)
      }
    )

  case object JsonCoercionViolation extends ValueCoercionViolation("Not valid JSON")

  def parseJson(s: String) = Try(s.parseJson) match {
    case Success(json) ⇒ Right(json)
    case Failure(_)    ⇒ Left(JsonCoercionViolation)
  }

  val JsonType = ScalarType[JsValue](
    "Json",
    description = Some("Raw JSON value"),
    coerceOutput = (value, _) ⇒ value,
    coerceUserInput = {
      case v: String     ⇒ Right(JsString(v))
      case v: Boolean    ⇒ Right(JsBoolean(v))
      case v: Int        ⇒ Right(JsNumber(v))
      case v: Long       ⇒ Right(JsNumber(v))
      case v: Float      ⇒ Right(JsNumber(v))
      case v: Double     ⇒ Right(JsNumber(v))
      case v: BigInt     ⇒ Right(JsNumber(v))
      case v: BigDecimal ⇒ Right(JsNumber(v))
      case v: DateTime ⇒
        Right(
          JsString(
            v.toString(DateTimeFormat
              .forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z")
              .withZoneUTC())))
      case v: JsValue ⇒ Right(v)
    },
    coerceInput = {
      case ast.StringValue(jsonStr, _, _) ⇒ parseJson(jsonStr)
      case _                              ⇒ Left(JsonCoercionViolation)
    }
  )

  def isScalar(typeIdentifier: TypeIdentifier.TypeIdentifier) = typeIdentifier != TypeIdentifier.Relation

  def isScalar(typeIdentifier: String) = TypeIdentifier.values.map(_.toString).contains(typeIdentifier)

  def parseValueFromString(value: String, typeIdentifier: TypeIdentifier, isList: Boolean): Option[Any] = {

    def parseOne(value: String): Option[Any] =
      try {
        typeIdentifier match {
          case TypeIdentifier.String    => Some(value)
          case TypeIdentifier.Int       => Some(Integer.parseInt(value))
          case TypeIdentifier.Float     => Some((if (value == null) { "0" } else { value }).toDouble)
          case TypeIdentifier.Boolean   => Some(value.toBoolean)
          case TypeIdentifier.Password  => Some(value)
          case TypeIdentifier.DateTime  => Some(new DateTime(value, DateTimeZone.UTC))
          case TypeIdentifier.GraphQLID => Some(value)
          case TypeIdentifier.Enum      => Some(value)
          case TypeIdentifier.Json      => Some(value.parseJson)
          case _                        => None
        }
      } catch {
        case e: Exception => None
      }

    if (isList) {
      var elements: Option[Vector[Option[Any]]] = None

      def trySplitting(function: => Option[Vector[Option[Any]]]) = {
        elements = try { function } catch { case e: Exception => None }
      }

      def stripBrackets = {
        if (!value.startsWith("[") || !value.endsWith("]")) { throw new Exception() }
        value.stripPrefix("[").stripSuffix("]").split(",").map(_.trim()).to[Vector]
      }

      def stripQuotes(x: String) = {
        if (!x.startsWith("\"") || !x.endsWith("\"")) { throw new Exception() }
        x.stripPrefix("\"").stripSuffix("\"")
      }

      def dateTimeList = { Some(stripBrackets.map(x => stripQuotes(x)).map(e => parseOne(e))) }
      def stringList   = { Some(stripBrackets.map(x => stripQuotes(x)).map(e => parseOne(e))) }
      def enumList     = { Some(stripBrackets.map(e => parseOne(e))) }
      def otherList    = { Some(value.parseJson.asInstanceOf[JsArray].elements.map(e => parseOne(e.toString()))) }

      if (value.replace(" ", "") == "[]") {
        return Some(value)
      } else {
        typeIdentifier match {
          case TypeIdentifier.DateTime => trySplitting(dateTimeList)
          case TypeIdentifier.String   => trySplitting(stringList)
          case TypeIdentifier.Enum     => trySplitting(enumList)
          case _                       => trySplitting(otherList)
        }
      }

      if (elements.isEmpty || elements.get.exists(_.isEmpty)) {
        None
      } else {
        Some(elements.map(_ collect { case Some(x) => x }))
      }
    } else {
      parseOne(value)
    }
  }

  def isValidScalarType(value: String, field: Field) = parseValueFromString(value, field.typeIdentifier, field.isList).isDefined

  def parseTypeIdentifier(typeIdentifier: String) =
    TypeIdentifier.values.map(_.toString).contains(typeIdentifier) match {
      case true  => TypeIdentifier.withName(typeIdentifier)
      case false => TypeIdentifier.Relation
    }
}
