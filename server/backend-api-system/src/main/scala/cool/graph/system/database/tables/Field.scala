package cool.graph.system.database.tables

import cool.graph.shared.models.RelationSide
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ForeignKeyQuery

case class Field(
    id: String,
    name: String,
    typeIdentifier: String,
    description: Option[String],
    isRequired: Boolean,
    isList: Boolean,
    isUnique: Boolean,
    isSystem: Boolean,
    isReadonly: Boolean,
    defaultValue: Option[String],
    relationId: Option[String],
    relationSide: Option[RelationSide.Value],
    modelId: String,
    enumId: Option[String]
)

class FieldTable(tag: Tag) extends Table[Field](tag, "Field") {

  implicit val relationSideMapper: JdbcType[RelationSide.Value] with BaseTypedType[RelationSide.Value] =
    MappedColumnType.base[RelationSide.Value, String](
      e => e.toString,
      s => RelationSide.withName(s)
    )

  def id: Rep[String]                                                               = column[String]("id", O.PrimaryKey)
  def name: Rep[String]                                                             = column[String]("fieldName") // TODO adjust db naming
  def typeIdentifier: Rep[String]                                                   = column[String]("typeIdentifier")
  def description: Rep[Option[String]]                                              = column[Option[String]]("description")
  def isRequired: Rep[Boolean]                                                      = column[Boolean]("isRequired")
  def isList: Rep[Boolean]                                                          = column[Boolean]("isList")
  def isUnique: Rep[Boolean]                                                        = column[Boolean]("isUnique")
  def isSystem: Rep[Boolean]                                                        = column[Boolean]("isSystem")
  def isReadonly: Rep[Boolean]                                                      = column[Boolean]("isReadonly")
  def defaultValue: Rep[Option[String]]                                             = column[Option[String]]("defaultValue")
  def relationSide: Rep[Option[_root_.cool.graph.shared.models.RelationSide.Value]] = column[Option[RelationSide.Value]]("relationSide")

  def modelId: Rep[String]                      = column[String]("modelId")
  def model: ForeignKeyQuery[ModelTable, Model] = foreignKey("field_modelid_fieldname", modelId, Tables.Models)(_.id)

  def relationId: Rep[Option[String]]                    = column[Option[String]]("relationId")
  def relation: ForeignKeyQuery[RelationTable, Relation] = foreignKey("field_relationid_foreign", relationId, Tables.Relations)(_.id.?)

  def enumId: Rep[Option[String]]            = column[Option[String]]("enumId")
  def enum: ForeignKeyQuery[EnumTable, Enum] = foreignKey("field_enumid_foreign", relationId, Tables.Enums)(_.id.?)

  def * =
    (id, name, typeIdentifier, description, isRequired, isList, isUnique, isSystem, isReadonly, defaultValue, relationId, relationSide, modelId, enumId) <> ((Field.apply _).tupled, Field.unapply)
}
