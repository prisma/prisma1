package com.prisma.deploy.connector.mongo.impl

import java.util.NoSuchElementException

import org.joda.time.DateTime
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDateTime, BsonDocument, BsonNull, BsonNumber, BsonObjectId, BsonString, BsonValue}
import play.api.libs.json._

sealed trait DocumentReadResult[+A] {
  def get: A
}
case class DocumentReadSuccess[A](value: A) extends DocumentReadResult[A] {
  def get = value
}
case class DocumentReadError(msg: String) extends DocumentReadResult[Nothing] {
  def get = throw new NoSuchElementException(msg)
}

trait DocumentReads[A] {
  def reads(document: Document): DocumentReadResult[A]
}

trait DocumentWrites[-A] {
  def writes(o: A): Document
}

trait DocumentFormat[A] extends DocumentReads[A] with DocumentWrites[A]

object DefaultDocumentReads {
  import com.prisma.utils.json.JsonUtils._

  import collection.JavaConverters._

  implicit class DocumentExtensions(val doc: Document) extends AnyVal {
    def readAs[A](implicit reads: DocumentReads[A]): DocumentReadResult[A] = reads.reads(doc)
    def as[A](implicit reads: DocumentReads[A]): A                         = readAs[A].get
  }

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
