package com.prisma.api.connector.mysql

import com.prisma.api.connector.mysql.database.MySqlDatabases
import com.prisma.api.connector.jdbc.database.{Databases, JdbcDataResolver}
import com.prisma.api.connector.jdbc.impl.JdbcDatabaseMutactionExecutor
import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor, NodeQueryCapability}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class MySqlApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases = MySqlDatabases.initialize(config)

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

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = JdbcDatabaseMutactionExecutor(databases.primary, createRelayIds = true)
  override def dataResolver(project: Project)                       = JdbcDataResolver(project, databases.replica, None)(ec)
  override def masterDataResolver(project: Project)                 = JdbcDataResolver(project, databases.primary, None)(ec)

  override def projectIdEncoder: ProjectIdEncoder = ProjectIdEncoder('@')

  override def capabilities = Vector(NodeQueryCapability)
}
