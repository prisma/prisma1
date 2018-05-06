package com.prisma.api.connector.mysql.database

import com.prisma.config.DatabaseConfig
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

case class Databases(master: DatabaseDef, readOnly: DatabaseDef)

object Databases {
  private lazy val dbDriver = new org.mariadb.jdbc.Driver

  def initialize(dbConfig: DatabaseConfig): Databases = {
    val config   = typeSafeConfigFromDatabaseConfig(dbConfig)
    val masterDb = Database.forConfig("database", config, driver = dbDriver)
    val dbs = Databases(
      master = masterDb,
      readOnly = masterDb //if (config.hasPath(readOnlyPath)) readOnlyDb else masterDb
    )

    dbs
  }

  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig): Config = {
    ConfigFactory
      .parseString(s"""
        |database {
        |  connectionInitSql="set names utf8mb4;"
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:mysql://${dbConfig.host}:${dbConfig.port}/?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000&usePipelineAuth=false&cachePrepStmts=true"
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
