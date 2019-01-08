package com.prisma.deploy.connector

import scala.concurrent.Future

trait DeployMutactionExecutor {
  def execute(mutaction: DeployMutaction, schemaBeforeMigration: Tables): Future[Unit]
  def rollback(mutaction: DeployMutaction, schemaBeforeMigration: Tables): Future[Unit]
}
