package com.prisma.deploy.connector.postgres.database

import slick.lifted.TableQuery

object Tables {
  val Migrations = TableQuery[MigrationTable]
}
