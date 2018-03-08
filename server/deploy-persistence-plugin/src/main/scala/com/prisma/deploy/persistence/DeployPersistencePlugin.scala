package com.prisma.deploy.persistence

import com.prisma.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.migration.mutactions.AnyMutactionExecutor

import scala.concurrent.Future

trait DeployPersistencePlugin {
  def projectPersistence: ProjectPersistence
  def migrationPersistence: MigrationPersistence
  def mutactionExecutor: AnyMutactionExecutor

  // other methods
  def createProjectDatabase(id: String): Future[Unit]
  def deleteProjectDatabase(id: String): Future[Unit]
  def getAllDatabaseSizes(): Future[Vector[DatabaseSize]]

  def initialize(): Future[Unit]
  def reset(): Future[Unit]
  def shutdown(): Future[Unit]
}

case class DatabaseSize(name: String, total: Double)
