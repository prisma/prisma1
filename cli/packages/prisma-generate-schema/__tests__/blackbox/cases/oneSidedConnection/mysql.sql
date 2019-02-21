-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@oneSidedConnection
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
-- Table structure for table `E`
--

DROP TABLE IF EXISTS `E`;
CREATE TABLE `E` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `F`
--

DROP TABLE IF EXISTS `F`;
CREATE TABLE `F` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `TypeWithId`
--

DROP TABLE IF EXISTS `TypeWithId`;
CREATE TABLE `TypeWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `TypeWithoutId`
--

DROP TABLE IF EXISTS `TypeWithoutId`;
CREATE TABLE `TypeWithoutId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToTypeWithId`
--

DROP TABLE IF EXISTS `_AToTypeWithId`;
CREATE TABLE `_AToTypeWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToTypeWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToTypeWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToTypeWithoutId`
--

DROP TABLE IF EXISTS `_AToTypeWithoutId`;
CREATE TABLE `_AToTypeWithoutId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToTypeWithoutId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `A` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToTypeWithoutId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithoutId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BToTypeWithId`
--

DROP TABLE IF EXISTS `_BToTypeWithId`;
CREATE TABLE `_BToTypeWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BToTypeWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `B` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BToTypeWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BToTypeWithoutId`
--

DROP TABLE IF EXISTS `_BToTypeWithoutId`;
CREATE TABLE `_BToTypeWithoutId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BToTypeWithoutId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `B` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BToTypeWithoutId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithoutId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CToTypeWithId`
--

DROP TABLE IF EXISTS `_CToTypeWithId`;
CREATE TABLE `_CToTypeWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CToTypeWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `C` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CToTypeWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CToTypeWithoutId`
--

DROP TABLE IF EXISTS `_CToTypeWithoutId`;
CREATE TABLE `_CToTypeWithoutId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CToTypeWithoutId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `C` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CToTypeWithoutId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithoutId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_DToTypeWithId`
--

DROP TABLE IF EXISTS `_DToTypeWithId`;
CREATE TABLE `_DToTypeWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_DToTypeWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `D` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_DToTypeWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_DToTypeWithoutId`
--

DROP TABLE IF EXISTS `_DToTypeWithoutId`;
CREATE TABLE `_DToTypeWithoutId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_DToTypeWithoutId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `D` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_DToTypeWithoutId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithoutId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_EToTypeWithId`
--

DROP TABLE IF EXISTS `_EToTypeWithId`;
CREATE TABLE `_EToTypeWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_EToTypeWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `E` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_EToTypeWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_EToTypeWithoutId`
--

DROP TABLE IF EXISTS `_EToTypeWithoutId`;
CREATE TABLE `_EToTypeWithoutId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_EToTypeWithoutId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `E` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_EToTypeWithoutId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithoutId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FToTypeWithId`
--

DROP TABLE IF EXISTS `_FToTypeWithId`;
CREATE TABLE `_FToTypeWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FToTypeWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `F` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FToTypeWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FToTypeWithoutId`
--

DROP TABLE IF EXISTS `_FToTypeWithoutId`;
CREATE TABLE `_FToTypeWithoutId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FToTypeWithoutId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `F` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FToTypeWithoutId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `TypeWithoutId` (`id`) ON DELETE CASCADE
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
