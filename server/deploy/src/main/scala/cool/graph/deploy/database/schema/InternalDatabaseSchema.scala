package cool.graph.deploy.database.schema

import slick.jdbc.MySQLProfile.api._

object InternalDatabaseSchema {

  def createSchemaActions(recreate: Boolean): DBIOAction[Unit, NoStream, Effect] = {
    if (recreate) {
      DBIO.seq(dropAction, setupActions)
    } else {
      setupActions
    }
  }

  lazy val dropAction = DBIO.seq(sqlu"DROP SCHEMA IF EXISTS `graphcool`;")

  lazy val setupActions = DBIO.seq(
    sqlu"CREATE SCHEMA IF NOT EXISTS `graphcool` DEFAULT CHARACTER SET latin1;",
    sqlu"USE `graphcool`;",
    // Project
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Project` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `ownerId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`),
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // Migration
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Migration` (
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `revision` int(11) NOT NULL DEFAULT '1',
        `schema` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        `steps` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        `hasBeenApplied` tinyint(1) NOT NULL DEFAULT '0',
        PRIMARY KEY (`projectId`, `revision`),
        CONSTRAINT `migrations_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;"""
  )
}
