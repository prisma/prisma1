package com.prisma.api.connector.mysql.database

import com.prisma.api.connector.ScalarListValue
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models._
import spray.json._

import scala.collection.immutable.Seq
import scala.util.{Failure, Success, Try}

case class ResolverListResult(items: Seq[ScalarListValue], hasNextPage: Boolean = false, hasPreviousPage: Boolean = false)

case class DataResolverValidations(f: String, v: Option[Any], model: Model, validate: Boolean) {

  private val field: Field = model.getFieldByName_!(f)

  private def enumOnFieldContainsValue(field: Field, value: Any): Boolean = {
    val enum = field.enum.getOrElse(sys.error("Field should have an Enum"))
    enum.values.contains(value)
  }

  def validateSingleJson(value: String) = {
    def parseJson = Try(value.parseJson) match {
      case Success(json) ⇒ Some(json)
      case Failure(_)    ⇒ if (validate) throw APIErrors.ValueNotAValidJson(f, value) else None
    }
    (f, parseJson)
  }

  def validateSingleBoolean = {
    (f, v.map {
      case v: Boolean => v
      case v: Integer => v == 1
      case v: String  => v.toBoolean
    })
  }

  def validateSingleEnum = {
    val validatedEnum = v match {
      case Some(value) if enumOnFieldContainsValue(field, value) => Some(value)
      case Some(_)                                               => if (validate) throw APIErrors.StoredValueForFieldNotValid(field.name, model.name) else None
      case _                                                     => None
    }
    (f, validatedEnum)
  }

  def validateListEnum = {
    def enumListValueValid(input: Any): Boolean = {
      val inputWithoutWhitespace = input.asInstanceOf[String].replaceAll(" ", "")

      inputWithoutWhitespace match {
        case "[]" =>
          true

        case _ =>
          val values        = inputWithoutWhitespace.stripPrefix("[").stripSuffix("]").split(",")
          val invalidValues = values.collect { case value if !enumOnFieldContainsValue(field, value.stripPrefix("\"").stripSuffix("\"")) => value }
          invalidValues.isEmpty
      }
    }

    val validatedEnumList = v match {
      case Some(x) if enumListValueValid(x) => Some(x)
      case Some(_)                          => if (validate) throw APIErrors.StoredValueForFieldNotValid(field.name, model.name) else None
      case _                                => None
    }
    (f, validatedEnumList)
  }
}
