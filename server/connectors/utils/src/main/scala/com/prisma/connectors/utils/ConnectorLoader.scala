package com.prisma.connectors.utils

import com.prisma.api.connector.ApiConnector
import com.prisma.api.connector.mongo.MongoApiConnector
import com.prisma.api.connector.mysql.MySqlApiConnector
import com.prisma.api.connector.postgres.PostgresApiConnector
import com.prisma.api.connector.sqlite.SQLiteApiConnector
import com.prisma.config.PrismaConfig
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.connector.mongo.MongoDeployConnector
import com.prisma.deploy.connector.mysql.MySqlDeployConnector
import com.prisma.deploy.connector.postgres.PostgresDeployConnector
import com.prisma.deploy.connector.sqlite.SQLiteDeployConnector

import scala.concurrent.ExecutionContext

object ConnectorLoader {
  def loadApiConnector(config: PrismaConfig)(implicit ec: ExecutionContext): ApiConnector = {
    val databaseConfig = config.databases.head
    (databaseConfig.connector, databaseConfig.active) match {
      case ("mysql", true)        => MySqlApiConnector(databaseConfig)
      case ("mysql", false)       => sys.error("There is no passive mysql deploy connector yet!")
      case ("sqlite", true)       => SQLiteApiConnector(databaseConfig)
      case ("sqlite", false)      => sys.error("There is no passive sqlite deploy connector yet!")
      case ("postgres", isActive) => PostgresApiConnector(databaseConfig, isActive = isActive)
      case ("mongo", _)           => MongoApiConnector(databaseConfig)
      case (conn, _)              => sys.error(s"Unknown connector $conn")
    }
  }

  def loadDeployConnector(config: PrismaConfig, isTest: Boolean = false)(implicit ec: ExecutionContext): DeployConnector = {
    val databaseConfig = config.databases.head
    (databaseConfig.connector, databaseConfig.active) match {
      case ("mysql", true)        => MySqlDeployConnector(databaseConfig)
      case ("mysql", false)       => sys.error("There is no passive mysql deploy connector yet!")
      case ("sqlite", true)       => SQLiteDeployConnector(databaseConfig)
      case ("sqlite", false)      => sys.error("There is no passive sqlite deploy connector yet!")
      case ("postgres", isActive) => PostgresDeployConnector(databaseConfig, isActive)
      case ("mongo", _)           => MongoDeployConnector(databaseConfig, isActive = true, isTest = isTest)
      case (conn, _)              => sys.error(s"Unknown connector $conn")
    }
  }
}
