-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@enum
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `A`
--

DROP TABLE IF EXISTS `A`;
CREATE TABLE `A` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `fieldA` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fieldB` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithId`
--

DROP TABLE IF EXISTS `AWithId`;
CREATE TABLE `AWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `fieldA` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fieldB` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithId_fieldC`
--

DROP TABLE IF EXISTS `AWithId_fieldC`;
CREATE TABLE `AWithId_fieldC` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `AWithId_fieldC_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `AWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `A_fieldC`
--

DROP TABLE IF EXISTS `A_fieldC`;
CREATE TABLE `A_fieldC` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `A_fieldC_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `A` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `B`
--

DROP TABLE IF EXISTS `B`;
CREATE TABLE `B` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `C`
--

DROP TABLE IF EXISTS `C`;
CREATE TABLE `C` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `C_field`
--

DROP TABLE IF EXISTS `C_field`;
CREATE TABLE `C_field` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` int(11) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `C_field_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `C` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `D`
--

DROP TABLE IF EXISTS `D`;
CREATE TABLE `D` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `D_field`
--

DROP TABLE IF EXISTS `D_field`;
CREATE TABLE `D_field` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` datetime(3) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `D_field_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `D` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `E`
--

DROP TABLE IF EXISTS `E`;
CREATE TABLE `E` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToB`
--

DROP TABLE IF EXISTS `_AToB`;
CREATE TABLE `_AToB` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `B` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToE`
--

DROP TABLE IF EXISTS `_AToE`;
CREATE TABLE `_AToE` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToE_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToE_ibfk_2` FOREIGN KEY (`B`) REFERENCES `E` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithIdToC`
--

DROP TABLE IF EXISTS `_AWithIdToC`;
CREATE TABLE `_AWithIdToC` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithIdToC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithId` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithIdToC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `C` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithIdToD`
--

DROP TABLE IF EXISTS `_AWithIdToD`;
CREATE TABLE `_AWithIdToD` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithIdToD_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithId` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithIdToD_ibfk_2` FOREIGN KEY (`B`) REFERENCES `D` (`id`) ON DELETE CASCADE
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


-- Dump completed on 2019-04-29 12:50:23
