-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@selfReferencing
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `MultiSelfReferencingB`
--

DROP TABLE IF EXISTS `MultiSelfReferencingB`;
CREATE TABLE `MultiSelfReferencingB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `MultiSelfReferencingBWithId`
--

DROP TABLE IF EXISTS `MultiSelfReferencingBWithId`;
CREATE TABLE `MultiSelfReferencingBWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `MultiSelfReferencingC`
--

DROP TABLE IF EXISTS `MultiSelfReferencingC`;
CREATE TABLE `MultiSelfReferencingC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `MultiSelfReferencingCWithId`
--

DROP TABLE IF EXISTS `MultiSelfReferencingCWithId`;
CREATE TABLE `MultiSelfReferencingCWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `SelfReferencingA`
--

DROP TABLE IF EXISTS `SelfReferencingA`;
CREATE TABLE `SelfReferencingA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `SelfReferencingAWithId`
--

DROP TABLE IF EXISTS `SelfReferencingAWithId`;
CREATE TABLE `SelfReferencingAWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `SelfReferencingB`
--

DROP TABLE IF EXISTS `SelfReferencingB`;
CREATE TABLE `SelfReferencingB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `SelfReferencingBWithId`
--

DROP TABLE IF EXISTS `SelfReferencingBWithId`;
CREATE TABLE `SelfReferencingBWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `SelfReferencingC`
--

DROP TABLE IF EXISTS `SelfReferencingC`;
CREATE TABLE `SelfReferencingC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `SelfReferencingCWithId`
--

DROP TABLE IF EXISTS `SelfReferencingCWithId`;
CREATE TABLE `SelfReferencingCWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` int(11) NOT NULL,
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
  CONSTRAINT `_AToA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `MultiSelfReferencingC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `MultiSelfReferencingC` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AToA2`
--

DROP TABLE IF EXISTS `_AToA2`;
CREATE TABLE `_AToA2` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AToA2_ibfk_1` FOREIGN KEY (`A`) REFERENCES `MultiSelfReferencingB` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AToA2_ibfk_2` FOREIGN KEY (`B`) REFERENCES `MultiSelfReferencingB` (`id`) ON DELETE CASCADE
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

--
-- Table structure for table `_SelfReferencingAToSelfReferencingAWithId`
--

DROP TABLE IF EXISTS `_SelfReferencingAToSelfReferencingAWithId`;
CREATE TABLE `_SelfReferencingAToSelfReferencingAWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_SelfReferencingAToSelfReferencingAWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `SelfReferencingA` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_SelfReferencingAToSelfReferencingAWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `SelfReferencingAWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_SelfReferencingAWithIdToSelfReferencingAWithId`
--

DROP TABLE IF EXISTS `_SelfReferencingAWithIdToSelfReferencingAWithId`;
CREATE TABLE `_SelfReferencingAWithIdToSelfReferencingAWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_SelfReferencingAWithIdToSelfReferencingAWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `SelfReferencingAWithId` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_SelfReferencingAWithIdToSelfReferencingAWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `SelfReferencingAWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_SelfReferencingBToSelfReferencingB`
--

DROP TABLE IF EXISTS `_SelfReferencingBToSelfReferencingB`;
CREATE TABLE `_SelfReferencingBToSelfReferencingB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_SelfReferencingBToSelfReferencingB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `SelfReferencingB` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_SelfReferencingBToSelfReferencingB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `SelfReferencingB` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_SelfReferencingBWithIdToSelfReferencingBWithId`
--

DROP TABLE IF EXISTS `_SelfReferencingBWithIdToSelfReferencingBWithId`;
CREATE TABLE `_SelfReferencingBWithIdToSelfReferencingBWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_SelfReferencingBWithIdToSelfReferencingBWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `SelfReferencingBWithId` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_SelfReferencingBWithIdToSelfReferencingBWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `SelfReferencingBWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_SelfReferencingBWithIdToSelfReferencingCWithId`
--

DROP TABLE IF EXISTS `_SelfReferencingBWithIdToSelfReferencingCWithId`;
CREATE TABLE `_SelfReferencingBWithIdToSelfReferencingCWithId` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_SelfReferencingBWithIdToSelfReferencingCWithId_ibfk_1` FOREIGN KEY (`A`) REFERENCES `SelfReferencingBWithId` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_SelfReferencingBWithIdToSelfReferencingCWithId_ibfk_2` FOREIGN KEY (`B`) REFERENCES `SelfReferencingCWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_SelfReferencingCToSelfReferencingC`
--

DROP TABLE IF EXISTS `_SelfReferencingCToSelfReferencingC`;
CREATE TABLE `_SelfReferencingCToSelfReferencingC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_SelfReferencingCToSelfReferencingC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `SelfReferencingC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_SelfReferencingCToSelfReferencingC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `SelfReferencingC` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_WithIdAToA`
--

DROP TABLE IF EXISTS `_WithIdAToA`;
CREATE TABLE `_WithIdAToA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_WithIdAToA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `MultiSelfReferencingCWithId` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_WithIdAToA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `MultiSelfReferencingCWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_WithIdAToA2`
--

DROP TABLE IF EXISTS `_WithIdAToA2`;
CREATE TABLE `_WithIdAToA2` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_WithIdAToA2_ibfk_1` FOREIGN KEY (`A`) REFERENCES `MultiSelfReferencingBWithId` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_WithIdAToA2_ibfk_2` FOREIGN KEY (`B`) REFERENCES `MultiSelfReferencingBWithId` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Dump completed on 2019-02-06 22:04:18
