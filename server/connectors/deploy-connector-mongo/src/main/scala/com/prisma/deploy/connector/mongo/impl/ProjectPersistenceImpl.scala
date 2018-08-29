package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.ProjectPersistence
import com.prisma.deploy.connector.mongo.database.ProjectDocument
import com.prisma.shared.models.{Migration, Project}
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

  val projects: MongoCollection[Document]   = internalDatabase.getCollection("Project")
  val migrations: MongoCollection[Document] = internalDatabase.getCollection("Migration")

  override def load(id: String): Future[Option[Project]] = {
    for {
      project   <- projects.find(Filters.eq("id", id)).collect.toFuture()
      migration <- lastSuccessfulMigrationForProjectId(id)
    } yield {
      if (migration.isEmpty) None else Some(DbMapper.convertToProjectModel(project.head, migration.head))
    }
  }

  override def create(project: Project): Future[Unit] = {
    projects.insertOne(DbMapper.convertToDocument(project)).toFuture().map(_ => ())
  }

  override def delete(projectId: String): Future[Unit] = {
    projects.deleteOne(Filters.eq("id", projectId)).toFuture().map(_ => ())
  }

  override def update(project: Project): Future[_] = {
    val dbRow = DbMapper.convertToDocument(project)
    projects.replaceOne(Filters.eq("id", project.id), dbRow).toFuture
  }

  override def loadAll(): Future[Seq[Project]] = {
    for {
      projects         <- projects.find().collect.toFuture()
      projectDocuments = projects.map(_.as[ProjectDocument])
      migrations       <- Future.sequence(projectDocuments.map(p => lastSuccessfulMigrationForProjectId(p.id)))
    } yield {
      projectDocuments.map(p => (p, migrations.flatten.find(_.projectId == p.id).get))
    }
    loadAllWithMigration().map(_.map { case (p, m) => DbMapper.convertToProjectModel(p, m) })
  }

  private def loadAllWithMigration(): Future[Seq[(ProjectDocument, Migration)]] = {
    for {
      projects         <- projects.find().collect.toFuture()
      projectDocuments = projects.map(_.as[ProjectDocument])
      migrations       <- Future.sequence(projectDocuments.map(p => lastSuccessfulMigrationForProjectId(p.id)))
    } yield {
      projectDocuments.map(p => (p, migrations.flatten.find(_.projectId == p.id).get))
    }
  }

  private def lastSuccessfulMigrationForProjectId(id: String): Future[Option[Migration]] = {
    migrations
      .find(Filters.and(Filters.eq("projectId", id), Filters.eq("status", "SUCCESS")))
      .sort(descending("revision"))
      .collect
      .toFuture()
      .map(_.headOption.map(DbMapper.convertToMigrationModel))
  }
}
