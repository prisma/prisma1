package com.prisma.api.connector.mysql

import java.sql.Driver

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

object MySqlDatabasesFactory {
  def initialize(dbConfig: DatabaseConfig, driver: Driver): Databases = {
    val config                       = typeSafeConfigFromDatabaseConfig(dbConfig)
    val masterDb                     = Database.forConfig("database", config, driver)
    val slickDatabase: SlickDatabase = SlickDatabase(MySQLProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }

  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig): Config = {
    ConfigFactory
      .parseString(s"""
        |database {
        |  connectionInitSql="set names utf8mb4;"
        |  dataSourceClass = "slick.jdbc.DriverDataSource"
        |  properties {
        |    url = "jdbc:mysql://${dbConfig.host}:${dbConfig.port}/?autoReconnect=true&useSSL=${dbConfig.ssl}&requireSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000&usePipelineAuth=false&cachePrepStmts=true"
        |    user = "${dbConfig.user}"
        |    password = "${dbConfig.password.getOrElse("")}"
        |  }
        |  numThreads = ${dbConfig.connectionLimit.getOrElse(10) - 1} // we subtract 1 because one connection is consumed already by deploy
        |  connectionTimeout = 5000
        |}
      """.stripMargin)
      .resolve
  }
}
