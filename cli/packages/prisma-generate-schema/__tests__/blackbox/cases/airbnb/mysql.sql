-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@airbnb
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `Amenities`
--

DROP TABLE IF EXISTS `Amenities`;
CREATE TABLE `Amenities` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `elevator` tinyint(1) NOT NULL,
  `petsAllowed` tinyint(1) NOT NULL,
  `internet` tinyint(1) NOT NULL,
  `kitchen` tinyint(1) NOT NULL,
  `wirelessInternet` tinyint(1) NOT NULL,
  `familyKidFriendly` tinyint(1) NOT NULL,
  `freeParkingOnPremises` tinyint(1) NOT NULL,
  `hotTub` tinyint(1) NOT NULL,
  `pool` tinyint(1) NOT NULL,
  `smokingAllowed` tinyint(1) NOT NULL,
  `wheelchairAccessible` tinyint(1) NOT NULL,
  `breakfast` tinyint(1) NOT NULL,
  `cableTv` tinyint(1) NOT NULL,
  `suitableForEvents` tinyint(1) NOT NULL,
  `dryer` tinyint(1) NOT NULL,
  `washer` tinyint(1) NOT NULL,
  `indoorFireplace` tinyint(1) NOT NULL,
  `tv` tinyint(1) NOT NULL,
  `heating` tinyint(1) NOT NULL,
  `hangers` tinyint(1) NOT NULL,
  `iron` tinyint(1) NOT NULL,
  `hairDryer` tinyint(1) NOT NULL,
  `doorman` tinyint(1) NOT NULL,
  `paidParkingOffPremises` tinyint(1) NOT NULL,
  `freeParkingOnStreet` tinyint(1) NOT NULL,
  `gym` tinyint(1) NOT NULL,
  `airConditioning` tinyint(1) NOT NULL,
  `shampoo` tinyint(1) NOT NULL,
  `essentials` tinyint(1) NOT NULL,
  `laptopFriendlyWorkspace` tinyint(1) NOT NULL,
  `privateEntrance` tinyint(1) NOT NULL,
  `buzzerWirelessIntercom` tinyint(1) NOT NULL,
  `babyBath` tinyint(1) NOT NULL,
  `babyMonitor` tinyint(1) NOT NULL,
  `babysitterRecommendations` tinyint(1) NOT NULL,
  `bathtub` tinyint(1) NOT NULL,
  `changingTable` tinyint(1) NOT NULL,
  `childrensBooksAndToys` tinyint(1) NOT NULL,
  `childrensDinnerware` tinyint(1) NOT NULL,
  `crib` tinyint(1) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Booking`
--

DROP TABLE IF EXISTS `Booking`;
CREATE TABLE `Booking` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `startDate` datetime(3) NOT NULL,
  `endDate` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `City`
--

DROP TABLE IF EXISTS `City`;
CREATE TABLE `City` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CreditCardInformation`
--

DROP TABLE IF EXISTS `CreditCardInformation`;
CREATE TABLE `CreditCardInformation` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `cardNumber` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiresOnMonth` int(11) NOT NULL,
  `expiresOnYear` int(11) NOT NULL,
  `securityCode` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `firstName` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `lastName` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `postalCode` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `country` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Experience`
--

DROP TABLE IF EXISTS `Experience`;
CREATE TABLE `Experience` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `title` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `pricePerPerson` int(11) NOT NULL,
  `popularity` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `ExperienceCategory`
--

DROP TABLE IF EXISTS `ExperienceCategory`;
CREATE TABLE `ExperienceCategory` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `mainColor` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `GuestRequirements`
--

DROP TABLE IF EXISTS `GuestRequirements`;
CREATE TABLE `GuestRequirements` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `govIssuedId` tinyint(1) NOT NULL,
  `recommendationsFromOtherHosts` tinyint(1) NOT NULL,
  `guestTripInformation` tinyint(1) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `HouseRules`
--

DROP TABLE IF EXISTS `HouseRules`;
CREATE TABLE `HouseRules` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `suitableForChildren` tinyint(1) DEFAULT NULL,
  `suitableForInfants` tinyint(1) DEFAULT NULL,
  `petsAllowed` tinyint(1) DEFAULT NULL,
  `smokingAllowed` tinyint(1) DEFAULT NULL,
  `partiesAndEventsAllowed` tinyint(1) DEFAULT NULL,
  `additionalRules` mediumtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Location`
--

DROP TABLE IF EXISTS `Location`;
CREATE TABLE `Location` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `lat` decimal(65,30) NOT NULL,
  `lng` decimal(65,30) NOT NULL,
  `address` mediumtext COLLATE utf8mb4_unicode_ci,
  `directions` mediumtext COLLATE utf8mb4_unicode_ci,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Message`
--

DROP TABLE IF EXISTS `Message`;
CREATE TABLE `Message` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `deliveredAt` datetime(3) NOT NULL,
  `readAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Neighbourhood`
--

DROP TABLE IF EXISTS `Neighbourhood`;
CREATE TABLE `Neighbourhood` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `featured` tinyint(1) NOT NULL,
  `popularity` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Notification`
--

DROP TABLE IF EXISTS `Notification`;
CREATE TABLE `Notification` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `type` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `readDate` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Payment`
--

DROP TABLE IF EXISTS `Payment`;
CREATE TABLE `Payment` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `serviceFee` decimal(65,30) NOT NULL,
  `placePrice` decimal(65,30) NOT NULL,
  `totalPrice` decimal(65,30) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `PaymentAccount`
--

DROP TABLE IF EXISTS `PaymentAccount`;
CREATE TABLE `PaymentAccount` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `type` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `PaypalInformation`
--

DROP TABLE IF EXISTS `PaypalInformation`;
CREATE TABLE `PaypalInformation` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `email` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Picture`
--

DROP TABLE IF EXISTS `Picture`;
CREATE TABLE `Picture` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `url` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Place`
--

DROP TABLE IF EXISTS `Place`;
CREATE TABLE `Place` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci,
  `size` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shortDescription` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `maxGuests` int(11) NOT NULL,
  `numBedrooms` int(11) NOT NULL,
  `numBeds` int(11) NOT NULL,
  `numBaths` int(11) NOT NULL,
  `popularity` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Policies`
--

DROP TABLE IF EXISTS `Policies`;
CREATE TABLE `Policies` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `checkInStartTime` decimal(65,30) NOT NULL,
  `checkInEndTime` decimal(65,30) NOT NULL,
  `checkoutTime` decimal(65,30) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Pricing`
--

DROP TABLE IF EXISTS `Pricing`;
CREATE TABLE `Pricing` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `monthlyDiscount` int(11) DEFAULT NULL,
  `weeklyDiscount` int(11) DEFAULT NULL,
  `perNight` int(11) NOT NULL,
  `smartPricing` tinyint(1) NOT NULL,
  `basePrice` int(11) NOT NULL,
  `averageWeekly` int(11) NOT NULL,
  `averageMonthly` int(11) NOT NULL,
  `cleaningFee` int(11) DEFAULT NULL,
  `securityDeposit` int(11) DEFAULT NULL,
  `extraGuests` int(11) DEFAULT NULL,
  `weekendPricing` int(11) DEFAULT NULL,
  `currency` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Restaurant`
--

DROP TABLE IF EXISTS `Restaurant`;
CREATE TABLE `Restaurant` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `title` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `avgPricePerPerson` int(11) NOT NULL,
  `isCurated` tinyint(1) NOT NULL,
  `slug` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `popularity` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Review`
--

DROP TABLE IF EXISTS `Review`;
CREATE TABLE `Review` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `text` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `stars` int(11) NOT NULL,
  `accuracy` int(11) NOT NULL,
  `location` int(11) NOT NULL,
  `checkIn` int(11) NOT NULL,
  `value` int(11) NOT NULL,
  `cleanliness` int(11) NOT NULL,
  `communication` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `User`
--

DROP TABLE IF EXISTS `User`;
CREATE TABLE `User` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `firstName` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `lastName` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `responseRate` decimal(65,30) DEFAULT NULL,
  `responseTime` int(11) DEFAULT NULL,
  `isSuperHost` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `email_UNIQUE` (`email`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Views`
--

DROP TABLE IF EXISTS `Views`;
CREATE TABLE `Views` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `lastWeek` int(11) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_AmenitiesToPlace`
--

DROP TABLE IF EXISTS `_AmenitiesToPlace`;
CREATE TABLE `_AmenitiesToPlace` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_AmenitiesToPlace_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Amenities` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_AmenitiesToPlace_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Place` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BookingToPayment`
--

DROP TABLE IF EXISTS `_BookingToPayment`;
CREATE TABLE `_BookingToPayment` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BookingToPayment_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Booking` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BookingToPayment_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Payment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BookingToPlace`
--

DROP TABLE IF EXISTS `_BookingToPlace`;
CREATE TABLE `_BookingToPlace` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BookingToPlace_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Booking` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BookingToPlace_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Place` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_BookingToUser`
--

DROP TABLE IF EXISTS `_BookingToUser`;
CREATE TABLE `_BookingToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_BookingToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Booking` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_BookingToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CityToNeighbourhood`
--

DROP TABLE IF EXISTS `_CityToNeighbourhood`;
CREATE TABLE `_CityToNeighbourhood` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CityToNeighbourhood_ibfk_1` FOREIGN KEY (`A`) REFERENCES `City` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CityToNeighbourhood_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Neighbourhood` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_CreditCardInformationToPaymentAccount`
--

DROP TABLE IF EXISTS `_CreditCardInformationToPaymentAccount`;
CREATE TABLE `_CreditCardInformationToPaymentAccount` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_CreditCardInformationToPaymentAccount_ibfk_1` FOREIGN KEY (`A`) REFERENCES `CreditCardInformation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_CreditCardInformationToPaymentAccount_ibfk_2` FOREIGN KEY (`B`) REFERENCES `PaymentAccount` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ExperienceToExperienceCategory`
--

DROP TABLE IF EXISTS `_ExperienceToExperienceCategory`;
CREATE TABLE `_ExperienceToExperienceCategory` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ExperienceToExperienceCategory_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Experience` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ExperienceToExperienceCategory_ibfk_2` FOREIGN KEY (`B`) REFERENCES `ExperienceCategory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ExperienceToLocation`
--

DROP TABLE IF EXISTS `_ExperienceToLocation`;
CREATE TABLE `_ExperienceToLocation` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ExperienceToLocation_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Experience` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ExperienceToLocation_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Location` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ExperienceToPicture`
--

DROP TABLE IF EXISTS `_ExperienceToPicture`;
CREATE TABLE `_ExperienceToPicture` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ExperienceToPicture_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Experience` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ExperienceToPicture_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Picture` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ExperienceToReview`
--

DROP TABLE IF EXISTS `_ExperienceToReview`;
CREATE TABLE `_ExperienceToReview` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ExperienceToReview_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Experience` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ExperienceToReview_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Review` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ExperienceToUser`
--

DROP TABLE IF EXISTS `_ExperienceToUser`;
CREATE TABLE `_ExperienceToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ExperienceToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Experience` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ExperienceToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_GuestRequirementsToPlace`
--

DROP TABLE IF EXISTS `_GuestRequirementsToPlace`;
CREATE TABLE `_GuestRequirementsToPlace` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_GuestRequirementsToPlace_ibfk_1` FOREIGN KEY (`A`) REFERENCES `GuestRequirements` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_GuestRequirementsToPlace_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Place` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_HouseRulesToPlace`
--

DROP TABLE IF EXISTS `_HouseRulesToPlace`;
CREATE TABLE `_HouseRulesToPlace` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_HouseRulesToPlace_ibfk_1` FOREIGN KEY (`A`) REFERENCES `HouseRules` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_HouseRulesToPlace_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Place` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_LocationToNeighbourhood`
--

DROP TABLE IF EXISTS `_LocationToNeighbourhood`;
CREATE TABLE `_LocationToNeighbourhood` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_LocationToNeighbourhood_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Location` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_LocationToNeighbourhood_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Neighbourhood` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_LocationToPlace`
--

DROP TABLE IF EXISTS `_LocationToPlace`;
CREATE TABLE `_LocationToPlace` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_LocationToPlace_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Location` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_LocationToPlace_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Place` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_LocationToRestaurant`
--

DROP TABLE IF EXISTS `_LocationToRestaurant`;
CREATE TABLE `_LocationToRestaurant` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_LocationToRestaurant_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Location` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_LocationToRestaurant_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Restaurant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_LocationToUser`
--

DROP TABLE IF EXISTS `_LocationToUser`;
CREATE TABLE `_LocationToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_LocationToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Location` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_LocationToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_NeighbourhoodToPicture`
--

DROP TABLE IF EXISTS `_NeighbourhoodToPicture`;
CREATE TABLE `_NeighbourhoodToPicture` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_NeighbourhoodToPicture_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Neighbourhood` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_NeighbourhoodToPicture_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Picture` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_NotificationToUser`
--

DROP TABLE IF EXISTS `_NotificationToUser`;
CREATE TABLE `_NotificationToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_NotificationToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Notification` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_NotificationToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PaymentAccountToPaypalInformation`
--

DROP TABLE IF EXISTS `_PaymentAccountToPaypalInformation`;
CREATE TABLE `_PaymentAccountToPaypalInformation` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PaymentAccountToPaypalInformation_ibfk_1` FOREIGN KEY (`A`) REFERENCES `PaymentAccount` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PaymentAccountToPaypalInformation_ibfk_2` FOREIGN KEY (`B`) REFERENCES `PaypalInformation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PaymentAccountToUser`
--

DROP TABLE IF EXISTS `_PaymentAccountToUser`;
CREATE TABLE `_PaymentAccountToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PaymentAccountToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `PaymentAccount` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PaymentAccountToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PaymentToPaymentAccount`
--

DROP TABLE IF EXISTS `_PaymentToPaymentAccount`;
CREATE TABLE `_PaymentToPaymentAccount` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PaymentToPaymentAccount_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Payment` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PaymentToPaymentAccount_ibfk_2` FOREIGN KEY (`B`) REFERENCES `PaymentAccount` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PictureToPlace`
--

DROP TABLE IF EXISTS `_PictureToPlace`;
CREATE TABLE `_PictureToPlace` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PictureToPlace_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Picture` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PictureToPlace_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Place` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PictureToRestaurant`
--

DROP TABLE IF EXISTS `_PictureToRestaurant`;
CREATE TABLE `_PictureToRestaurant` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PictureToRestaurant_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Picture` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PictureToRestaurant_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Restaurant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PictureToUser`
--

DROP TABLE IF EXISTS `_PictureToUser`;
CREATE TABLE `_PictureToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PictureToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Picture` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PictureToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PlaceToPolicies`
--

DROP TABLE IF EXISTS `_PlaceToPolicies`;
CREATE TABLE `_PlaceToPolicies` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PlaceToPolicies_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Place` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PlaceToPolicies_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Policies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PlaceToPricing`
--

DROP TABLE IF EXISTS `_PlaceToPricing`;
CREATE TABLE `_PlaceToPricing` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PlaceToPricing_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Place` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PlaceToPricing_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Pricing` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PlaceToReview`
--

DROP TABLE IF EXISTS `_PlaceToReview`;
CREATE TABLE `_PlaceToReview` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PlaceToReview_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Place` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PlaceToReview_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Review` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PlaceToUser`
--

DROP TABLE IF EXISTS `_PlaceToUser`;
CREATE TABLE `_PlaceToUser` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PlaceToUser_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Place` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PlaceToUser_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_PlaceToViews`
--

DROP TABLE IF EXISTS `_PlaceToViews`;
CREATE TABLE `_PlaceToViews` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_PlaceToViews_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Place` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_PlaceToViews_ibfk_2` FOREIGN KEY (`B`) REFERENCES `Views` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `_ReceivedMessages`
--

DROP TABLE IF EXISTS `_ReceivedMessages`;
CREATE TABLE `_ReceivedMessages` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_ReceivedMessages_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Message` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_ReceivedMessages_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
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
-- Table structure for table `_SentMessages`
--

DROP TABLE IF EXISTS `_SentMessages`;
CREATE TABLE `_SentMessages` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `A` char(25) CHARACTER SET utf8 NOT NULL,
  `B` char(25) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `AB_unique` (`A`,`B`),
  KEY `A` (`A`),
  KEY `B` (`B`),
  CONSTRAINT `_SentMessages_ibfk_1` FOREIGN KEY (`A`) REFERENCES `Message` (`id`) ON DELETE CASCADE,
  CONSTRAINT `_SentMessages_ibfk_2` FOREIGN KEY (`B`) REFERENCES `User` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Dump completed on 2019-02-05 13:39:11
