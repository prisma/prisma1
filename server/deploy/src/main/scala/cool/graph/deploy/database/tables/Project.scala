package cool.graph.deploy.database.tables

import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

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
      if migration.projectId === id && project.id === id && migration.hasBeenApplied
    } yield (project, migration)

    baseQuery.sortBy(_._2.revision.desc).take(1).result.headOption
  }
}
