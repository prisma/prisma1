package com.prisma.deploy.connector

import scala.concurrent.Future

trait DeployMutactionExecutor {
  def execute(mutaction: DeployMutaction): Future[Unit]
  def rollback(mutaction: DeployMutaction): Future[Unit]
}
