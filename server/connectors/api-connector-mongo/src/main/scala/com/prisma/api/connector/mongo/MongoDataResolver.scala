package com.prisma.api.connector.mongo

import java.util

import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models._
import org.bson.BsonString
import org.joda.time.DateTime
import org.mongodb.scala.bson.{BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonNull, BsonValue}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class MongoDataResolver(project: Project, client: MongoClient)(implicit ec: ExecutionContext) extends DataResolver {
  val database = client.getDatabase(project.id)

  override def getModelForGlobalId(globalId: CuidGCValue): Future[Option[Model]] = ???

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(where.model.dbName)

    val fieldName = if (where.fieldName == "id") "_id" else where.fieldName
    val filter    = Filters.eq(fieldName, where.fieldGCValue.value)

    collection.find(filter).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(where.model, result)
        PrismaNode(root.idField, root)
      }
    }
  }

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = ???

  // implement these later
  override def getNodes(model: Model, args: Option[QueryArguments], selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] =
    Future.successful(ResolverResult[PrismaNode](Vector.empty, false, false, None))

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               args: Option[QueryArguments],
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = ???

  override def getScalarListValues(model: Model, listField: ScalarField, args: Option[QueryArguments]): Future[ResolverResult[ScalarListValues]] = ???

  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] = ???

  override def getRelationNodes(relationTableName: String, args: Option[QueryArguments]): Future[ResolverResult[RelationNode]] = ???
}

object DocumentToRoot {
  def apply(model: Model, document: Document): RootGCValue =
    RootGCValue(document.map {
      case ("_id", v)       => "id"        -> BisonToGC(model.getScalarFieldByName_!("id"), v)
      case ("createdAt", v) => "createdAt" -> BisonToGC(TypeIdentifier.DateTime, v)
      case ("updatedAt", v) => "updatedAt" -> BisonToGC(TypeIdentifier.DateTime, v)
      case (k, v)           => k           -> BisonToGC(model.getFieldByName_!(k), v)
    }.toMap)
}

object BisonToGC {
  import scala.collection.JavaConverters._

  def apply(field: Field, bison: BsonValue): GCValue = {
    (field.isList, field.isRelation) match {
      case (true, false) if bison.isArray =>
        val arrayValues: mutable.Seq[BsonValue] = bison.asArray().getValues.asScala
        ListGCValue(arrayValues.map(v => apply(field.typeIdentifier, v)).toVector)

      case (false, false) =>
        apply(field.typeIdentifier, bison)

      case (true, true) if bison.isArray =>
        val arrayValues: mutable.Seq[BsonValue] = bison.asArray().getValues.asScala
        ListGCValue(arrayValues.map(v => DocumentToRoot(field.asInstanceOf[RelationField].relatedModel_!, v.asDocument())).toVector)

      case (false, true) =>
        DocumentToRoot(field.asInstanceOf[RelationField].relatedModel_!, bison.asDocument())
    }
  }

  def apply(typeIdentifier: TypeIdentifier, bison: BsonValue): GCValue = (typeIdentifier, bison) match {
    case (TypeIdentifier.String, value: BsonString)     => StringGCValue(value.getValue)
    case (TypeIdentifier.Int, value: BsonInt32)         => IntGCValue(value.getValue)
    case (TypeIdentifier.Float, value: BsonDouble)      => FloatGCValue(value.getValue)
    case (TypeIdentifier.Enum, value: BsonString)       => EnumGCValue(value.getValue)
    case (TypeIdentifier.Cuid, value: BsonString)       => CuidGCValue(value.getValue)
    case (TypeIdentifier.Boolean, value: BsonBoolean)   => BooleanGCValue(value.getValue)
    case (TypeIdentifier.DateTime, value: BsonDateTime) => DateTimeGCValue(new DateTime(value.getValue))
    case (TypeIdentifier.Json, value: BsonString)       => JsonGCValue(Json.parse(value.getValue))
    case (TypeIdentifier.UUID, value: BsonString)       => sys.error("implement this")
    case (_, value: BsonNull)                           => NullGCValue
    case (x, y)                                         => sys.error("Not implemented: " + x + y)
  }
}
