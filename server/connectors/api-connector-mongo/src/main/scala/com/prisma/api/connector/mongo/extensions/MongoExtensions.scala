package com.prisma.api.connector.mongo.extensions

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.FilterConditionBuilder
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.gc_values._
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models._
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonNull, BsonString, BsonTransformer, BsonValue}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters.notEqual
import play.api.libs.json.Json

import scala.collection.mutable

object GCBisonTransformer {

  implicit object GCToBson extends BsonTransformer[GCValue] {
    override def apply(value: GCValue): BsonValue = value match {
      case StringGCValue(v)   => BsonString(v)
      case IntGCValue(v)      => BsonInt32(v)
      case FloatGCValue(v)    => BsonDouble(v)
      case JsonGCValue(v)     => BsonString(v.toString())
      case EnumGCValue(v)     => BsonString(v)
      case StringIdGCValue(v) => BsonString(v)
      case UuidGCValue(v)     => BsonString(v.toString)
      case DateTimeGCValue(v) => BsonDateTime(v.getMillis)
      case BooleanGCValue(v)  => BsonBoolean(v)
      case ListGCValue(list)  => BsonArray(list.map(x => GCToBson(x)))
      case NullGCValue        => null
      case _: RootGCValue     => sys.error("not implemented")
    }
  }
}

object NodeSelectorBsonTransformer {
  implicit def whereToBson(where: NodeSelector): Bson = {
    val fieldName = if (where.field.isId) "_id" else where.fieldName
    val value     = GCToBson(where.fieldGCValue)

    Filters.eq(fieldName, value)
  }
}

object DocumentToId {
  def toCUIDGCValue(document: Document): IdGCValue = StringIdGCValue(document("_id").asString.getValue)
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
    case (TypeIdentifier.Cuid, value: BsonString)       => StringIdGCValue(value.getValue)
    case (TypeIdentifier.Boolean, value: BsonBoolean)   => BooleanGCValue(value.getValue)
    case (TypeIdentifier.DateTime, value: BsonDateTime) => DateTimeGCValue(new DateTime(value.getValue, DateTimeZone.UTC))
    case (TypeIdentifier.Json, value: BsonString)       => JsonGCValue(Json.parse(value.getValue))
    case (TypeIdentifier.UUID, value: BsonString)       => sys.error("implement this" + value)
    case (_, value: BsonNull)                           => NullGCValue
    case (x, y)                                         => sys.error("Not implemented: " + x + y)
  }
}

object DocumentToRoot {
  def apply(model: Model, document: Document): RootGCValue = {
    val nonReservedFields = model.scalarNonListFields.filter(_ != model.idField_!)

    val scalarNonList: List[(String, GCValue)] =
      nonReservedFields.map(field => field.name -> document.get(field.name).map(v => BisonToGC(field, v)).getOrElse(NullGCValue))

    val id: (String, GCValue) =
      document.get("_id").map(v => model.idField_!.name -> BisonToGC(model.idField_!, v)).getOrElse(model.idField_!.name -> StringIdGCValue.dummy)

    val scalarList: List[(String, GCValue)] =
      model.scalarListFields.map(field => field.name -> document.get(field.name).map(v => BisonToGC(field, v)).getOrElse(ListGCValue.empty))

    val relationFields: List[(String, GCValue)] = model.relationFields.collect {
      case f if !f.relation.isInlineRelation => f.name -> document.get(f.dbName).map(v => BisonToGC(f, v)).getOrElse(NullGCValue)
    }

    //inline Ids, needs to fetch lists or single values

    val listRelationFieldsWithInlineManifestationOnThisSide = model.relationListFields.filter(f => f.relationIsInlinedInParent && !f.isHidden)

    val nonListRelationFieldsWithInlineManifestationOnThisSide = model.relationNonListFields.filter(f => f.relationIsInlinedInParent && !f.isHidden)

    val singleInlineIds = nonListRelationFieldsWithInlineManifestationOnThisSide.map(f =>
      f.name -> document.get(f.dbName).map(v => BisonToGC(model.idField_!, v)).getOrElse(NullGCValue))

    val listInlineIds = listRelationFieldsWithInlineManifestationOnThisSide.map(f =>
      f.name -> document.get(f.dbName).map(v => BisonToGC(model.idField_!.copy(isList = true), v)).getOrElse(NullGCValue))

//    RootGCValue((scalarNonList ++ scalarList ++ relationFields ++ singleInlineIds ++ listInlineIds ++ createdAt ++ updatedAt :+ id).toMap)
    RootGCValue((scalarNonList ++ scalarList ++ relationFields ++ singleInlineIds ++ listInlineIds :+ id).toMap)
  }
}

object FieldCombinators {
  def combineThree(path: String, relationField: String, field: String): String = {
    path match {
      case ""   => s"$relationField.$field"
      case path => s"$path.$relationField.$field"
    }
  }

  def combineTwo(path: String, relationField: String): String = path match {
    case ""   => relationField
    case path => s"$path.$relationField"
  }
}

object HackforTrue {
  val hackForTrue = notEqual("_id", -1)
}

object ArrayFilter extends FilterConditionBuilder {

  //Fixme: we are using uniques here, but these might change during an update

  def arrayFilter(path: Path): Vector[Bson] = path.segments.lastOption match {
    case None                                       => Vector.empty
    case Some(ToOneSegment(_))                      => Vector.empty
    case Some(ToManySegment(rf, where))             => Vector(Filters.equal(s"${path.operatorName(rf, where)}.${fieldName(where)}", GCToBson(where.fieldGCValue)))
    case Some(ToManyFilterSegment(rf, whereFilter)) => Vector(buildConditionForScalarFilter(path.operatorName(rf, whereFilter), whereFilter))
  }

  def fieldName(where: NodeSelector): String = where.field.isId match {
    case true  => "_id"
    case false => where.field.name
  }
}
