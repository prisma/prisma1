package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.DocumentToRoot
import com.prisma.api.connector.mongo.database.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.api.connector.mongo.database.NodeSelectorBsonTransformer.WhereToBson
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values._
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonString, BsonTransformer, BsonValue}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.{Document, MongoCollection}

import scala.concurrent.{ExecutionContext, Future}

object GCBisonTransformer {

  implicit object GCValueBsonTransformer extends BsonTransformer[GCValue] {
    override def apply(value: GCValue): BsonValue = value match {
      case StringGCValue(v)   => BsonString(v)
      case IntGCValue(v)      => BsonInt32(v)
      case FloatGCValue(v)    => BsonDouble(v)
      case JsonGCValue(v)     => BsonString(v.toString())
      case EnumGCValue(v)     => BsonString(v)
      case CuidGCValue(v)     => BsonString(v)
      case UuidGCValue(v)     => BsonString(v.toString)
      case DateTimeGCValue(v) => BsonDateTime(v.getMillis)
      case BooleanGCValue(v)  => BsonBoolean(v)
      case ListGCValue(list)  => BsonArray(list.map(x => GCValueBsonTransformer(x)))
      case NullGCValue        => null
      case _: RootGCValue     => sys.error("not implemented")
    }
  }

}

object NodeSelectorBsonTransformer {
  implicit object WhereToBson {
    def apply(where: NodeSelector): Bson = {
      val fieldName = if (where.fieldName == "id") "_id" else where.fieldName
      Filters.eq(fieldName, where.fieldGCValue.value)
    }
  }
}

trait NodeActions {

  def createNode(mutaction: CreateNode, includeRelayRow: Boolean)(implicit ec: ExecutionContext): SimpleMongoAction[CreateNodeResult] = SimpleMongoAction {
    database =>
      val collection: MongoCollection[Document] = database.getCollection(mutaction.model.name)
      val id                                    = CuidGCValue.random()

      val nonListValues =
        mutaction.model.scalarNonListFields
          .filter(field => mutaction.nonListArgs.hasArgFor(field) && mutaction.nonListArgs.getFieldValue(field.name).get != NullGCValue)
          .map(field => field.name -> mutaction.nonListArgs.getFieldValue(field).get)

      val document = Document(nonListValues :+ "_id" -> id :+ "createdAt" -> currentDateTimeGCValue :+ "updatedAt" -> currentDateTimeGCValue)

      collection.insertOne(document).toFuture().map(_ => CreateNodeResult(id, mutaction))
  }

  def deleteNode(mutaction: TopLevelDeleteNode)(implicit ec: ExecutionContext): SimpleMongoAction[DeleteNodeResult] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document] = database.getCollection(mutaction.model.name)
    val filter                                = WhereToBson(mutaction.where)

    val previousValues: Future[Option[PrismaNode]] = collection.find(filter).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(mutaction.model, result)
        PrismaNode(root.idField, root)
      }
    }

    previousValues.flatMap {
      case Some(node) => collection.deleteOne(filter).toFuture().map(_ => DeleteNodeResult(node.id, node, mutaction))
      case None       => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
    }
  }

  def updateNode(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext): SimpleMongoAction[UpdateNodeResult] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document] = database.getCollection(mutaction.model.name)
    val filter                                = WhereToBson(mutaction.where)
    val previousValues: Future[Option[PrismaNode]] = collection.find(filter).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(mutaction.model, result)
        PrismaNode(root.idField, root)
      }
    }

    val fieldsSet = mutaction.nonListArgs.raw.asRoot.map.map {
      case ("id", v) => set("_id", GCValueBsonTransformer(v))
      case (k, v)    => set(k, GCValueBsonTransformer(v))
    }.toVector

    val updatedAt = set("updatedAt", GCValueBsonTransformer(currentDateTimeGCValue))

    val updates = combine(fieldsSet :+ updatedAt: _*)

    previousValues.flatMap {
      case Some(node) =>
        collection
          .updateOne(filter, updates)
          .toFuture()
          .map(_ => UpdateNodeResult(node.id, node, mutaction))
      case None => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
    }

  }

  //Fixme the core could do the datetime stuff
  private def currentDateTimeGCValue = DateTimeGCValue(DateTime.now(DateTimeZone.UTC))

}
