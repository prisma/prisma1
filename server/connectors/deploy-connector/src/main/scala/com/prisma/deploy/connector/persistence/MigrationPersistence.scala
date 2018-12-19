package com.prisma.deploy.connector.persistence

import com.prisma.shared.models.MigrationStatus.MigrationStatus
import com.prisma.shared.models.{Migration, MigrationId, MigrationStatus, Schema}
import org.joda.time.DateTime

import scala.concurrent.Future

trait MigrationPersistence {
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

  /**
    * This method can be used in implementations of `loadAll`.
    * The implementation can then load all migrations and pass it to this function.
    * The migrations must be sorted descending on the revision field.
    */
  protected def enrichWithPreviousSchemas(migrations: Vector[Migration]): Vector[Migration] = {
    migrations.map { migration =>
      val previousMigration = migrations.find(mig => mig.status == MigrationStatus.Success && mig.revision < migration.revision)
      migration.copy(previousSchema = previousMigration.map(_.schema).getOrElse(Schema.empty))
    }
  }
}
