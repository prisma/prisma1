-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@relations
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `BillingInfo`
--

DROP TABLE IF EXISTS `BillingInfo`;
CREATE TABLE `BillingInfo` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `account` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BillingInfoWithoutConnection`
--

DROP TABLE IF EXISTS `BillingInfoWithoutConnection`;
CREATE TABLE `BillingInfoWithoutConnection` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `account` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `OptionalDetails`
--

DROP TABLE IF EXISTS `OptionalDetails`;
CREATE TABLE `OptionalDetails` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `text` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `OptionalDetailsWithoutConnection`
--

DROP TABLE IF EXISTS `OptionalDetailsWithoutConnection`;
CREATE TABLE `OptionalDetailsWithoutConnection` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `text` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Post`
--

DROP TABLE IF EXISTS `Post`;
CREATE TABLE `Post` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `text` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `count` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `PostWithoutConnection`
--

DROP TABLE IF EXISTS `PostWithoutConnection`;
CREATE TABLE `PostWithoutConnection` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `text` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `count` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `User`
--

DROP TABLE IF EXISTS `User`;
CREATE TABLE `User` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BillingInfoToUser`
--

DROP TABLE IF EXISTS `_BillingInfoToUser`;
CREATE TABLE `_BillingInfoToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BillingInfoToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `BillingInfo` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BillingInfoToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BillingInfoWithoutConnectionToUser`
--

DROP TABLE IF EXISTS `_BillingInfoWithoutConnectionToUser`;
CREATE TABLE `_BillingInfoWithoutConnectionToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BillingInfoWithoutConnectionToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `BillingInfoWithoutConnection` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BillingInfoWithoutConnectionToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_LikesByUser`
--

DROP TABLE IF EXISTS `_LikesByUser`;
CREATE TABLE `_LikesByUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_LikesByUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_LikesByUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_OptionalDetailsToUser`
--

DROP TABLE IF EXISTS `_OptionalDetailsToUser`;
CREATE TABLE `_OptionalDetailsToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_OptionalDetailsToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `OptionalDetails` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_OptionalDetailsToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_OptionalDetailsWithoutConnectionToUser`
--

DROP TABLE IF EXISTS `_OptionalDetailsWithoutConnectionToUser`;
CREATE TABLE `_OptionalDetailsWithoutConnectionToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_OptionalDetailsWithoutConnectionToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `OptionalDetailsWithoutConnection` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_OptionalDetailsWithoutConnectionToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PostWithoutConnectionToUser`
--

DROP TABLE IF EXISTS `_PostWithoutConnectionToUser`;
CREATE TABLE `_PostWithoutConnectionToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PostWithoutConnectionToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `PostWithoutConnection` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PostWithoutConnectionToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PostsByUser`
--

DROP TABLE IF EXISTS `_PostsByUser`;
CREATE TABLE `_PostsByUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PostsByUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PostsByUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
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
