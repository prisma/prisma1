package cool.graph.deploy.database.tables

import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

case class Project(
    id: String,
    clientId: String
)

class ProjectTable(tag: Tag) extends Table[Project](tag, "Project") {
  def id       = column[String]("id", O.PrimaryKey)
  def clientId = column[String]("clientId")

  def client = foreignKey("project_clientid_foreign", clientId, Tables.Clients)(_.id)
  def *      = (id, clientId) <> ((Project.apply _).tupled, Project.unapply)
}

object ProjectTable {
  def byId(id: String): SqlAction[Option[Project], NoStream, Read] = {
    Tables.Projects.filter { _.id === id }.take(1).result.headOption
  }

  def byIdWithMigration(id: String): SqlAction[Option[(Project, Migration)], NoStream, Read] = {
    val baseQuery = for {
      project   <- Tables.Projects
      migration <- Tables.Migrations
      if migration.projectId === id && project.id === id && migration.hasBeenApplied
    } yield (project, migration)

    baseQuery.sortBy(_._2.revision.desc).take(1).result.headOption
  }

//  def byIdWithsNextMigration(id: String): SqlAction[Option[(Project, Migration)], NoStream, Read] = {
//    val baseQuery = for {
//      project   <- Tables.Projects
//      migration <- Tables.Migrations
//      if migration.projectId === project.id && !migration.hasBeenApplied
//    } yield (project, migration)
//
//    baseQuery.sortBy(_._2.revision.asc).take(1).result.headOption
//  }
}

//  def currentProjectByIdOrAlias(idOrAlias: String): SqlAction[Option[Project], NoStream, Read] = {
//    val baseQuery = for {
//      project <- Tables.Projects
//      if project.id === idOrAlias || project.alias === idOrAlias
//      //if project.hasBeenApplied
//    } yield project
//    val query = baseQuery.sortBy(_.revision * -1).take(1)
//
//    query.result.headOption
//  }

//  def markAsApplied(id: String, revision: Int): FixedSqlAction[Int, NoStream, Write] = {
//    val baseQuery = for {
//      project <- Tables.Projects
//      if project.id === id
//      if project.revision === revision
//    } yield project
//
//    baseQuery.map(_.hasBeenApplied).update(true)
//  }
//
//  def unappliedMigrations(): FixedSqlStreamingAction[Seq[Project], Project, Read] = {
//    val baseQuery = for {
//      project <- Tables.Projects
//      if !project.hasBeenApplied
//    } yield project
//    val sorted = baseQuery.sortBy(_.revision * -1).take(1) // bug: use lowest unapplied
//    sorted.result
//  }
//}
