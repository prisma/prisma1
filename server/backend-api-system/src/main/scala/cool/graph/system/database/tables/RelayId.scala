package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class RelayId(id: String, typeName: String)

class RelayIdTable(tag: Tag) extends Table[RelayId](tag, "RelayId") {

  def id       = column[String]("id", O.PrimaryKey)
  def typeName = column[String]("typeName")

  def * = (id, typeName) <> ((RelayId.apply _).tupled, RelayId.unapply)
}
