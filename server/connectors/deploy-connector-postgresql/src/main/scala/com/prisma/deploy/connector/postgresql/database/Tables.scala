package com.prisma.deploy.connector.postgresql.database

import slick.lifted.TableQuery

object Tables {
  val Projects   = TableQuery[ProjectTable]
  val Migrations = TableQuery[MigrationTable]
}
