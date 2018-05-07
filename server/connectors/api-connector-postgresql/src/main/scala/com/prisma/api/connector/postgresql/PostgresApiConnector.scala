package com.prisma.api.connector.postgresql

import com.prisma.api.connector.postgresql.database.{Databases, PostgresDataResolver}
import com.prisma.api.connector.postgresql.impl.DatabaseMutactionExecutorImpl
import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor, NodeQueryCapability}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class PostgresApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
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

  override def databaseMutactionExecutor: DatabaseMutactionExecutorImpl = DatabaseMutactionExecutorImpl(databases.master)
  override def dataResolver(project: Project)                           = PostgresDataResolver(project, databases.readOnly, schemaName = None)
  override def masterDataResolver(project: Project)                     = PostgresDataResolver(project, databases.master, schemaName = None)

  override def projectIdEncoder: ProjectIdEncoder = ProjectIdEncoder('$')

  override def capabilities = Vector(NodeQueryCapability)
}
