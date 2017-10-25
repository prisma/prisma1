package cool.graph.client.database

import slick.jdbc.MySQLProfile.api._

case class ProjectRelayId(id: String, modelId: String)

class ProjectRelayIdTable(tag: Tag, schema: String) extends Table[ProjectRelayId](tag, Some(schema), "_RelayId") {

  def id      = column[String]("id", O.PrimaryKey)
  def modelId = column[String]("modelId")

  def * = (id, modelId) <> ((ProjectRelayId.apply _).tupled, ProjectRelayId.unapply)
}
