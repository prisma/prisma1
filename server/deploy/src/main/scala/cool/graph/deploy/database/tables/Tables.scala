package cool.graph.deploy.database.tables

import slick.lifted.TableQuery

object Tables {
  val Projects   = TableQuery[ProjectTable]
  val Migrations = TableQuery[MigrationTable]
}
