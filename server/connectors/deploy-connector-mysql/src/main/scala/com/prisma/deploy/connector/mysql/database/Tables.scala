package com.prisma.deploy.connector.mysql.database

import slick.lifted.TableQuery

object Tables {
  val Projects   = TableQuery[ProjectTable]
  val Migrations = TableQuery[MigrationTable]
}
