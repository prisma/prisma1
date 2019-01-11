package com.prisma.api.connector.postgres

import com.prisma.api.connector.ApiConnector
import com.prisma.api.connector.jdbc.impl.{JdbcDataResolver, JdbcDatabaseMutactionExecutor}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability, Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class PostgresApiConnector(config: DatabaseConfig, isActive: Boolean, isPrototype: Boolean)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases = PostgresDatabasesFactory.initialize(config)

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

  override val databaseMutactionExecutor: JdbcDatabaseMutactionExecutor = JdbcDatabaseMutactionExecutor(databases.primary, isActive, schemaName = config.schema)
  override def dataResolver(project: Project)                           = JdbcDataResolver(project, databases.primary, schemaName = config.schema)
  override def masterDataResolver(project: Project)                     = JdbcDataResolver(project, databases.primary, schemaName = config.schema)
  override def projectIdEncoder: ProjectIdEncoder                       = ProjectIdEncoder('$')
  override val capabilities = {
    if (isPrototype) {
      ConnectorCapabilities.postgresPrototype
    } else {
      ConnectorCapabilities.postgres(isActive = isActive)
    }
  }
}
