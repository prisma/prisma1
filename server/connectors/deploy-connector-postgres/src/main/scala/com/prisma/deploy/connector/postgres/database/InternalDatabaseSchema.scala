package com.prisma.deploy.connector.postgres.database

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

object InternalDatabaseSchema {
  def createDatabaseAction(db: String) = sql"""CREATE DATABASE "#$db";""".as[Option[String]]

  def createSchemaActions(internalSchema: String, recreate: Boolean)(implicit ec: ExecutionContext): DBIO[Unit] = {
    if (recreate) {
      DBIO.seq(dropAction(internalSchema), setupActions(internalSchema))
    } else {
      setupActions(internalSchema)
    }
  }

  def dropAction(internalSchema: String) = DBIO.seq(sqlu"""DROP SCHEMA IF EXISTS "#$internalSchema" CASCADE;""")

  def setupActions(internalSchema: String)(implicit ec: ExecutionContext) =
    DBIO
      .seq(
        sqlu"""CREATE SCHEMA IF NOT EXISTS "#$internalSchema";""",
        sqlu"""SET SCHEMA '#$internalSchema';""",
        // Project
        sqlu"""
      CREATE TABLE IF NOT EXISTS "Project" (
        "id" varchar(200) NOT NULL DEFAULT '',
        "secrets" text DEFAULT NULL,
        "allowQueries" boolean NOT NULL DEFAULT TRUE,
        "allowMutations" boolean NOT NULL DEFAULT TRUE,
        "functions" text DEFAULT NULL,
        PRIMARY KEY ("id")
      );""",
        // Migration
        sqlu"""
      CREATE TABLE IF NOT EXISTS "Migration" (
        "projectId" varchar(200)  NOT NULL DEFAULT '',
        "revision" int NOT NULL DEFAULT '1',
        "schema" text DEFAULT NULL,
        "functions" text  DEFAULT NULL,
        "status" varchar(20) NOT NULL  check ("status" in('PENDING', 'IN_PROGRESS', 'SUCCESS', 'ROLLING_BACK', 'ROLLBACK_SUCCESS', 'ROLLBACK_FAILURE')) DEFAULT 'PENDING',
        "applied" int NOT NULL DEFAULT 0,
        "rolledBack" int NOT NULL DEFAULT 0,
        "steps" text  DEFAULT NULL,
        "errors" text DEFAULT NULL,
        "startedAt" timestamp DEFAULT NULL,
        "finishedAt" timestamp DEFAULT NULL,
        PRIMARY KEY ("projectId", "revision"),
        CONSTRAINT "migrations_projectid_foreign" FOREIGN KEY ("projectId") REFERENCES "Project" ("id") ON DELETE CASCADE ON UPDATE CASCADE
      );""",
        addDataModelColumnToMigrationTable(internalSchema),
        // Internal migrations
        sqlu"""
      CREATE TABLE IF NOT EXISTS "InternalMigration" (
        "id" varchar(255)  NOT NULL,
        "appliedAt" timestamp NOT NULL,
        PRIMARY KEY ("id")
      );""",
        // Telemetry
        sqlu"""
      CREATE TABLE IF NOT EXISTS "TelemetryInfo" (
        "id" varchar(255)  NOT NULL,
        "lastPinged" timestamp,
        PRIMARY KEY ("id")
      );""",
        // CloudSecret
        sqlu"""
      CREATE TABLE IF NOT EXISTS "CloudSecret" (
        "secret" varchar(255) NOT NULL,
        PRIMARY KEY ("secret")
      );"""
      )
      .withPinnedSession // used pinned connection so that the SET SCHEMA statement is valid throughout all statements

  def addDataModelColumnToMigrationTable(internalSchema: String)(implicit ec: ExecutionContext) =
    for {
      doesExist <- doesColumnExist(internalSchema, "Migration", "datamodel")
      _         <- if (doesExist) DBIO.successful(()) else sqlu"""ALTER TABLE "Migration" ADD COLUMN "datamodel" text DEFAULT NULL;"""
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
