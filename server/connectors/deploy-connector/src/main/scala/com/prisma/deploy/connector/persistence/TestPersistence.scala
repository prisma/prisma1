package com.prisma.deploy.connector.persistence

import com.prisma.shared.models.MigrationStatus.MigrationStatus
import com.prisma.shared.models.{Migration, MigrationId}
import org.joda.time.DateTime

import scala.concurrent.Future

trait TestPersistence {
  def lock(): Future[Int]

  def byId(migrationId: MigrationId): Future[Option[Migration]]
  def loadAll(projectId: String): Future[Seq[Migration]]
  def create(migration: Migration): Future[Migration]
  def getNextMigration(projectId: String): Future[Option[Migration]]
  def getLastMigration(projectId: String): Future[Option[Migration]]

  def updateMigrationStatus(id: MigrationId, status: MigrationStatus): Future[Unit]
  def updateMigrationErrors(id: MigrationId, errors: Vector[String]): Future[Unit]
  def updateMigrationApplied(id: MigrationId, applied: Int): Future[Unit]
  def updateMigrationRolledBack(id: MigrationId, rolledBack: Int): Future[Unit]
  def updateStartedAt(id: MigrationId, startedAt: DateTime): Future[Unit]
  def updateFinishedAt(id: MigrationId, finishedAt: DateTime): Future[Unit]

  def loadDistinctUnmigratedProjectIds(): Future[Seq[String]]
}
