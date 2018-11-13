package com.prisma.api.connector.postgres

import java.sql.Driver

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

object PostgresDatabasesFactory {
  // PostgreSQL db used for all Prisma schemas (must be in sync with the deploy connector)
  val defaultDatabase = "prisma"

  // Schema to use in the database
  val schema = "public" // default schema

  def initialize(dbConfig: DatabaseConfig, driver: Driver): Databases = {
    val masterDb                     = databaseForConfig(dbConfig)
    val slickDatabase: SlickDatabase = SlickDatabase(PostgresProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }

  def databaseForConfig(dbConfig: DatabaseConfig) = {
    val source         = hikariDataSource(dbConfig)
    val poolName       = "database"
    val numThreads     = dbConfig.connectionLimit.getOrElse(10) - 1
    val maxConnections = numThreads
    val executor       = AsyncExecutor(poolName, numThreads, numThreads, 1000, maxConnections, registerMbeans = false)

    Database.forSource(source, executor)
  }

  def hikariDataSource(dbConfig: DatabaseConfig) = {
    val database = dbConfig.database.getOrElse(defaultDatabase)
    val hconf    = new HikariConfig()

    hconf.setJdbcUrl(
      s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema&ssl=${dbConfig.ssl}&sslfactory=org.postgresql.ssl.NonValidatingFactory"
    )

    hconf.setUsername(dbConfig.user)
    hconf.setPassword(dbConfig.password.getOrElse(""))

    // Pool configuration
    hconf.setConnectionTimeout(5000)
    hconf.setValidationTimeout(1000)
    hconf.setIdleTimeout(600000)
    hconf.setMaxLifetime(1800000)
    hconf.setLeakDetectionThreshold(0)
    hconf.setInitializationFailFast(false)

    val numThreads = dbConfig.connectionLimit.map(_ - 1).getOrElse(10)
    hconf.setMaximumPoolSize(numThreads)
    hconf.setMinimumIdle(numThreads)
    hconf.setPoolName("database")
    hconf.setRegisterMbeans(false)

    hconf.setReadOnly(false)
    hconf.setCatalog(null)

    val ds = new HikariDataSource(hconf)
    new HikariCPJdbcDataSource(ds, hconf)
  }
}
