package com.prisma.deploy.connector.postgres

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.PostgresProfile

case class PostgresInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  import slick.jdbc.PostgresProfile.api._

  // Only used during setup - this is the default PSQL db, which is only used for administrative commands
  lazy val setupDatabase = getDatabase("postgres", "public")

  // Used during runtime & setup
  lazy val dbName               = dbConfig.database.getOrElse("prisma")
  lazy val managementSchemaName = dbConfig.managementSchema.getOrElse("management")
  lazy val managementDatabase   = getDatabase(dbName, managementSchemaName)

  private lazy val dbDriver = new org.postgresql.Driver

  def getDatabase(dbToUse: String, schemaToUse: String): Databases = {
    val config        = typeSafeConfigFromDatabaseConfig(dbToUse, schemaToUse, dbConfig)
    val masterDb      = Database.forConfig("database", config, driver = dbDriver)
    val slickDatabase = SlickDatabase(PostgresProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
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
