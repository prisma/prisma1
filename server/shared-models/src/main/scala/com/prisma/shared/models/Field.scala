package com.prisma.shared.models

import com.prisma.gc_values.GCValue
import com.prisma.shared.models.Manifestations._

import scala.language.implicitConversions

object RelationSide extends Enumeration {
  type RelationSide = Value
  val A = Value("A")
  val B = Value("B")

  def opposite(side: RelationSide.Value) = if (side == A) B else A
}

object TypeIdentifier extends Enumeration {
  // note: casing of values are chosen to match our TypeIdentifiers
  type TypeIdentifier = Value
  val String    = Value("String")
  val Int       = Value("Int")
  val Float     = Value("Float")
  val Boolean   = Value("Boolean")
  val DateTime  = Value("DateTime")
  val GraphQLID = Value("GraphQLID")
  val Enum      = Value("Enum")
  val Json      = Value("Json")
  val Relation  = Value("Relation")

  def withNameOpt(name: String): Option[TypeIdentifier.Value] = name match {
    case "ID" => Some(GraphQLID)
    case _    => this.values.find(_.toString == name)
  }

  def withNameHacked(name: String) = name match {
    case "ID" => GraphQLID
    case _    => withName(name)
  }
}

case class Enum(
    name: String,
    values: Vector[String] = Vector.empty
)

case class FieldTemplate(
    name: String,
    typeIdentifier: TypeIdentifier.Value,
    isRequired: Boolean,
    isList: Boolean,
    isUnique: Boolean,
    isHidden: Boolean = false,
    isReadonly: Boolean = false,
    enum: Option[Enum],
    defaultValue: Option[GCValue],
    relationName: Option[String],
    relationSide: Option[RelationSide.Value],
    manifestation: Option[FieldManifestation],
    constraints: List[FieldConstraint] = List.empty
) {
  def build(model: Model): Field = new Field(this, model)
}

object Field {
  implicit def asFieldTemplate(field: Field): FieldTemplate = field.template
}
class Field(
    val template: FieldTemplate,
    val model: Model
) {
  import template._

  val schema = model.schema

  lazy val relation: Option[Relation] = relationName.flatMap(schema.getRelationByName)

  lazy val dbName = {
    relation match {
      case Some(r) if r.isInlineRelation => r.manifestation.get.asInstanceOf[InlineRelationManifestation].referencingColumn
      case None                          => manifestation.map(_.dbName).getOrElse(name)
      case _                             => sys.error("not a valid call on relations manifested via a table")
    }
  }
  lazy val isScalar: Boolean                        = typeIdentifier != TypeIdentifier.Relation
  lazy val isRelation: Boolean                      = typeIdentifier == TypeIdentifier.Relation
  lazy val isScalarList: Boolean                    = isScalar && isList
  lazy val isScalarNonList: Boolean                 = isScalar && !isList
  lazy val isRelationList: Boolean                  = isRelation && isList
  lazy val isRelationNonList: Boolean               = isRelation && !isList
  def isRelationWithId(relationId: String): Boolean = relation.exists(_.relationTableName == relationId)

  def isRelationWithIdAndSide(relationId: String, relationSide: RelationSide.Value): Boolean = {
    isRelationWithId(relationId) && this.relationSide.contains(relationSide)
  }

  private val excludedFromMutations = Vector("updatedAt", "createdAt", "id")
  lazy val isWritable: Boolean      = !isReadonly && !excludedFromMutations.contains(name)
  lazy val isVisible: Boolean       = !isHidden

  lazy val oppositeRelationSide: Option[RelationSide.Value] = {
    relationSide match {
      case Some(RelationSide.A) => Some(RelationSide.B)
      case Some(RelationSide.B) => Some(RelationSide.A)
      case x                    => ??? //throw SystemErrors.InvalidStateException(message = s" relationSide was $x")
    }
  }

  lazy val relatedModel: Option[Model] = {
    relation.flatMap(relation => {
      relationSide match {
        case Some(RelationSide.A) => relation.modelB
        case Some(RelationSide.B) => relation.modelA
        case x                    => ??? //throw SystemErrors.InvalidStateException(message = s" relationSide was $x")
      }
    })
  }

  lazy val relatedModel_! : Model = {
    relatedModel match {
      case None        => sys.error(s"Could not find relatedModel for field [$name] on model [$model]")
      case Some(model) => model
    }
  }

  //todo this is dangerous in combination with self relations since it will return the field itself as related field
  //this should be removed where possible
  lazy val relatedField: Option[Field] = {
    val fields = relatedModel_!.fields

    val returnField = fields.find { field =>
      field.relation.exists { relation =>
        val isTheSameField    = field.name == this.name
        val isTheSameRelation = relation.relationTableName == this.relation.get.relationTableName
        isTheSameRelation && !isTheSameField
      }
    }
    val fallback = fields.find { relatedField =>
      relatedField.relation.exists { relation =>
        relation.relationTableName == this.relation.get.relationTableName
      }
    }

    returnField.orElse(fallback)
  }

  //this really does return None if there is no opposite field
  lazy val otherRelationField: Option[Field] = {
    val fields = relatedModel_!.fields

    fields.find { field =>
      field.relation.exists { relation =>
        val isTheSameField    = field.name == this.name
        val isTheSameRelation = relation.relationTableName == this.relation.get.relationTableName
        isTheSameRelation && !isTheSameField
      }
    }
  }
}
