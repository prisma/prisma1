package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.DocumentToRoot
import com.prisma.api.connector.mongo.database.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.api.connector.mongo.database.NodeSelectorBsonTransformer.WhereToBson
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values._
import com.prisma.shared.models.RelationField
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonString, BsonTransformer, BsonValue}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.{Document, MongoCollection}

import scala.collection.immutable
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

  def createToDoc(mutaction: CreateNode, results: Vector[DatabaseMutactionResult] = Vector.empty): (Document, Vector[DatabaseMutactionResult]) = {
    val nonListValues: List[(String, GCValue)] =
      mutaction.model.scalarNonListFields
        .filter(field => mutaction.nonListArgs.hasArgFor(field) && mutaction.nonListArgs.getFieldValue(field.name).get != NullGCValue)
        .map(field => field.name -> mutaction.nonListArgs.getFieldValue(field).get)

    val nestedCreates: immutable.Seq[(RelationField, (Document, Vector[DatabaseMutactionResult]))] =
      mutaction.nestedCreates.map(m => m.relationField -> createToDoc(m))

    val childResults = nestedCreates.flatMap(x => x._2._2).toVector

    val grouped: Map[RelationField, immutable.Seq[Document]] = nestedCreates.groupBy(_._1).mapValues(_.map(_._2._1))

    val nestedCreateFields = grouped.foldLeft(Map.empty[String, BsonValue]) { (map, t) =>
      val rf: RelationField = t._1
      val documents         = t._2.map(_.toBsonDocument)

      if (rf.isList) {
        map + (rf.name -> BsonArray(documents))
      } else {
        map + (rf.name -> documents.head)
      }
    }

    val doc = Document(nonListValues ++ mutaction.listArgs) ++ nestedCreateFields

    (doc, childResults)
  }

  def createNode(mutaction: CreateNode, includeRelayRow: Boolean)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection: MongoCollection[Document] = database.getCollection(mutaction.model.dbName)
      val id                                    = CuidGCValue.random()

      val (docWithoutId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(mutaction)
      val docWithId                                                               = Document(docWithoutId.toMap + ("_id" -> GCValueBsonTransformer(id)))

      collection.insertOne(docWithId).toFuture().map(_ => MutactionResults(Vector(CreateNodeResult(id, mutaction)) ++ childResults))
    }

  def createNestedNode(mutaction: NestedCreateNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val parentModel                                                             = mutaction.relationField.model
      val relatedField                                                            = mutaction.relationField
      val filter                                                                  = WhereToBson(NodeSelector.forIdGCValue(parentModel, parentId))
      val collection: MongoCollection[Document]                                   = database.getCollection(parentModel.dbName)
      val (docWithoutId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(mutaction)
      val updates                                                                 = set(relatedField.name, docWithoutId)

      collection
        .updateOne(filter, updates)
        .toFuture()
        .map(_ => MutactionResults(Vector(CreateNodeResult(CuidGCValue.random(), mutaction)) ++ childResults))
    }

  def deleteNode(mutaction: TopLevelDeleteNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document] = database.getCollection(mutaction.model.name)
    val filter                                = WhereToBson(mutaction.where)

    val previousValues: Future[Option[PrismaNode]] = collection.find(filter).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(mutaction.model, result)
        PrismaNode(root.idField, root)
      }
    }

    previousValues.flatMap {
      case Some(node) => collection.deleteOne(filter).toFuture().map(_ => MutactionResults(Vector(DeleteNodeResult(node.id, node, mutaction))))
      case None       => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
    }
  }

  def updateNode(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document] = database.getCollection(mutaction.model.name)
    val filter                                = WhereToBson(mutaction.where)
    val previousValues: Future[Option[PrismaNode]] = collection.find(filter).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(mutaction.model, result)
        PrismaNode(root.idField, root)
      }
    }

    val nonListValues = mutaction.nonListArgs.raw.asRoot.map.map { case (k, v) => set(k, GCValueBsonTransformer(v)) }.toVector

    val listValues = mutaction.listArgs.map { case (f, v) => set(f, v) }

    val updates = combine(nonListValues ++ listValues: _*)

    previousValues.flatMap {
      case Some(node) =>
        collection
          .updateOne(filter, updates)
          .toFuture()
          .map(_ => MutactionResults(Vector(UpdateNodeResult(node.id, node, mutaction))))
      case None => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
    }

  }

  //Fixme the core could do the datetime stuff
  private def currentDateTimeGCValue = DateTimeGCValue(DateTime.now(DateTimeZone.UTC))

}
