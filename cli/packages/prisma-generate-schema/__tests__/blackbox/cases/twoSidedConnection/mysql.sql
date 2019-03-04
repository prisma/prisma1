-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@twoSidedConnection
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `AWithA`
--

DROP TABLE IF EXISTS `AWithA`;
CREATE TABLE `AWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithB`
--

DROP TABLE IF EXISTS `AWithB`;
CREATE TABLE `AWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithC`
--

DROP TABLE IF EXISTS `AWithC`;
CREATE TABLE `AWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithIdWithA`
--

DROP TABLE IF EXISTS `AWithIdWithA`;
CREATE TABLE `AWithIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithIdWithB`
--

DROP TABLE IF EXISTS `AWithIdWithB`;
CREATE TABLE `AWithIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithIdWithC`
--

DROP TABLE IF EXISTS `AWithIdWithC`;
CREATE TABLE `AWithIdWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithoutIdWithA`
--

DROP TABLE IF EXISTS `AWithoutIdWithA`;
CREATE TABLE `AWithoutIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithoutIdWithB`
--

DROP TABLE IF EXISTS `AWithoutIdWithB`;
CREATE TABLE `AWithoutIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `AWithoutIdWithC`
--

DROP TABLE IF EXISTS `AWithoutIdWithC`;
CREATE TABLE `AWithoutIdWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithA`
--

DROP TABLE IF EXISTS `BWithA`;
CREATE TABLE `BWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithB`
--

DROP TABLE IF EXISTS `BWithB`;
CREATE TABLE `BWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithC`
--

DROP TABLE IF EXISTS `BWithC`;
CREATE TABLE `BWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithIdWithA`
--

DROP TABLE IF EXISTS `BWithIdWithA`;
CREATE TABLE `BWithIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithIdWithB`
--

DROP TABLE IF EXISTS `BWithIdWithB`;
CREATE TABLE `BWithIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithIdWithC`
--

DROP TABLE IF EXISTS `BWithIdWithC`;
CREATE TABLE `BWithIdWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithoutIdWithA`
--

DROP TABLE IF EXISTS `BWithoutIdWithA`;
CREATE TABLE `BWithoutIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithoutIdWithB`
--

DROP TABLE IF EXISTS `BWithoutIdWithB`;
CREATE TABLE `BWithoutIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `BWithoutIdWithC`
--

DROP TABLE IF EXISTS `BWithoutIdWithC`;
CREATE TABLE `BWithoutIdWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithA`
--

DROP TABLE IF EXISTS `CWithA`;
CREATE TABLE `CWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithB`
--

DROP TABLE IF EXISTS `CWithB`;
CREATE TABLE `CWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithC`
--

DROP TABLE IF EXISTS `CWithC`;
CREATE TABLE `CWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithIdWithA`
--

DROP TABLE IF EXISTS `CWithIdWithA`;
CREATE TABLE `CWithIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithIdWithB`
--

DROP TABLE IF EXISTS `CWithIdWithB`;
CREATE TABLE `CWithIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithIdWithC`
--

DROP TABLE IF EXISTS `CWithIdWithC`;
CREATE TABLE `CWithIdWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithoutIdWithA`
--

DROP TABLE IF EXISTS `CWithoutIdWithA`;
CREATE TABLE `CWithoutIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithoutIdWithB`
--

DROP TABLE IF EXISTS `CWithoutIdWithB`;
CREATE TABLE `CWithoutIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CWithoutIdWithC`
--

DROP TABLE IF EXISTS `CWithoutIdWithC`;
CREATE TABLE `CWithoutIdWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `field` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithAToAWithIdWithA`
--

DROP TABLE IF EXISTS `_AWithAToAWithIdWithA`;
CREATE TABLE `_AWithAToAWithIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithAToAWithIdWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithA` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithAToAWithIdWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `AWithIdWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithAToAWithoutIdWithA`
--

DROP TABLE IF EXISTS `_AWithAToAWithoutIdWithA`;
CREATE TABLE `_AWithAToAWithoutIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithAToAWithoutIdWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithA` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithAToAWithoutIdWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `AWithoutIdWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithBToBWithIdWithA`
--

DROP TABLE IF EXISTS `_AWithBToBWithIdWithA`;
CREATE TABLE `_AWithBToBWithIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithBToBWithIdWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithB` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithBToBWithIdWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `BWithIdWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithBToBWithoutIdWithA`
--

DROP TABLE IF EXISTS `_AWithBToBWithoutIdWithA`;
CREATE TABLE `_AWithBToBWithoutIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithBToBWithoutIdWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithB` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithBToBWithoutIdWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `BWithoutIdWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithCToCWithIdWithA`
--

DROP TABLE IF EXISTS `_AWithCToCWithIdWithA`;
CREATE TABLE `_AWithCToCWithIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithCToCWithIdWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithCToCWithIdWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithIdWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithCToCWithoutIdWithA`
--

DROP TABLE IF EXISTS `_AWithCToCWithoutIdWithA`;
CREATE TABLE `_AWithCToCWithoutIdWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithCToCWithoutIdWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithCToCWithoutIdWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithoutIdWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithIdWithBToBWithA`
--

DROP TABLE IF EXISTS `_AWithIdWithBToBWithA`;
CREATE TABLE `_AWithIdWithBToBWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithIdWithBToBWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithIdWithB` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithIdWithBToBWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `BWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithIdWithCToCWithA`
--

DROP TABLE IF EXISTS `_AWithIdWithCToCWithA`;
CREATE TABLE `_AWithIdWithCToCWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithIdWithCToCWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithIdWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithIdWithCToCWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithoutIdWithBToBWithA`
--

DROP TABLE IF EXISTS `_AWithoutIdWithBToBWithA`;
CREATE TABLE `_AWithoutIdWithBToBWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithoutIdWithBToBWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithoutIdWithB` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithoutIdWithBToBWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `BWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AWithoutIdWithCToCWithA`
--

DROP TABLE IF EXISTS `_AWithoutIdWithCToCWithA`;
CREATE TABLE `_AWithoutIdWithCToCWithA` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AWithoutIdWithCToCWithA_ibfk_1` FOREIGN KEY (`A`) REFERENCES `AWithoutIdWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AWithoutIdWithCToCWithA_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithA` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BWithBToBWithIdWithB`
--

DROP TABLE IF EXISTS `_BWithBToBWithIdWithB`;
CREATE TABLE `_BWithBToBWithIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BWithBToBWithIdWithB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `BWithB` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BWithBToBWithIdWithB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `BWithIdWithB` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BWithBToBWithoutIdWithB`
--

DROP TABLE IF EXISTS `_BWithBToBWithoutIdWithB`;
CREATE TABLE `_BWithBToBWithoutIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BWithBToBWithoutIdWithB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `BWithB` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BWithBToBWithoutIdWithB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `BWithoutIdWithB` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BWithCToCWithIdWithB`
--

DROP TABLE IF EXISTS `_BWithCToCWithIdWithB`;
CREATE TABLE `_BWithCToCWithIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BWithCToCWithIdWithB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `BWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BWithCToCWithIdWithB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithIdWithB` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BWithCToCWithoutIdWithB`
--

DROP TABLE IF EXISTS `_BWithCToCWithoutIdWithB`;
CREATE TABLE `_BWithCToCWithoutIdWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BWithCToCWithoutIdWithB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `BWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BWithCToCWithoutIdWithB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithoutIdWithB` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BWithIdWithCToCWithB`
--

DROP TABLE IF EXISTS `_BWithIdWithCToCWithB`;
CREATE TABLE `_BWithIdWithCToCWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BWithIdWithCToCWithB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `BWithIdWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BWithIdWithCToCWithB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithB` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BWithoutIdWithCToCWithB`
--

DROP TABLE IF EXISTS `_BWithoutIdWithCToCWithB`;
CREATE TABLE `_BWithoutIdWithCToCWithB` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BWithoutIdWithCToCWithB_ibfk_1` FOREIGN KEY (`A`) REFERENCES `BWithoutIdWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BWithoutIdWithCToCWithB_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithB` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CWithCToCWithIdWithC`
--

DROP TABLE IF EXISTS `_CWithCToCWithIdWithC`;
CREATE TABLE `_CWithCToCWithIdWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CWithCToCWithIdWithC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `CWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CWithCToCWithIdWithC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithIdWithC` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CWithCToCWithoutIdWithC`
--

DROP TABLE IF EXISTS `_CWithCToCWithoutIdWithC`;
CREATE TABLE `_CWithCToCWithoutIdWithC` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CWithCToCWithoutIdWithC_ibfk_1` FOREIGN KEY (`A`) REFERENCES `CWithC` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CWithCToCWithoutIdWithC_ibfk_2` FOREIGN KEY (`B`) REFERENCES `CWithoutIdWithC` (`id`) ON DELETE CASCADE
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


-- Dump completed on 2019-02-06 22:04:18
