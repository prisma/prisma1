package com.prisma.connectors.utils
import com.prisma.api.connector.ApiConnector
import com.prisma.api.connector.mongo.MongoApiConnector
import com.prisma.api.connector.mysql.MySqlApiConnector
import com.prisma.api.connector.postgres.PostgresApiConnector
import com.prisma.api.connector.sqlite.SQLiteApiConnector
import com.prisma.api.connector.sqlite.native.SQLiteApiConnectorNative
import com.prisma.config.PrismaConfig
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.connector.mongo.MongoDeployConnector
import com.prisma.deploy.connector.mysql.MySqlDeployConnector
import com.prisma.deploy.connector.postgres.PostgresDeployConnector
import com.prisma.deploy.connector.sqlite.SQLiteDeployConnector

import scala.concurrent.ExecutionContext

object ConnectorLoader {
  def loadApiConnector(config: PrismaConfig)(implicit ec: ExecutionContext, drivers: SupportedDrivers): ApiConnector = {
    val databaseConfig = config.databases.head

    (databaseConfig.connector, databaseConfig.active) match {
      case ("mysql", true)        => MySqlApiConnector(databaseConfig, drivers(SupportedDrivers.MYSQL), config.isPrototype)
      case ("mysql", false)       => sys.error("There is not passive mysql deploy connector yet!")
      case ("postgres", isActive) => PostgresApiConnector(databaseConfig, drivers(SupportedDrivers.POSTGRES), isActive, config.isPrototype)
      case ("sqlite-native", _)   => SQLiteApiConnectorNative(databaseConfig, config.isPrototype)
      case ("sqlite", true)       => SQLiteApiConnector(databaseConfig, drivers(SupportedDrivers.SQLITE), isPrototype = config.isPrototype)
      case ("sqlite", false)      => sys.error("There is no passive sqlite deploy connector yet!")
      case ("mongo", _)           => MongoApiConnector(databaseConfig)
      case (conn, _)              => sys.error(s"Unknown connector $conn")
    }
  }

  def loadDeployConnector(config: PrismaConfig, isTest: Boolean = false)(implicit ec: ExecutionContext, drivers: SupportedDrivers): DeployConnector = {
    val databaseConfig = config.databases.head
    (databaseConfig.connector, databaseConfig.active) match {
      case ("mysql", true)        => MySqlDeployConnector(databaseConfig, drivers(SupportedDrivers.MYSQL), config.isPrototype)
      case ("mysql", false)       => sys.error("There is not passive mysql deploy connector yet!")
      case ("postgres", isActive) => PostgresDeployConnector(databaseConfig, drivers(SupportedDrivers.POSTGRES), isActive, config.isPrototype)
      case ("mongo", isActive)    => MongoDeployConnector(databaseConfig, isActive = true, isTest = isTest)
      case ("sqlite-native", _)   => SQLiteDeployConnector(databaseConfig, drivers(SupportedDrivers.SQLITE), isPrototype = config.isPrototype)
      case ("sqlite", true)       => SQLiteDeployConnector(databaseConfig, drivers(SupportedDrivers.SQLITE), isPrototype = config.isPrototype)
      case ("sqlite", false)      => sys.error("There is no passive sqlite deploy connector yet!")
      case (conn, _)              => sys.error(s"Unknown connector $conn")
    }
  }
}
