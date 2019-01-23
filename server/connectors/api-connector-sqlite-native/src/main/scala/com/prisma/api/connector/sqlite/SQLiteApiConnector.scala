package com.prisma.api.connector.sqlite

import com.prisma.api.connector.{ApiConnector, DataResolver, DatabaseMutactionExecutor}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class SQLiteApiConnector(config: DatabaseConfig, isPrototype: Boolean)(implicit ec: ExecutionContext) extends ApiConnector {
  override def initialize() = { Future.unit }
  override def shutdown()   = { Future.unit }

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = ???
  override def dataResolver(project: Project): DataResolver         = ???
  override def masterDataResolver(project: Project): DataResolver   = ???
  override def projectIdEncoder: ProjectIdEncoder                   = ProjectIdEncoder('@')

  override val capabilities = ???
}
