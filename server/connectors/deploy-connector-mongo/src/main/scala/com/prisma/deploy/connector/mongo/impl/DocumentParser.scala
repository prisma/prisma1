package com.prisma.deploy.connector.mongo.impl

import java.util.NoSuchElementException

import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDocument, BsonNull, BsonNumber, BsonObjectId, BsonString, BsonValue}
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

//trait DocumentWrites[-A] {
//  def writes(o: A): Document
//}

object DefaultDocumentReads {
  import collection.JavaConverters._

  implicit class DocumentExtensions(val doc: Document) extends AnyVal {
    def readAs[A](implicit reads: DocumentReads[A]): DocumentReadResult[A] = reads.reads(doc)
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

  def jsonToBson(json: JsValue): BsonValue = json match {
    case JsString(v)  => BsonString(v)
    case JsBoolean(v) => BsonBoolean(v)
    case JsNumber(v)  => BsonNumber(v.toDouble)
    case JsNull       => BsonNull()
    case JsArray(v)   => BsonArray(v.map(x => jsonToBson(x)))
    case v: JsObject  => BsonDocument(v.value.mapValues(jsonToBson))
    case x            => sys.error(s"$x not supported here")
  }

  def bsonToJson(bson: BsonValue): JsValue = bson match {
    case v: BsonString   => JsString(v.getValue)
    case v: BsonBoolean  => JsBoolean(v.getValue)
    case v: BsonNumber   => JsNumber(v.decimal128Value().bigDecimalValue())
    case v: BsonObjectId => JsString(v.getValue.toString)
    case _: BsonNull     => JsNull
    case v: BsonArray    => JsArray(v.getValues.asScala.map(bsonToJson))
    case v: BsonDocument =>
      val tuples = v.entrySet.asScala.map { entry =>
        entry.getKey -> bsonToJson(entry.getValue)
      }
      JsObject(tuples.toMap)
    case x => sys.error(s"$x not supported here")
  }
}
