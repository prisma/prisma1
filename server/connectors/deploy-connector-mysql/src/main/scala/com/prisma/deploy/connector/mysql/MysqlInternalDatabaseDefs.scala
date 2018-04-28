package com.prisma.deploy.connector.mysql

import com.prisma.config.DatabaseConfig
import com.typesafe.config.{Config, ConfigFactory}

case class MysqlInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  import slick.jdbc.MySQLProfile.api._

  val internalDb = "graphcool"

  lazy val internalDatabaseRoot = database(root = true)
  lazy val internalDatabase     = database(root = false)

  private lazy val dbDriver = new org.mariadb.jdbc.Driver

  def database(root: Boolean) = {
    val config = if (root) {
      typeSafeConfigFromDatabaseConfig(dbConfig)
    } else {
      typeSafeConfigFromDatabaseConfig(dbConfig, internalDb)
    }

    Database.forConfig("database", config, driver = dbDriver)
  }

  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig, database: String = ""): Config = {
    val pooled = if (dbConfig.pooled) "" else "connectionPool = disabled"

    ConfigFactory
      .parseString(s"""
        |database {
        |  connectionInitSql="set names utf8mb4"
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:mysql://${dbConfig.host}:${dbConfig.port}/$database?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000&usePipelineAuth=false"
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
