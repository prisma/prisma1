-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@financial
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `Campus`
--

DROP TABLE IF EXISTS `Campus`;
CREATE TABLE `Campus` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci,
  `isActive` tinyint(1) DEFAULT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `FinancialAccount`
--

DROP TABLE IF EXISTS `FinancialAccount`;
CREATE TABLE `FinancialAccount` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `key` mediumtext COLLATE utf8mb4_unicode_ci,
  `description` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `isActive` tinyint(1) NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `key_UNIQUE` (`key`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `FinancialPaymentDetail`
--

DROP TABLE IF EXISTS `FinancialPaymentDetail`;
CREATE TABLE `FinancialPaymentDetail` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `accountNumberMasked` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `creditCardType` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `achType` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `currencyType` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expirationDate` datetime(3) NOT NULL,
  `nameOnCard` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `FinancialScheduledTransaction`
--

DROP TABLE IF EXISTS `FinancialScheduledTransaction`;
CREATE TABLE `FinancialScheduledTransaction` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `endDate` datetime(3) DEFAULT NULL,
  `isActive` tinyint(1) NOT NULL,
  `startDate` datetime(3) DEFAULT NULL,
  `frequency` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(65,30) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `FinancialTransaction`
--

DROP TABLE IF EXISTS `FinancialTransaction`;
CREATE TABLE `FinancialTransaction` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `processedDate` datetime(3) DEFAULT NULL,
  `status` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transactionDate` datetime(3) DEFAULT NULL,
  `amount` decimal(65,30) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Group`
--

DROP TABLE IF EXISTS `Group`;
CREATE TABLE `Group` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `key` mediumtext COLLATE utf8mb4_unicode_ci,
  `description` mediumtext COLLATE utf8mb4_unicode_ci,
  `isActive` tinyint(1) NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `key_UNIQUE` (`key`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `GroupInvite`
--

DROP TABLE IF EXISTS `GroupInvite`;
CREATE TABLE `GroupInvite` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `email` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `GroupMember`
--

DROP TABLE IF EXISTS `GroupMember`;
CREATE TABLE `GroupMember` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `GroupRole`
--

DROP TABLE IF EXISTS `GroupRole`;
CREATE TABLE `GroupRole` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `canEdit` tinyint(1) NOT NULL,
  `canView` tinyint(1) NOT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `isLeader` tinyint(1) DEFAULT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `name_UNIQUE` (`name`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `GroupType`
--

DROP TABLE IF EXISTS `GroupType`;
CREATE TABLE `GroupType` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `name_UNIQUE` (`name`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Location`
--

DROP TABLE IF EXISTS `Location`;
CREATE TABLE `Location` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `city` mediumtext COLLATE utf8mb4_unicode_ci,
  `locationType` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `postalCode` mediumtext COLLATE utf8mb4_unicode_ci,
  `state` mediumtext COLLATE utf8mb4_unicode_ci,
  `street1` mediumtext COLLATE utf8mb4_unicode_ci,
  `street2` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Person`
--

DROP TABLE IF EXISTS `Person`;
CREATE TABLE `Person` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `email` mediumtext COLLATE utf8mb4_unicode_ci,
  `firstName` mediumtext COLLATE utf8mb4_unicode_ci,
  `lastName` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `PhoneNumber`
--

DROP TABLE IF EXISTS `PhoneNumber`;
CREATE TABLE `PhoneNumber` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `number` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
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
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CampusToFinancialAccount`
--

DROP TABLE IF EXISTS `_CampusToFinancialAccount`;
CREATE TABLE `_CampusToFinancialAccount` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CampusToFinancialAccount_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Campus` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CampusToFinancialAccount_ibfk_2` FOREIGN KEY (`B`) REFERENCES `FinancialAccount` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CampusToGroup`
--

DROP TABLE IF EXISTS `_CampusToGroup`;
CREATE TABLE `_CampusToGroup` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CampusToGroup_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Campus` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CampusToGroup_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CampusToLocation`
--

DROP TABLE IF EXISTS `_CampusToLocation`;
CREATE TABLE `_CampusToLocation` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CampusToLocation_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Campus` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CampusToLocation_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Location` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CampusToPhoneNumber`
--

DROP TABLE IF EXISTS `_CampusToPhoneNumber`;
CREATE TABLE `_CampusToPhoneNumber` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CampusToPhoneNumber_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Campus` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CampusToPhoneNumber_ibfk_2` FOREIGN KEY (`B`) REFERENCES `PhoneNumber` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialAccountToFinancialScheduledTransaction`
--

DROP TABLE IF EXISTS `_FinancialAccountToFinancialScheduledTransaction`;
CREATE TABLE `_FinancialAccountToFinancialScheduledTransaction` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialAccountToFinancialScheduledTransaction_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialAccount` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialAccountToFinancialScheduledTransaction_ibfk_2` FOREIGN KEY (`B`) REFERENCES `FinancialScheduledTransaction` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialAccountToFinancialTransaction`
--

DROP TABLE IF EXISTS `_FinancialAccountToFinancialTransaction`;
CREATE TABLE `_FinancialAccountToFinancialTransaction` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialAccountToFinancialTransaction_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialAccount` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialAccountToFinancialTransaction_ibfk_2` FOREIGN KEY (`B`) REFERENCES `FinancialTransaction` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialPaymentDetailToFinancialScheduledTransaction`
--

DROP TABLE IF EXISTS `_FinancialPaymentDetailToFinancialScheduledTransaction`;
CREATE TABLE `_FinancialPaymentDetailToFinancialScheduledTransaction` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialPaymentDetailToFinancialScheduledTransaction_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialPaymentDetail` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialPaymentDetailToFinancialScheduledTransaction_ibfk_2` FOREIGN KEY (`B`) REFERENCES `FinancialScheduledTransaction` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialPaymentDetailToFinancialTransaction`
--

DROP TABLE IF EXISTS `_FinancialPaymentDetailToFinancialTransaction`;
CREATE TABLE `_FinancialPaymentDetailToFinancialTransaction` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialPaymentDetailToFinancialTransaction_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialPaymentDetail` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialPaymentDetailToFinancialTransaction_ibfk_2` FOREIGN KEY (`B`) REFERENCES `FinancialTransaction` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialPaymentDetailToLocation`
--

DROP TABLE IF EXISTS `_FinancialPaymentDetailToLocation`;
CREATE TABLE `_FinancialPaymentDetailToLocation` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialPaymentDetailToLocation_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialPaymentDetail` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialPaymentDetailToLocation_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Location` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialScheduledTransactionToFinancialTransaction`
--

DROP TABLE IF EXISTS `_FinancialScheduledTransactionToFinancialTransaction`;
CREATE TABLE `_FinancialScheduledTransactionToFinancialTransaction` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialScheduledTransactionToFinancialTransaction_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialScheduledTransaction` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialScheduledTransactionToFinancialTransaction_ibfk_2` FOREIGN KEY (`B`) REFERENCES `FinancialTransaction` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialScheduledTransactionToPerson`
--

DROP TABLE IF EXISTS `_FinancialScheduledTransactionToPerson`;
CREATE TABLE `_FinancialScheduledTransactionToPerson` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialScheduledTransactionToPerson_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialScheduledTransaction` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialScheduledTransactionToPerson_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialTransactionToGroup`
--

DROP TABLE IF EXISTS `_FinancialTransactionToGroup`;
CREATE TABLE `_FinancialTransactionToGroup` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialTransactionToGroup_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialTransaction` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialTransactionToGroup_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_FinancialTransactionToPerson`
--

DROP TABLE IF EXISTS `_FinancialTransactionToPerson`;
CREATE TABLE `_FinancialTransactionToPerson` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_FinancialTransactionToPerson_ibfk_1` FOREIGN KEY (`A`) REFERENCES `FinancialTransaction` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_FinancialTransactionToPerson_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GroupInviteToGroupRole`
--

DROP TABLE IF EXISTS `_GroupInviteToGroupRole`;
CREATE TABLE `_GroupInviteToGroupRole` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GroupInviteToGroupRole_ibfk_1` FOREIGN KEY (`A`) REFERENCES `GroupInvite` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GroupInviteToGroupRole_ibfk_2` FOREIGN KEY (`B`) REFERENCES `GroupRole` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GroupMemberToGroupRole`
--

DROP TABLE IF EXISTS `_GroupMemberToGroupRole`;
CREATE TABLE `_GroupMemberToGroupRole` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GroupMemberToGroupRole_ibfk_1` FOREIGN KEY (`A`) REFERENCES `GroupMember` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GroupMemberToGroupRole_ibfk_2` FOREIGN KEY (`B`) REFERENCES `GroupRole` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GroupMemberToPerson`
--

DROP TABLE IF EXISTS `_GroupMemberToPerson`;
CREATE TABLE `_GroupMemberToPerson` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GroupMemberToPerson_ibfk_1` FOREIGN KEY (`A`) REFERENCES `GroupMember` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GroupMemberToPerson_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GroupRoleToGroupType`
--

DROP TABLE IF EXISTS `_GroupRoleToGroupType`;
CREATE TABLE `_GroupRoleToGroupType` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GroupRoleToGroupType_ibfk_1` FOREIGN KEY (`A`) REFERENCES `GroupRole` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GroupRoleToGroupType_ibfk_2` FOREIGN KEY (`B`) REFERENCES `GroupType` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GroupToGroup`
--

DROP TABLE IF EXISTS `_GroupToGroup`;
CREATE TABLE `_GroupToGroup` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GroupToGroup_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GroupToGroup_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GroupToGroupInvite`
--

DROP TABLE IF EXISTS `_GroupToGroupInvite`;
CREATE TABLE `_GroupToGroupInvite` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GroupToGroupInvite_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GroupToGroupInvite_ibfk_2` FOREIGN KEY (`B`) REFERENCES `GroupInvite` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GroupToGroupMember`
--

DROP TABLE IF EXISTS `_GroupToGroupMember`;
CREATE TABLE `_GroupToGroupMember` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GroupToGroupMember_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GroupToGroupMember_ibfk_2` FOREIGN KEY (`B`) REFERENCES `GroupMember` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GroupToGroupType`
--

DROP TABLE IF EXISTS `_GroupToGroupType`;
CREATE TABLE `_GroupToGroupType` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GroupToGroupType_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GroupToGroupType_ibfk_2` FOREIGN KEY (`B`) REFERENCES `GroupType` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PersonToPhoneNumber`
--

DROP TABLE IF EXISTS `_PersonToPhoneNumber`;
CREATE TABLE `_PersonToPhoneNumber` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PersonToPhoneNumber_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Person` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PersonToPhoneNumber_ibfk_2` FOREIGN KEY (`B`) REFERENCES `PhoneNumber` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PersonToUser`
--

DROP TABLE IF EXISTS `_PersonToUser`;
CREATE TABLE `_PersonToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PersonToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Person` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PersonToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
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
