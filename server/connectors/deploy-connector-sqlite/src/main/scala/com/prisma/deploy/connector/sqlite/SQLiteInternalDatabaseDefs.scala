package com.prisma.deploy.connector.sqlite

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import slick.jdbc.SQLiteProfile

case class SQLiteInternalDatabaseDefs(dbConfig: DatabaseConfig) {
  import slick.jdbc.SQLiteProfile.api._

  val managementSchemaName = dbConfig.managementSchema.getOrElse("prisma")

  lazy val setupDatabases      = databases(root = true)
  lazy val managementDatabases = databases(root = false)

  def databases(root: Boolean): Databases = {
//    val config        = typeSafeConfigFromDatabaseConfig(dbConfig, root)
    val masterDb = Database.forURL("jdbc:sqlite:management.db", driver = "org.sqlite.JDBC")

    val slickDatabase = SlickDatabase(SQLiteProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }

//  def typeSafeConfigFromDatabaseConfig(dbConfig: DatabaseConfig, root: Boolean): Config = {
//    val pooled = if (dbConfig.pooled) "" else "connectionPool = disabled"
//    val schema = if (root) "" else managementSchemaName
//
//    ConfigFactory
//      .parseString(s"""
//        |database {
//        |  connectionInitSql="set names utf8mb4"
//        |  dataSourceClass = "slick.jdbc.DriverDataSource"
//        |  properties {
//        |    url = "jdbc:mysql://${dbConfig.host}:${dbConfig.port}/$schema?autoReconnect=true&useSSL=${dbConfig.ssl}&requireSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000&usePipelineAuth=false"
//        |    user = "${dbConfig.user}"
//        |    password = "${dbConfig.password.getOrElse("")}"
//        |  }
//        |  numThreads = 1
//        |  connectionTimeout = 5000
//        |  $pooled
//        |}
//      """.stripMargin)
//      .resolve
//  }
}
