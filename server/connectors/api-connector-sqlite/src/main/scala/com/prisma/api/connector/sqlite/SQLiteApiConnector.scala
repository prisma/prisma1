package com.prisma.api.connector.sqlite

import java.sql.Driver

import com.prisma.api.connector.jdbc.impl.{JdbcDataResolver, JdbcDatabaseMutactionExecutor}
import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{ConnectorCapabilities, Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class SQLiteApiConnector(config: DatabaseConfig, driver: Driver, isPrototype: Boolean)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases = SQLiteDatabasesFactory.initialize(config, driver)

  override def initialize(): Future[Unit] = {
    databases
    Future.unit
  }

  override def shutdown(): Future[Unit] = {
    for {
      _ <- databases.primary.database.shutdown
      _ <- databases.replica.database.shutdown
    } yield ()
  }

  override def databaseMutactionExecutor: JdbcDatabaseMutactionExecutor = JdbcDatabaseMutactionExecutor(databases.primary, manageRelayIds = true)
  override def dataResolver(project: Project)                           = JdbcDataResolver(project, databases.replica)(ec)
  override def masterDataResolver(project: Project)                     = JdbcDataResolver(project, databases.primary)(ec)

  override def projectIdEncoder: ProjectIdEncoder = ProjectIdEncoder('_')

  override val capabilities = ConnectorCapabilities.sqlite
}
