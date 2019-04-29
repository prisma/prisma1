-- MySQL dump 10.17  Distrib 10.3.12-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: schema-generator@relations-4424
-- ------------------------------------------------------
-- Server version	5.7.23


--
-- Table structure for table `Customer`
--

DROP TABLE IF EXISTS `Customer`;
CREATE TABLE `Customer` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `uid` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `emailId` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `mobileNumber` mediumtext COLLATE utf8mb4_unicode_ci,
  `name` mediumtext COLLATE utf8mb4_unicode_ci,
  `createdAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uid_UNIQUE` (`uid`(191)),
  UNIQUE KEY `emailId_UNIQUE` (`emailId`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CustomerCartItem`
--

DROP TABLE IF EXISTS `CustomerCartItem`;
CREATE TABLE `CustomerCartItem` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `addedAtPrice` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `savedForLater` tinyint(1) NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  `product` char(25) CHARACTER SET utf8 DEFAULT NULL,
  `customer` char(25) CHARACTER SET utf8 DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `product` (`product`),
  KEY `customer` (`customer`),
  CONSTRAINT `CustomerCartItem_ibfk_1` FOREIGN KEY (`product`) REFERENCES `Product` (`id`) ON DELETE SET NULL,
  CONSTRAINT `CustomerCartItem_ibfk_2` FOREIGN KEY (`customer`) REFERENCES `Customer` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `CustomerWishlist`
--

DROP TABLE IF EXISTS `CustomerWishlist`;
CREATE TABLE `CustomerWishlist` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `listName` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer` char(25) CHARACTER SET utf8 DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `customer` (`customer`),
  CONSTRAINT `CustomerWishlist_ibfk_1` FOREIGN KEY (`customer`) REFERENCES `Customer` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Post`
--

DROP TABLE IF EXISTS `Post`;
CREATE TABLE `Post` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `text` mediumtext COLLATE utf8mb4_unicode_ci,
  `createdAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `PostsProductsRelation`
--

DROP TABLE IF EXISTS `PostsProductsRelation`;
CREATE TABLE `PostsProductsRelation` (
  `post` char(25) CHARACTER SET utf8 NOT NULL,
  `product` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `PostsProductsRelation_AB_unique` (`post`,`product`),
  KEY `product` (`product`),
  CONSTRAINT `PostsProductsRelation_ibfk_1` FOREIGN KEY (`post`) REFERENCES `Post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `PostsProductsRelation_ibfk_2` FOREIGN KEY (`product`) REFERENCES `Product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Product`
--

DROP TABLE IF EXISTS `Product`;
CREATE TABLE `Product` (
  `id` char(25) CHARACTER SET utf8 NOT NULL,
  `name` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `brand` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdAt` datetime(3) NOT NULL,
  `updatedAt` datetime(3) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Product_ratingsDistribution`
--

DROP TABLE IF EXISTS `Product_ratingsDistribution`;
CREATE TABLE `Product_ratingsDistribution` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` int(11) NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`),
  CONSTRAINT `Product_ratingsDistribution_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `Product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `Product_tags`
--

DROP TABLE IF EXISTS `Product_tags`;
CREATE TABLE `Product_tags` (
  `nodeId` char(25) CHARACTER SET utf8 NOT NULL,
  `position` int(4) NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`nodeId`,`position`),
  KEY `value` (`value`(191)),
  CONSTRAINT `Product_tags_ibfk_1` FOREIGN KEY (`nodeId`) REFERENCES `Product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `WishlistedProductsRelation`
--

DROP TABLE IF EXISTS `WishlistedProductsRelation`;
CREATE TABLE `WishlistedProductsRelation` (
  `wishlist` char(25) CHARACTER SET utf8 NOT NULL,
  `product` char(25) CHARACTER SET utf8 NOT NULL,
  UNIQUE KEY `WishlistedProductsRelation_AB_unique` (`wishlist`,`product`),
  KEY `product` (`product`),
  CONSTRAINT `WishlistedProductsRelation_ibfk_1` FOREIGN KEY (`wishlist`) REFERENCES `CustomerWishlist` (`id`) ON DELETE CASCADE,
  CONSTRAINT `WishlistedProductsRelation_ibfk_2` FOREIGN KEY (`product`) REFERENCES `Product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Dump completed on 2019-04-29 12:50:23
