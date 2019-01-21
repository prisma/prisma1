package com.prisma.api.connector.mysql

import com.prisma.api.connector.jdbc.impl.{JdbcDataResolver, JdbcDatabaseMutactionExecutor}
import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability, Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class MySqlApiConnector(config: DatabaseConfig, isPrototype: Boolean)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases = MySqlDatabasesFactory.initialize(config)

  override def initialize() = {
    databases
    Future.unit
  }

  override def shutdown() = {
    for {
      _ <- databases.primary.database.shutdown
      _ <- databases.replica.database.shutdown
    } yield ()
  }

  override def databaseMutactionExecutor            = JdbcDatabaseMutactionExecutor(databases.primary, manageRelayIds = !isPrototype)
  override def dataResolver(project: Project)       = JdbcDataResolver(project, databases.replica)(ec)
  override def masterDataResolver(project: Project) = JdbcDataResolver(project, databases.primary)(ec)

  override def projectIdEncoder: ProjectIdEncoder = ProjectIdEncoder('@')

  override val capabilities = {
    if (isPrototype) {
      ConnectorCapabilities.mysqlPrototype
    } else {
      ConnectorCapabilities.mysql
    }
  }

}
