package cool.graph.deploy.database.tables

import cool.graph.shared.models.MigrationStatus
import cool.graph.shared.models.MigrationStatus.MigrationStatus
import play.api.libs.json.JsValue
import slick.dbio.Effect.{Read, Write}
import slick.jdbc.MySQLProfile.api._
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction, SqlAction}

case class Migration(
    projectId: String,
    revision: Int,
    schema: JsValue,
    status: MigrationStatus,
    progress: Int,
    steps: JsValue,
    errors: JsValue
)

class MigrationTable(tag: Tag) extends Table[Migration](tag, "Migration") {
  implicit val statusMapper = MigrationTable.statusMapper
  implicit val jsonMapper   = MappedColumns.jsonMapper

  def projectId = column[String]("projectId")
  def revision  = column[Int]("revision")
  def schema    = column[JsValue]("schema")
  def status    = column[MigrationStatus]("status")
  def progress  = column[Int]("progress")
  def steps     = column[JsValue]("steps")
  def errors    = column[JsValue]("errors")

  def migration = foreignKey("migrations_projectid_foreign", projectId, Tables.Projects)(_.id)
  def *         = (projectId, revision, schema, status, progress, steps, errors) <> (Migration.tupled, Migration.unapply)
}

object MigrationTable {
  implicit val statusMapper = MappedColumnType.base[MigrationStatus, String](
    _.toString,
    MigrationStatus.withName
  )

  // todo: Take a hard look at the code and determine if this is necessary
  // Retrieves the last migration for the project, regardless of its status
  def lastRevision(projectId: String): SqlAction[Option[Int], NoStream, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === projectId
    } yield migration.revision

    baseQuery.max.result
  }

  def lastSuccessfulMigration(projectId: String): SqlAction[Option[Migration], NoStream, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === projectId && migration.status === MigrationStatus.Success
    } yield migration

    val query = baseQuery.sortBy(_.revision.desc).take(1)
    query.result.headOption
  }

  def nextOpenMigration(projectId: String): SqlAction[Option[Migration], NoStream, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === projectId
      if migration.status inSet MigrationStatus.openStates
    } yield migration

    val query = baseQuery.sortBy(_.revision.asc).take(1)
    query.result.headOption
  }

  def updateMigrationStatus(projectId: String, revision: Int, status: MigrationStatus): FixedSqlAction[Int, NoStream, Write] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === projectId
      if migration.revision === revision
    } yield migration

    baseQuery.map(_.status).update(status)
  }

  def loadByRevision(projectId: String, revision: Int): SqlAction[Option[Migration], NoStream, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === projectId && migration.revision === revision
    } yield migration

    baseQuery.take(1).result.headOption
  }

  def distinctUnmigratedProjectIds(): FixedSqlStreamingAction[Seq[String], String, Read] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.status inSet MigrationStatus.openStates
    } yield migration.projectId

    baseQuery.distinct.result
  }
}
