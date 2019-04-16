-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@scalars
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `LotsOfRequiredScalars`
--

DROP TABLE IF EXISTS `LotsOfRequiredScalars`;
CREATE TABLE `LotsOfRequiredScalars` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `int` int(11) NOT NULL,
  `string` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `float` decimal(65,30) NOT NULL,
  `dateTime` datetime(3) NOT NULL,
  `json` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `boolean` tinyint(1) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfRequiredScalarsWithID`
--

DROP TABLE IF EXISTS `LotsOfRequiredScalarsWithID`;
CREATE TABLE `LotsOfRequiredScalarsWithID` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `int` int(11) NOT NULL,
  `string` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `float` decimal(65,30) NOT NULL,
  `dateTime` datetime(3) NOT NULL,
  `json` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `boolean` tinyint(1) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarLists`
--

DROP TABLE IF EXISTS `LotsOfScalarLists`;
CREATE TABLE `LotsOfScalarLists` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarListsWithID`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID`;
CREATE TABLE `LotsOfScalarListsWithID` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarListsWithID_boolean`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_boolean`;
CREATE TABLE `LotsOfScalarListsWithID_boolean` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` tinyint(1) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarListsWithID_boolean_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarListsWithID_dateTime`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_dateTime`;
CREATE TABLE `LotsOfScalarListsWithID_dateTime` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` datetime(3) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarListsWithID_dateTime_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarListsWithID_float`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_float`;
CREATE TABLE `LotsOfScalarListsWithID_float` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` decimal(65,30) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarListsWithID_float_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarListsWithID_int`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_int`;
CREATE TABLE `LotsOfScalarListsWithID_int` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` int(11) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarListsWithID_int_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarListsWithID_json`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_json`;
CREATE TABLE `LotsOfScalarListsWithID_json` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `LotsOfScalarListsWithID_json_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarListsWithID_string`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_string`;
CREATE TABLE `LotsOfScalarListsWithID_string` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `LotsOfScalarListsWithID_string_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarLists_boolean`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_boolean`;
CREATE TABLE `LotsOfScalarLists_boolean` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` tinyint(1) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarLists_boolean_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarLists_dateTime`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_dateTime`;
CREATE TABLE `LotsOfScalarLists_dateTime` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` datetime(3) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarLists_dateTime_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarLists_float`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_float`;
CREATE TABLE `LotsOfScalarLists_float` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` decimal(65,30) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarLists_float_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarLists_int`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_int`;
CREATE TABLE `LotsOfScalarLists_int` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` int(11) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarLists_int_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarLists_json`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_json`;
CREATE TABLE `LotsOfScalarLists_json` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `LotsOfScalarLists_json_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarLists_string`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_string`;
CREATE TABLE `LotsOfScalarLists_string` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `LotsOfScalarLists_string_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalars`
--

DROP TABLE IF EXISTS `LotsOfScalars`;
CREATE TABLE `LotsOfScalars` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `int` int(11) DEFAULT NULL,
  `string` mediumtext COLLATE utf8mb4_unicode_ci,
  `float` decimal(65,30) DEFAULT NULL,
  `dateTime` datetime(3) DEFAULT NULL,
  `json` mediumtext COLLATE utf8mb4_unicode_ci,
  `boolean` tinyint(1) DEFAULT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `LotsOfScalarsWithID`
--

DROP TABLE IF EXISTS `LotsOfScalarsWithID`;
CREATE TABLE `LotsOfScalarsWithID` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `int` int(11) DEFAULT NULL,
  `string` mediumtext COLLATE utf8mb4_unicode_ci,
  `float` decimal(65,30) DEFAULT NULL,
  `dateTime` datetime(3) DEFAULT NULL,
  `json` mediumtext COLLATE utf8mb4_unicode_ci,
  `boolean` tinyint(1) DEFAULT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_RelayId`
--

DROP TABLE IF EXISTS `_RelayId`;
CREATE TABLE `_RelayId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `stableModelIdentifier` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Dump completed on 2019-04-10 20:39:46
