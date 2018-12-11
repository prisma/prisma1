package com.prisma.deploy.connector.jdbc.persistence

import java.sql.ResultSet

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.deploy.connector.persistence.MigrationPersistence
import com.prisma.shared.models
import com.prisma.shared.models.MigrationStatus.MigrationStatus
import com.prisma.shared.models._
import org.joda.time.DateTime
import org.jooq.impl.DSL
import org.jooq.impl.DSL._
import play.api.libs.json.Json
import slick.dbio.DBIO

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

object MigrationTable {
  val migrationTableName = "Migration"
  val t                  = table(name(migrationTableName))
  val projectId          = field(name(migrationTableName, "projectId"))
  val revision           = field(name(migrationTableName, "revision"))
  val schema             = field(name(migrationTableName, "schema"))
  val functions          = field(name(migrationTableName, "functions"))
  val status             = field(name(migrationTableName, "status"))
  val applied            = field(name(migrationTableName, "applied"))
  val rolledBack         = field(name(migrationTableName, "rolledBack"))
  val steps              = field(name(migrationTableName, "steps"))
  val errors             = field(name(migrationTableName, "errors"))
  val startedAt          = field(name(migrationTableName, "startedAt"))
  val finishedAt         = field(name(migrationTableName, "finishedAt"))

  val *             = Seq(projectId, revision, schema, functions, status, applied, rolledBack, steps, errors, startedAt, finishedAt)
  val insertColumns = Seq(revision, schema, functions, status, applied, rolledBack, steps, errors, startedAt, finishedAt)
}

case class JdbcMigrationPersistence(slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcBase with MigrationPersistence {
  import com.prisma.shared.models.MigrationStepsJsonFormatter._
  import com.prisma.shared.models.ProjectJsonFormatter._

  import collection.JavaConverters._

  val mt = MigrationTable

  override def byId(migrationId: MigrationId): Future[Option[Migration]] = {
    val query = sql
      .select(mt.* : _*)
      .from(mt.t)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.revision.equal(placeHolder))

    val migration = database.run(
      queryToDBIO(query)(
        setParams = { pp =>
          pp.setString(migrationId.projectId)
          pp.setInt(migrationId.revision)
        },
        readResult = { rs =>
          if (rs.next()) {
            Some(migrationFromResultSet(rs))
          } else {
            None
          }
        }
      ))
    enrichWithPreviousMigration(migration)
  }

  override def loadAll(projectId: String): Future[Seq[Migration]] = {
    val query = sql
      .select(mt.* : _*)
      .from(mt.t)
      .where(mt.projectId.equal(placeHolder))
      .orderBy(mt.revision.desc())

    database.run(
      queryToDBIO(query)(
        setParams = { pp =>
          pp.setString(projectId)
        },
        readResult = { rs =>
          val buffer = ArrayBuffer.empty[models.Migration]
          while (rs.next()) {
            buffer += migrationFromResultSet(rs)
          }

          enrichWithPreviousSchemas(buffer.toVector)
        }
      ))
  }

  override def create(migration: Migration): Future[Migration] = {
    // Has to be 2 separate queries, not all dbs are able to support INSERT...RETURNING for example
    val revisionQuery = sql.select(
      DSL.coalesce(sql
                     .select(max(mt.revision))
                     .from(mt.t)
                     .where(mt.projectId.equal(placeHolder)),
                   inline(0)))

    val insertQuery = sql
      .insertInto(mt.t)
      .columns(mt.* : _*)
      .values(
        placeHolder,
        placeHolder,
        placeHolder,
        placeHolder,
        placeHolder,
        placeHolder,
        placeHolder,
        placeHolder,
        placeHolder,
        placeHolder,
        placeHolder
      )

    database
      .run(
        queryToDBIO(revisionQuery)(
          setParams = { pp =>
            pp.setString(migration.projectId)
          },
          readResult = { rs =>
            if (rs.next()) {
              rs.getInt(1) + 1
            } else {
              1
            }
          }
        ))
      .flatMap { revision =>
        database
          .run(insertToDBIO(insertQuery)(
            setParams = { pp =>
              val schema    = Json.toJson(migration.schema).toString()
              val functions = Json.toJson(migration.functions).toString()
              val errors    = Json.toJson(migration.errors).toString()
              val steps     = Json.toJson(migration.steps).toString()
              val startedAt = migration.startedAt match {
                case Some(ts) => jodaDateTimeToSqlTimestampUTC(ts)
                case None     => null
              }

              val finishedAt = migration.finishedAt match {
                case Some(ts) => jodaDateTimeToSqlTimestampUTC(ts)
                case None     => null
              }

              pp.setString(migration.projectId)
              pp.setInt(revision)
              pp.setString(schema)
              pp.setString(functions)
              pp.setString(migration.status.toString)
              pp.setInt(migration.applied)
              pp.setInt(migration.rolledBack)
              pp.setString(steps)
              pp.setString(errors)
              pp.setTimestamp(startedAt)
              pp.setTimestamp(finishedAt)
            }
          ))
          .map(_ => migration.copy(revision = revision))
      }
      .flatMap(enrichWithPreviousMigration)
  }

  override def getNextMigration(projectId: String): Future[Option[Migration]] = {
    val query = sql
      .select(mt.* : _*)
      .from(mt.t)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.status.in(Seq.tabulate(MigrationStatus.openStates.length)(_ => placeHolder).asJavaCollection))
      .orderBy(mt.revision.asc())
      .limit(DSL.inline(1))

    val migration = database.run(
      queryToDBIO(query)(
        setParams = { pp =>
          pp.setString(projectId)
          MigrationStatus.openStates.foreach(state => pp.setString(state.toString))
        },
        readResult = { rs =>
          if (rs.next()) {
            Some(migrationFromResultSet(rs))
          } else {
            None
          }
        }
      ))

    enrichWithPreviousMigration(migration)
  }

  override def getLastMigration(projectId: String): Future[Option[Migration]] = {
    val query = sql
      .select(mt.* : _*)
      .from(mt.t)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.status.equal(placeHolder))
      .orderBy(mt.revision.desc())
      .limit(DSL.inline(1))

    val migration = database
      .run(
        queryToDBIO(query)(
          setParams = { pp =>
            pp.setString(projectId)
            pp.setString(MigrationStatus.Success.toString)
          },
          readResult = { rs =>
            if (rs.next()) {
              Some(migrationFromResultSet(rs))
            } else {
              None
            }
          }
        ))

    enrichWithPreviousMigration(migration)
  }

  override def updateMigrationStatus(id: MigrationId, status: MigrationStatus): Future[Unit] = {
    val query = sql
      .update(mt.t)
      .set(mt.status, placeHolder)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.revision.equal(placeHolder))

    database
      .run(
        updateToDBIO(query)(
          setParams = { pp =>
            pp.setString(status.toString)
            pp.setString(id.projectId)
            pp.setInt(id.revision)
          }
        ))
      .map(_ => ())
  }

  override def updateMigrationErrors(id: MigrationId, errors: Vector[String]): Future[Unit] = {
    val query = sql
      .update(mt.t)
      .set(mt.errors, placeHolder)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.revision.equal(placeHolder))

    database
      .run(
        updateToDBIO(query)(
          setParams = { pp =>
            val e = Json.toJson(errors).toString

            pp.setString(e)
            pp.setString(id.projectId)
            pp.setInt(id.revision)
          }
        ))
      .map(_ => ())
  }

  override def updateMigrationApplied(id: MigrationId, applied: Int): Future[Unit] = {
    val query = sql
      .update(mt.t)
      .set(mt.applied, placeHolder)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.revision.equal(placeHolder))

    database
      .run(
        updateToDBIO(query)(
          setParams = { pp =>
            pp.setInt(applied)
            pp.setString(id.projectId)
            pp.setInt(id.revision)
          }
        ))
      .map(_ => ())
  }

  override def updateMigrationRolledBack(id: MigrationId, rolledBack: Int): Future[Unit] = {
    val query = sql
      .update(mt.t)
      .set(mt.rolledBack, placeHolder)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.revision.equal(placeHolder))

    database
      .run(
        updateToDBIO(query)(
          setParams = { pp =>
            pp.setInt(rolledBack)
            pp.setString(id.projectId)
            pp.setInt(id.revision)
          }
        ))
      .map(_ => ())
  }

  override def updateStartedAt(id: MigrationId, startedAt: DateTime): Future[Unit] = {
    val query = sql
      .update(mt.t)
      .set(mt.startedAt, placeHolder)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.revision.equal(placeHolder))

    database
      .run(
        updateToDBIO(query)(
          setParams = { pp =>
            pp.setTimestamp(jodaDateTimeToSqlTimestampUTC(startedAt))
            pp.setString(id.projectId)
            pp.setInt(id.revision)
          }
        ))
      .map(_ => ())
  }

  override def updateFinishedAt(id: MigrationId, finishedAt: DateTime): Future[Unit] = {
    val query = sql
      .update(mt.t)
      .set(mt.finishedAt, placeHolder)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.revision.equal(placeHolder))

    database
      .run(
        updateToDBIO(query)(
          setParams = { pp =>
            pp.setTimestamp(jodaDateTimeToSqlTimestampUTC(finishedAt))
            pp.setString(id.projectId)
            pp.setInt(id.revision)
          }
        ))
      .map(_ => ())
  }

  override def loadDistinctUnmigratedProjectIds(): Future[Seq[String]] = {
    val query = sql
      .selectDistinct(mt.projectId)
      .from(mt.t)
      .where(mt.status.in(Seq.tabulate(MigrationStatus.openStates.length)(_ => placeHolder).asJavaCollection))

    database.run(
      queryToDBIO(query)(
        setParams = { pp =>
          MigrationStatus.openStates.foreach(state => pp.setString(state.toString))
        },
        readResult = { rs =>
          val buffer = ArrayBuffer.empty[String]
          while (rs.next()) {
            buffer += rs.getString(mt.projectId.getName)
          }

          buffer
        }
      ))
  }

  private def enrichWithPreviousMigration(migration: Future[Option[Migration]]): Future[Option[Migration]] = {
    migration.flatMap {
      case Some(mig) => enrichWithPreviousMigration(mig).map(Some(_))
      case None      => Future.successful(None)
    }
  }

  private def enrichWithPreviousMigration(migration: Migration): Future[Migration] = {
    val query = sql
      .select(mt.* : _*)
      .from(mt.t)
      .where(mt.projectId.equal(placeHolder))
      .and(mt.revision.lt(placeHolder))
      .and(mt.status.equal(placeHolder))
      .orderBy(mt.revision.desc())
      .limit(DSL.inline(1))

    database.run(
      queryToDBIO(query)(
        setParams = { pp =>
          pp.setString(migration.projectId)
          pp.setInt(migration.revision)
          pp.setString(MigrationStatus.Success.toString)
        },
        readResult = { rs =>
          if (rs.next()) {
            val previousMigration = migrationFromResultSet(rs)
            migration.copy(previousSchema = previousMigration.schema)
          } else {
            migration
          }
        }
      )
    )
  }

  private def migrationFromResultSet(rs: ResultSet): Migration = {
    val schema    = Json.parse(rs.getString(mt.schema.getName)).as[Schema]
    val functions = Json.parse(rs.getString(mt.functions.getName)).as[Vector[models.Function]]
    val steps     = Json.parse(rs.getString(mt.steps.getName)).as[Vector[MigrationStep]]
    val errors    = Json.parse(rs.getString(mt.errors.getName)).as[Vector[String]]
    val startedAt = rs.getTimestamp(mt.startedAt.getName) match {
      case null => None
      case x    => Some(sqlTimestampToDateTime(x))
    }

    val finishedAt = rs.getTimestamp(mt.finishedAt.getName) match {
      case null => None
      case x    => Some(sqlTimestampToDateTime(x))
    }

    models.Migration(
      projectId = rs.getString(mt.projectId.getName),
      revision = rs.getInt(mt.revision.getName),
      schema,
      functions,
      status = MigrationStatus.withName(rs.getString(mt.status.getName)),
      applied = rs.getInt(mt.applied.getName),
      rolledBack = rs.getInt(mt.rolledBack.getName),
      steps,
      errors,
      startedAt,
      finishedAt,
      previousSchema = Schema.empty
    )
  }
}
