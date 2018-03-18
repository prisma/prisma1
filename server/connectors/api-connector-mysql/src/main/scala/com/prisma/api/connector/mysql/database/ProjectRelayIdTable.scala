package com.prisma.api.connector.mysql.database

import slick.jdbc.MySQLProfile.api._

case class ProjectRelayId(id: String, stableModelIdentifier: String)

class ProjectRelayIdTable(tag: Tag, schema: String) extends Table[ProjectRelayId](tag, Some(schema), "_RelayId") {

  def id                    = column[String]("id", O.PrimaryKey)
  def stableModelIdentifier = column[String]("stableModelIdentifier")

  def * = (id, stableModelIdentifier) <> ((ProjectRelayId.apply _).tupled, ProjectRelayId.unapply)
}
