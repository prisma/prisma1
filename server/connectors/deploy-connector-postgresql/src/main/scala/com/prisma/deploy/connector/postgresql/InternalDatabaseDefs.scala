package com.prisma.deploy.connector.postgresql

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector.postgresql.database.InternalDatabaseSchema
import com.typesafe.config.{Config, ConfigFactory}

case class InternalDatabaseDefs(dbConfig: DatabaseConfig) {
  import slick.jdbc.PostgresProfile.api._

  lazy val internalDatabaseRoot = getDatabase("postgres")
  lazy val internalDatabase     = getDatabase(InternalDatabaseSchema.database)

  private lazy val dbDriver = new org.postgresql.Driver

  def getDatabase(dbToUse: String) = {
    val config = typeSafeConfigFromDatabaseConfig(dbToUse, dbConfig)
    Database.forConfig("database", config, driver = dbDriver)
  }

  def typeSafeConfigFromDatabaseConfig(database: String, dbConfig: DatabaseConfig): Config = {
    val pooled = if (dbConfig.pooled) "" else "connectionPool = disabled"

    ConfigFactory
      .parseString(s"""
        |database {
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database"
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
