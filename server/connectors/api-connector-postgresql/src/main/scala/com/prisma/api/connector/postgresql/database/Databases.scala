package com.prisma.api.connector.postgresql.database

import com.prisma.config.DatabaseConfig
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.DatabaseDef

case class Databases(master: DatabaseDef, readOnly: DatabaseDef)

object Databases {
  private lazy val dbDriver = new org.postgresql.Driver

  def initialize(dbConfig: DatabaseConfig): Databases = {
    val config   = typeSafeConfigFromDatabaseConfig(dbConfig)
    val masterDb = Database.forConfig("database", config, driver = dbDriver)
    val dbs = Databases(
      master = masterDb,
      readOnly = masterDb
    )

    dbs
  }

  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig): Config = {
    ConfigFactory
      .parseString(s"""
        |database {
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.database.getOrElse("")}"
        |    user = ${dbConfig.user}
        |    password = ${dbConfig.password.getOrElse("")}
        |  }
        |  numThreads = ${dbConfig.connectionLimit.getOrElse(10)}
        |  connectionTimeout = 5000
        |}
      """.stripMargin)
      .resolve
  }
}
