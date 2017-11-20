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
    // CLIENT
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Client` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `email` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `gettingStartedStatus` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `password` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `createdAt` datetime(3) NOT NULL,
        `updatedAt` datetime(3) NOT NULL,
        `resetPasswordSecret` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `source` varchar(255) CHARACTER SET utf8 NOT NULL,
        `auth0Id` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `Auth0IdentityProvider` enum('auth0','github','google-oauth2') COLLATE utf8_unicode_ci DEFAULT NULL,
        `isAuth0IdentityProviderEmail` tinyint(4) NOT NULL DEFAULT '0',
        `isBeta` tinyint(1) NOT NULL DEFAULT '0',
        PRIMARY KEY (`id`),
        UNIQUE KEY `client_auth0id_uniq` (`auth0Id`),
        UNIQUE KEY `email_UNIQUE` (`email`(191))
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // PROJECT
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Project` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `alias` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
        `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `revision` int(11) NOT NULL DEFAULT '1',
        `clientId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `allowQueries` tinyint(1) NOT NULL DEFAULT '1',
        `allowMutations` tinyint(1) NOT NULL DEFAULT '1',
       |`model` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
       |`migrationSteps` mediumtext COLLATE utf8_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`, `revision`),
        UNIQUE KEY `project_clientid_projectname_uniq` (`clientId`,`name`),
        UNIQUE KEY `project_alias_uniq` (`alias`),
        CONSTRAINT `project_clientid_foreign` FOREIGN KEY (`clientId`) REFERENCES `Client` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // SEAT
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Seat` (
        `id` varchar(25) CHARACTER SET utf8 NOT NULL DEFAULT '',
        `clientId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `status` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
        `email` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`),
        UNIQUE KEY `seat_clientId_projectid_uniq` (`clientId`,`projectId`),
        UNIQUE KEY `seat_projectid_email_uniq` (`projectId`,`email`),
        KEY `seat_clientid_foreign` (`clientId`),
        CONSTRAINT `seat_clientid_foreign` FOREIGN KEY (`clientId`) REFERENCES `Client` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `seat_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;"""
  )
}
