package com.prisma.api.connector.mysql

import com.prisma.api.connector.mysql.database.{DataResolverImpl, Databases}
import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor}
import com.prisma.api.connector.mysql.impl.DatabaseMutactionExecutorImpl
import com.prisma.shared.models.Project
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

case class ApiConnectorImpl(clientDb: DatabaseDef)(implicit ec: ExecutionContext) extends ApiConnector {
  private lazy val config: Config = ConfigFactory.load()
  private lazy val databases      = Databases.initialize(config)

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

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = DatabaseMutactionExecutorImpl(clientDb)
  override def dataResolver(project: Project)                       = DataResolverImpl(project, databases.readOnly)
  override def masterDataResolver(project: Project)                 = DataResolverImpl(project, databases.master)

}
