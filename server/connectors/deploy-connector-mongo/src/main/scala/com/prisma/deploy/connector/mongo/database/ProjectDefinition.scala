package com.prisma.deploy.connector.mongo.database

import com.prisma.shared.models.MigrationStatus
import org.bson.types.ObjectId
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ProjectDefinition {
  def apply(id: String,
            ownerId: Option[String],
            webhookUrl: Option[String],
            secrets: JsValue,
            allowQueries: Boolean,
            allowMutations: Boolean,
            functions: JsValue): ProjectDefinition =
    ProjectDefinition(_id = new ObjectId(), id, ownerId, webhookUrl, secrets, allowQueries, allowMutations, functions)
}

case class ProjectDefinition(
    _id: ObjectId,
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

    val projects: MongoCollection[ProjectDefinition] = database.getCollection("Project")
    val migrations: MongoCollection[Migration]       = database.getCollection("Migration")

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

  def loadAllWithMigration(): Future[Seq[(ProjectDefinition, Migration)]] = {
//    // For each project, the latest successful migration (there has to be at least one, e.g. the initial migtation during create)
//    val baseQuery = for {
//      projectIdWithMax <- Tables.Migrations.filter(_.status === MigrationStatus.Success).groupBy(_.projectId).map(x => (x._1, x._2.map(_.revision).max))
//      projectAndMigration <- Tables.Projects join Tables.Migrations on { (pro, mig) =>
//                              pro.id === projectIdWithMax._1 && pro.id === mig.projectId && mig.revision === projectIdWithMax._2
//                            }
//    } yield (projectAndMigration._1, projectAndMigration._2)
//
//    baseQuery.result
    ???

  }
}
