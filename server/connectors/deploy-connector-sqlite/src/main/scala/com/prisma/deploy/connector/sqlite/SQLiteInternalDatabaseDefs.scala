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
    val config   = typeSafeConfigFromDatabaseConfig(dbConfig, root)
    val masterDb = Database.forConfig("database", config, driver)

    val slickDatabase = SlickDatabase(SQLiteProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }

  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig, root: Boolean): Config = {
    val pooled = if (dbConfig.pooled) "" else "connectionPool = disabled"

    ConfigFactory
      .parseString(s"""
        |database {
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:sqlite:management.db"
        |    user = "${dbConfig.user}"
        |    password = "${dbConfig.password.getOrElse("")}"
        |  }
        |  numThreads = 1
        |  connectionTimeout = 5000
        |  $pooled
        |}
      """.stripMargin)
      .resolve
  }
}
