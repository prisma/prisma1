package com.prisma.api.connector.postgresql.database

import com.prisma.config.DatabaseConfig
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{JdbcProfile, PostgresProfile}

case class Databases(
    primary: SlickDatabase,
    replica: SlickDatabase
)

case class SlickDatabase(
    profile: JdbcProfile,
    database: JdbcProfile#Backend#Database
)

object Databases {
  private lazy val dbDriver = new org.postgresql.Driver

  // PostgreSQL db used for all Prisma schemas (must be in sync with the deploy connector)
  val defaultDatabase = "prisma"

  // Schema to use in the database
  val schema = "public" // default schema

  def initialize(dbConfig: DatabaseConfig): Databases = {
    val theConfig                    = typeSafeConfigFromDatabaseConfig(dbConfig)
    val masterDb                     = Database.forConfig("database", theConfig, driver = dbDriver)
    val slickDatabase: SlickDatabase = SlickDatabase(PostgresProfile, masterDb)
    Databases(primary = slickDatabase, replica = slickDatabase)
  }

  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig): Config = {
    val pooled   = if (dbConfig.pooled) "" else "connectionPool = disabled"
    val database = dbConfig.database.getOrElse(defaultDatabase)

    ConfigFactory
      .parseString(s"""
        |database {
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema&ssl=${dbConfig.ssl}&sslfactory=org.postgresql.ssl.NonValidatingFactory"
        |    user = "${dbConfig.user}"
        |    password = "${dbConfig.password.getOrElse("")}"
        |  }
        |  numThreads = ${dbConfig.connectionLimit.getOrElse(10)}
        |  connectionTimeout = 5000
        |  $pooled
        |}
      """.stripMargin)
      .resolve
  }
}
