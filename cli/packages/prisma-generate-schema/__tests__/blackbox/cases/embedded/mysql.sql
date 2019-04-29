-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@embedded
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `Child`
--

DROP TABLE IF EXISTS `Child`;
CREATE TABLE `Child` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `c` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `c_UNIQUE` (`c`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Friend`
--

DROP TABLE IF EXISTS `Friend`;
CREATE TABLE `Friend` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `f` mediumtext COLLATE utf8mb4_unicode_ci,
  `test` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `f_UNIQUE` (`f`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Joint`
--

DROP TABLE IF EXISTS `Joint`;
CREATE TABLE `Joint` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `j` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Parent`
--

DROP TABLE IF EXISTS `Parent`;
CREATE TABLE `Parent` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `p` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `p_UNIQUE` (`p`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ChildToFriend`
--

DROP TABLE IF EXISTS `_ChildToFriend`;
CREATE TABLE `_ChildToFriend` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ChildToFriend_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Child` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ChildToFriend_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Friend` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ChildToJoint`
--

DROP TABLE IF EXISTS `_ChildToJoint`;
CREATE TABLE `_ChildToJoint` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ChildToJoint_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Child` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ChildToJoint_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Joint` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ChildToParent`
--

DROP TABLE IF EXISTS `_ChildToParent`;
CREATE TABLE `_ChildToParent` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ChildToParent_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Child` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ChildToParent_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Parent` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_JointToParent`
--

DROP TABLE IF EXISTS `_JointToParent`;
CREATE TABLE `_JointToParent` (
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_JointToParent_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Joint` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_JointToParent_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Parent` (`id`) ON DELETE CASCADE
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
