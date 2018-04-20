package com.prisma.deploy.connector.postgresql

import com.prisma.config.DatabaseConfig
import com.typesafe.config.{Config, ConfigFactory}

case class InternalDatabaseDefs(dbConfig: DatabaseConfig) {
  import slick.jdbc.PostgresProfile.api._

  lazy val internalDatabaseRoot = database
  lazy val internalDatabase     = database

  private lazy val dbDriver = new org.postgresql.Driver

  lazy val database = {
    val config = typeSafeConfigFromDatabaseConfig(dbConfig)
    Database.forConfig("database", config, driver = dbDriver)
  }

  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig): Config = {
    val pooled = if (dbConfig.pooled) "" else "connectionPool = disabled"

    ConfigFactory
      .parseString(s"""
        |database {
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/"
        |    user = ${dbConfig.user}
        |    password = ${dbConfig.password.getOrElse("")}
        |  }
        |  numThreads = ${dbConfig.connectionLimit.getOrElse(10)}
        |  connectionTimeout = 5000
        |  $pooled
        |}
      """.stripMargin)
      .resolve
  }
}
