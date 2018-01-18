package com.prisma.deploy.database.tables

import com.github.tototoshi.slick.MySQLJodaSupport
import com.prisma.shared.models.MigrationStatus
import com.prisma.shared.models.MigrationStatus.MigrationStatus
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import slick.dbio.Effect.{Read, Write}
import slick.jdbc.MySQLProfile.api._
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction, SqlAction}

case class Migration(
    projectId: String,
    revision: Int,
    schema: JsValue,
    functions: JsValue,
    status: MigrationStatus,
    applied: Int,
    rolledBack: Int,
    steps: JsValue,
    errors: JsValue,
    startedAt: Option[DateTime],
    finishedAt: Option[DateTime]
)

class MigrationTable(tag: Tag) extends Table[Migration](tag, "Migration") {
  implicit val statusMapper = MigrationTable.statusMapper
  implicit val jsonMapper   = MigrationTable.jsonMapper
  implicit val jodaMapper   = MySQLJodaSupport.datetimeTypeMapper

  def projectId  = column[String]("projectId")
  def revision   = column[Int]("revision")
  def schema     = column[JsValue]("schema")
  def functions  = column[JsValue]("functions")
  def status     = column[MigrationStatus]("status")
  def applied    = column[Int]("applied")
  def rolledBack = column[Int]("rolledBack")
  def steps      = column[JsValue]("steps")
  def errors     = column[JsValue]("errors")
  def startedAt  = column[Option[DateTime]]("startedAt")
  def finishedAt = column[Option[DateTime]]("finishedAt")

  def migration = foreignKey("migrations_projectid_foreign", projectId, Tables.Projects)(_.id)
  def *         = (projectId, revision, schema, functions, status, applied, rolledBack, steps, errors, startedAt, finishedAt) <> (Migration.tupled, Migration.unapply)
}

object MigrationTable {
  implicit val jsonMapper = MappedColumns.jsonMapper
  implicit val jodaMapper = MySQLJodaSupport.datetimeTypeMapper
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

  private def updateBaseQuery(projectId: String, revision: Int) = {
    for {
      migration <- Tables.Migrations
      if migration.projectId === projectId
      if migration.revision === revision
    } yield migration
  }

  def updateMigrationStatus(projectId: String, revision: Int, status: MigrationStatus): FixedSqlAction[Int, NoStream, Write] = {
    updateBaseQuery(projectId, revision).map(_.status).update(status)
  }

  def updateMigrationErrors(projectId: String, revision: Int, errors: JsValue) = {
    updateBaseQuery(projectId, revision).map(_.errors).update(errors)
  }

  def updateMigrationApplied(projectId: String, revision: Int, applied: Int): FixedSqlAction[Int, NoStream, Write] = {
    updateBaseQuery(projectId, revision).map(_.applied).update(applied)
  }

  def updateMigrationRolledBack(projectId: String, revision: Int, rolledBack: Int): FixedSqlAction[Int, NoStream, Write] = {
    updateBaseQuery(projectId, revision).map(_.rolledBack).update(rolledBack)
  }

  def updateStartedAt(projectId: String, revision: Int, startedAt: DateTime) = {
    updateBaseQuery(projectId, revision).map(_.startedAt).update(Some(startedAt))
  }

  def updateFinishedAt(projectId: String, revision: Int, finishedAt: DateTime) = {
    updateBaseQuery(projectId, revision).map(_.finishedAt).update(Some(finishedAt))
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
