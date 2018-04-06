package com.prisma.api.schema

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import sangria.ast
import sangria.schema._
import sangria.validation.ValueCoercionViolation
import spray.json._

import scala.util.{Failure, Success, Try}

object CustomScalarTypes {

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
        case ast.StringValue(s, _, _, _, _) ⇒ parseDate(s)
        case _                              ⇒ Left(DateCoercionViolation)
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
      case ast.StringValue(jsonStr, _, _, _, _) ⇒ parseJson(jsonStr)
      case _                                    ⇒ Left(JsonCoercionViolation)
    }
  )
}
