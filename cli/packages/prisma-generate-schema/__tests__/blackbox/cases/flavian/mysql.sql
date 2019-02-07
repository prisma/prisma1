-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@flavian
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `Post`
--

DROP TABLE IF EXISTS `Post`;
CREATE TABLE `Post` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `published` tinyint(1) NOT NULL,
  `title` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` mediumtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `User`
--

DROP TABLE IF EXISTS `User`;
CREATE TABLE `User` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `email` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email_UNIQUE` (`email`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PostToUser`
--

DROP TABLE IF EXISTS `_PostToUser`;
CREATE TABLE `_PostToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `PostToUser_AB_unique` (`A`,`B`),
  KEY `B` (`B`),
  CONSTRAINT `_PostToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PostToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_RelayId`
--

DROP TABLE IF EXISTS `_RelayId`;
CREATE TABLE `_RelayId` (
  `id` varchar(36) NOT NULL,
  `stableModelIdentifier` varchar(25) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


-- Dump completed on 2019-02-06 22:04:18
