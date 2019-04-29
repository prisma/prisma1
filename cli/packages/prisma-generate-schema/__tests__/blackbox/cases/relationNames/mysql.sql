-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@relationNames
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `A`
--

DROP TABLE IF EXISTS `A`;
CREATE TABLE `A` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
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
-- Table structure for table `_AToC`
--

DROP TABLE IF EXISTS `_AToC`;
CREATE TABLE `_AToC` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `C` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BToC`
--

DROP TABLE IF EXISTS `_BToC`;
CREATE TABLE `_BToC` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BToC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `B` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BToC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `C` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_RaToB`
--

DROP TABLE IF EXISTS `_RaToB`;
CREATE TABLE `_RaToB` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_RaToB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_RaToB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `B` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_RaToB2`
--

DROP TABLE IF EXISTS `_RaToB2`;
CREATE TABLE `_RaToB2` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_RaToB2_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_RaToB2_ibfk_2` FOREIGN KEY (`B`) REFERENCES `B` (`id`) ON DELETE CASCADE
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
