package com.prisma.api.connector.sqlite.native

import com.prisma.api.connector.{ApiConnector, DataResolver, DatabaseMutactionExecutor}
import com.prisma.shared.models.{ConnectorCapabilities, Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class SQLiteApiConnectorNative()(implicit ec: ExecutionContext) extends ApiConnector {
  override def initialize() = Future.unit
  override def shutdown()   = Future.unit

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = ???
  override def dataResolver(project: Project): DataResolver         = SQLiteNativeDataResolver()
  override def masterDataResolver(project: Project): DataResolver   = SQLiteNativeDataResolver()
  override def projectIdEncoder: ProjectIdEncoder                   = ProjectIdEncoder('_')

  override val capabilities = ConnectorCapabilities.mysql
}
