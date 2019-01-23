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
    val loadNative     = sys.env.getOrElse("USE_NATIVE_DRIVER", "0") == "1"

    (databaseConfig.connector, databaseConfig.active, loadNative) match {
      case ("mysql", true, _)        => MySqlApiConnector(databaseConfig, drivers(SupportedDrivers.MYSQL), config.isPrototype)
      case ("mysql", false, _)       => sys.error("There is not passive mysql deploy connector yet!")
      case ("postgres", isActive, _) => PostgresApiConnector(databaseConfig, drivers(SupportedDrivers.POSTGRES), isActive, config.isPrototype)
//      case ("sqlite", _, true)       => SQLiteApiConnectorNative()
      case ("sqlite", true, _)  => SQLiteApiConnector(databaseConfig, isPrototype = config.isPrototype)
      case ("sqlite", false, _) => sys.error("There is no passive sqlite deploy connector yet!")
      case ("mongo", _, _)      => MongoApiConnector(databaseConfig)
      case (conn, _, _)         => sys.error(s"Unknown connector $conn")
    }
  }

  def loadDeployConnector(config: PrismaConfig, isTest: Boolean = false)(implicit ec: ExecutionContext, drivers: SupportedDrivers): DeployConnector = {
    val databaseConfig = config.databases.head
    (databaseConfig.connector, databaseConfig.active) match {
      case ("mysql", true)        => MySqlDeployConnector(databaseConfig, drivers(SupportedDrivers.MYSQL), config.isPrototype)
      case ("mysql", false)       => sys.error("There is not passive mysql deploy connector yet!")
      case ("postgres", isActive) => PostgresDeployConnector(databaseConfig, drivers(SupportedDrivers.POSTGRES), isActive, config.isPrototype)
      case ("mongo", isActive)    => MongoDeployConnector(databaseConfig, isActive = true, isTest = isTest)
      case ("sqlite", true)       => SQLiteDeployConnector(databaseConfig, isPrototype = config.isPrototype)
      case ("sqlite", false)      => sys.error("There is no passive sqlite deploy connector yet!")
      case (conn, _)              => sys.error(s"Unknown connector $conn")
    }
  }
}
