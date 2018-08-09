package com.prisma.deploy.connector.mongo.database

import org.bson.types.ObjectId
import play.api.libs.json.JsValue
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromRegistries, fromProviders}

object ProjectDefinition {

  val codecRegistry = fromRegistries(fromProviders(classOf[ProjectDefinition]), DEFAULT_CODEC_REGISTRY)
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
//  implicit val statusMapper = MigrationTable.statusMapper
//
//  def byId(id: String): SqlAction[Option[ProjectDefinition], NoStream, Read] = {
//    Tables.Projects
//      .filter {
//        _.id === id
//      }
//      .take(1)
//      .result
//      .headOption
//  }
//
//  def byIdWithMigration(id: String): SqlAction[Option[(ProjectDefinition, Migration)], NoStream, Read] = {
//    val baseQuery = for {
//      project   <- Tables.Projects
//      migration <- Tables.Migrations
//      if migration.projectId === id && project.id === id && migration.status === MigrationStatus.Success
//    } yield (project, migration)
//
//    baseQuery.sortBy(_._2.revision.desc).take(1).result.headOption
//  }
//
//  def loadAllWithMigration(): SqlAction[Seq[(ProjectDefinition, Migration)], NoStream, Read] = {
//    // For each project, the latest successful migration (there has to be at least one, e.g. the initial migtation during create)
//    val baseQuery = for {
//      projectIdWithMax <- Tables.Migrations.filter(_.status === MigrationStatus.Success).groupBy(_.projectId).map(x => (x._1, x._2.map(_.revision).max))
//      projectAndMigration <- Tables.Projects join Tables.Migrations on { (pro, mig) =>
//                              pro.id === projectIdWithMax._1 && pro.id === mig.projectId && mig.revision === projectIdWithMax._2
//                            }
//    } yield (projectAndMigration._1, projectAndMigration._2)
//
//    baseQuery.result
//  }
}
