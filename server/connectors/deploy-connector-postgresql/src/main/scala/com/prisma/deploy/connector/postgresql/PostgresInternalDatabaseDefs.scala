package com.prisma.deploy.connector.postgresql

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector.postgresql.database.InternalDatabaseSchema
import com.typesafe.config.{Config, ConfigFactory}

case class PostgresInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  import slick.jdbc.PostgresProfile.api._

  // Only used during setup - this is the default PSQL db, which is only used for administrative commands
  lazy val setupDatabase = getDatabase("postgres")

  // Used during runtime & setup
  lazy val dbName               = dbConfig.database.getOrElse("prisma")
  lazy val internalDatabaseRoot = getDatabase(dbName)
  lazy val internalDatabase     = getDatabase(dbName, InternalDatabaseSchema.internalSchema)

  private lazy val dbDriver = new org.postgresql.Driver

  def getDatabase(dbToUse: String, schemaToUse: String = "public") = {
    val config = typeSafeConfigFromDatabaseConfig(dbToUse, schemaToUse, dbConfig)
    Database.forConfig("database", config, driver = dbDriver)
  }

  def typeSafeConfigFromDatabaseConfig(database: String, schema: String, dbConfig: DatabaseConfig): Config = {
    val pooled = if (dbConfig.pooled) "" else "connectionPool = disabled"

    ConfigFactory
      .parseString(s"""
        |database {
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema"
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
