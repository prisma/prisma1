package com.prisma.deploy.persistence

import com.prisma.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.migration.MigrationStepMapper
import com.prisma.deploy.migration.mutactions.{AnyMutactionExecutor, MutactionExecutor}

import scala.concurrent.Future

trait DeployPersistencePlugin {
  def projectPersistence: ProjectPersistence
  def migrationPersistence: MigrationPersistence
  def migrationStepMapper: MigrationStepMapper
  def mutactionExecutor: AnyMutactionExecutor

  // other methods
  def createInternalSchema(): Future[Unit] // should be probably rather called initialize
  def createProjectDatabase(id: String): Future[Unit]
  def deleteProjectDatabase(id: String): Future[Unit]
  def getAllDatabaseSizes(): Future[Vector[DatabaseSize]]
}

case class DatabaseSize(name: String, total: Double)
