package com.prisma.image

import com.prisma.api.connector.ApiConnector
import com.prisma.api.connector.mysql.MySqlApiConnector
import com.prisma.config.{ConfigLoader, PrismaConfig}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.connector.mysql.MySqlDeployConnector

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait ImageUtils {
  def loadConfig(): PrismaConfig = {
    ConfigLoader.load() match {
      case Success(c)   => c
      case Failure(err) => sys.error(s"Unable to load Prisma config: $err")
    }
  }

  def loadApiConnector(config: PrismaConfig)(implicit ec: ExecutionContext): ApiConnector = {
    val databaseConfig = config.databases.head
    databaseConfig.connector match {
      case "mysql" => MySqlApiConnector(databaseConfig)
    }
  }

  def loadDeployConnector(config: PrismaConfig)(implicit ec: ExecutionContext): DeployConnector = {
    val databaseConfig = config.databases.head
    databaseConfig.connector match {
      case "mysql" => MySqlDeployConnector(databaseConfig)
    }
  }
}
