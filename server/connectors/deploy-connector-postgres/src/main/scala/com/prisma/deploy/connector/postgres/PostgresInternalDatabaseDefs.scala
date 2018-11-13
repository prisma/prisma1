package com.prisma.deploy.connector.postgres

import java.sql.Driver

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.zaxxer.hikari.HikariDataSource
import slick.jdbc.PostgresProfile
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

case class PostgresInternalDatabaseDefs(dbConfig: DatabaseConfig, driver: Driver) {
  import slick.jdbc.PostgresProfile.api._

  // Only used during setup - this is the default PSQL db, which is only used for administrative commands
  lazy val setupDatabase = getDatabase("postgres", "public")

  // Used during runtime & setup
  lazy val dbName               = dbConfig.database.getOrElse("prisma")
  lazy val managementSchemaName = dbConfig.managementSchema.getOrElse("management")
  lazy val managementDatabase   = getDatabase(dbName, managementSchemaName)

  def getDatabase(dbToUse: String, schemaToUse: String): Databases = {
    val masterDb      = databaseForConfig(dbToUse, schemaToUse)
    val slickDatabase = SlickDatabase(PostgresProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }

  def databaseForConfig(database: String, schema: String) = {
    val source         = hikariDataSource(database, schema)
    val poolName       = "database"
    val numThreads     = 1
    val maxConnections = 1
    val executor       = AsyncExecutor(poolName, numThreads, numThreads, 1000, maxConnections, registerMbeans = false)
    Database.forSource(source, executor)
  }

  def hikariDataSource(database: String, schema: String) = {
    val ds = new HikariDataSource()

    ds.setJdbcUrl(
      s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema&ssl=${dbConfig.ssl}&sslfactory=org.postgresql.ssl.NonValidatingFactory")

    ds.setUsername(dbConfig.user)
    ds.setPassword(dbConfig.password.getOrElse(""))

    // Pool configuration
    ds.setConnectionTimeout(60000)
    ds.setValidationTimeout(1000)
    ds.setIdleTimeout(600000)
    ds.setMaxLifetime(1800000)
    ds.setLeakDetectionThreshold(0)
    ds.setInitializationFailFast(false)

    val numThreads = 1
    ds.setMaximumPoolSize(numThreads)
    ds.setMinimumIdle(numThreads)
    ds.setPoolName("database")
    ds.setRegisterMbeans(false)

    ds.setReadOnly(false)
    ds.setCatalog(null)

    new HikariCPJdbcDataSource(ds, ds)
  }
}
