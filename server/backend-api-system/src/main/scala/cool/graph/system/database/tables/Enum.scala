package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class Enum(
    id: String,
    projectId: String,
    name: String,
    values: String
)

class EnumTable(tag: Tag) extends Table[Enum](tag, "Enum") {
  def id        = column[String]("id", O.PrimaryKey)
  def name      = column[String]("name")
  def values    = column[String]("values")
  def projectId = column[String]("projectId")
  def project   = foreignKey("enum_projectid_foreign", projectId, Tables.Projects)(_.id)

  def * = (id, projectId, name, values) <> ((Enum.apply _).tupled, Enum.unapply)
}
