package com.prisma.deploy.connector.sqlite

import java.sql.Driver

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.SQLiteProfile

case class SQLiteInternalDatabaseDefs(dbConfig: DatabaseConfig, driver: Driver) {
  import slick.jdbc.SQLiteProfile.api._

  val managementSchemaName = dbConfig.managementSchema.getOrElse("prisma")

  lazy val setupDatabases      = databases(root = true)
  lazy val managementDatabases = databases(root = false)

  def databases(root: Boolean): Databases = {
    val masterDb      = Database.forDriver(driver, "jdbc:sqlite:management.db")
    val slickDatabase = SlickDatabase(SQLiteProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }
}
