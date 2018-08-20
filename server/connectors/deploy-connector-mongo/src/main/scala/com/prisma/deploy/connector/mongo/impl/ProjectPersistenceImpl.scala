package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.ProjectPersistence
import com.prisma.deploy.connector.mongo.database.{ProjectDefinition, ProjectTable}
import com.prisma.shared.models.Project
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

case class ProjectPersistenceImpl(
    internalDatabase: MongoDatabase
)(implicit ec: ExecutionContext)
    extends ProjectPersistence {
  val projects: MongoCollection[ProjectDefinition] = internalDatabase.getCollection("Project")

  override def load(id: String): Future[Option[Project]] = {
    ProjectTable
      .byIdWithMigration(id, internalDatabase)
      .map(optRes => optRes.map(res => DbToModelMapper.convert(res._1, res._2)))
  }

  override def create(project: Project): Future[Unit] = {
    val addProject: ProjectDefinition = ModelToDbMapper.convert(project)

    projects.insertOne(addProject).toFuture().map(_ => ())
  }

  override def delete(projectId: String): Future[Unit] = {
    projects.deleteOne(Filters.eq("id", projectId)).toFuture().map(_ => ())
  }

  override def loadAll(): Future[Seq[Project]] = {
    ProjectTable.loadAllWithMigration(internalDatabase).map(_.map { case (p, m) => DbToModelMapper.convert(p, m) })
  }

  override def update(project: Project): Future[_] = {
    val dbRow = ModelToDbMapper.convert(project)
    projects.replaceOne(Filters.eq("id", project.id), dbRow).toFuture
  }
}
