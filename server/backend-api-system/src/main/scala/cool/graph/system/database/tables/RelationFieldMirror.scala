package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class RelationFieldMirror(
    id: String,
    relationId: String,
    fieldId: String
)

class RelationFieldMirrorTable(tag: Tag) extends Table[RelationFieldMirror](tag, "RelationFieldMirror") {

  def id = column[String]("id", O.PrimaryKey)

  def relationId = column[String]("relationId")
  def relation =
    foreignKey("relationfieldmirror_relationid_foreign", relationId, Tables.Relations)(_.id)

  def fieldId = column[String]("fieldId")
  def field =
    foreignKey("relationfieldmirror_fieldid_foreign", fieldId, Tables.Fields)(_.id)

  def * =
    (id, relationId, fieldId) <> ((RelationFieldMirror.apply _).tupled, RelationFieldMirror.unapply)
}
