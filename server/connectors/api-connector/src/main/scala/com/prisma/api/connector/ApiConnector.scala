package com.prisma.api.connector

import scala.concurrent.Future

trait ApiConnector {
  def apiMutactionExecutor: ApiMutactionExecutor
}

trait ApiMutactionExecutor {
  def execute(mutaction: ApiMutaction): Future[Unit]
}
