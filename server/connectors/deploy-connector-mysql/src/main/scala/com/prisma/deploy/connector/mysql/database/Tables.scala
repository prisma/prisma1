package com.prisma.deploy.connector.mysql.database

import slick.lifted.TableQuery

object Tables {
  val Migrations = TableQuery[MigrationTable]
}
