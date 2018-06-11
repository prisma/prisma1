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

object TypeIdentifier {
  sealed trait TypeIdentifier {
    def code: String
  }

  object Relation extends TypeIdentifier { def code = "Relation" }

  sealed trait ScalarTypeIdentifier extends TypeIdentifier
  object String                     extends ScalarTypeIdentifier { def code = "String" }
  object Int                        extends ScalarTypeIdentifier { def code = "Int" }
  object Float                      extends ScalarTypeIdentifier { def code = "Float" }
  object Boolean                    extends ScalarTypeIdentifier { def code = "Boolean" }
  object Enum                       extends ScalarTypeIdentifier { def code = "Enum" }
  object Json                       extends ScalarTypeIdentifier { def code = "Json" }
  object DateTime                   extends ScalarTypeIdentifier { def code = "DateTime" }
  object GraphQLID                  extends ScalarTypeIdentifier { def code = "GraphQLID" }
  object UUID                       extends ScalarTypeIdentifier { def code = "UUID" }

  // compatibility with Enumeration interface
  type Value = TypeIdentifier

  private val instances = Vector(Relation, String, Int, Float, Boolean, Enum, Json, DateTime, GraphQLID)

  def withNameHacked(name: String) = name match {
    case "ID" => GraphQLID
    case _    => withName(name)
  }
  def withName(name: String): TypeIdentifier            = withNameOpt(name).getOrElse(throw new NoSuchElementException(s"No value found for '$name'"))
  def withNameOpt(name: String): Option[TypeIdentifier] = instances.find(_.code == name)

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
    manifestation: Option[FieldManifestation]
) {
  def build(model: Model): Field = {
    typeIdentifier match {
      case TypeIdentifier.Relation =>
        RelationField(
          name = name,
          isRequired = isRequired,
          isList = isList,
          isHidden = isHidden,
          relationName = relationName.get,
          relationSide = relationSide.get,
          template = this,
          model = model
        )
      case ti: TypeIdentifier.ScalarTypeIdentifier =>
        ScalarField(
          name = name,
          typeIdentifier = ti,
          isRequired = isRequired,
          isList = isList,
          isUnique = isUnique,
          isHidden = isHidden,
          isReadonly = isReadonly,
          enum = enum,
          defaultValue = defaultValue,
          manifestation = manifestation,
          template = this,
          model = model
        )

    }
  }
}

object Field {
  val magicalBackRelationPrefix     = "_MagicalBackRelation_"
  private val excludedFromMutations = Vector("updatedAt", "createdAt", "id")
}

sealed trait Field {
  def name: String
  def typeIdentifier: TypeIdentifier.Value
  def isRequired: Boolean
  def isList: Boolean
  def isUnique: Boolean
  def isHidden: Boolean
  def isReadonly: Boolean
  def enum: Option[Enum]
  def defaultValue: Option[GCValue]
  def relationOpt: Option[Relation]
  def model: Model
  def schema: Schema
  def template: FieldTemplate
  def isRelation: Boolean
  def isScalar: Boolean
  def dbName: String

  lazy val isScalarList: Boolean      = isScalar && isList
  lazy val isScalarNonList: Boolean   = isScalar && !isList
  lazy val isRelationList: Boolean    = isRelation && isList
  lazy val isRelationNonList: Boolean = isRelation && !isList
  lazy val isWritable: Boolean        = !isReadonly && !Field.excludedFromMutations.contains(name)
  lazy val isVisible: Boolean         = !isHidden

  val isMagicalBackRelation = name.startsWith(Field.magicalBackRelationPrefix)
}

case class RelationField(
    name: String,
    isRequired: Boolean,
    isList: Boolean,
    isHidden: Boolean,
    relationName: String,
    relationSide: RelationSide.Value,
    template: FieldTemplate,
    model: Model
) extends Field {
  override def typeIdentifier = TypeIdentifier.Relation
  override def isRelation     = true
  override def isScalar       = false
  override def isUnique       = false
  override def isReadonly     = false
  override def enum           = None
  override def defaultValue   = None
  override def schema         = model.schema

  lazy val dbName = relation.manifestation match {
    case Some(m: InlineRelationManifestation) => m.referencingColumn
    case _                                    => sys.error("not a valid call on relations manifested via a table")
  }

  lazy val relation: Relation            = schema.getRelationByName_!(relationName)
  lazy val relationOpt: Option[Relation] = Some(relation)

  lazy val relatedModel_! : Model = {
    relationSide match {
      case RelationSide.A => relation.modelB
      case RelationSide.B => relation.modelA
      case x              => sys.error(s"received invalid relation side $x")
    }
  }

  lazy val relatedField: RelationField = {
    relatedModel_!.relationFields.find { field =>
      val relation          = field.relation
      val isTheSameField    = field.name == this.name
      val isTheSameRelation = relation.relationTableName == this.relation.relationTableName
      isTheSameRelation && !isTheSameField
    }.get
  }

  lazy val oppositeRelationSide: RelationSide.Value = {
    relationSide match {
      case RelationSide.A => RelationSide.B
      case RelationSide.B => RelationSide.A
      case x              => sys.error(s"received invalid relation side $x")
    }
  }

  def isRelationWithNameAndSide(relationName: String, side: RelationSide.Value): Boolean = relation.name == relationName && this.relationSide == side
}

case class ScalarField(
    name: String,
    typeIdentifier: TypeIdentifier.ScalarTypeIdentifier,
    isRequired: Boolean,
    isList: Boolean,
    isUnique: Boolean,
    isHidden: Boolean,
    isReadonly: Boolean,
    enum: Option[Enum],
    defaultValue: Option[GCValue],
    manifestation: Option[FieldManifestation],
    template: FieldTemplate,
    model: Model
) extends Field {
  override def isRelation  = false
  override def isScalar    = true
  override def relationOpt = None
  override val dbName      = manifestation.map(_.dbName).getOrElse(name)

  override def schema = model.schema
}
