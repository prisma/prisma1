package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.ProjectPersistence
import com.prisma.deploy.connector.mongo.database.{MigrationDocument, ProjectDocument}
import com.prisma.shared.models.Project
import com.prisma.utils.mongo.MongoExtensions
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Sorts.descending
import org.mongodb.scala.{Document, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

case class ProjectPersistenceImpl(
    internalDatabase: MongoDatabase
)(implicit ec: ExecutionContext)
    extends ProjectPersistence
    with MongoExtensions {

  import DbMapper._

  val projects: MongoCollection[Document] = internalDatabase.getCollection("Project")

  override def load(id: String): Future[Option[Project]] = {
    byIdWithMigration(id, internalDatabase)
      .map(optRes => optRes.map(res => DbMapper.convertToProjectModel(res._1, res._2)))
  }

  override def create(project: Project): Future[Unit] = {
    projects.insertOne(DbMapper.convertToDocument(project)).toFuture().map(_ => ())
  }

  override def delete(projectId: String): Future[Unit] = {
    projects.deleteOne(Filters.eq("id", projectId)).toFuture().map(_ => ())
  }

  override def loadAll(): Future[Seq[Project]] = {
    loadAllWithMigration(internalDatabase).map(_.map { case (p, m) => DbMapper.convertToProjectModel(p, m) })
  }

  override def update(project: Project): Future[_] = {
    val dbRow = DbMapper.convertToDocument(project)
    projects.replaceOne(Filters.eq("id", project.id), dbRow).toFuture
  }

  private def loadAllWithMigration(database: MongoDatabase): Future[Seq[(ProjectDocument, MigrationDocument)]] = {
    // For each project, the latest successful migration (there has to be at least one, e.g. the initial migtation during create)

    val projects: MongoCollection[Document]   = database.getCollection("Project")
    val migrations: MongoCollection[Document] = database.getCollection("Migration")

    val allProjects = projects.find().collect.toFuture()

    def successfulMigrationsForId(id: String): Future[MigrationDocument] =
      migrations
        .find(Filters.and(Filters.eq("projectId", id), Filters.eq("status", "SUCCESS")))
        .sort(descending("revision"))
        .collect
        .toFuture()
        .map(_.head.as[MigrationDocument])

    for {
      projects         <- allProjects
      projectDocuments = projects.map(_.as[ProjectDocument])
      migrations       <- Future.sequence(projectDocuments.map(p => successfulMigrationsForId(p.id)))
    } yield {
      projectDocuments.map(p => (p, migrations.find(_.projectId == p.id).get))
    }
  }

  private def byIdWithMigration(id: String, database: MongoDatabase): Future[Option[(Document, Document)]] = {
    val migrations = database.getCollection("Migration")
    val successfulMigrationsForId =
      migrations
        .find(Filters.and(Filters.eq("projectId", id), Filters.eq("status", "SUCCESS")))
        .sort(descending("revision"))
        .collect
        .toFuture()

    val projectForId = projects.find(Filters.eq("id", id)).collect.toFuture()

    for {
      migrations <- successfulMigrationsForId
      project    <- projectForId
    } yield {
      if (migrations.isEmpty) None else Some((project.head, migrations.head))
    }
  }
}
