package cool.graph.deploy.database.persistence

import cool.graph.shared.models.{Migration, MigrationId}
import cool.graph.shared.models.MigrationStatus.MigrationStatus
import org.joda.time.DateTime

import scala.concurrent.Future

trait MigrationPersistence {
  // todo: this is temporary, as it should probably move to a general db utility or similar.
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
