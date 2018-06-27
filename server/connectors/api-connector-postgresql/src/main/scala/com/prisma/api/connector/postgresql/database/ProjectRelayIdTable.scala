package com.prisma.api.connector.postgresql.database

import slick.jdbc.JdbcProfile

case class ProjectRelayId(id: String, stableModelIdentifier: String)

case class RelayIdTableWrapper(jdbcProfile: JdbcProfile) {
  import jdbcProfile.api._

  class SlickTable(tag: Tag, schema: String) extends Table[ProjectRelayId](tag, Some(schema), "_RelayId") {

    def id                    = column[String]("id", O.PrimaryKey)
    def stableModelIdentifier = column[String]("stableModelIdentifier")

    def * = (id, stableModelIdentifier) <> ((ProjectRelayId.apply _).tupled, ProjectRelayId.unapply)
  }
}
