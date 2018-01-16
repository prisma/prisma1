package cool.graph.deploy.schema

import org.joda.time.{DateTime, DateTimeZone}
import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation

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

}
