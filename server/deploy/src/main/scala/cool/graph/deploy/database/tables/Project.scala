package cool.graph.deploy.database.tables

import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

case class Project(
    id: String,
    alias: Option[String],
    name: String,
    ownerId: String
)

class ProjectTable(tag: Tag) extends Table[Project](tag, "Project") {
  def id      = column[String]("id", O.PrimaryKey)
  def alias   = column[Option[String]]("alias")
  def name    = column[String]("name")
  def ownerId = column[String]("ownerId")
  def *       = (id, alias, name, ownerId) <> ((Project.apply _).tupled, Project.unapply)
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

  def byIdOrAlias(idOrAlias: String): SqlAction[Option[Project], NoStream, Read] = {
    Tables.Projects
      .filter { t =>
        t.id === idOrAlias || t.alias === idOrAlias
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

  def byIdOrAliasWithMigration(id: String): SqlAction[Option[(Project, Migration)], NoStream, Read] = {
    val baseQuery = for {
      project   <- Tables.Projects
      migration <- Tables.Migrations
      if project.id === id || project.alias === id
      if migration.projectId === project.id
      if migration.hasBeenApplied
    } yield (project, migration)

    baseQuery.sortBy(_._2.revision.desc).take(1).result.headOption
  }
}
