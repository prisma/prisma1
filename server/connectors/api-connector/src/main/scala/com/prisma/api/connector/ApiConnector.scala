package com.prisma.api.connector

import scala.concurrent.Future

trait ApiConnector {
  def databaseMutactionExecutor: DatabaseMutactionExecutor
}

trait DatabaseMutactionExecutor {
  def execute(mutaction: DatabaseMutaction): Future[Unit]
}
