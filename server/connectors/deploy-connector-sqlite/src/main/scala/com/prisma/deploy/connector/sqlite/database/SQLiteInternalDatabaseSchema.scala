package com.prisma.deploy.connector.sqlite.database

import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.ExecutionContext

object SQLiteInternalDatabaseSchema {
  def createSchemaActions(databaseName: String, recreate: Boolean)(implicit ec: ExecutionContext): DBIO[Unit] = {
    if (recreate) {
      DBIO.seq(dropAction(databaseName), setupActions(databaseName))
    } else {
      setupActions(databaseName)
    }
  }

  def dropAction(db: String) = DBIO.seq(sqlu"DROP SCHEMA IF EXISTS `#$db`;")

  def setupActions(db: String)(implicit ec: ExecutionContext) =
    DBIO
      .seq(
        sqlu"CREATE SCHEMA IF NOT EXISTS `#$db` DEFAULT CHARACTER SET latin1;",
        sqlu"USE `#$db`;",
        // Project
        sqlu"""
      CREATE TABLE IF NOT EXISTS `Project` (
        `id` varchar(200) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `secrets` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        `allowQueries` tinyint(1) NOT NULL DEFAULT '1',
        `allowMutations` tinyint(1) NOT NULL DEFAULT '1',
        `functions` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
        // Migration
        sqlu"""
      CREATE TABLE IF NOT EXISTS `Migration` (
        `projectId` varchar(200) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `revision` int NOT NULL DEFAULT '1',
        `schema` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        `functions` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        `status` ENUM('PENDING', 'IN_PROGRESS', 'SUCCESS', 'ROLLING_BACK', 'ROLLBACK_SUCCESS', 'ROLLBACK_FAILURE') NOT NULL DEFAULT 'PENDING',
        `applied` int NOT NULL default 0,
        `rolledBack` int NOT NULL default 0,
        `steps` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        `errors` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        `startedAt` datetime DEFAULT NULL,
        `finishedAt` datetime DEFAULT NULL,
        PRIMARY KEY (`projectId`, `revision`),
        CONSTRAINT `migrations_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
        addDataModelColumnToMigrationTable(db),
        // Internal migrations
        sqlu"""
      CREATE TABLE IF NOT EXISTS `InternalMigration` (
        `id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
        `appliedAt` datetime NOT NULL,
        PRIMARY KEY (`id`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
        // Telemetry
        sqlu"""
      CREATE TABLE IF NOT EXISTS `TelemetryInfo` (
        `id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
        `lastPinged` datetime DEFAULT NULL,
        PRIMARY KEY (`id`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
        // CloudSecret
        sqlu"""
      CREATE TABLE IF NOT EXISTS `CloudSecret` (
        `secret` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
        PRIMARY KEY (`secret`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
      )
      .withPinnedSession // used pinned connection so that the USE statement is valid throughout all statements

  def addDataModelColumnToMigrationTable(internalSchema: String)(implicit ec: ExecutionContext) =
    for {
      doesExist <- doesColumnExist(internalSchema, "Migration", "datamodel")
      _ <- if (doesExist) DBIO.successful(())
          else sqlu"""ALTER TABLE `Migration` ADD COLUMN `datamodel` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL;"""
    } yield ()

  def doesColumnExist(schema: String, table: String, column: String)(implicit ec: ExecutionContext): DBIO[Boolean] = {
    sql"""
         select column_name from information_schema.columns
         where table_schema = '#$schema'
         and table_name = '#$table'
         and column_name = '#$column'
      """.as[String].map(_.nonEmpty)
  }
}
