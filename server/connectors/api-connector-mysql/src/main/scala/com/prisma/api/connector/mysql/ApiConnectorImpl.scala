package com.prisma.api.connector.mysql

import com.prisma.api.connector.{ApiConnector, DatabaseMutactionExecutor}
import com.prisma.api.connector.mysql.impl.DatabaseMutactionExecutorImpl
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.ExecutionContext

case class ApiConnectorImpl(clientDb: DatabaseDef)(implicit ec: ExecutionContext) extends ApiConnector {

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = DatabaseMutactionExecutorImpl(clientDb)
}
