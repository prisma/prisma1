package cool.graph.deploy.database.tables

import cool.graph.shared.models.Region
import cool.graph.shared.models.Region.Region
import play.api.libs.json.JsValue
import slick.dbio.Effect.{Read, Write}
import slick.jdbc.MySQLProfile.api._
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction, SqlAction}

case class Migration(
    projectId: String,
    revision: Int,
    schema: JsValue,
    steps: JsValue,
    hasBeenApplied: Boolean
)

class MigrationTable(tag: Tag) extends Table[Migration](tag, "Migration") {
//  implicit val RegionMapper     = ProjectTable.regionMapper
//  implicit val stringListMapper = MappedColumns.stringListMapper
  implicit val jsonMapper = MappedColumns.jsonMapper

  def projectId      = column[String]("projectId")
  def revision       = column[Int]("revision")
  def schema         = column[JsValue]("schema")
  def steps          = column[JsValue]("steps")
  def hasBeenApplied = column[Boolean]("hasBeenApplied")
  //  def id       = column[String]("id", O.PrimaryKey)
  //  def alias    = column[Option[String]]("alias")
  //  def name     = column[String]("name")
  //  def clientId = column[String]("clientId")
//  def pk        = primaryKey("pk_migrations", (projectId, revision))
  def migration = foreignKey("migrations_projectid_foreign", projectId, Tables.Projects)(_.id)
  def *         = (projectId, revision, schema, steps, hasBeenApplied) <> ((Migration.apply _).tupled, Migration.unapply)
}

object MigrationTable {

  // Retrieves the last migration for the project, regardless of it being applied or unapplied
  def lastMigrationForProject(id: String): SqlAction[Option[Migration], NoStream, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === id
    } yield migration

    val query = baseQuery.sortBy(_.revision.desc).take(1)
    query.result.headOption
  }

  def lastAppliedMigrationForProject(id: String): SqlAction[Option[Migration], NoStream, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === id && migration.hasBeenApplied
    } yield migration

    val query = baseQuery.sortBy(_.revision.desc).take(1)
    query.result.headOption
  }

  def nextUnappliedMigrationForProject(id: String): SqlAction[Option[Migration], NoStream, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === id
      if !migration.hasBeenApplied
    } yield migration

    val query = baseQuery.sortBy(_.revision.asc).take(1)
    query.result.headOption
  }

  def markAsApplied(id: String, revision: Int): FixedSqlAction[Int, NoStream, Write] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === id
      if migration.revision === revision
    } yield migration

    baseQuery.map(_.hasBeenApplied).update(true)
  }

  def getUnappliedMigration: SqlAction[Option[Migration], NoStream, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if !migration.hasBeenApplied
    } yield migration

    baseQuery.sortBy(_.revision.asc).take(1).result.headOption
  }

//  def unappliedMigrations(): FixedSqlStreamingAction[Seq[Project], Project, Read] = {
//    val baseQuery = for {
//      project <- Tables.Projects
//      if !project.hasBeenApplied
//    } yield project
//    val sorted = baseQuery.sortBy(_.revision * -1).take(1) // bug: use lowest unapplied
//    sorted.result
//  }
}
