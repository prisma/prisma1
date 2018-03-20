package com.prisma.api.connector

import com.prisma.shared.models.Project

import scala.concurrent.Future

trait ApiConnector {
  def databaseMutactionExecutor: DatabaseMutactionExecutor
  def dataResolver(project: Project): DataResolver
  def masterDataResolver(project: Project): DataResolver

  def initialize(): Future[Unit]
  def shutdown(): Future[Unit]
}

trait DatabaseMutactionExecutor {
  def execute(mutactions: Vector[DatabaseMutaction], runTransactionally: Boolean = true): Future[Unit]
}
