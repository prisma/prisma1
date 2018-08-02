package com.prisma.api.connector.mysql

import com.prisma.api.connector.jdbc.database.{Databases, SlickDatabase}
import com.prisma.config.DatabaseConfig
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

object MySqlDatabasesFactory {
//  private lazy val dbDriver = new org.mariadb.jdbc.Driver
  private lazy val dbDriver = new com.mysql.jdbc.Driver

  def initialize(dbConfig: DatabaseConfig): Databases = {
    val config                       = typeSafeConfigFromDatabaseConfig(dbConfig)
    val masterDb                     = Database.forConfig("database", config, driver = dbDriver)
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
        |    url = "jdbc:mysql://${dbConfig.host}:${dbConfig.port}/?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000&usePipelineAuth=false&cachePrepStmts=true"
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
