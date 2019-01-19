package com.prisma.deploy.connector.sqlite.database

import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.ExecutionContext

object SQLiteInternalDatabaseSchema {
  def createSchemaActions(databaseName: String, recreate: Boolean)(implicit ec: ExecutionContext): DBIO[Unit] = {
    if (recreate) {
      DBIO.seq(setupActions(databaseName))
    } else {
      setupActions(databaseName)
    }
  }

  def dropAction(db: String) = DBIO.seq(sqlu"DROP SCHEMA IF EXISTS `#$db`;")

  def setupActions(db: String)(implicit ec: ExecutionContext) =
    DBIO
      .seq(
        sqlu"""
      CREATE TABLE IF NOT EXISTS `Project` (
        `id` varchar(200)  NOT NULL DEFAULT '',
        `secrets` mediumtext  DEFAULT NULL,
        `allowQueries` tinyint(1) NOT NULL DEFAULT '1',
        `allowMutations` tinyint(1) NOT NULL DEFAULT '1',
        `functions` mediumtext  DEFAULT NULL,
        PRIMARY KEY (`id`)
      );""",
        // Migration
        sqlu"""
      CREATE TABLE IF NOT EXISTS `Migration` (
        `projectId` varchar(200) NOT NULL DEFAULT '',
        `revision` int NOT NULL DEFAULT '1',
        `schema` mediumtext DEFAULT NULL,
        `functions` mediumtext DEFAULT NULL,
        `status` mediumtext NOT NULL ,
        `applied` int NOT NULL default 0,
        `rolledBack` int NOT NULL default 0,
        `steps` mediumtext DEFAULT NULL,
        `errors` mediumtext DEFAULT NULL,
        `startedAt` datetime DEFAULT NULL,
        `finishedAt` datetime DEFAULT NULL,
        PRIMARY KEY (`projectId`, `revision`),
        CONSTRAINT `migrations_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      );""",
        addDataModelColumnToMigrationTable(db),
        // Internal migrations
        sqlu"""
      CREATE TABLE IF NOT EXISTS `InternalMigration` (
        `id` varchar(255) NOT NULL,
        `appliedAt` datetime NOT NULL,
        PRIMARY KEY (`id`)
      );""",
        // Telemetry
        sqlu"""
      CREATE TABLE IF NOT EXISTS `TelemetryInfo` (
        `id` varchar(255) NOT NULL,
        `lastPinged` datetime DEFAULT NULL,
        PRIMARY KEY (`id`)
      );""",
        // CloudSecret
        sqlu"""
      CREATE TABLE IF NOT EXISTS `CloudSecret` (
        `secret` varchar(255) NOT NULL,
        PRIMARY KEY (`secret`)
      );""",
      )
      .withPinnedSession // used pinned connection so that the USE statement is valid throughout all statements

  def addDataModelColumnToMigrationTable(internalSchema: String)(implicit ec: ExecutionContext) =
    for { // Fixme
      doesExist <- doesColumnExist(internalSchema, "Migration", "datamodel")
      _ <- if (doesExist) DBIO.successful(())
          else sqlu"""ALTER TABLE `Migration` ADD COLUMN `datamodel` mediumtext DEFAULT NULL;"""
    } yield ()

  //https://stackoverflow.com/questions/6460671/sqlite-schema-information-metadata
  def doesColumnExist(schema: String, table: String, column: String)(implicit ec: ExecutionContext): DBIO[Boolean] = {
    sql"""SELECT m.name FROM sqlite_master AS m JOIN pragma_table_info(m.name) AS p WHERE m.type = 'table' AND m.name = '#$table' AND p.name = '#$column'"""
      .as[String]
      .map(_.nonEmpty)
  }
}
