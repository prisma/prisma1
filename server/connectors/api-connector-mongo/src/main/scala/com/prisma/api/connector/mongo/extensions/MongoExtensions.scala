package com.prisma.api.connector.mongo.extensions

import com.prisma.api.connector.NodeSelector
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.gc_values._
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Field, Model, RelationField, TypeIdentifier}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonNull, BsonString, BsonTransformer, BsonValue}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters.notEqual
import play.api.libs.json.Json

import scala.collection.mutable

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
  implicit def whereToBson(where: NodeSelector): Bson = {
    val fieldName = if (where.fieldName == "id") "_id" else where.fieldName
    val value     = GCValueBsonTransformer(where.fieldGCValue)

    Filters.eq(fieldName, value)
  }
}

object DocumentToId {
  def toCUIDGCValue(document: Document): IdGCValue = CuidGCValue(document("_id").asString.getValue)
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

      case (false, true) if bison.isString => //Fixme just a hack

        NullGCValue
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
    case (TypeIdentifier.DateTime, value: BsonDateTime) => DateTimeGCValue(new DateTime(value.getValue, DateTimeZone.UTC))
    case (TypeIdentifier.Json, value: BsonString)       => JsonGCValue(Json.parse(value.getValue))
    case (TypeIdentifier.UUID, value: BsonString)       => sys.error("implement this" + value)
    case (_, value: BsonNull)                           => NullGCValue
    case (x, y)                                         => sys.error("Not implemented: " + x + y)
  }
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
      model.scalarListFields.map(field => field.name -> document.get(field.name).map(v => BisonToGC(field, v)).getOrElse(ListGCValue.empty))

    val relationFields: List[(String, GCValue)] =
      model.relationFields.map(field => field.name -> document.get(field.name).map(v => BisonToGC(field, v)).getOrElse(NullGCValue))

    RootGCValue((scalarNonList ++ scalarList ++ relationFields :+ createdAt :+ updatedAt :+ id).toMap)
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

object Path {
  def empty = Path(List.empty)
}

case class Path(segments: List[PathSegment]) {
  def append(rF: RelationField, where: NodeSelector): Path = this.copy(segments = this.segments :+ ToManySegment(rF, where))
  def append(rF: RelationField): Path                      = this.copy(segments = this.segments :+ ToOneSegment(rF))

  def string: String = stringGen(segments).mkString(".")

  private def stringGen(segments: List[PathSegment]): Vector[String] = segments match {
    case Nil                          => Vector.empty
    case ToOneSegment(rf) :: tail     => rf.name +: stringGen(tail)
    case ToManySegment(rf, _) :: tail => rf.name +: stringGen(tail)
  }

  def stringForField(field: String): String = stringGen2(field, segments).mkString(".")

  private def stringGen2(field: String, segments: List[PathSegment]): Vector[String] = segments match {
    case Nil                              => Vector(field)
    case ToOneSegment(rf) :: tail         => rf.name +: stringGen2(field, tail)
    case ToManySegment(rf, where) :: tail => Vector(rf.name, "$[" + operatorName(rf, where) + "]") ++ stringGen2(field, tail)
  }

  def arrayFilter: Vector[Bson] = segments.last match {
    case ToOneSegment(_)          => sys.error("")
    case ToManySegment(rf, where) => Vector(Filters.equal(s"${operatorName(rf, where)}.${where.fieldName}", GCValueBsonTransformer(where.fieldGCValue)))
  }

  def operatorName(field: RelationField, where: NodeSelector) = s"${field.name}X${where.fieldName}X${where.hashCode().toString.replace("-", "M")}"

}

sealed trait PathSegment {
  def rf: RelationField
}

case class ToOneSegment(rf: RelationField)                       extends PathSegment
case class ToManySegment(rf: RelationField, where: NodeSelector) extends PathSegment

object HackforTrue {
  val hackForTrue = notEqual("_id", -1)
}
