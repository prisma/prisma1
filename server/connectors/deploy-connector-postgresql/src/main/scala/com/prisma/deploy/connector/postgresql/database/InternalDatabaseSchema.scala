package com.prisma.deploy.connector.postgresql.database

import slick.jdbc.PostgresProfile.api._

object InternalDatabaseSchema {
  def createDatabaseAction(db: String) = sql"""CREATE DATABASE "#$db";""".as[Option[String]]

  def createSchemaActions(internalSchema: String, recreate: Boolean): DBIOAction[Unit, NoStream, Effect] = {
    if (recreate) {
      DBIO.seq(dropAction(internalSchema), setupActions(internalSchema))
    } else {
      setupActions(internalSchema)
    }
  }

  def dropAction(internalSchema: String) = DBIO.seq(sqlu"""DROP SCHEMA IF EXISTS "#$internalSchema" CASCADE;""")

  def setupActions(internalSchema: String) = DBIO.seq(
    sqlu"""CREATE SCHEMA IF NOT EXISTS "#$internalSchema";""",
    sqlu"""SET SCHEMA '#$internalSchema';""",
    // Project
    sqlu"""
      CREATE TABLE IF NOT EXISTS "Project" (
        "id" varchar(200) NOT NULL DEFAULT '',
        "ownerId" varchar(25) DEFAULT NULL,
        "webhookUrl" varchar(255)  DEFAULT NULL,
        "secrets" text DEFAULT NULL,
        "seats" text DEFAULT NULL,
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
        "lastPinged" timestamp NOT NULL,
        PRIMARY KEY ("id")
      );""",
  )
}
