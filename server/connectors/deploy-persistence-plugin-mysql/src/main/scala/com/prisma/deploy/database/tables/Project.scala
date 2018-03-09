package com.prisma.deploy.database.tables

import com.prisma.shared.models.MigrationStatus
import play.api.libs.json.JsValue
import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

case class Project(
    id: String,
    ownerId: Option[String],
    webhookUrl: Option[String],
    secrets: JsValue,
    allowQueries: Boolean,
    allowMutations: Boolean,
    functions: JsValue
)

class ProjectTable(tag: Tag) extends Table[Project](tag, "Project") {
  implicit val jsonMapper = MappedColumns.jsonMapper

  def id             = column[String]("id", O.PrimaryKey)
  def ownerId        = column[Option[String]]("webhookUrl")
  def webhookUrl     = column[Option[String]]("ownerId")
  def secrets        = column[JsValue]("secrets")
  def allowQueries   = column[Boolean]("allowQueries")
  def allowMutations = column[Boolean]("allowMutations")
  def functions      = column[JsValue]("functions")

  def * = (id, ownerId, webhookUrl, secrets, allowQueries, allowMutations, functions) <> ((Project.apply _).tupled, Project.unapply)
}

object ProjectTable {
  implicit val statusMapper = MigrationTable.statusMapper

  def byId(id: String): SqlAction[Option[Project], NoStream, Read] = {
    Tables.Projects
      .filter {
        _.id === id
      }
      .take(1)
      .result
      .headOption
  }

  def byIdWithMigration(id: String): SqlAction[Option[(Project, Migration)], NoStream, Read] = {
    val baseQuery = for {
      project   <- Tables.Projects
      migration <- Tables.Migrations
      if migration.projectId === id && project.id === id && migration.status === MigrationStatus.Success
    } yield (project, migration)

    baseQuery.sortBy(_._2.revision.desc).take(1).result.headOption
  }

  def loadAllWithMigration(): SqlAction[Seq[(Project, Migration)], NoStream, Read] = {
    // For each project, the latest successful migration (there has to be at least one, e.g. the initial migtation during create)
    val baseQuery = for {
      projectIdWithMax <- Tables.Migrations.filter(_.status === MigrationStatus.Success).groupBy(_.projectId).map(x => (x._1, x._2.map(_.revision).max))
      projectAndMigration <- Tables.Projects join Tables.Migrations on { (pro, mig) =>
                              pro.id === projectIdWithMax._1 && pro.id === mig.projectId && mig.revision === projectIdWithMax._2
                            }
    } yield (projectAndMigration._1, projectAndMigration._2)

    baseQuery.result
  }
}
