package cool.graph.system.database.schema

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
    // PROJECT DATABASE
    sqlu"""
      CREATE TABLE IF NOT EXISTS `ProjectDatabase` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `region` varchar(255) CHARACTER SET utf8 NOT NULL DEFAULT 'eu-west-1',
        `name` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
        `isDefaultForRegion` tinyint(1) NOT NULL DEFAULT '0',
        PRIMARY KEY (`id`),
        UNIQUE KEY `region_name_uniq` (`region`,`name`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // PROJECT
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Project` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `clientId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `webhookUrl` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `oauthRedirectUrl` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `twitterConsumerKey` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `twitterConsumerSecret` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `alias` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
        `allowQueries` tinyint(1) NOT NULL DEFAULT '1',
        `allowMutations` tinyint(1) NOT NULL DEFAULT '1',
        `region` varchar(255) CHARACTER SET utf8 NOT NULL DEFAULT 'eu-west-1',
        `revision` int(11) NOT NULL DEFAULT '1',
        `typePositions` text CHARACTER SET utf8,
        `projectDatabaseId` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'eu-west-1-legacy',
        `isEjected` tinyint(1) NOT NULL DEFAULT '0',
        `hasGlobalStarPermission` tinyint(1) NOT NULL DEFAULT '0',
        PRIMARY KEY (`id`),
        UNIQUE KEY `project_clientid_projectname_uniq` (`clientId`,`name`),
        UNIQUE KEY `project_alias_uniq` (`alias`),
        KEY `project_databaseid_foreign` (`projectDatabaseId`),
        CONSTRAINT `project_clientid_foreign` FOREIGN KEY (`clientId`) REFERENCES `Client` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `project_databaseid_foreign` FOREIGN KEY (`projectDatabaseId`) REFERENCES `ProjectDatabase` (`id`) ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // PACKAGEDEFINITION
    sqlu"""
       CREATE TABLE IF NOT EXISTS `PackageDefinition` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `name` varchar(255) CHARACTER SET utf8 NOT NULL,
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `formatVersion` int(11) NOT NULL DEFAULT '1',
        `definition` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
        PRIMARY KEY (`id`),
        KEY `packagedefinition_projectid_foreign` (`projectId`),
        CONSTRAINT `packagedefinition_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
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
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // ACTION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Action` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `isActive` tinyint(1) NOT NULL,
        `triggerType` enum('MUTATION_MODEL','MUTATION_RELATION') COLLATE utf8_unicode_ci NOT NULL,
        `handlerType` enum('WEBHOOK') COLLATE utf8_unicode_ci NOT NULL,
        `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`),
        KEY `fk_Action_Project_projectId` (`projectId`),
        CONSTRAINT `fk_Action_Project_projectId` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // ACTIONHANDLERWEBHOOK
    sqlu"""
      CREATE TABLE IF NOT EXISTS `ActionHandlerWebhook` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `actionId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `url` varchar(2048) CHARACTER SET utf8 NOT NULL DEFAULT '',
        `isAsync` tinyint(1) NOT NULL DEFAULT '1',
        PRIMARY KEY (`id`),
        KEY `fk_ActionHandlerWebhook_Action_actionId` (`actionId`),
        CONSTRAINT `fk_ActionHandlerWebhook_Action_actionId` FOREIGN KEY (`actionId`) REFERENCES `Action` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // MUTATIONLOG
    sqlu"""
       CREATE TABLE IF NOT EXISTS `MutationLog` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `projectId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `clientId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
        `startedAt` datetime(3) NOT NULL,
        `finishedAt` datetime(3) DEFAULT NULL,
        `status` enum('SCHEDULED','SUCCESS','FAILURE','ROLLEDBACK') COLLATE utf8_unicode_ci NOT NULL,
        `failedMutaction` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `input` mediumtext COLLATE utf8_unicode_ci NOT NULL,
        PRIMARY KEY (`id`),
        KEY `mutationlog_clientid_foreign` (`clientId`),
        KEY `mutationlog_projectid_foreign` (`projectId`),
        CONSTRAINT `mutationlog_clientid_foreign` FOREIGN KEY (`clientId`) REFERENCES `Client` (`id`) ON DELETE CASCADE,
        CONSTRAINT `mutationlog_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // MUTATION LOG MUTACTION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `MutationLogMutaction` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `mutationLogId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `index` smallint(6) NOT NULL,
        `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
        `finishedAt` datetime(3) DEFAULT NULL,
        `status` enum('SCHEDULED','SUCCESS','FAILURE','ROLLEDBACK') COLLATE utf8_unicode_ci NOT NULL,
        `error` text COLLATE utf8_unicode_ci,
        `rollbackError` text COLLATE utf8_unicode_ci,
        `input` mediumtext COLLATE utf8_unicode_ci NOT NULL,
        PRIMARY KEY (`id`),
        KEY `mutationlogmutaction_mutationlogid_foreign` (`mutationLogId`),
        CONSTRAINT `mutationlogmutaction_mutationlogid_foreign` FOREIGN KEY (`mutationLogId`) REFERENCES `MutationLog` (`id`) ON DELETE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // MODEL
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Model` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `projectId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `modelName` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `isSystem` tinyint(1) NOT NULL,
        `fieldPositions` text CHARACTER SET utf8,
        PRIMARY KEY (`id`),
        UNIQUE KEY `model_projectid_modelname_uniq` (`projectId`,`modelName`),
        CONSTRAINT `model_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // FUNCTION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Function` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `name` varchar(255) CHARACTER SET utf8 NOT NULL,
        `binding` enum('CUSTOM_MUTATION','CUSTOM_QUERY','SERVERSIDE_SUBSCRIPTION','TRANSFORM_REQUEST','TRANSFORM_ARGUMENT','PRE_WRITE','TRANSFORM_PAYLOAD','TRANSFORM_RESPONSE') COLLATE utf8_unicode_ci NOT NULL,
        `type` enum('WEBHOOK','LAMBDA','AUTH0') COLLATE utf8_unicode_ci NOT NULL,
        `requestPipelineMutationModelId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `serversideSubscriptionQuery` text CHARACTER SET utf8,
        `serversideSubscriptionQueryFilePath` text CHARACTER SET utf8 DEFAULT NULL,
        `lambdaArn` varchar(1000) COLLATE utf8_unicode_ci DEFAULT NULL,
        `webhookUrl` text CHARACTER SET utf8,
        `webhookHeaders` text CHARACTER SET utf8,
        `inlineCode` mediumtext CHARACTER SET utf8,
        `inlineCodeFilePath` text CHARACTER SET utf8 DEFAULT NULL,
        `auth0Id` varchar(1000) COLLATE utf8_unicode_ci DEFAULT NULL,
        `isActive` tinyint(1) NOT NULL DEFAULT '1',
        `requestPipelineMutationOperation` enum('CREATE','UPDATE','DELETE') COLLATE utf8_unicode_ci DEFAULT NULL,
        `schema` mediumtext CHARACTER SET utf8,
        `schemaFilePath` text CHARACTER SET utf8 DEFAULT NULL,
        PRIMARY KEY (`id`),
        UNIQUE KEY `function_projectid_name_uniq` (`projectId`,`name`),
        KEY `function_requestPipelineMutationModelId_foreign` (`requestPipelineMutationModelId`),
        CONSTRAINT `function_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `function_requestPipelineMutationModelId_foreign` FOREIGN KEY (`requestPipelineMutationModelId`) REFERENCES `Model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // INTEGRATION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Integration` (
        `id` varchar(25) CHARACTER SET utf8 NOT NULL,
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `name` varchar(255) CHARACTER SET utf8 NOT NULL,
        `integrationType` varchar(255) CHARACTER SET utf8 NOT NULL,
        `isEnabled` tinyint(1) NOT NULL DEFAULT '1',
        PRIMARY KEY (`id`),
        KEY `integration_projectid_foreign` (`projectId`),
        CONSTRAINT `integration_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // AUTH PROVIDER DIGITS
    sqlu"""
      CREATE TABLE IF NOT EXISTS `AuthProviderDigits` (
        `id` varchar(25) CHARACTER SET utf8 NOT NULL,
        `integrationId` varchar(25) CHARACTER SET utf8 NOT NULL,
        `consumerKey` varchar(255) CHARACTER SET utf8 NOT NULL,
        `consumerSecret` varchar(255) CHARACTER SET utf8 NOT NULL,
        PRIMARY KEY (`id`),
        KEY `authproviderdigits_integrationid_foreign` (`integrationId`),
        CONSTRAINT `authproviderdigits_integrationid_foreign` FOREIGN KEY (`integrationId`) REFERENCES `Integration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // AUTH PROVIDER AUTH0
    sqlu"""
      CREATE TABLE IF NOT EXISTS `AuthProviderAuth0` (
        `id` varchar(25) CHARACTER SET utf8 NOT NULL,
        `integrationId` varchar(25) CHARACTER SET utf8 NOT NULL,
        `domain` varchar(255) CHARACTER SET utf8 NOT NULL,
        `clientId` varchar(255) CHARACTER SET utf8 NOT NULL,
        `clientSecret` varchar(255) CHARACTER SET utf8 NOT NULL,
        PRIMARY KEY (`id`),
        KEY `authproviderauth0_integrationid_foreign` (`integrationId`),
        CONSTRAINT `authproviderauth0_integrationid_foreign` FOREIGN KEY (`integrationId`) REFERENCES `Integration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // SEARCHPROVIDERALGOLIA
    sqlu"""
      CREATE TABLE IF NOT EXISTS `SearchProviderAlgolia` (
        `id` varchar(25) CHARACTER SET utf8 NOT NULL,
        `integrationId` varchar(25) CHARACTER SET utf8 NOT NULL,
        `applicationId` varchar(255) CHARACTER SET utf8 NOT NULL,
        `apiKey` varchar(255) CHARACTER SET utf8 NOT NULL,
        PRIMARY KEY (`id`),
        KEY `searchprovideralgolia_integrationid_foreign` (`integrationId`),
        CONSTRAINT `searchprovideralgolia_integrationid_foreign` FOREIGN KEY (`integrationId`) REFERENCES `Integration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // ALGOLIASYNCQUERY
    sqlu"""
      CREATE TABLE IF NOT EXISTS `AlgoliaSyncQuery` (
        `id` varchar(25) CHARACTER SET utf8 NOT NULL,
        `modelId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `searchProviderAlgoliaId` varchar(25) CHARACTER SET utf8 NOT NULL,
        `indexName` varchar(255) CHARACTER SET utf8 NOT NULL,
        `query` text CHARACTER SET utf8 NOT NULL,
        `isEnabled` tinyint(4) NOT NULL,
        PRIMARY KEY (`id`),
        KEY `algoliasyncquery_modelid_foreign` (`modelId`),
        KEY `algoliasyncquery_searchprovideralgoliaid_foreign` (`searchProviderAlgoliaId`),
        CONSTRAINT `algoliasyncquery_modelid_foreign` FOREIGN KEY (`modelId`) REFERENCES `Model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `algoliasyncquery_searchprovideralgoliaid_foreign` FOREIGN KEY (`searchProviderAlgoliaId`) REFERENCES `SearchProviderAlgolia` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // ACTIONTRIGGERMUTATIONMODEL
    sqlu"""
      CREATE TABLE IF NOT EXISTS `ActionTriggerMutationModel` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `actionId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `modelId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `mutationType` enum('CREATE','UPDATE','DELETE') COLLATE utf8_unicode_ci NOT NULL,
        `fragment` text COLLATE utf8_unicode_ci NOT NULL,
        PRIMARY KEY (`id`),
        KEY `fk_ActionTriggerMutationModel_Action_actionId` (`actionId`),
        KEY `fk_ActionTriggerMutationModel_Model_modelId` (`modelId`),
        CONSTRAINT `fk_ActionTriggerMutationModel_Action_actionId` FOREIGN KEY (`actionId`) REFERENCES `Action` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `fk_ActionTriggerMutationModel_Model_modelId` FOREIGN KEY (`modelId`) REFERENCES `Model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // RELATION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Relation` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `projectId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `modelAId` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `modelBId` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `name` varchar(191) COLLATE utf8_unicode_ci NOT NULL,
        `description` text COLLATE utf8_unicode_ci,
        PRIMARY KEY (`id`),
        UNIQUE KEY `projectId_name_UNIQUE` (`projectId`,`name`),
        KEY `relation_modelaid_foreign` (`modelAId`),
        KEY `relation_modelbid_foreign` (`modelBId`),
        CONSTRAINT `relation_modelaid_foreign` FOREIGN KEY (`modelAId`) REFERENCES `Model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `relation_modelbid_foreign` FOREIGN KEY (`modelBId`) REFERENCES `Model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `relation_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // ACTIONTRIGGERMUTATIONRELATION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `ActionTriggerMutationRelation` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `actionId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `relationId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `mutationType` enum('ADD','REMOVE') COLLATE utf8_unicode_ci NOT NULL,
        `fragment` text COLLATE utf8_unicode_ci NOT NULL,
        PRIMARY KEY (`id`),
        KEY `fk_ActionTriggerMutationRelation_Action_actionId` (`actionId`),
        KEY `fk_ActionTriggerMutationRelation_Relation_relationId` (`relationId`),
        CONSTRAINT `fk_ActionTriggerMutationRelation_Action_actionId` FOREIGN KEY (`actionId`) REFERENCES `Action` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `fk_ActionTriggerMutationRelation_Relation_relationId` FOREIGN KEY (`relationId`) REFERENCES `Relation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // ENUM
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Enum` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `name` varchar(255) CHARACTER SET utf8 NOT NULL,
        `values` text CHARACTER SET utf8,
        PRIMARY KEY (`id`),
        UNIQUE KEY `enum_projectid_name_uniq` (`projectId`,`name`),
        CONSTRAINT `enum_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // FIELD
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Field` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `modelId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        `fieldName` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `typeIdentifier` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `relationId` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `relationSide` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `enumValues` text COLLATE utf8_unicode_ci,
        `isRequired` tinyint(1) DEFAULT NULL,
        `isList` tinyint(1) DEFAULT NULL,
        `isUnique` tinyint(1) DEFAULT NULL,
        `isSystem` tinyint(1) DEFAULT NULL,
        `defaultValue` text CHARACTER SET utf8,
        `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `isReadonly` tinyint(1) NOT NULL DEFAULT '0',
        `enumId` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`),
        UNIQUE KEY `field_modelid_fieldname` (`modelId`,`fieldName`),
        KEY `field_relationid_foreign` (`relationId`),
        KEY `field_enumid_foreign_2` (`enumId`),
        CONSTRAINT `field_enumid_foreign_2` FOREIGN KEY (`enumId`) REFERENCES `Enum` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
        CONSTRAINT `field_modelid_foreign` FOREIGN KEY (`modelId`) REFERENCES `Model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `field_relationid_foreign` FOREIGN KEY (`relationId`) REFERENCES `Relation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    //FieldConstraint
    sqlu"""
      CREATE TABLE IF NOT EXISTS `FieldConstraint` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `fieldId` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `constraintType` enum('STRING','NUMBER','BOOLEAN','LIST') COLLATE utf8_unicode_ci NOT NULL,
        `equalsNumber` decimal(65,30) DEFAULT NULL,
        `oneOfNumber` text CHARACTER SET utf8,
        `min` decimal(65,30) DEFAULT NULL,
        `max` decimal(65,30) DEFAULT NULL,
        `exclusiveMin` decimal(65,30) DEFAULT NULL,
        `exclusiveMax` decimal(65,30) DEFAULT NULL,
        `multipleOf` decimal(65,30) DEFAULT NULL,
        `equalsString` text CHARACTER SET utf8mb4,
        `oneOfString` text CHARACTER SET utf8mb4,
        `minLength` int(11) DEFAULT NULL,
        `maxLength` int(11) DEFAULT NULL,
        `startsWith` text CHARACTER SET utf8mb4,
        `endsWith` text CHARACTER SET utf8mb4,
        `includes` text CHARACTER SET utf8mb4,
        `regex` text CHARACTER SET utf8mb4,
        `equalsBoolean` tinyint(1) DEFAULT NULL,
        `uniqueItems` tinyint(1) DEFAULT NULL,
        `minItems` int(11) DEFAULT NULL,
        `maxItems` int(11) DEFAULT NULL,
        PRIMARY KEY (`id`),
        UNIQUE KEY `fieldconstraint_fieldid_uniq` (`fieldId`,`constraintType`),
        CONSTRAINT `fieldconstraint_fieldid_foreign` FOREIGN KEY (`fieldId`) REFERENCES `Field` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // RELATIONFIELDMIRROR
    sqlu"""
      CREATE TABLE IF NOT EXISTS `RelationFieldMirror` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `relationId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `fieldId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        PRIMARY KEY (`id`),
        KEY `relationfieldmirror_relationid_foreign` (`relationId`),
        KEY `relationfieldmirror_fieldid_foreign` (`fieldId`),
        CONSTRAINT `relationfieldmirror_fieldid_foreign` FOREIGN KEY (`fieldId`) REFERENCES `Field` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `relationfieldmirror_relationid_foreign` FOREIGN KEY (`relationId`) REFERENCES `Relation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // PERMISSION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `Permission` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `fieldId` varchar(25) COLLATE utf8_unicode_ci DEFAULT '',
        `userType` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `userPath` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `userRole` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `allowRead` tinyint(1) DEFAULT NULL,
        `allowCreate` tinyint(1) DEFAULT NULL,
        `allowUpdate` tinyint(1) DEFAULT NULL,
        `allowDelete` tinyint(1) DEFAULT NULL,
        `comment` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`),
        KEY `permission_fieldid_foreign` (`fieldId`),
        CONSTRAINT `permission_fieldid_foreign` FOREIGN KEY (`fieldId`) REFERENCES `Field` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // MODELPERMISSION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `ModelPermission` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `modelId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `operation` enum('READ','CREATE','UPDATE','DELETE') COLLATE utf8_unicode_ci NOT NULL,
        `userType` enum('EVERYONE','AUTHENTICATED') COLLATE utf8_unicode_ci NOT NULL,
        `rule` enum('NONE','GRAPH','WEBHOOK') CHARACTER SET utf8 NOT NULL,
        `ruleName` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `ruleGraphQuery` text COLLATE utf8_unicode_ci,
        `ruleGraphQueryFilePath` text COLLATE utf8_unicode_ci DEFAULT NULL,
        `ruleWebhookUrl` text COLLATE utf8_unicode_ci,
        `applyToWholeModel` tinyint(1) NOT NULL DEFAULT '0',
        `isActive` tinyint(1) NOT NULL DEFAULT '0',
        `description` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
        PRIMARY KEY (`id`),
        KEY `modelpermission_modelid_foreign` (`modelId`),
        CONSTRAINT `modelpermission_modelid_foreign` FOREIGN KEY (`modelId`) REFERENCES `Model` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // RELATIONPERMISSION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `RelationPermission` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `relationId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `connect` tinyint(1) NOT NULL,
        `disconnect` tinyint(1) NOT NULL,
        `userType` enum('EVERYONE','AUTHENTICATED') COLLATE utf8_unicode_ci NOT NULL,
        `rule` enum('NONE','GRAPH','WEBHOOK') CHARACTER SET utf8 NOT NULL,
        `ruleName` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `ruleGraphQuery` text COLLATE utf8_unicode_ci,
        `ruleGraphQueryFilePath` text COLLATE utf8_unicode_ci DEFAULT NULL,
        `ruleWebhookUrl` text COLLATE utf8_unicode_ci,
        `isActive` tinyint(4) NOT NULL DEFAULT '0',
        `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`),
        KEY `relationpermission_relationid_foreign` (`relationId`),
        CONSTRAINT `relationpermission_relationid_foreign` FOREIGN KEY (`relationId`) REFERENCES `Relation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // MODELPERMISSIONFIELD
    sqlu"""
      CREATE TABLE IF NOT EXISTS `ModelPermissionField` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `modelPermissionId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `fieldId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        PRIMARY KEY (`id`),
        KEY `modelpermissionfield_modelpermissionid_foreign` (`modelPermissionId`),
        KEY `modelpermission_field_foreign` (`fieldId`),
        CONSTRAINT `modelpermission_fieldid_foreign` FOREIGN KEY (`fieldId`) REFERENCES `Field` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT `modelpermissionfield_modelpermisisonid_foreign` FOREIGN KEY (`modelPermissionId`) REFERENCES `ModelPermission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // RELAYID
    sqlu"""
      CREATE TABLE IF NOT EXISTS `RelayId` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `typeName` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
        PRIMARY KEY (`id`),
        KEY `relayid_typename` (`typeName`) USING BTREE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // ROOTTOKEN
    sqlu"""
      CREATE TABLE IF NOT EXISTS `PermanentAuthToken` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `token` text COLLATE utf8_unicode_ci NOT NULL,
        `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
        `created` datetime DEFAULT NULL,
        PRIMARY KEY (`id`),
        KEY `systemtoken_projectid_foreign` (`projectId`),
        CONSTRAINT `systemtoken_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // FEATURE TOGGLE
    sqlu"""
      CREATE TABLE IF NOT EXISTS `FeatureToggle` (
        `id` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `projectId` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
        `name` varchar(255) CHARACTER SET utf8 NOT NULL,
        `isEnabled` tinyint(1) NOT NULL,
        PRIMARY KEY (`id`),
        UNIQUE KEY `featuretoggle_projectid_name_uniq` (`projectId`,`name`),
        KEY `featuretoggle_projectid_foreign` (`projectId`),
        CONSTRAINT `featuretoggle_projectid_foreign` FOREIGN KEY (`projectId`) REFERENCES `Project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""",
    // _MIGRATION
    sqlu"""
      CREATE TABLE IF NOT EXISTS `_Migration` (
        `id` varchar(4) COLLATE utf8_unicode_ci NOT NULL,
        `runAt` datetime NOT NULL
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;"""
  )
}
