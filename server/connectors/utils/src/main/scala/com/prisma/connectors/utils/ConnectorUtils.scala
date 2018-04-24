package com.prisma.connectors.utils

import com.prisma.api.connector.ApiConnector
import com.prisma.api.connector.mysql.MySqlApiConnector
import com.prisma.api.connector.postgresql.PostgresApiConnector
import com.prisma.config.PrismaConfig
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.connector.mysql.MySqlDeployConnector
import com.prisma.deploy.connector.postgresql.PostgresDeployConnector

import scala.concurrent.ExecutionContext

object ConnectorUtils {
  def loadApiConnector(config: PrismaConfig)(implicit ec: ExecutionContext): ApiConnector = {
    val databaseConfig = config.databases.head
    databaseConfig.connector match {
      case "mysql"    => MySqlApiConnector(databaseConfig)
      case "postgres" => PostgresApiConnector(databaseConfig)
    }
  }

  def loadDeployConnector(config: PrismaConfig)(implicit ec: ExecutionContext): DeployConnector = {
    val databaseConfig = config.databases.head
    databaseConfig.connector match {
      case "mysql"    => MySqlDeployConnector(databaseConfig)
      case "postgres" => PostgresDeployConnector(databaseConfig)
    }
  }
}
