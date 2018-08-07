package com.prisma.api.connector.mongo

import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models._
import org.bson.BsonString
import org.joda.time.DateTime
import org.mongodb.scala.bson.{BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonValue}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoCollection, MongoDatabase}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

case class MongoDataResolver(project: Project, database: MongoDatabase)(implicit ec: ExecutionContext) extends DataResolver {
  override def getModelForGlobalId(globalId: CuidGCValue): Future[Option[Model]] = ???

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(where.model.name)

    val fieldName = if (where.fieldName == "id") "_id" else where.fieldName
    val filter    = Filters.eq(fieldName, where.fieldGCValue.value)

    collection.find(filter).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(where.model, result)
        PrismaNode(root.idField, root)
      }
    }
  }

  override def getNodes(model: Model, args: Option[QueryArguments], selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = ???

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               args: Option[QueryArguments],
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = ???

  override def getScalarListValues(model: Model, listField: ScalarField, args: Option[QueryArguments]): Future[ResolverResult[ScalarListValues]] = ???

  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] = ???

  override def getRelationNodes(relationTableName: String, args: Option[QueryArguments]): Future[ResolverResult[RelationNode]] = ???

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = ???
}

object DocumentToRoot {
  def apply(model: Model, document: Document): RootGCValue =
    RootGCValue(document.map {
      case ("_id", v) => "id" -> BisonToGC(model.getScalarFieldByName_!("id"), v)
      case (k, v)     => k    -> BisonToGC(model.getScalarFieldByName_!(k), v)
    }.toMap)
}

object BisonToGC {
  def apply(field: Field, bison: BsonValue): GCValue = (field.typeIdentifier, bison) match {
    case (TypeIdentifier.String, value: BsonString)     => StringGCValue(value.getValue)
    case (TypeIdentifier.Int, value: BsonInt32)         => IntGCValue(value.getValue)
    case (TypeIdentifier.Float, value: BsonDouble)      => FloatGCValue(value.getValue)
    case (TypeIdentifier.Enum, value: BsonString)       => EnumGCValue(value.getValue)
    case (TypeIdentifier.Cuid, value: BsonString)       => CuidGCValue(value.getValue)
    case (TypeIdentifier.Boolean, value: BsonBoolean)   => BooleanGCValue(value.getValue)
    case (TypeIdentifier.DateTime, value: BsonDateTime) => DateTimeGCValue(new DateTime(value.getValue))
    case (TypeIdentifier.Json, value: BsonString)       => JsonGCValue(Json.parse(value.getValue))
    case (TypeIdentifier.UUID, value: BsonString)       => sys.error("implement this")
    case (x, y)                                         => sys.error("Not implemented: " + x + y)
  }
}
