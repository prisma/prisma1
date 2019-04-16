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
    databaseConfig.connector match {
      case "mysql"                    => MySqlApiConnector(databaseConfig, drivers(SupportedDrivers.MYSQL))
      case "postgres"                 => PostgresApiConnector(databaseConfig, drivers(SupportedDrivers.POSTGRES))
      case "sqlite-native"            => SQLiteApiConnectorNative(databaseConfig)
      case "native-integration-tests" => SQLiteApiConnectorNative(databaseConfig)
      case "sqlite"                   => SQLiteApiConnector(databaseConfig, drivers(SupportedDrivers.SQLITE))
      case "mongo"                    => MongoApiConnector(databaseConfig)
      case conn                       => sys.error(s"Unknown connector $conn")
    }
  }

  def loadDeployConnector(config: PrismaConfig)(implicit ec: ExecutionContext, drivers: SupportedDrivers): DeployConnector = {
    val databaseConfig = config.databases.head
    databaseConfig.connector match {
      case "mysql"                    => MySqlDeployConnector(databaseConfig, drivers(SupportedDrivers.MYSQL))
      case "postgres"                 => PostgresDeployConnector(databaseConfig, drivers(SupportedDrivers.POSTGRES))
      case "sqlite-native"            => SQLiteDeployConnector(databaseConfig, drivers(SupportedDrivers.SQLITE))
      case "native-integration-tests" => SQLiteDeployConnector(databaseConfig, drivers(SupportedDrivers.SQLITE))
      case "sqlite"                   => SQLiteDeployConnector(databaseConfig, drivers(SupportedDrivers.SQLITE))
      case "mongo"                    => MongoDeployConnector(databaseConfig)
      case conn                       => sys.error(s"Unknown connector $conn")
    }
  }
}
