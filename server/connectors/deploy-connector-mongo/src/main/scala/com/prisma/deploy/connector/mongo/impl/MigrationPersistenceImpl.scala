package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.MigrationPersistence
import com.prisma.deploy.connector.mongo.database.MigrationDocument
import com.prisma.shared.models.MigrationStatus.MigrationStatus
import com.prisma.shared.models.{Migration, MigrationId}
import com.prisma.utils.mongo.MongoExtensions
import org.joda.time.DateTime
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.{Filters, Updates}
import org.mongodb.scala.{MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

case class MigrationPersistenceImpl(
    internalDatabase: MongoDatabase
)(implicit ec: ExecutionContext)
    extends MigrationPersistence
    with MongoExtensions {
  import DbMapper._

  val migrations: MongoCollection[Document] = internalDatabase.getCollection("Migration")

  def lock(): Future[Unit] = Future.successful(())

  override def byId(migrationId: MigrationId): Future[Option[Migration]] = {
    migrations
      .find(Filters.and(Filters.eq("projectId", migrationId.projectId), Filters.eq("revision", migrationId.revision)))
      .collect
      .toFuture()
      .map(_.headOption.map(DbMapper.convertToMigrationModel))
  }

  override def loadAll(projectId: String): Future[Seq[Migration]] = {
    migrations
      .find(Filters.eq("projectId", projectId))
      .sort(descending("revision"))
      .collect
      .toFuture()
      .map(_.map(DbMapper.convertToMigrationModel))
  }

  override def create(migration: Migration): Future[Migration] = {
    def lastRevision =
      migrations
        .find(Filters.eq("projectId", migration.projectId))
        .sort(descending("revision"))
        .collect
        .toFuture()
        .map(_.headOption.map(_.as[MigrationDocument].revision))

    for {
      lastRevision       <- lastRevision
      withRevisionBumped = migration.copy(revision = lastRevision.getOrElse(0) + 1)
      _                  <- migrations.insertOne(DbMapper.convertToDocument(withRevisionBumped)).toFuture()
    } yield migration.copy(revision = withRevisionBumped.revision)
  }

  private def updateColumn(id: MigrationId, field: String, value: Any) = {
    migrations
      .updateOne(Filters.and(Filters.eq("projectId", id.projectId), Filters.eq("revision", id.revision)), Updates.set(field, value))
      .toFuture
      .map(_ => ())
  }

  override def updateMigrationStatus(id: MigrationId, status: MigrationStatus): Future[Unit] = {
    updateColumn(id, "status", status.toString)
  }

  override def updateMigrationErrors(id: MigrationId, errors: Vector[String]): Future[Unit] = {
    updateColumn(id, "errors", errors)
  }

  override def updateMigrationApplied(id: MigrationId, applied: Int): Future[Unit] = {
    updateColumn(id, "applied", applied)
  }

  override def updateMigrationRolledBack(id: MigrationId, rolledBack: Int): Future[Unit] = {
    updateColumn(id, "rolledBack", rolledBack)
  }

  override def updateStartedAt(id: MigrationId, startedAt: DateTime): Future[Unit] = {
    updateColumn(id, "startedAt", BsonDateTime(startedAt.getMillis))
  }

  override def updateFinishedAt(id: MigrationId, finishedAt: DateTime): Future[Unit] = {
    updateColumn(id, "finishedAt", BsonDateTime(finishedAt.getMillis))

  }

  override def getLastMigration(projectId: String): Future[Option[Migration]] = {
    migrations
      .find(Filters.and(Filters.eq("projectId", projectId), Filters.eq("status", "SUCCESS")))
      .sort(descending("revision"))
      .collect
      .toFuture()
      .map(_.headOption.map(DbMapper.convertToMigrationModel))
  }

  override def getNextMigration(projectId: String): Future[Option[Migration]] = {
    migrations
      .find(Filters.and(Filters.eq("projectId", projectId),
                        Filters.or(Filters.eq("status", "PENDING"), Filters.eq("status", "IN_PROGRESS"), Filters.eq("status", "ROLLING_BACK"))))
      .sort(ascending("revision"))
      .collect
      .toFuture()
      .map(_.headOption.map(DbMapper.convertToMigrationModel))
  }

  override def loadDistinctUnmigratedProjectIds(): Future[Seq[String]] = {
    migrations
      .find(Filters.or(Filters.eq("status", "PENDING"), Filters.eq("status", "IN_PROGRESS"), Filters.eq("status", "ROLLING_BACK")))
      .collect
      .toFuture()
      .map(_.map(DbMapper.convertToMigrationModel(_).projectId).distinct)
  }
}
