package com.prisma.api.connector.postgresql

import com.prisma.api.connector.{ApiConnector, NodeQueryCapability}
import com.prisma.api.connector.postgresql.database.{Databases, PostgresDataResolver}
import com.prisma.api.connector.postgresql.impl.PostgresDatabaseMutactionExecutor
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class PostgresApiConnector(config: DatabaseConfig, createRelayIds: Boolean)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases = Databases.initialize(config)

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

  override val databaseMutactionExecutor: PostgresDatabaseMutactionExecutor = PostgresDatabaseMutactionExecutor(databases.primary, createRelayIds)
  override def dataResolver(project: Project)                               = PostgresDataResolver(project, databases.primary, schemaName = None)
  override def masterDataResolver(project: Project)                         = PostgresDataResolver(project, databases.primary, schemaName = None)
  override def projectIdEncoder: ProjectIdEncoder                           = ProjectIdEncoder('$')
  override def capabilities                                                 = Vector(NodeQueryCapability)
}
