package cool.graph.deploy.database.tables

import cool.graph.shared.models.MigrationStatus
import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.sql.{FixedSqlStreamingAction, SqlAction}

case class Project(
    id: String,
    ownerId: Option[String]
)

class ProjectTable(tag: Tag) extends Table[Project](tag, "Project") {
  def id      = column[String]("id", O.PrimaryKey)
  def ownerId = column[Option[String]]("ownerId")
  def *       = (id, ownerId) <> ((Project.apply _).tupled, Project.unapply)
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

  def allWithUnappliedMigrations: FixedSqlStreamingAction[Seq[Project], Project, Read] = {
    val baseQuery = for {
      project   <- Tables.Projects
      migration <- Tables.Migrations
      if project.id === migration.projectId
      if migration.status inSet MigrationStatus.openStates
    } yield project

    baseQuery.distinct.result
  }
}
