package cool.graph.deploy.database.persistence

import cool.graph.shared.models.Migration
import cool.graph.shared.models.MigrationStatus.MigrationStatus

import scala.concurrent.Future

trait MigrationPersistence {
  //  def getUnappliedMigration(projectId: String): Future[Option[UnappliedMigration]]
  def loadAll(projectId: String): Future[Seq[Migration]]
  def create(migration: Migration): Future[Migration]
  def getNextMigration(projectId: String): Future[Option[Migration]]
  def getLastMigration(projectId: String): Future[Option[Migration]]
  def updateMigrationStatus(migration: Migration, status: MigrationStatus): Future[Unit]
  def loadDistinctUnmigratedProjectIds(): Future[Seq[String]]
}
