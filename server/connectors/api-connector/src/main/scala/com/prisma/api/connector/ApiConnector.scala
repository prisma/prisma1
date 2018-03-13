package com.prisma.api.connector

import scala.concurrent.Future

trait ApiConnector {
  def databaseMutactionExecutor: DatabaseMutactionExecutor
}

trait DatabaseMutactionExecutor {
  def execute(mutactions: Vector[DatabaseMutaction]): Future[Unit]
}
