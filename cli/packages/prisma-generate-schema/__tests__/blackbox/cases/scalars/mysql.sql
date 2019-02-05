-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@scalars
-- ------------------------------------------------------
-- Server version	5.7.23

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `LotsOfRequiredScalars`
--

DROP TABLE IF EXISTS `LotsOfRequiredScalars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfRequiredScalarsWithID`
--

DROP TABLE IF EXISTS `LotsOfRequiredScalarsWithID`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarLists`
--

DROP TABLE IF EXISTS `LotsOfScalarLists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarLists` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarListsWithID`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarListsWithID` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarListsWithID_boolean`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_boolean`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarListsWithID_boolean` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` tinyint(1) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarListsWithID_boolean_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarListsWithID_dateTime`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_dateTime`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarListsWithID_dateTime` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` datetime(3) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarListsWithID_dateTime_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarListsWithID_float`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_float`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarListsWithID_float` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` decimal(65,30) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarListsWithID_float_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarListsWithID_int`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_int`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarListsWithID_int` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` int(11) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarListsWithID_int_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarListsWithID_json`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_json`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarListsWithID_json` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `LotsOfScalarListsWithID_json_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarListsWithID_string`
--

DROP TABLE IF EXISTS `LotsOfScalarListsWithID_string`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarListsWithID_string` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `LotsOfScalarListsWithID_string_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarListsWithID` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarLists_boolean`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_boolean`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarLists_boolean` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` tinyint(1) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarLists_boolean_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarLists_dateTime`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_dateTime`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarLists_dateTime` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` datetime(3) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarLists_dateTime_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarLists_float`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_float`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarLists_float` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` decimal(65,30) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarLists_float_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarLists_int`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_int`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarLists_int` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` int(11) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `LotsOfScalarLists_int_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarLists_json`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_json`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarLists_json` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `LotsOfScalarLists_json_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarLists_string`
--

DROP TABLE IF EXISTS `LotsOfScalarLists_string`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LotsOfScalarLists_string` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `LotsOfScalarLists_string_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `LotsOfScalarLists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalars`
--

DROP TABLE IF EXISTS `LotsOfScalars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LotsOfScalarsWithID`
--

DROP TABLE IF EXISTS `LotsOfScalarsWithID`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_RelayId`
--

DROP TABLE IF EXISTS `_RelayId`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_RelayId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `stableModelIdentifier` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-02-05  9:56:09
