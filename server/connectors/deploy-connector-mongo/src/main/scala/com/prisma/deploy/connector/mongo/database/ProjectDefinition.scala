package com.prisma.deploy.connector.mongo.database

import org.bson.types.ObjectId
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ProjectDefinition(
    id: String,
    ownerId: Option[String],
    webhookUrl: Option[String],
    secrets: JsValue,
    allowQueries: Boolean,
    allowMutations: Boolean,
    functions: JsValue
)

object ProjectTable {

  def byIdWithMigration(id: String, database: MongoDatabase) = {

    val projects: MongoCollection[ProjectDefinition]     = database.getCollection("Project")
    val migrations: MongoCollection[MigrationDefinition] = database.getCollection("Migration")

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

  def loadAllWithMigration(database: MongoDatabase): Future[Seq[(ProjectDefinition, MigrationDefinition)]] = {

// For each project, the latest successful migration (there has to be at least one, e.g. the initial migtation during create)

    val projects: MongoCollection[ProjectDefinition]     = database.getCollection("Project")
    val migrations: MongoCollection[MigrationDefinition] = database.getCollection("Migration")

    val allProjects = projects.find().collect.toFuture()

    def successfulMigrationsForId(id: String): Future[MigrationDefinition] =
      migrations
        .find(Filters.and(Filters.eq("projectId", id), Filters.eq("status", "SUCCESS")))
        .sort(descending("revision"))
        .collect
        .toFuture()
        .map(_.head)

    for {
      projects   <- allProjects
      migrations <- Future.sequence(projects.map(p => successfulMigrationsForId(p.id)))
    } yield {
      projects.map(p => (p, migrations.find(_.projectId == p.id).get))
    }
  }
}
