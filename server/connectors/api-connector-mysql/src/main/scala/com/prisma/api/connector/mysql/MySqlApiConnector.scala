package com.prisma.api.connector.mysql

import com.prisma.api.connector.mysql.database.{DataResolverImpl, Databases}
import com.prisma.api.connector.mysql.impl.DatabaseMutactionExecutorImpl
import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.Project

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

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = DatabaseMutactionExecutorImpl(databases.master)
  override def dataResolver(project: Project)                       = DataResolverImpl(project, databases.readOnly)
  override def masterDataResolver(project: Project)                 = DataResolverImpl(project, databases.master)
}
