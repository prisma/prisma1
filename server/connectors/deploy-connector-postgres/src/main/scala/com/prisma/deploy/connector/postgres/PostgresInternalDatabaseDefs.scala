package com.prisma.deploy.connector.postgres

import com.prisma.config.DatabaseConfig
import com.prisma.native_jdbc.CustomJdbcDriver
import com.typesafe.config.{Config, ConfigFactory}

case class PostgresInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  import slick.jdbc.PostgresProfile.api._

  // Only used during setup - this is the default PSQL db, which is only used for administrative commands
  lazy val setupDatabase = getDatabase("postgres", "public")

  // Used during runtime & setup
  lazy val dbName               = dbConfig.database.getOrElse("prisma")
  lazy val managementSchemaName = dbConfig.managementSchema.getOrElse("management")
  lazy val managementDatabase   = getDatabase(dbName, managementSchemaName)

  private lazy val dbDriver = CustomJdbcDriver.jna()

  def getDatabase(dbToUse: String, schemaToUse: String) = {
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
        |    url = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema&ssl=${dbConfig.ssl}&sslfactory=org.postgresql.ssl.NonValidatingFactory"
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
