package com.prisma.api.connector.mongo

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.NodeSelectorBsonTransformer.WhereToBson
import com.prisma.gc_values._
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models._
import org.bson.BsonString
import org.joda.time.DateTime
import org.mongodb.scala.bson.{BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonNull, BsonValue}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoClient, MongoCollection}
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class MongoDataResolver(project: Project, client: MongoClient)(implicit ec: ExecutionContext) extends DataResolver {
  val database = client.getDatabase(project.id)

  override def getModelForGlobalId(globalId: CuidGCValue): Future[Option[Model]] = {
    val outer = project.models.map { model =>
      val collection: MongoCollection[Document] = database.getCollection(model.name)
      collection.find(Filters.eq("_id", globalId.value)).collect().toFuture.map { results: Seq[Document] =>
        if (results.nonEmpty) Vector(model) else Vector.empty
      }
    }

    val sequence: Future[List[Vector[Model]]] = Future.sequence(outer)

    sequence.map(_.flatten.headOption)
  }

  //Fixme this does not use selected fields
  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(where.model.dbName)
    collection.find(WhereToBson(where)).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(where.model, result)
        PrismaNode(root.idField, root, Some(where.model.name))
      }
    }
  }

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = database.getCollection(table).countDocuments().toFuture.map(_.toInt)

  // Fixme this does not use filters or selected fields
  override def getNodes(model: Model, args: Option[QueryArguments], selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(model.dbName)

    val nodes: Future[Seq[PrismaNode]] = collection.find().collect().toFuture.map { results: Seq[Document] =>
      results.map { result =>
        val root = DocumentToRoot(model, result)
        PrismaNode(root.idField, root, Some(model.name))
      }
    }
    nodes.map(n => ResolverResult[PrismaNode](n.toVector, false, false, None))
  }

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               args: Option[QueryArguments],
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = ???

  override def getScalarListValues(model: Model, listField: ScalarField, args: Option[QueryArguments]): Future[ResolverResult[ScalarListValues]] = ???

  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] = ???

  override def getRelationNodes(relationTableName: String, args: Option[QueryArguments]): Future[ResolverResult[RelationNode]] = ???
}

object DocumentToRoot {
  def apply(model: Model, document: Document): RootGCValue = {
    val nonReservedFields = model.scalarNonListFields.filter(f => f.name != "createdAt" && f.name != "updatedAt" && f.name != "id")

    val scalarNonList: List[(String, GCValue)] =
      nonReservedFields.map(field => field.name -> document.get(field.name).map(v => BisonToGC(field, v)).getOrElse(NullGCValue))

    val createdAt: (String, GCValue) =
      document.get("createdAt").map(v => "createdAt" -> BisonToGC(TypeIdentifier.DateTime, v)).getOrElse("createdAt" -> NullGCValue)

    val updatedAt: (String, GCValue) =
      document.get("updatedAt").map(v => "updatedAt" -> BisonToGC(TypeIdentifier.DateTime, v)).getOrElse("updatedAt" -> NullGCValue)

    val id: (String, GCValue) = document.get("_id").map(v => "id" -> BisonToGC(model.fields.find(_.name == "id").get, v)).getOrElse("id" -> CuidGCValue.random)

    val scalarList: List[(String, GCValue)] =
      model.scalarListFields.map(field => field.name -> document.get(field.name).map(v => BisonToGC(field, v)).getOrElse(NullGCValue))

    val relationFields: List[(String, GCValue)] =
      model.relationFields.map(field => field.name -> document.get(field.name).map(v => BisonToGC(field, v)).getOrElse(NullGCValue))

    RootGCValue((scalarNonList ++ scalarList ++ relationFields :+ createdAt :+ updatedAt :+ id).toMap)
  }
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
    case (TypeIdentifier.UUID, value: BsonString)       => sys.error("implement this" + value)
    case (_, value: BsonNull)                           => NullGCValue
    case (x, y)                                         => sys.error("Not implemented: " + x + y)
  }
}
