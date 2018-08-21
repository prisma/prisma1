package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.MigrationPersistence
import com.prisma.deploy.connector.mongo.database.MigrationDefinition
import com.prisma.shared.models.MigrationStatus.MigrationStatus
import com.prisma.shared.models.{Migration, MigrationId}
import org.joda.time.DateTime
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.{MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

case class MigrationPersistenceImpl(
    internalDatabase: MongoDatabase
)(implicit ec: ExecutionContext)
    extends MigrationPersistence {

  val migrations: MongoCollection[MigrationDefinition] = internalDatabase.getCollection("Migration")

  def lock(): Future[Int] = {
    //    internalDatabase.run(sql"SELECT pg_advisory_lock(1000);".as[String].head.withPinnedSession).transformWith {
    //      case Success(_)   => Future.successful(1)
    //      case Failure(err) => Future.failed(err)
    //    }
    Future.successful(1)
  }

  override def byId(migrationId: MigrationId): Future[Option[Migration]] = {

    val futureMigration: Future[Option[MigrationDefinition]] = migrations
      .find(Filters.and(Filters.eq("projectId", migrationId.projectId), Filters.eq("revision", migrationId.revision)))
      .collect
      .toFuture()
      .map(_.headOption)

    futureMigration.map(migOpt => migOpt.map(mig => DbToModelMapper.convert(mig)))
  }

  override def loadAll(projectId: String): Future[Seq[Migration]] = {
    val futureMigration: Future[Seq[MigrationDefinition]] = migrations
      .find(Filters.eq("projectId", projectId))
      .collect
      .toFuture()

    futureMigration.map(migSeq => migSeq.map(mig => DbToModelMapper.convert(mig)))
  }

  override def create(migration: Migration): Future[Migration] = {
    def lastRevision =
      migrations
        .find(Filters.eq("projectId", migration.projectId))
        .sort(descending("revision"))
        .collect
        .toFuture()
        .map(_.headOption.map(_.revision))

    def addMigration(mig: MigrationDefinition) = migrations.insertOne(mig).toFuture()

    for {
      lastRevision       <- lastRevision
      dbMigration        = ModelToDbMapper.convert(migration)
      withRevisionBumped = dbMigration.copy(revision = lastRevision.getOrElse(0) + 1)
      _                  <- addMigration(withRevisionBumped)
    } yield migration.copy(revision = withRevisionBumped.revision)
  }

  private def updateColumn(id: MigrationId, doc: Document) = {
    migrations
      .updateOne(Filters.and(Filters.eq("projectId", id.projectId), Filters.eq("revision", id.revision)), doc)
      .toFuture
      .map(_ => ())
  }

  override def updateMigrationStatus(id: MigrationId, status: MigrationStatus): Future[Unit] = {
    updateColumn(id, Document("status" -> MigrationDefinition.stringStatus(status)))
  }

  override def updateMigrationErrors(id: MigrationId, errors: Vector[String]): Future[Unit] = {
    updateColumn(id, Document("errors" -> errors))
  }

  override def updateMigrationApplied(id: MigrationId, applied: Int): Future[Unit] = {
    updateColumn(id, Document("applied" -> applied))
  }

  override def updateMigrationRolledBack(id: MigrationId, rolledBack: Int): Future[Unit] = {
    updateColumn(id, Document("rolledBack" -> rolledBack))
  }

  override def updateStartedAt(id: MigrationId, startedAt: DateTime): Future[Unit] = {
    updateColumn(id, Document("startedAt" -> BsonDateTime(startedAt.getMillis)))
  }

  override def updateFinishedAt(id: MigrationId, finishedAt: DateTime): Future[Unit] = {
    updateColumn(id, Document("finishedAt" -> BsonDateTime(finishedAt.getMillis)))

  }

  override def getLastMigration(projectId: String): Future[Option[Migration]] = {
    migrations
      .find(Filters.and(Filters.eq("projectId", projectId), Filters.eq("status", "SUCCESS")))
      .sort(descending("revision"))
      .collect
      .toFuture()
      .map(_.headOption.map(DbToModelMapper.convert))
  }

  override def getNextMigration(projectId: String): Future[Option[Migration]] = {
    migrations
      .find(Filters.and(Filters.eq("projectId", projectId),
                        Filters.or(Filters.eq("status", "PENDING"), Filters.eq("status", "IN_PROGRESS"), Filters.eq("status", "ROLLING_BACK"))))
      .sort(ascending("revision"))
      .collect
      .toFuture()
      .map(_.headOption.map(DbToModelMapper.convert))
  }

  override def loadDistinctUnmigratedProjectIds(): Future[Seq[String]] = {
    migrations
      .find(Filters.or(Filters.eq("status", "PENDING"), Filters.eq("status", "IN_PROGRESS"), Filters.eq("status", "ROLLING_BACK")))
      .collect
      .toFuture()
      .map(_.map(_.projectId).distinct)
  }
}
