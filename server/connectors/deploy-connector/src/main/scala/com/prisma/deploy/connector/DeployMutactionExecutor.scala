package com.prisma.deploy.connector

import scala.concurrent.Future

trait DeployMutactionExecutor {
  def execute(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema): Future[Unit]
  def rollback(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema): Future[Unit]
}
