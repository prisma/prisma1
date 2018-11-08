package com.prisma.connectors.utils

import com.prisma.api.connector.ApiConnector
import com.prisma.api.connector.mongo.MongoApiConnector
import com.prisma.api.connector.mysql.MySqlApiConnector
import com.prisma.api.connector.postgres.PostgresApiConnector
import com.prisma.config.PrismaConfig
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.connector.mongo.MongoDeployConnector
import com.prisma.deploy.connector.mysql.MySqlDeployConnector
import com.prisma.deploy.connector.postgres.PostgresDeployConnector
import com.prisma.native_jdbc.CustomJdbcDriver

import scala.concurrent.ExecutionContext

object ConnectorLoader {
  def loadApiConnector(config: PrismaConfig)(implicit ec: ExecutionContext): ApiConnector = {
    val databaseConfig = config.databases.head
    (databaseConfig.connector, databaseConfig.active) match {
      case ("mysql", true)  => MySqlApiConnector(databaseConfig, new org.mariadb.jdbc.Driver)
      case ("mysql", false) => sys.error("There is not passive mysql deploy connector yet!")
//      case ("postgres", isActive) => PostgresApiConnector(databaseConfig, new org.postgresql.Driver, isActive = isActive)
      case ("postgres", isActive) => PostgresApiConnector(databaseConfig, CustomJdbcDriver.jna(), isActive = isActive) // todo tmp solution for tests
      case ("mongo", _)           => MongoApiConnector(databaseConfig)
      case (conn, _)              => sys.error(s"Unknown connector $conn")
    }
  }

  def loadDeployConnector(config: PrismaConfig)(implicit ec: ExecutionContext): DeployConnector = {
    val databaseConfig = config.databases.head
    (databaseConfig.connector, databaseConfig.active) match {
      case ("mysql", true)  => MySqlDeployConnector(databaseConfig, new org.mariadb.jdbc.Driver)
      case ("mysql", false) => sys.error("There is not passive mysql deploy connector yet!")
//      case ("postgres", isActive) => PostgresDeployConnector(databaseConfig, new org.postgresql.Driver, isActive)
      case ("postgres", isActive) => PostgresDeployConnector(databaseConfig, CustomJdbcDriver.jna(), isActive) // todo tmp solution for tests
      case ("mongo", isActive)    => MongoDeployConnector(databaseConfig, isActive = true)
      case (conn, _)              => sys.error(s"Unknown connector $conn")
    }
  }
}
