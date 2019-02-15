package com.prisma.api.connector.sqlite

import java.sql.Driver

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._

object SQLiteDatabasesFactory {

  def initialize(dbConfig: DatabaseConfig, driver: Driver): Databases = {
    val config = typeSafeConfigFromDatabaseConfig(dbConfig)

    val masterDb                     = Database.forConfig("database", config, driver)
    val slickDatabase: SlickDatabase = SlickDatabase(SQLiteProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }

  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig): Config = {
    ConfigFactory
      .parseString(s"""
        |database {
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:sqlite:database.db"
        |    user = "${dbConfig.user}"
        |    password = "${dbConfig.password.getOrElse("")}"
        |  }
        |  numThreads = ${dbConfig.connectionLimit.getOrElse(10) - 1} // we subtract 1 because one connection is consumed already by deploy
        |  connectionTimeout = 5000
        |}
      """.stripMargin)
      .resolve
  }
}
