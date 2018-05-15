package com.prisma.deploy.migration.validation

import com.prisma.deploy.migration.DirectiveTypes.RelationDBDirective
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.OnDelete.OnDelete
import com.prisma.shared.models.TypeIdentifier
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier

case class PrismaSdl(typesFn: Vector[PrismaSdl => PrismaType], enumsFn: Vector[PrismaSdl => PrismaEnum]) {
  val types: Vector[PrismaType] = typesFn.map(_.apply(this))
  val enums: Vector[PrismaEnum] = enumsFn.map(_.apply(this))
}

case class PrismaType(name: String, tableName: Option[String], fieldFn: Vector[PrismaType => PrismaField])(val sdl: PrismaSdl) {
  val fields: Vector[PrismaField] = fieldFn.map(_.apply(this))

  val relationalPrismaFields = fields.collect { case x: RelationalPrismaField => x }
  val nonRelationalPrismaFields = fields.collect {
    case x: EnumPrismaField   => x
    case y: ScalarPrismaField => y
  }

  def tableName_! = tableName.getOrElse(name)
}

object Foo {

  val fld: PrismaType => PrismaField =
    EnumPrismaField("MyEnumField", columnName = None, isList = false, isRequired = false, isUnique = false, enumName = "Enum", defaultValue = None)
  val tpe: PrismaSdl => PrismaType = PrismaType("MyType", Some("table"), Vector(fld))
  val enm: PrismaSdl => PrismaEnum = PrismaEnum("MyEnum", Vector("A"))
  val sdl                          = PrismaSdl(Vector(tpe), Vector(enm))

}

sealed trait PrismaField {
  def name: String
  def typeIdentifier: TypeIdentifier
  def isList: Boolean
  def isRequired: Boolean
  def tpe: PrismaType
}

case class ScalarPrismaField(name: String,
                             columnName: Option[String],
                             isList: Boolean,
                             isRequired: Boolean,
                             isUnique: Boolean,
                             typeIdentifier: TypeIdentifier,
                             defaultValue: Option[GCValue])(val tpe: PrismaType)
    extends PrismaField

case class EnumPrismaField(name: String,
                           columnName: Option[String],
                           isList: Boolean,
                           isRequired: Boolean,
                           isUnique: Boolean,
                           enumName: String,
                           defaultValue: Option[GCValue])(val tpe: PrismaType)
    extends PrismaField {
  override def typeIdentifier: TypeIdentifier = TypeIdentifier.Enum
}

case class RelationalPrismaField(name: String,
                                 inlineDirectiveColumn: Option[RelationDBDirective],
                                 isList: Boolean,
                                 isRequired: Boolean,
                                 referencesType: String,
                                 relationName: Option[String],
                                 cascade: OnDelete)(val tpe: PrismaType)
    extends PrismaField {
  override def typeIdentifier: TypeIdentifier = TypeIdentifier.Relation

  def relatedField: Option[RelationalPrismaField] = {
    val otherFieldsOnOppositeModel = tpe.sdl.types.find(_.name == referencesType).get match {
      case sameModel if sameModel.name == tpe.name => sameModel.relationalPrismaFields.filter(_.referencesType == tpe.name).filter(_.name != name)
      case otherModel                              => otherModel.relationalPrismaFields.filter(_.referencesType == tpe.name)
    }

    relationName match {
      case Some(relationName) => otherFieldsOnOppositeModel.find(field => field.relationName.contains(relationName))
      case None               => otherFieldsOnOppositeModel.headOption
    }
  }

  def referencedType: PrismaType = tpe.sdl.types.find(_.name == referencesType).get
}

case class PrismaEnum(name: String, values: Vector[String])(sdl: PrismaSdl)
