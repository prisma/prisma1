package com.prisma.api.import_export

import com.prisma.gc_values._
import com.prisma.shared.models.{Enum, FieldBehaviour, Model, ScalarField, TypeIdentifier}
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

import scala.collection.immutable.SortedMap
import scala.util.{Failure, Success, Try}

object GCValueJsonFormatter {

  /**
    * WRITERS
    */
  implicit object GcValueWrites extends Writes[GCValue] {
    val isoFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    override def writes(gcValue: GCValue): JsValue = gcValue match {
      case v: StringGCValue   => JsString(v.value)
      case v: StringIdGCValue => JsString(v.value)
      case v: UuidGCValue     => JsString(v.value.toString)
      case v: EnumGCValue     => JsString(v.value)
      case v: DateTimeGCValue => JsString(isoFormatter.print(v.value.withZone(DateTimeZone.UTC)))
      case v: BooleanGCValue  => JsBoolean(v.value)
      case v: IntGCValue      => JsNumber(v.value)
      case v: FloatGCValue    => JsNumber(v.value)
      case v: JsonGCValue     => v.value
      case NullGCValue        => JsNull
      case v: ListGCValue     => JsArray(v.values.map(writes))
      case v: RootGCValue     => JsObject(v.map.mapValues(writes))
    }
  }

  implicit object RootGcValueWrites extends OWrites[RootGCValue] {
    override def writes(v: RootGCValue): JsObject = JsObject(v.map.mapValues(GcValueWrites.writes))
  }

  implicit object RootGcValueWritesWithoutNulls extends OWrites[RootGCValue] {
    override def writes(v: RootGCValue) = {
      val withoutNulls = v.filterValues(_ != NullGCValue)
      RootGcValueWrites.writes(withoutNulls)
    }
  }

  /**
    * READERS
    */
  case class UnknownFieldException(field: String, model: Model)      extends Exception
  case class InvalidFieldValueException(field: String, model: Model) extends Exception

  def readModelAwareGcValue(model: Model)(json: JsValue): JsResult[RootGCValue] = {

    //filter out createdAt, updatedAt if there is no such field on the model
    def filterCreatedAtUpdatedAt(tuple: (String, JsValue)): Boolean = tuple._1 match {
      case "createdAt" if model.fields.exists(_.behaviour.contains(FieldBehaviour.CreatedAtBehaviour))                   => true
      case "createdAt" if model.fields.exists(x => x.name == "createdAt" && x.typeIdentifier == TypeIdentifier.DateTime) => true
      case "updatedAt" if model.fields.exists(_.behaviour.contains(FieldBehaviour.UpdatedAtBehaviour))                   => true
      case "updatedAt" if model.fields.exists(x => x.name == "updatedAt" && x.typeIdentifier == TypeIdentifier.DateTime) => true
      case "updatedAt" | "createdAt"                                                                                     => false
      case _                                                                                                             => true

    }

    for {
      jsObject       <- json.validate[JsObject]
      filteredTuples = jsObject.fields.toVector.filter(filterCreatedAtUpdatedAt)
      formattedValues = filteredTuples.map { tuple =>
        val (key, value) = tuple
        model.getScalarFieldByName(key) match {
          case Some(field) =>
            readLeafGCValueForField(field)(value) match {
              case JsSuccess(gcValue, _) => JsSuccess(key -> gcValue)
              case e: JsError            => throw InvalidFieldValueException(key, model)
            }
          case None =>
            throw UnknownFieldException(key, model)
        }
      }
      tuples <- sequenceJsResult(formattedValues)
    } yield {
      RootGCValue(SortedMap(tuples: _*))
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

  implicit object GraphQLIDValueReads extends Reads[StringIdGCValue] {
    override def reads(json: JsValue) = {
      json.validate[JsString] match {
        case JsSuccess(json, _) => JsSuccess(StringIdGCValue(json.value))
        case e: JsError         => e
      }
    }
  }

  implicit object UUIDValueReads extends Reads[UuidGCValue] {
    override def reads(json: JsValue) = {
      json.validate[JsString] match {
        case JsSuccess(json, _) =>
          UuidGCValue.parse(json.value) match {
            case Success(x) => JsSuccess(x)
            case Failure(e) => JsError(e.getMessage)
          }
        case e: JsError => e
      }
    }
  }

  implicit object DateTimeGCValueReads extends Reads[DateTimeGCValue] {
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

  def readListGCValue(field: ScalarField)(json: JsValue): JsResult[ListGCValue] = {
    require(field.isList)
    for {
      jsArray        <- json.validate[JsArray]
      gcValueResults = jsArray.value.map(element => readLeafGCValueForField(field)(element)).toVector
      gcValues       <- sequenceJsResult(gcValueResults)
    } yield ListGCValue(gcValues)
  }

  def readLeafGCValueForField(field: ScalarField)(json: JsValue): JsResult[LeafGCValue] = {
    field.typeIdentifier match {
      case TypeIdentifier.String   => json.validate[StringGCValue]
      case TypeIdentifier.Cuid     => json.validate[StringIdGCValue]
      case TypeIdentifier.UUID     => json.validate[UuidGCValue]
      case TypeIdentifier.Enum     => readEnumGCValue(field.enum.get)(json)
      case TypeIdentifier.DateTime => json.validate[DateTimeGCValue]
      case TypeIdentifier.Boolean  => json.validate[BooleanGCValue]
      case TypeIdentifier.Int      => json.validate[IntGCValue]
      case TypeIdentifier.Float    => json.validate[FloatGCValue]
      case TypeIdentifier.Json     => json.validate[JsonGCValue]
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
