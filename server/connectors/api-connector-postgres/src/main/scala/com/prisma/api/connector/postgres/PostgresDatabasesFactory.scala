package com.prisma.api.connector.postgres

import java.sql.Driver

import com.typesafe.config.ConfigFactory
import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.jdbc.{DataSourceJdbcDataSource, DriverDataSource, PostgresProfile}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

object PostgresDatabasesFactory {
  // PostgreSQL db used for all Prisma schemas (must be in sync with the deploy connector)
  val defaultDatabase = "prisma"

  // Schema to use in the database
  val schema = "public" // default schema

  def initialize(dbConfig: DatabaseConfig, driver: Driver): Databases = {
    val masterDb                     = databaseForConfig(dbConfig, driver)
    val slickDatabase: SlickDatabase = SlickDatabase(PostgresProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }

  private def isPooled(dbConfig: DatabaseConfig) = {
    dbConfig.pooled
  }

  def databaseForConfig(dbConfig: DatabaseConfig, driver: Driver) = {
    if (isPooled(dbConfig))
      pooledDataSource(dbConfig, driver)
    else
      simpleDataSource(dbConfig, driver)
  }

  def simpleDataSource(dbConfig: DatabaseConfig, driver: Driver) = {
    val database = dbConfig.database.getOrElse(defaultDatabase)

    val source = new DataSourceJdbcDataSource(
      new DriverDataSource(
        url =
          s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema&ssl=${dbConfig.ssl}&sslfactory=org.postgresql.ssl.NonValidatingFactory",
        user = dbConfig.user,
        password = dbConfig.password.getOrElse(""),
        driverObject = driver
      ),
      keepAliveConnection = true,
      maxConnections = None
    )

    val poolName       = "database"
    val numThreads     = dbConfig.connectionLimit.getOrElse(10) - 1
    val maxConnections = numThreads
    val executor       = AsyncExecutor(poolName, numThreads, numThreads, 1000, maxConnections, registerMbeans = false)

    Database.forSource(source, executor)
  }

  def pooledDataSource(dbConfig: DatabaseConfig, driver: Driver) = {
    val pooled   = if (dbConfig.pooled) "" else "connectionPool = disabled"
    val database = dbConfig.database.getOrElse(defaultDatabase)

    val config = ConfigFactory
      .parseString(s"""
                      |database {
                      |  url = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema&ssl=${dbConfig.ssl}&sslfactory=org.postgresql.ssl.NonValidatingFactory"
                      |  properties {
                      |    user = "${dbConfig.user}"
                      |    password = "${dbConfig.password.getOrElse("")}"
                      |  }
                      |  numThreads = ${dbConfig.connectionLimit.getOrElse(10) - 1} // we subtract 1 because one connection is consumed already by deploy
                      |  connectionTimeout = 5000
                      |  $pooled
                      |}
      """.stripMargin)
      .resolve

    Database.forConfig("database", config, driver)
  }
}
