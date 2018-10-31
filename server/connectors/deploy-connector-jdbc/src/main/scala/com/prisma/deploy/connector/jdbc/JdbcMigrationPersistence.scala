package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.persistence.MigrationPersistence
import com.prisma.shared.models.{Migration, MigrationId}
import com.prisma.shared.models.MigrationStatus.MigrationStatus
import org.joda.time.DateTime

import scala.concurrent.Future
import org.jooq.impl.DSL._

object MigrationTable {
  val migrationTableName = "Migration"
  val t                  = table(name(migrationTableName))
  val projectId          = field(name(migrationTableName, "projectId"))
  val revision           = field(name(migrationTableName, "revision"))
  val schema             = field(name(migrationTableName, "schema"))
  val functions          = field(name(migrationTableName, "functions"))
  val status             = field(name(migrationTableName, "status"))
  val applied            = field(name(migrationTableName, "applied"))
  val rolledBack         = field(name(migrationTableName, "rolledBack"))
  val steps              = field(name(migrationTableName, "steps"))
  val errors             = field(name(migrationTableName, "errors"))
  val startedAt          = field(name(migrationTableName, "startedAt"))
  val finishedAt         = field(name(migrationTableName, "finishedAt"))
}

case class JdbcMigrationPersistence(slickDatabase: SlickDatabase) extends JdbcPersistenceBase with MigrationPersistence {
  override def byId(migrationId: MigrationId): Future[Option[Migration]] = ???

  override def loadAll(projectId: String): Future[Seq[Migration]] = ???

  override def create(migration: Migration): Future[Migration] = ???

  override def getNextMigration(projectId: String): Future[Option[Migration]] = ???

  override def getLastMigration(projectId: String): Future[Option[Migration]] = ???

  override def updateMigrationStatus(id: MigrationId, status: MigrationStatus): Future[Unit] = ???

  override def updateMigrationErrors(id: MigrationId, errors: Vector[String]): Future[Unit] = ???

  override def updateMigrationApplied(id: MigrationId, applied: Int): Future[Unit] = ???

  override def updateMigrationRolledBack(id: MigrationId, rolledBack: Int): Future[Unit] = ???

  override def updateStartedAt(id: MigrationId, startedAt: DateTime): Future[Unit] = ???

  override def updateFinishedAt(id: MigrationId, finishedAt: DateTime): Future[Unit] = ???

  override def loadDistinctUnmigratedProjectIds(): Future[Seq[String]] = ???
}
