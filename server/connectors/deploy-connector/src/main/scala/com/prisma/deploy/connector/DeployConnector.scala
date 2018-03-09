package com.prisma.deploy.connector

import scala.concurrent.Future

trait DeployConnector {
  def projectPersistence: ProjectPersistence
  def migrationPersistence: MigrationPersistence
  def deployMutactionExecutor: DeployMutactionExecutor

  def initialize(): Future[Unit]
  def reset(): Future[Unit]
  def shutdown(): Future[Unit]

  // other methods
  def createProjectDatabase(id: String): Future[Unit]
  def deleteProjectDatabase(id: String): Future[Unit]
  def getAllDatabaseSizes(): Future[Vector[DatabaseSize]]
}

case class DatabaseSize(name: String, total: Double)
