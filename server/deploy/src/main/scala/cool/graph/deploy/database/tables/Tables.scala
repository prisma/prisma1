package cool.graph.deploy.database.tables

import slick.lifted.TableQuery

object Tables {
  val Clients    = TableQuery[ClientTable]
  val Projects   = TableQuery[ProjectTable]
  val Migrations = TableQuery[MigrationTable]
  val Seats      = TableQuery[SeatTable]
}
