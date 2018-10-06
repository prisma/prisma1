package com.prisma.api.connector.mongo.database

import com.mongodb.MongoClientSettings
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonValue, conversions}
import org.mongodb.scala.model.Updates.combine

case class CombinedPullDefinition(keys: Vector[String], value: BsonValue)

object CustomUpdateCombiner {

  def customCombine(updates: Vector[conversions.Bson]): conversions.Bson = {
    val rawUpdates = updates.map(update => (update.toBsonDocument(classOf[Document], MongoClientSettings.getDefaultCodecRegistry), update))

    val pulls  = rawUpdates.filter(_._1.getFirstKey == "$pull")
    val others = rawUpdates.filter(_._1.getFirstKey != "$pull")

    val convertedPulls                                                    = pulls.map(x => documentToCombinedPullDefinition(x._1))
    val groupedPulls: Map[Vector[String], Vector[CombinedPullDefinition]] = convertedPulls.groupBy(_.keys)

    val changedPulls = groupedPulls.map { group =>
      bsonDocumentFilter(group._1.toList, BsonArray(group._2.map(_.value)))
    }

    combine(others.map(_._2) ++ changedPulls: _*)
  }

  private def bsonDocumentFilter(keys: List[String], array: BsonArray): Document = keys match {
    case Nil          => sys.error("should not happen")
    case head :: Nil  => BsonDocument(head -> BsonDocument("$in" -> array))
    case head :: tail => BsonDocument(head -> bsonDocumentFilter(tail, array))
  }

  private def documentToCombinedPullDefinition(doc: Document, keys: Vector[String] = Vector.empty): CombinedPullDefinition = {
    val key     = doc.keys.head
    val value   = doc.get(key).get
    val newKeys = keys :+ key

    if (value.isDocument) documentToCombinedPullDefinition(value.asDocument(), newKeys) else CombinedPullDefinition(newKeys, value)
  }

}
