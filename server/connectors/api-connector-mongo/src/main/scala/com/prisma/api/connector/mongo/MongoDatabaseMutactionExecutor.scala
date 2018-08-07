package com.prisma.api.connector.mongo

import com.prisma.api.connector._
import com.prisma.gc_values._
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonString, BsonTransformer, BsonValue}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoCollection, MongoDatabase, Observable}

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

class MongoDatabaseMutactionExecutor(database: MongoDatabase)(implicit ec: ExecutionContext) extends DatabaseMutactionExecutor {
  import GCBisonTransformer._

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = {
    interpreterFor(mutaction)
  }

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = ???

  def interpreterFor(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = mutaction match {
    case m: TopLevelCreateNode => CreateNodeInterpreter(mutaction = m, includeRelayRow = false)
    case m: TopLevelUpdateNode => ???
    case m: TopLevelUpsertNode => ???
    case m: TopLevelDeleteNode => DeleteNodeInterpreter(mutaction = m)
    case m: UpdateNodes        => ???
    case m: DeleteNodes        => ???
    case m: ResetData          => ???
    case m: ImportNodes        => ???
    case m: ImportRelations    => ???
    case m: ImportScalarLists  => ???
  }

  def interpreterFor(mutaction: NestedDatabaseMutaction): Observable[_] = mutaction match {
    case m: NestedCreateNode => ???
    case m: NestedUpdateNode => ???
    case m: NestedUpsertNode => ???
    case m: NestedDeleteNode => ???
    case m: NestedConnect    => ???
    case m: NestedDisconnect => ???
  }

  def CreateNodeInterpreter(mutaction: CreateNode, includeRelayRow: Boolean) = {
    val collection: MongoCollection[Document] = database.getCollection(mutaction.model.name)
    val id                                    = CuidGCValue.random()

    val nonListValues =
      mutaction.model.scalarNonListFields
        .filter(field => mutaction.nonListArgs.hasArgFor(field) && mutaction.nonListArgs.getFieldValue(field.name).get != NullGCValue)
        .map(field => field.name -> mutaction.nonListArgs.getFieldValue(field).get)

    val document = Document(nonListValues :+ "_id" -> id)

    collection.insertOne(document).toFuture().map(_ => MutactionResults(CreateNodeResult(id, mutaction), Vector.empty))
  }

  def DeleteNodeInterpreter(mutaction: TopLevelDeleteNode) = {
    val collection: MongoCollection[Document] = database.getCollection(mutaction.model.name)
    val fieldName                             = if (mutaction.where.fieldName == "id") "_id" else mutaction.where.fieldName
    val filter                                = Filters.eq(fieldName, mutaction.where.fieldGCValue.value)

    val previousValues: Future[Option[PrismaNode]] = collection.find(filter).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(mutaction.where.model, result)
        PrismaNode(root.idField, root)
      }
    }

    previousValues.flatMap {
      case Some(node) => collection.deleteOne(filter).toFuture().map(_ => MutactionResults(DeleteNodeResult(node.id, node, mutaction), Vector.empty))
      case None       => sys.error("handle this")
    }
  }
}
