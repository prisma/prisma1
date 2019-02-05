-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@meshRelation
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `A`
--

DROP TABLE IF EXISTS `A`;
CREATE TABLE `A` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `B`
--

DROP TABLE IF EXISTS `B`;
CREATE TABLE `B` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `B_field`
--

DROP TABLE IF EXISTS `B_field`;
CREATE TABLE `B_field` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `B_field_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `B` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `C`
--

DROP TABLE IF EXISTS `C`;
CREATE TABLE `C` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `expirationDate` datetime(3) DEFAULT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `D`
--

DROP TABLE IF EXISTS `D`;
CREATE TABLE `D` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
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
-- Table structure for table `_AToA`
--

DROP TABLE IF EXISTS `_AToA`;
CREATE TABLE `_AToA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `A` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToB`
--

DROP TABLE IF EXISTS `_AToB`;
CREATE TABLE `_AToB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `B` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToC`
--

DROP TABLE IF EXISTS `_AToC`;
CREATE TABLE `_AToC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `C` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToD`
--

DROP TABLE IF EXISTS `_AToD`;
CREATE TABLE `_AToD` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToD_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToD_ibfk_2` FOREIGN KEY (`B`) REFERENCES `D` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToE`
--

DROP TABLE IF EXISTS `_AToE`;
CREATE TABLE `_AToE` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToE_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToE_ibfk_2` FOREIGN KEY (`B`) REFERENCES `E` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BToB`
--

DROP TABLE IF EXISTS `_BToB`;
CREATE TABLE `_BToB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BToB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `B` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BToB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `B` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BToC`
--

DROP TABLE IF EXISTS `_BToC`;
CREATE TABLE `_BToC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BToC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `B` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BToC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `C` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BToC2`
--

DROP TABLE IF EXISTS `_BToC2`;
CREATE TABLE `_BToC2` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BToC2_ibfk_1` FOREIGN KEY (`A`) REFERENCES `B` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BToC2_ibfk_2` FOREIGN KEY (`B`) REFERENCES `C` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BToD`
--

DROP TABLE IF EXISTS `_BToD`;
CREATE TABLE `_BToD` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BToD_ibfk_1` FOREIGN KEY (`A`) REFERENCES `B` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BToD_ibfk_2` FOREIGN KEY (`B`) REFERENCES `D` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CToC`
--

DROP TABLE IF EXISTS `_CToC`;
CREATE TABLE `_CToC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CToC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `C` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CToC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `C` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CToD`
--

DROP TABLE IF EXISTS `_CToD`;
CREATE TABLE `_CToD` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CToD_ibfk_1` FOREIGN KEY (`A`) REFERENCES `C` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CToD_ibfk_2` FOREIGN KEY (`B`) REFERENCES `D` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_DToD`
--

DROP TABLE IF EXISTS `_DToD`;
CREATE TABLE `_DToD` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_DToD_ibfk_1` FOREIGN KEY (`A`) REFERENCES `D` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_DToD_ibfk_2` FOREIGN KEY (`B`) REFERENCES `D` (`id`) ON DELETE CASCADE
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


-- Dump completed on 2019-02-05 13:39:12
