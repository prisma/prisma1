package com.prisma.api.connector.mysql

import com.prisma.api.connector.mysql.database.{Databases, MySqlDataResolver}
import com.prisma.api.connector.mysql.impl.MySqlDatabaseMutactionExecutor
import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor, NodeQueryCapability}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class MySqlApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases = Databases.initialize(config)

  override def initialize() = {
    databases
    Future.unit
  }

  override def shutdown() = {
    for {
      _ <- databases.master.shutdown
      _ <- databases.readOnly.shutdown
    } yield ()
  }

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = MySqlDatabaseMutactionExecutor(databases.master)
  override def dataResolver(project: Project)                       = MySqlDataResolver(project, databases.readOnly)(ec)
  override def masterDataResolver(project: Project)                 = MySqlDataResolver(project, databases.master)(ec)

  override def projectIdEncoder: ProjectIdEncoder = ProjectIdEncoder('@')

  override def capabilities = Vector(NodeQueryCapability)
}
