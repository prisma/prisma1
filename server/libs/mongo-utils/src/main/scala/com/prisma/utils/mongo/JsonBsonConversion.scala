package com.prisma.utils.mongo

import com.prisma.utils.json.JsonUtils
import org.joda.time.DateTime
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDateTime, BsonDocument, BsonNull, BsonNumber, BsonObjectId, BsonString, BsonValue}
import play.api.libs.json._

trait JsonBsonConversion extends JsonUtils {
  import collection.JavaConverters._

  implicit def playReadsToDocumentReads[A](playReads: Reads[A]): DocumentReads[A] = new DocumentReads[A] {
    override def reads(document: Document) = {
      val json = bsonToJson(document.toBsonDocument)
      playReads.reads(json) match {
        case JsSuccess(v, _) => DocumentReadSuccess(v)
        case err: JsError    => DocumentReadError(JsError.toJson(err).toString)
      }
    }
  }

  implicit def playWritesToDocumentWrites[A](playWrites: OWrites[A]) = new DocumentWrites[A] {
    override def writes(obj: A) = {
      val json = playWrites.writes(obj)
      Document(jsObjectToBson(json))
    }
  }

  implicit def playFormatToDocumentFormat[A](playFormat: OFormat[A]) = new DocumentFormat[A] {
    override def writes(o: A) = playWritesToDocumentWrites(playFormat).writes(o)

    override def reads(document: Document) = playReadsToDocumentReads(playFormat).reads(document)
  }

  def jsObjectToBson(jsObject: JsObject): BsonDocument = {
    BsonDocument(jsObject.value.mapValues(jsonToBson))
  }

  def jsonToBson(json: JsValue): BsonValue = json match {
    case JsString(v)  => BsonString(v)
    case JsBoolean(v) => BsonBoolean(v)
    case JsNumber(v)  => BsonNumber(v.toDouble)
    case JsNull       => BsonNull()
    case JsArray(v)   => BsonArray(v.map(x => jsonToBson(x)))
    case v: JsObject  => jsObjectToBson(v)
    case x            => sys.error(s"$x not supported here")
  }

  def bsonToJson(bson: BsonValue): JsValue = bson match {
    case v: BsonString   => JsString(v.getValue)
    case v: BsonBoolean  => JsBoolean(v.getValue)
    case v: BsonNumber   => JsNumber(v.decimal128Value().bigDecimalValue())
    case v: BsonObjectId => JsString(v.getValue.toString)
    case _: BsonNull     => JsNull
    case v: BsonArray    => JsArray(v.getValues.asScala.map(bsonToJson))
    case v: BsonDateTime => Json.toJson(new DateTime(v.getValue))
    case v: BsonDocument =>
      val tuples = v.entrySet.asScala.map { entry =>
        entry.getKey -> bsonToJson(entry.getValue)
      }
      JsObject(tuples.toMap)
    case x => sys.error(s"$x not supported here")
  }
}
