package com.prisma.deploy.connector.mongo.database

import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import play.api.libs.json._

object MongoCodecRegistry {

//  def jsonToBson(json: JsValue): BsonValue = json match {
//    case JsString(v) => BsonString(v)
//    case JsArray(v)  => BsonArray(v.map(x => jsonToBson(x)))
//    case v: JsObject => BsonDocument(v.value.mapValues(jsonToBson))
//    case x           => sys.error(s"$x not supported here")
//  }
//
//  def bsonToJson(bson: BsonValue): JsValue = bson match {
//    case v: BsonString => JsString(v.getValue)
//    case v: BsonArray  => JsArray(v.getValues.asScala.map(bsonToJson))
//    case v: BsonDocument =>
//      val tuples = v.entrySet.asScala.map { entry =>
//        entry.getKey -> bsonToJson(entry.getValue)
//      }
//      JsObject(tuples.toMap)
//    case x => sys.error(s"$x not supported here")
//  }
//
  val jsonCodec = new Codec[JsValue] {
    override def decode(reader: BsonReader, decoderContext: DecoderContext) = {
      val str = reader.readString()
      Json.parse(str)
    }

    override def encode(writer: BsonWriter, value: JsValue, encoderContext: EncoderContext) = {
//      val bson    = jsonToBson(value)
//      val encoder = new BasicBSONEncoder()
//      encoder.encode(bson)
//      val writer = new JsonWriter(value.toString())
      writer.writeString(value.toString())
    }

    override def getEncoderClass = classOf[JsValue]
  }

  val jsonCodecProvider = new CodecProvider {
    override def get[T](clazz: Class[T], registry: CodecRegistry) = {
      if (classOf[JsValue].isAssignableFrom(clazz)) jsonCodec.asInstanceOf[Codec[T]] else null
    }
  }

  val dateTimeCodec = new Codec[DateTime] {
    override def decode(reader: BsonReader, decoderContext: DecoderContext) = {
      new DateTime(reader.readDateTime())
    }

    override def encode(writer: BsonWriter, value: DateTime, encoderContext: EncoderContext) = {
      writer.writeDateTime(value.getMillis)
    }

    override def getEncoderClass = classOf[DateTime]
  }

  val dateTimeCodecProvider = new CodecProvider {
    override def get[T](clazz: Class[T], registry: CodecRegistry) = {
      if (classOf[DateTime].isAssignableFrom(clazz)) {
        dateTimeCodec.asInstanceOf[Codec[T]]
      } else {
        null
      }
    }
  }

  val codecRegistry = fromRegistries(
    fromProviders(dateTimeCodecProvider, jsonCodecProvider, classOf[MigrationDefinition], classOf[ProjectDefinition]),
    DEFAULT_CODEC_REGISTRY
  )

}
