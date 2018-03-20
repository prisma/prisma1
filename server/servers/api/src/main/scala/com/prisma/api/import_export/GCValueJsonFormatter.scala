package com.prisma.api.import_export

import com.prisma.gc_values._
import com.prisma.shared.models.{Enum, Field, Model, TypeIdentifier}
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import play.api.libs.json._

import scala.collection.immutable
import scala.util.Try

object GCValueJsonFormatter {

  case class UnknownFieldException(field: String, model: Model) extends Exception

  def readModelAwareGcValue(model: Model)(json: JsValue): JsResult[RootGCValue] = {
    for {
      jsObject <- json.validate[JsObject]
      formattedValues = jsObject.fields.toVector.map { tuple =>
        val (key, value) = tuple
        model.getFieldByName(key) match {
          case Some(field) =>
            readLeafGCValueForField(field)(value) match {
              case JsSuccess(gcValue, _) => JsSuccess(key -> gcValue)
              case e: JsError            => e
            }
          case None =>
            throw UnknownFieldException(key, model)
        }
      }
      tuples <- sequenceJsResult(formattedValues)
    } yield {
      RootGCValue(tuples.toMap)
    }
  }

  //  implicit object RootGCValueReads extends Reads[RootGCValue] {
  //    override def reads(json: JsValue) = {}
  //  }

  implicit object StringGCValueReads extends Reads[StringGCValue] {
    override def reads(json: JsValue) = {
      json.validate[JsString] match {
        case JsSuccess(json, _) => JsSuccess(StringGCValue(json.value))
        case e: JsError         => e
      }
    }
  }

  implicit object GraphQLIDValueReads extends Reads[GraphQLIdGCValue] {
    override def reads(json: JsValue) = {
      json.validate[JsString] match {
        case JsSuccess(json, _) => JsSuccess(GraphQLIdGCValue(json.value))
        case e: JsError         => e
      }
    }
  }

  implicit object DateTimeGCValueReads extends Reads[DateTimeGCValue] {
    ISODateTimeFormat.basicDateTime()
    val isoFormatter            = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    val fallbackIsoFormatter    = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
    val secondFallbackFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

    override def reads(json: JsValue) = {
      json.validate[JsString].map { jsString =>
        val dateTime = Try { isoFormatter.parseDateTime(jsString.value) }
          .recover { case _ => fallbackIsoFormatter.parseDateTime(jsString.value) }
          .recover { case _ => secondFallbackFormatter.parseDateTime(jsString.value) }

        DateTimeGCValue(dateTime.get)
      }
    }
  }

  implicit object NullGcValueReads extends Reads[NullGCValue.type] {
    override def reads(json: JsValue) = {
      json match {
        case JsNull => JsSuccess(NullGCValue)
        case x      => JsError(s"Expected JsNull but got $x")
      }
    }
  }

  implicit object IntGCValueReads extends Reads[IntGCValue] {
    override def reads(json: JsValue) = {
      json.validate[JsNumber] match {
        case JsSuccess(json, _) => JsSuccess(IntGCValue(json.value.toInt))
        case e: JsError         => e
      }
    }
  }

  implicit object FloatGCValueReads extends Reads[FloatGCValue] {
    override def reads(json: JsValue) = {
      json.validate[JsNumber] match {
        case JsSuccess(json, _) => JsSuccess(FloatGCValue(json.value.toDouble))
        case e: JsError         => e
      }
    }
  }

  implicit object BooleanGCValueReads extends Reads[BooleanGCValue] {
    override def reads(json: JsValue) = {
      json.validate[JsBoolean] match {
        case JsSuccess(json, _) => JsSuccess(BooleanGCValue(json.value))
        case e: JsError         => e
      }
    }
  }

  implicit object JsonGCValueReads extends Reads[JsonGCValue] {
    override def reads(json: JsValue) = JsSuccess(JsonGCValue(json))
  }

  def readListGCValue(field: Field)(json: JsValue): JsResult[ListGCValue] = {
    require(field.isList)
    for {
      jsArray        <- json.validate[JsArray]
      gcValueResults = jsArray.value.map(element => readLeafGCValueForField(field)(element)).toVector
      gcValues       <- sequenceJsResult(gcValueResults)
    } yield ListGCValue(gcValues)
  }

  def readLeafGCValueForField(field: Field)(json: JsValue): JsResult[LeafGCValue] = {
    field.typeIdentifier match {
      case TypeIdentifier.String    => json.validate[StringGCValue]
      case TypeIdentifier.GraphQLID => json.validate[GraphQLIdGCValue]
      case TypeIdentifier.Enum      => readEnumGCValue(field.enum.get)(json)
      case TypeIdentifier.DateTime  => json.validate[DateTimeGCValue]
      case TypeIdentifier.Boolean   => json.validate[BooleanGCValue]
      case TypeIdentifier.Int       => json.validate[IntGCValue]
      case TypeIdentifier.Float     => json.validate[FloatGCValue]
      case TypeIdentifier.Json      => json.validate[JsonGCValue]
      case TypeIdentifier.Relation  => JsError("TypeIdentifier Relation is not supported here,")
    }
  }

  def readEnumGCValue(enum: Enum)(json: JsValue): JsResult[EnumGCValue] = {
    json.validate[JsString] match {
      case JsSuccess(json, _) if enum.values.contains(json.value) => JsSuccess(EnumGCValue(json.value))
      case JsSuccess(json, _)                                     => JsError(s"${json.value} is not a valid value for Enum ${enum.name}")
      case e: JsError                                             => e
    }
  }

  def sequenceJsResult[T](jsResults: Vector[JsResult[T]]): JsResult[Vector[T]] = {
    val errors = jsResults.collect { case e: JsError => e }
    if (errors.isEmpty) {
      val successes = jsResults.collect { case JsSuccess(value, _) => value }
      JsSuccess(successes)
    } else {
      val combinedJsError = errors.foldLeft(JsError())(JsError.merge)
      combinedJsError
    }
  }
}
