-- MySQL dump 10.17  Distrib 10.3.11-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: allsquare
-- ------------------------------------------------------
-- Server version	10.3.11-MariaDB-1:10.3.11+maria~jessie-log
--
-- Table structure for table `access_types`
--
DROP TABLE IF EXISTS `access_types`;
CREATE TABLE `access_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `addresses`
--
DROP TABLE IF EXISTS `addresses`;
CREATE TABLE `addresses` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lat` float DEFAULT NULL,
  `lng` float DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `google_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1040 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `ahoy_events`
--
DROP TABLE IF EXISTS `ahoy_events`;
CREATE TABLE `ahoy_events` (
  `id` binary(16) NOT NULL,
  `visit_id` binary(16) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `properties` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_ahoy_events_on_id` (`id`) USING BTREE,
  KEY `index_ahoy_events_on_visit_id` (`visit_id`) USING BTREE,
  KEY `index_ahoy_events_on_type` (`type`(191)) USING BTREE,
  KEY `index_ahoy_events_on_user_id` (`user_id`) USING BTREE,
  KEY `index_ahoy_events_on_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `albums`
--
DROP TABLE IF EXISTS `albums`;
CREATE TABLE `albums` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `albumable_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `albumable_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_picture_id` int(11) DEFAULT NULL,
  `priority` int(11) DEFAULT NULL,
  `media_id` int(11) DEFAULT NULL,
  `attachment_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_albums_on_albumable_type_and_albumable_id` (`albumable_type`,`albumable_id`) USING BTREE,
  KEY `index_albums_on_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=177178 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `ambassador_contests`
--
DROP TABLE IF EXISTS `ambassador_contests`;
CREATE TABLE `ambassador_contests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `start_at` datetime DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `winner_id` int(11) DEFAULT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contest_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `counter` int(11) DEFAULT NULL,
  `small_description` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `open` tinyint(1) DEFAULT 0,
  `finished` tinyint(1) DEFAULT 0,
  `picture_id` int(11) DEFAULT NULL,
  `prize_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_ambassador_contests_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `ambassador_user_contests`
--
DROP TABLE IF EXISTS `ambassador_user_contests`;
CREATE TABLE `ambassador_user_contests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `ambassador_contest_id` int(11) DEFAULT NULL,
  `validated` tinyint(1) DEFAULT 0,
  `win` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `amenities`
--
DROP TABLE IF EXISTS `amenities`;
CREATE TABLE `amenities` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amenity_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` int(11) DEFAULT NULL,
  `priority_type` int(11) DEFAULT NULL,
  `friendly_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `clubs`
--
DROP TABLE IF EXISTS `clubs`;
CREATE TABLE `clubs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `street` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `zipcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `closest_city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `closest_airport` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_file_size` int(11) DEFAULT NULL,
  `logo_updated_at` datetime DEFAULT NULL,
  `scorecard_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scorecard_content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scorecard_file_size` int(11) DEFAULT NULL,
  `scorecard_updated_at` datetime DEFAULT NULL,
  `online` tinyint(1) DEFAULT 1,
  `simple` tinyint(1) DEFAULT 0,
  `perma` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `edit_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manager_fullname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manager_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manager_phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pin_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `has_carts` tinyint(1) DEFAULT NULL,
  `has_driving_range` tinyint(1) DEFAULT NULL,
  `access_type_id` int(11) DEFAULT NULL,
  `merge_course` tinyint(1) DEFAULT 1,
  `cover_album_id` int(11) DEFAULT NULL,
  `booking_link` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `meta_visible` tinyint(1) DEFAULT 0,
  `external_video_id` int(11) DEFAULT NULL,
  `logo_picture_id` int(11) DEFAULT 3,
  `cover_picture_id` int(11) DEFAULT 1,
  `country_id` int(11) DEFAULT NULL,
  `facebook_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `continent_id` int(11) DEFAULT NULL,
  `area_id` int(11) DEFAULT NULL,
  `destination_id` int(11) DEFAULT NULL,
  `top100continent` int(11) DEFAULT NULL,
  `top100area` int(11) DEFAULT NULL,
  `top100country` int(11) DEFAULT NULL,
  `top100world` int(11) DEFAULT NULL,
  `facebook_user_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `altitude` float DEFAULT NULL,
  `altitude_resolution` float DEFAULT NULL,
  `score` float DEFAULT 0,
  `green_fee` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handicap` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `open_informations` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amenities_activated` tinyint(1) DEFAULT 0,
  `booking_available` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_clubs_on_name` (`name`) USING BTREE,
  KEY `index_clubs_slug` (`slug`),
  KEY `idx_club_area` (`area_id`),
  KEY `idx_club_country` (`country_id`),
  KEY `idx_club_destination` (`destination_id`),
  KEY `idxclubdeletedat` (`deleted_at`),
  KEY `idx_club_continent` (`continent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=37789 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `amenities_clubs`
--
DROP TABLE IF EXISTS `amenities_clubs`;
CREATE TABLE `amenities_clubs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `amenity_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `club_id` (`club_id`),
  KEY `amenity_id` (`amenity_id`),
  CONSTRAINT `amenities_clubs_ibfk_1` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`),
  CONSTRAINT `amenities_clubs_ibfk_2` FOREIGN KEY (`amenity_id`) REFERENCES `amenities` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=845 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `architects`
--
DROP TABLE IF EXISTS `architects`;
CREATE TABLE `architects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `firstname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `biography` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comittee` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comittee_website` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `website` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `top_ranking` int(11) DEFAULT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `slug` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_architects_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=659 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `areas`
--
DROP TABLE IF EXISTS `areas`;
CREATE TABLE `areas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `slug` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nb_top100` int(11) DEFAULT NULL,
  `rank` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_slug_areas` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `articles`
--
DROP TABLE IF EXISTS `articles`;
CREATE TABLE `articles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_file_size` int(11) DEFAULT NULL,
  `picture_updated_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `club_id` int(11) DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `private` tinyint(1) DEFAULT 0,
  `mobile_picture_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mobile_picture_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mobile_picture_file_size` int(11) DEFAULT NULL,
  `mobile_picture_updated_at` datetime DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_articles_on_club_id` (`club_id`) USING BTREE,
  KEY `index_articles_on_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `attachments`
--
DROP TABLE IF EXISTS `attachments`;
CREATE TABLE `attachments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mime` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachable_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachable_id` int(11) DEFAULT NULL,
  `file_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_file_size` int(11) DEFAULT NULL,
  `file_updated_at` datetime DEFAULT NULL,
  `pos` int(11) DEFAULT NULL,
  `attached_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attached_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14497 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `attendings`
--
DROP TABLE IF EXISTS `attendings`;
CREATE TABLE `attendings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `event_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `attending` int(11) DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `invitor_id` int(11) DEFAULT NULL,
  `invitor_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_attendings_on_user_id` (`user_id`),
  KEY `index_attendings_on_event_id` (`event_id`,`user_id`) USING BTREE,
  KEY `idx_attending_invitor_id` (`invitor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17113 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `blogs`
--
DROP TABLE IF EXISTS `blogs`;
CREATE TABLE `blogs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `cover_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_file_size` int(11) DEFAULT NULL,
  `cover_updated_at` datetime DEFAULT NULL,
  `content_preview` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `booking_requests`
--
DROP TABLE IF EXISTS `booking_requests`;
CREATE TABLE `booking_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1146 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `braintree_customers`
--
DROP TABLE IF EXISTS `braintree_customers`;
CREATE TABLE `braintree_customers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `braintree_handle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vat_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `braintree_subscriptions`
--
DROP TABLE IF EXISTS `braintree_subscriptions`;
CREATE TABLE `braintree_subscriptions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `braintree_handle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `amount` float DEFAULT NULL,
  `currency` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `braintree_customer_id` int(11) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `payment_method_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `braintree_transactions`
--
DROP TABLE IF EXISTS `braintree_transactions`;
CREATE TABLE `braintree_transactions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `braintree_handle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receipt_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `amount` float DEFAULT NULL,
  `currency` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `success` tinyint(1) DEFAULT NULL,
  `braintree_customer_id` int(11) DEFAULT NULL,
  `braintree_subscription_id` int(11) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `contact_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `brands`
--
DROP TABLE IF EXISTS `brands`;
CREATE TABLE `brands` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `year` int(11) DEFAULT NULL,
  `picture_id` int(11) DEFAULT NULL,
  `cover_picture_id` int(11) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `slug` varchar(255) DEFAULT NULL,
  `media_id` int(11) DEFAULT NULL,
  `page_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_brands_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=149 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `brands_competitors`
--
DROP TABLE IF EXISTS `brands_competitors`;
CREATE TABLE `brands_competitors` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `product_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `brand_id` int(11) DEFAULT NULL,
  `competitor_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `brand_id` (`brand_id`),
  KEY `competitor_id` (`competitor_id`),
  CONSTRAINT `brands_competitors_ibfk_1` FOREIGN KEY (`brand_id`) REFERENCES `brands` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `brands_competitors_ibfk_2` FOREIGN KEY (`competitor_id`) REFERENCES `brands` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=721 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `business_admins`
--
DROP TABLE IF EXISTS `business_admins`;
CREATE TABLE `business_admins` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `business_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `checkins`
--
DROP TABLE IF EXISTS `checkins`;
CREATE TABLE `checkins` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_url_shortcut` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `album_id` int(11) DEFAULT NULL,
  `media_processing` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `index_checkins_on_club_id` (`club_id`) USING BTREE,
  KEY `index_checkins_on_user_id` (`user_id`) USING BTREE,
  KEY `index_checkins_on_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=24709 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `checkins_users`
--
DROP TABLE IF EXISTS `checkins_users`;
CREATE TABLE `checkins_users` (
  `checkin_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `index_checkins_users_on_checkin_id_and_user_id` (`checkin_id`,`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2819 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `client_accesses`
--
DROP TABLE IF EXISTS `client_accesses`;
CREATE TABLE `client_accesses` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ip_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `version` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `accesses_count` int(11) DEFAULT NULL,
  `last_access_date` datetime DEFAULT NULL,
  `platform` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `clothes_products`
--
DROP TABLE IF EXISTS `clothes_products`;
CREATE TABLE `clothes_products` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `brand_id` int(11) DEFAULT NULL,
  `price_id` int(11) DEFAULT NULL,
  `album_id` int(11) DEFAULT NULL,
  `picture_id` int(11) DEFAULT NULL,
  `internal_video_id` int(11) DEFAULT NULL,
  `gender_restriction` int(11) DEFAULT NULL,
  `item_type` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_clothes_products_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=5134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `clothes_sizes`
--
DROP TABLE IF EXISTS `clothes_sizes`;
CREATE TABLE `clothes_sizes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `size` int(11) DEFAULT NULL,
  `clothes_product_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17602 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `club_administrators`
--
DROP TABLE IF EXISTS `club_administrators`;
CREATE TABLE `club_administrators` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_club_administrators_on_club_id` (`club_id`) USING BTREE,
  KEY `index_club_administrators_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=433 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `club_amenities`
--
DROP TABLE IF EXISTS `club_amenities`;
CREATE TABLE `club_amenities` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `amenity_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `club_bookings`
--
DROP TABLE IF EXISTS `club_bookings`;
CREATE TABLE `club_bookings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `nb_players` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `golf_cart` tinyint(1) DEFAULT NULL,
  `rental_clubs` tinyint(1) DEFAULT NULL,
  `responsible_id` int(11) DEFAULT NULL,
  `current_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comment_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=412 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `club_course_reviews`
--
DROP TABLE IF EXISTS `club_course_reviews`;
CREATE TABLE `club_course_reviews` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `course_id` int(11) DEFAULT NULL,
  `course_review_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_club_course_reviews_on_course_id` (`course_id`) USING BTREE,
  KEY `index_club_course_reviews_on_course_review_id` (`course_review_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `club_requests`
--
DROP TABLE IF EXISTS `club_requests`;
CREATE TABLE `club_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `seen` tinyint(1) DEFAULT 0,
  `status` int(11) DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_club_requests_on_club_id` (`club_id`) USING BTREE,
  KEY `index_club_requests_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=20212 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `club_words`
--
DROP TABLE IF EXISTS `club_words`;
CREATE TABLE `club_words` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `word` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `checked` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6504 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `club_words_clubs`
--
DROP TABLE IF EXISTS `club_words_clubs`;
CREATE TABLE `club_words_clubs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `club_word_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_mrr_on_club_id` (`club_id`) USING BTREE,
  KEY `index_mrr_on_word_id` (`club_word_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=21102 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `users`
--
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `encrypted_password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reset_password_token` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reset_password_sent_at` datetime DEFAULT NULL,
  `remember_created_at` datetime DEFAULT NULL,
  `sign_in_count` int(11) DEFAULT 0,
  `current_sign_in_at` datetime DEFAULT NULL,
  `last_sign_in_at` datetime DEFAULT NULL,
  `current_sign_in_ip` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_sign_in_ip` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `firstname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `uid` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `oauth_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `oauth_secret` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `oauth_expires_at` datetime DEFAULT NULL,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `from_country_id` int(11) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `handicap` int(11) DEFAULT NULL,
  `work` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `points` int(11) DEFAULT 0,
  `latitude` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  `job` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sex` int(11) DEFAULT NULL,
  `authentication_token` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enable_notifications` tinyint(1) DEFAULT 1,
  `birthdate` date DEFAULT NULL,
  `notification_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mixpanel_distinct_id` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `confirmation_token` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `confirmation_sent_at` datetime DEFAULT NULL,
  `unconfirmed_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tokens` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `official` tinyint(1) DEFAULT 0,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `step` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT 'step1',
  `ip_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `no_club` tinyint(1) DEFAULT 0,
  `pro` tinyint(1) DEFAULT 0,
  `ranking_id` int(11) DEFAULT NULL,
  `address_id` int(11) DEFAULT NULL,
  `private` tinyint(1) DEFAULT 0,
  `miles` tinyint(1) DEFAULT NULL,
  `fahrenheit` tinyint(1) DEFAULT NULL,
  `ambassador_club_id` int(11) DEFAULT NULL,
  `profile_picture_id` int(11) DEFAULT 2,
  `cover_picture_id` int(11) DEFAULT 1,
  `ios_app` tinyint(1) DEFAULT 0,
  `android_app` tinyint(1) DEFAULT 0,
  `verified_ambassador` tinyint(1) DEFAULT 0,
  `signup_nb_courses_played` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_admin` tinyint(1) DEFAULT 0,
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `google_place_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_users_on_authentication_token` (`authentication_token`) USING BTREE,
  KEY `index_users_on_provider_and_uid` (`uid`,`provider`) USING BTREE,
  KEY `index_users_on_deleted_at` (`deleted_at`),
  KEY `usersclubid` (`club_id`),
  KEY `index_users_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=25705 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `clubs_administrators`
--
DROP TABLE IF EXISTS `clubs_administrators`;
CREATE TABLE `clubs_administrators` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `club_id` (`club_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `clubs_administrators_ibfk_1` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `clubs_administrators_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `clubs_geographical_areas`
--
DROP TABLE IF EXISTS `clubs_geographical_areas`;
CREATE TABLE `clubs_geographical_areas` (
  `club_id` int(11) DEFAULT NULL,
  `geographical_area_id` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_on_club_id_and_geographical_area_id` (`club_id`,`geographical_area_id`)
) ENGINE=InnoDB AUTO_INCREMENT=85433 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `comments`
--
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `commentable_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `commentable_id` int(11) DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `author_id` int(11) DEFAULT NULL,
  `author_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `public` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_comments_on_deleted_at` (`deleted_at`),
  KEY `idxcommentcommentableid` (`commentable_id`),
  KEY `idxcommentauthorid` (`author_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15485 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `continents`
--
DROP TABLE IF EXISTS `continents`;
CREATE TABLE `continents` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `slug` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nb_top100` int(11) DEFAULT NULL,
  `rank` int(11) DEFAULT NULL,
  `latitude` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_continents_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `countries`
--
DROP TABLE IF EXISTS `countries`;
CREATE TABLE `countries` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `position` int(11) DEFAULT NULL,
  `alpha2` tinytext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alpha3` tinytext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `calling` tinytext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `continent` tinytext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numeric3` tinytext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `currency` tinytext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nb_top100` int(11) DEFAULT NULL,
  `google_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `google_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rank` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_countries_on_name` (`name`) USING BTREE,
  KEY `idxcountriesgoogleid` (`google_id`),
  KEY `idxcountrieslug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=267 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `course_reviews`
--
DROP TABLE IF EXISTS `course_reviews`;
CREATE TABLE `course_reviews` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `score` int(11) DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `online` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `index_course_reviews_on_user_id` (`user_id`) USING BTREE,
  KEY `index_course_reviews_on_score` (`score`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `courses`
--
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `holes` int(11) DEFAULT NULL,
  `par` int(11) DEFAULT NULL,
  `length` int(11) DEFAULT NULL,
  `slope` int(11) DEFAULT NULL,
  `year_built` int(11) DEFAULT NULL,
  `visitors` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `allowed_players` int(11) DEFAULT NULL,
  `currency_id` int(11) DEFAULT NULL,
  `green_fee_price` float DEFAULT NULL,
  `latitude` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  `architect` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `designed_at` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `top100_world` int(11) DEFAULT NULL,
  `top100_continent` int(11) DEFAULT NULL,
  `top100_country` int(11) DEFAULT NULL,
  `top100_area` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `course_record` int(11) DEFAULT NULL,
  `gender_restriction` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_courses_on_club_id` (`club_id`) USING BTREE,
  KEY `index_courses_on_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=67132 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `courses_architects`
--
DROP TABLE IF EXISTS `courses_architects`;
CREATE TABLE `courses_architects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `course_id` int(11) DEFAULT NULL,
  `architect_id` int(11) DEFAULT NULL,
  `firstname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20528 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `courses_golf_types`
--
DROP TABLE IF EXISTS `courses_golf_types`;
CREATE TABLE `courses_golf_types` (
  `course_id` int(11) DEFAULT NULL,
  `golf_type_id` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2065 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `currencies`
--
DROP TABLE IF EXISTS `currencies`;
CREATE TABLE `currencies` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `destinations`
--
DROP TABLE IF EXISTS `destinations`;
CREATE TABLE `destinations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `picture_id` int(11) DEFAULT NULL,
  `location_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT 'destination',
  `album_id` int(11) DEFAULT NULL,
  `rank` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idxdestinatjonslug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `device_tokens`
--
DROP TABLE IF EXISTS `device_tokens`;
CREATE TABLE `device_tokens` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `device` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `push_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `one_signal_player_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `devicetokenuserid` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=171375 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `discussions`
--
DROP TABLE IF EXISTS `discussions`;
CREATE TABLE `discussions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `discussion_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `title` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_file_size` int(11) DEFAULT NULL,
  `picture_updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1759 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `discussions_users`
--
DROP TABLE IF EXISTS `discussions_users`;
CREATE TABLE `discussions_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `discussion_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `last_seen` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `last_received` datetime DEFAULT NULL,
  `last_typing` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `discussionsuersdiscussiondi` (`discussion_id`),
  KEY `discussionuseruserid` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3501 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `driving_ranges`
--
DROP TABLE IF EXISTS `driving_ranges`;
CREATE TABLE `driving_ranges` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `range_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `email_notifications`
--
DROP TABLE IF EXISTS `email_notifications`;
CREATE TABLE `email_notifications` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activated` tinyint(1) DEFAULT 0,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_email_notifications_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=165245 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `email_types`
--
DROP TABLE IF EXISTS `email_types`;
CREATE TABLE `email_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `emailtypeuserid` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11286 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `emails`
--
DROP TABLE IF EXISTS `emails`;
CREATE TABLE `emails` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `events`
--
DROP TABLE IF EXISTS `events`;
CREATE TABLE `events` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `starts_at` datetime DEFAULT NULL,
  `ends_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `eventable_type` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `eventable_id` int(11) DEFAULT NULL,
  `public` tinyint(1) DEFAULT 0,
  `fee` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `perma` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `slug` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `online` tinyint(1) DEFAULT 1,
  `author_id` int(11) DEFAULT NULL,
  `author_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_picture_id` int(11) DEFAULT 1,
  `currency` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` int(11) DEFAULT NULL,
  `is_travel` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `index_events_on_club_id` (`club_id`) USING BTREE,
  KEY `index_events_on_deleted_at` (`deleted_at`),
  KEY `eventauthorid` (`author_id`),
  KEY `index_events_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=1522 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `external_videos`
--
DROP TABLE IF EXISTS `external_videos`;
CREATE TABLE `external_videos` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `video_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `thumbnail_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `media_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=861 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `feedbacks`
--
DROP TABLE IF EXISTS `feedbacks`;
CREATE TABLE `feedbacks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `navigator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_feedbacks_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `followings`
--
DROP TABLE IF EXISTS `followings`;
CREATE TABLE `followings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `followed_id` int(11) DEFAULT NULL,
  `followed_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idxfollowuser` (`user_id`,`followed_id`,`followed_type`) USING BTREE,
  KEY `followedid` (`followed_id`),
  KEY `idx_followings_userid` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=154947 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `friendly_id_slugs`
--
DROP TABLE IF EXISTS `friendly_id_slugs`;
CREATE TABLE `friendly_id_slugs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sluggable_id` int(11) NOT NULL,
  `sluggable_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_friendly_id_slugs_on_slug_and_sluggable_type` (`slug`(191),`sluggable_type`) USING BTREE,
  KEY `index_friendly_id_slugs_on_sluggable_id` (`sluggable_id`) USING BTREE,
  KEY `index_friendly_id_slugs_on_sluggable_type` (`sluggable_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=115834 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `gamers`
--
DROP TABLE IF EXISTS `gamers`;
CREATE TABLE `gamers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `game_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `validated` tinyint(1) DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_gamers_on_game_id` (`game_id`) USING BTREE,
  KEY `index_gamers_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6751 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `games`
--
DROP TABLE IF EXISTS `games`;
CREATE TABLE `games` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `starts_at` date DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_games_on_club_id` (`club_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5350 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `geographical_areas`
--
DROP TABLE IF EXISTS `geographical_areas`;
CREATE TABLE `geographical_areas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `geographical_areable_id` int(11) DEFAULT NULL,
  `geographical_areable_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parent_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `picture_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_file_size` int(11) DEFAULT NULL,
  `picture_updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_geographical_areas_on_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=416 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `glossary_terms`
--
DROP TABLE IF EXISTS `glossary_terms`;
CREATE TABLE `glossary_terms` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `definition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `slug` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `top_search` int(11) DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_glossary_terms_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=437 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `golf_types`
--
DROP TABLE IF EXISTS `golf_types`;
CREATE TABLE `golf_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `greenfees`
--
DROP TABLE IF EXISTS `greenfees`;
CREATE TABLE `greenfees` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `comment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `firstname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `responsible_id` int(11) DEFAULT NULL,
  `current_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comment_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` int(11) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=259 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `hits`
--
DROP TABLE IF EXISTS `hits`;
CREATE TABLE `hits` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_hits_on_club_id` (`club_id`) USING BTREE,
  KEY `index_hits_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1248547 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `hosters`
--
DROP TABLE IF EXISTS `hosters`;
CREATE TABLE `hosters` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `event_id` int(11) DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_hosters_on_user_id` (`user_id`) USING BTREE,
  KEY `index_hosters_on_event_id` (`event_id`) USING BTREE,
  KEY `index_hosters_on_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=423 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `hotel_bookings`
--
DROP TABLE IF EXISTS `hotel_bookings`;
CREATE TABLE `hotel_bookings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `hotel_id` int(11) DEFAULT NULL,
  `nb_players` int(11) DEFAULT NULL,
  `nb_nights` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=67 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `hotels`
--
DROP TABLE IF EXISTS `hotels`;
CREATE TABLE `hotels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `zip` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city_hotel` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cc1` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `classs` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  `latitude` float DEFAULT NULL,
  `public_ranking` float DEFAULT NULL,
  `hotel_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `continent_id` float DEFAULT NULL,
  `review_score` float DEFAULT NULL,
  `photo_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_file_size` int(11) DEFAULT NULL,
  `photo_updated_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `id_booking` bigint(20) DEFAULT NULL,
  `image_file_name` varchar(255) DEFAULT NULL,
  `image_content_type` varchar(255) DEFAULT NULL,
  `image_file_size` int(11) DEFAULT NULL,
  `image_updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_id_booking` (`id_booking`),
  KEY `index_hotels_on_longitude` (`longitude`) USING BTREE,
  KEY `index_hotels_on_latitude` (`latitude`) USING BTREE,
  KEY `index_hotels_on_name` (`name`(191)) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2971862 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `influencers`
--
DROP TABLE IF EXISTS `influencers`;
CREATE TABLE `influencers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `priority` int(11) DEFAULT NULL,
  `influencable_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `influencable_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=166 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `internal_videos`
--
DROP TABLE IF EXISTS `internal_videos`;
CREATE TABLE `internal_videos` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `media_id` int(11) DEFAULT NULL,
  `video_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `video_content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `video_file_size` int(11) DEFAULT NULL,
  `video_updated_at` datetime DEFAULT NULL,
  `media_processing` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `video_width` int(11) DEFAULT NULL,
  `video_height` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2318 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `invitations`
--
DROP TABLE IF EXISTS `invitations`;
CREATE TABLE `invitations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `invitable_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `invitable_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `invited_user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `invitableid` (`invitable_id`),
  KEY `userid` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=51037 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `items`
--
DROP TABLE IF EXISTS `items`;
CREATE TABLE `items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `position` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `preposition` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `year` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `job_offers`
--
DROP TABLE IF EXISTS `job_offers`;
CREATE TABLE `job_offers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `department` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `active` tinyint(1) DEFAULT NULL,
  `slug` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_job_offers_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `likes`
--
DROP TABLE IF EXISTS `likes`;
CREATE TABLE `likes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `likable_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `likable_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `author_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_likes_on_likable_id_and_likable_type` (`likable_id`,`likable_type`) USING BTREE,
  KEY `index_likes_on_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=121242 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `linked_clubs`
--
DROP TABLE IF EXISTS `linked_clubs`;
CREATE TABLE `linked_clubs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) DEFAULT NULL,
  `owner_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `persistent` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2883 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `logins`
--
DROP TABLE IF EXISTS `logins`;
CREATE TABLE `logins` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ip` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logged_in_at` datetime DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `latitude` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `device_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_type` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_logins_on_logged_in_at` (`logged_in_at`) USING BTREE,
  KEY `index_logins_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5104 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `medias`
--
DROP TABLE IF EXISTS `medias`;
CREATE TABLE `medias` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mediable_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mediable_id` int(11) DEFAULT NULL,
  `owner_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14151 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `membership_wishes`
--
DROP TABLE IF EXISTS `membership_wishes`;
CREATE TABLE `membership_wishes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `has_accepted` tinyint(1) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_membership_wishes_on_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=latin1;
--
-- Table structure for table `merged_pictures`
--
DROP TABLE IF EXISTS `merged_pictures`;
CREATE TABLE `merged_pictures` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `merged_pictures_pictures`
--
DROP TABLE IF EXISTS `merged_pictures_pictures`;
CREATE TABLE `merged_pictures_pictures` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `merged_picture_id` int(11) DEFAULT NULL,
  `picture_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_merged_pictures_pictures_on_merged_picture_id` (`merged_picture_id`),
  KEY `index_merged_pictures_pictures_on_picture_id` (`picture_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `merged_relationships`
--
DROP TABLE IF EXISTS `merged_relationships`;
CREATE TABLE `merged_relationships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `merged_relationships_relationships`
--
DROP TABLE IF EXISTS `merged_relationships_relationships`;
CREATE TABLE `merged_relationships_relationships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `merged_relationship_id` int(11) DEFAULT NULL,
  `relationship_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_mrr_on_merged_relationship_id` (`merged_relationship_id`) USING BTREE,
  KEY `index_mrr_on_relationship_id` (`relationship_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `message_reads`
--
DROP TABLE IF EXISTS `message_reads`;
CREATE TABLE `message_reads` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `message_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `useridmessageread` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10780 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `messages`
--
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `discussion_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  PRIMARY KEY (`id`),
  KEY `index_messages_on_discussion_id` (`discussion_id`) USING BTREE,
  KEY `index_messages_on_discussion_id_and_sender_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10845 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `metatags`
--
DROP TABLE IF EXISTS `metatags`;
CREATE TABLE `metatags` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(400) DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1383557 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `meteos`
--
DROP TABLE IF EXISTS `meteos`;
CREATE TABLE `meteos` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `wind` int(11) DEFAULT NULL,
  `longitude` int(11) DEFAULT NULL,
  `latitude` int(11) DEFAULT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `day` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `temperature_c` int(11) DEFAULT NULL,
  `temperature_f` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_meteos_on_longitude` (`longitude`) USING BTREE,
  KEY `index_meteos_on_latitude` (`latitude`) USING BTREE,
  KEY `index_meteos_on_day` (`day`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11031684 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `models_links`
--
DROP TABLE IF EXISTS `models_links`;
CREATE TABLE `models_links` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `source_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_id` int(11) DEFAULT NULL,
  `target_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15437 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `multimedias`
--
DROP TABLE IF EXISTS `multimedias`;
CREATE TABLE `multimedias` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) DEFAULT NULL,
  `owner_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `media_id` int(11) DEFAULT NULL,
  `media_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41340 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `notation_labels`
--
DROP TABLE IF EXISTS `notation_labels`;
CREATE TABLE `notation_labels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `notation_id` int(11) DEFAULT NULL,
  `label` int(11) DEFAULT NULL,
  `score` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `notations`
--
DROP TABLE IF EXISTS `notations`;
CREATE TABLE `notations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `notable_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notable_id` int(11) DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `score` int(11) DEFAULT NULL,
  `pros` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cons` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `notices`
--
DROP TABLE IF EXISTS `notices`;
CREATE TABLE `notices` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `review_id` int(11) DEFAULT NULL,
  `notice_type` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `notification_dispatches`
--
DROP TABLE IF EXISTS `notification_dispatches`;
CREATE TABLE `notification_dispatches` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dispatcher_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `args` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `current_user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `error` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26641 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `notification_notifies`
--
DROP TABLE IF EXISTS `notification_notifies`;
CREATE TABLE `notification_notifies` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dispatch_id` int(11) DEFAULT NULL,
  `notifier_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `disabled` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `identifier` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `dispatch_id` (`dispatch_id`),
  CONSTRAINT `notification_notifies_ibfk_1` FOREIGN KEY (`dispatch_id`) REFERENCES `notification_dispatches` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=46933 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `notification_notifies_targets`
--
DROP TABLE IF EXISTS `notification_notifies_targets`;
CREATE TABLE `notification_notifies_targets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `notify_id` int(11) DEFAULT NULL,
  `target_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_id` int(11) DEFAULT NULL,
  `disabled` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `notify_id` (`notify_id`),
  CONSTRAINT `notification_notifies_targets_ibfk_1` FOREIGN KEY (`notify_id`) REFERENCES `notification_notifies` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=520059 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `notifications`
--
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `author_id` int(11) DEFAULT NULL,
  `notifable_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notifable_id` int(11) DEFAULT NULL,
  `seen` tinyint(1) DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `notified_id` int(11) DEFAULT NULL,
  `notified_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `read` tinyint(1) DEFAULT 0,
  `notification_type` int(11) DEFAULT NULL,
  `params` text DEFAULT NULL,
  `post_id` int(11) DEFAULT NULL,
  `version` int(11) DEFAULT 2,
  `grouping_key` int(11) DEFAULT 0,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_likes_on_notifable_id_and_notifable_type` (`notifable_id`,`notifable_type`) USING BTREE,
  KEY `notifiedid` (`notified_id`)
) ENGINE=InnoDB AUTO_INCREMENT=943143 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `opening_days`
--
DROP TABLE IF EXISTS `opening_days`;
CREATE TABLE `opening_days` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `day` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `open_time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `close_time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fullday` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `open` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=435 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `page_admins`
--
DROP TABLE IF EXISTS `page_admins`;
CREATE TABLE `page_admins` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `page_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=274 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `pages`
--
DROP TABLE IF EXISTS `pages`;
CREATE TABLE `pages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_file_size` int(11) DEFAULT NULL,
  `logo_updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `hide` tinyint(1) DEFAULT 1,
  `logo_picture_id` int(11) DEFAULT 3,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `suggestion_priority` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_pages_on_deleted_at` (`deleted_at`),
  KEY `index_pages_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=900065 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `pdfs`
--
DROP TABLE IF EXISTS `pdfs`;
CREATE TABLE `pdfs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `attachment_id` int(11) DEFAULT NULL,
  `file_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_file_size` int(11) DEFAULT NULL,
  `file_updated_at` datetime DEFAULT NULL,
  `media_processing` tinyint(1) DEFAULT 1,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `pictures`
--
DROP TABLE IF EXISTS `pictures`;
CREATE TABLE `pictures` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `album_id` int(11) DEFAULT NULL,
  `picture_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_file_size` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `position` int(11) DEFAULT NULL,
  `width` int(11) DEFAULT NULL,
  `height` int(11) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `picture_url_shortcut` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `media_processing` tinyint(4) DEFAULT 0,
  `media_id` int(11) DEFAULT NULL,
  `album_position` float DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_pictures_on_album_id` (`album_id`) USING BTREE,
  KEY `index_pictures_on_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=188021 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `places`
--
DROP TABLE IF EXISTS `places`;
CREATE TABLE `places` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `placable_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `placable_id` int(11) DEFAULT NULL,
  `hand_picked` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `distance` float DEFAULT NULL,
  `priority` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `index_places_on_club_id` (`club_id`) USING BTREE,
  KEY `idx_count_by_user` (`user_id`),
  KEY `placableid` (`placable_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17165005 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `post_views`
--
DROP TABLE IF EXISTS `post_views`;
CREATE TABLE `post_views` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `post_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=216716 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `posts`
--
DROP TABLE IF EXISTS `posts`;
CREATE TABLE `posts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `postable_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `postable_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `shared_from` int(11) DEFAULT NULL,
  `posted_id` int(11) DEFAULT NULL,
  `posted_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `video_link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `web_link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publish` datetime DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `author_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `postauthorid` (`author_id`),
  KEY `postpostedid` (`posted_id`),
  KEY `postpostableid` (`postable_id`)
) ENGINE=InnoDB AUTO_INCREMENT=145462 DEFAULT CHARSET=utf8mb4;
-- D_ELIMITER ;;
-- D_ELIMITER ;
--
-- Table structure for table `posts_views`
--
DROP TABLE IF EXISTS `posts_views`;
CREATE TABLE `posts_views` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `post_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `posts_views_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `posts_views_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21881 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `prices`
--
DROP TABLE IF EXISTS `prices`;
CREATE TABLE `prices` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pricable_id` int(11) DEFAULT NULL,
  `pricable_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `currency` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price_value` int(11) DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6559 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `prizes`
--
DROP TABLE IF EXISTS `prizes`;
CREATE TABLE `prizes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `picture_id` int(11) DEFAULT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sponsor_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `products`
--
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `brand_id` int(11) DEFAULT NULL,
  `item_id` int(11) DEFAULT NULL,
  `photo_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_file_size` int(11) DEFAULT NULL,
  `photo_updated_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `year` int(11) DEFAULT NULL,
  `product_type` varchar(255) DEFAULT NULL,
  `album_id` int(11) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `price_id` int(11) DEFAULT NULL,
  `spec_picture_id` int(11) DEFAULT NULL,
  `slug` varchar(255) DEFAULT NULL,
  `internal_video_id` int(11) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `score` float DEFAULT NULL,
  `score_feel` float DEFAULT NULL,
  `score_accuracy` float DEFAULT NULL,
  `score_distance` float DEFAULT NULL,
  `score_forgiveness` float DEFAULT NULL,
  `score_design` float DEFAULT NULL,
  `picture_id` int(11) DEFAULT NULL,
  `gender_restriction` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_products_on_name` (`name`) USING BTREE,
  KEY `index_products_on_brand_id` (`brand_id`) USING BTREE,
  KEY `index_products_on_item_id` (`item_id`) USING BTREE,
  KEY `index_products_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=1891 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `products_reviews`
--
DROP TABLE IF EXISTS `products_reviews`;
CREATE TABLE `products_reviews` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pros` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cons` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `score` int(11) DEFAULT NULL,
  `score_feel` int(11) DEFAULT NULL,
  `score_accuracy` int(11) DEFAULT NULL,
  `score_distance` int(11) DEFAULT NULL,
  `score_forgiveness` int(11) DEFAULT NULL,
  `score_design` int(11) DEFAULT NULL,
  `online` tinyint(1) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `product_id` int(11) DEFAULT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `products_reviews_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `products_reviews_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `rankings`
--
DROP TABLE IF EXISTS `rankings`;
CREATE TABLE `rankings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23671 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `relationships`
--
DROP TABLE IF EXISTS `relationships`;
CREATE TABLE `relationships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `target_id` int(11) DEFAULT NULL,
  `state` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `target_state` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idxuniqusertarget` (`user_id`,`target_id`) USING BTREE,
  KEY `idx_target` (`target_id`) USING BTREE,
  KEY `idx_user_issou` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=355135 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `reports`
--
DROP TABLE IF EXISTS `reports`;
CREATE TABLE `reports` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `reportable_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reportable_id` int(11) DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_reports_on_reportable_type_and_reportable_id` (`reportable_type`,`reportable_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=113 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `restaurants`
--
DROP TABLE IF EXISTS `restaurants`;
CREATE TABLE `restaurants` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cooking_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `review_stats`
--
DROP TABLE IF EXISTS `review_stats`;
CREATE TABLE `review_stats` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `nb_reviews` int(11) DEFAULT NULL,
  `average` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `clubidreviewstat` (`club_id`)
) ENGINE=InnoDB AUTO_INCREMENT=30670 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `reviews`
--
DROP TABLE IF EXISTS `reviews`;
CREATE TABLE `reviews` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `club_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `score` int(11) DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `online` tinyint(1) DEFAULT 0,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  PRIMARY KEY (`id`),
  KEY `index_reviews_on_user_id` (`user_id`) USING BTREE,
  KEY `index_reviews_on_club_id` (`club_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25027 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `roles`
--
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resource_id` int(11) DEFAULT NULL,
  `resource_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `saved_errors`
--
DROP TABLE IF EXISTS `saved_errors`;
CREATE TABLE `saved_errors` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `current_user_id` int(11) DEFAULT NULL,
  `environment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `kind` int(11) DEFAULT NULL,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operation_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `origin` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `variables` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `query` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stack` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `num_occurences` int(11) DEFAULT NULL,
  `num_occurences_when_discarded` int(11) DEFAULT NULL,
  `discarded_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `user_agent` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `schema_migrations`
--
DROP TABLE IF EXISTS `schema_migrations`;
CREATE TABLE `schema_migrations` (
  `version` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  UNIQUE KEY `unique_schema_migrations` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `serialized_amenities`
--
DROP TABLE IF EXISTS `serialized_amenities`;
CREATE TABLE `serialized_amenities` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `serialized_data` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `approved` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `sessions`
--
DROP TABLE IF EXISTS `sessions`;
CREATE TABLE `sessions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_sessions_on_session_id` (`session_id`) USING BTREE,
  KEY `index_sessions_on_updated_at` (`updated_at`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=839446 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `shaft_flexes`
--
DROP TABLE IF EXISTS `shaft_flexes`;
CREATE TABLE `shaft_flexes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `torque_min` float DEFAULT NULL,
  `torque_max` float DEFAULT NULL,
  `flex_type_id` int(11) DEFAULT NULL,
  `shaft_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2096 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `shaft_flexes_types`
--
DROP TABLE IF EXISTS `shaft_flexes_types`;
CREATE TABLE `shaft_flexes_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=74 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `shaft_flexes_weights`
--
DROP TABLE IF EXISTS `shaft_flexes_weights`;
CREATE TABLE `shaft_flexes_weights` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `weight` float DEFAULT NULL,
  `flex_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1696 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `shaft_products`
--
DROP TABLE IF EXISTS `shaft_products`;
CREATE TABLE `shaft_products` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `brand_id` int(11) DEFAULT NULL,
  `price_id` int(11) DEFAULT NULL,
  `album_id` int(11) DEFAULT NULL,
  `picture_id` int(11) DEFAULT NULL,
  `internal_video_id` int(11) DEFAULT NULL,
  `gender_restriction` int(11) DEFAULT NULL,
  `item_type` int(11) DEFAULT NULL,
  `launch` int(11) DEFAULT NULL,
  `spin` int(11) DEFAULT NULL,
  `length` float DEFAULT NULL,
  `butt_frequency` float DEFAULT NULL,
  `tip_frequency` float DEFAULT NULL,
  `tip_diameter` float DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_shaft_products_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=775 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `shoes_sizes`
--
DROP TABLE IF EXISTS `shoes_sizes`;
CREATE TABLE `shoes_sizes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `size` float DEFAULT NULL,
  `clothes_product_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7919 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `signup_stats_followings`
--
DROP TABLE IF EXISTS `signup_stats_followings`;
CREATE TABLE `signup_stats_followings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `followed_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `followed_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=160441 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `signup_stats_user_suggestions`
--
DROP TABLE IF EXISTS `signup_stats_user_suggestions`;
CREATE TABLE `signup_stats_user_suggestions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `num_requests` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3410 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `simple_reviews`
--
DROP TABLE IF EXISTS `simple_reviews`;
CREATE TABLE `simple_reviews` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `score` int(11) DEFAULT NULL,
  `author_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `reviewed_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewed_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00',
  PRIMARY KEY (`id`),
  KEY `index_simple_reviews_on_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `sinup_stats_followings`
--
DROP TABLE IF EXISTS `sinup_stats_followings`;
CREATE TABLE `sinup_stats_followings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `followed_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `followed_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `slopes`
--
DROP TABLE IF EXISTS `slopes`;
CREATE TABLE `slopes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `length` int(11) DEFAULT NULL,
  `slope` int(11) DEFAULT NULL,
  `par` int(11) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `unit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=129 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `slugs`
--
DROP TABLE IF EXISTS `slugs`;
CREATE TABLE `slugs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `resource_id` int(11) DEFAULT NULL,
  `resource_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `base_slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `suffix` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=68574 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `social_network_accounts`
--
DROP TABLE IF EXISTS `social_network_accounts`;
CREATE TABLE `social_network_accounts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `provider` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `appid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `publish` tinyint(1) DEFAULT 0,
  `facebook_user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `secret_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=505 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `sponsors`
--
DROP TABLE IF EXISTS `sponsors`;
CREATE TABLE `sponsors` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_file_size` int(11) DEFAULT NULL,
  `logo_updated_at` datetime DEFAULT NULL,
  `sponsorable_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sponsorable_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `website` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `sponsorableid` (`sponsorable_id`)
) ENGINE=InnoDB AUTO_INCREMENT=841 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `statuses`
--
DROP TABLE IF EXISTS `statuses`;
CREATE TABLE `statuses` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `video_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `video_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `video_file_size` int(11) DEFAULT NULL,
  `video_updated_at` datetime DEFAULT NULL,
  `image_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_file_size` int(11) DEFAULT NULL,
  `image_updated_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `private` tinyint(1) DEFAULT 0,
  `publish` datetime DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `author_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `external_video_id` int(11) DEFAULT NULL,
  `online` tinyint(1) DEFAULT 1,
  `video_processing` tinyint(1) DEFAULT NULL,
  `image_width` int(11) DEFAULT NULL,
  `image_height` int(11) DEFAULT NULL,
  `media_processing` tinyint(1) DEFAULT 0,
  `push` tinyint(1) DEFAULT 0,
  `video_width` int(11) DEFAULT NULL,
  `video_height` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `statusauthorid` (`author_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23303 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `sticky_note_users`
--
DROP TABLE IF EXISTS `sticky_note_users`;
CREATE TABLE `sticky_note_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `sticky_note_id` int(11) DEFAULT NULL,
  `seen` tinyint(1) DEFAULT NULL,
  `clicked` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `sticky_notes`
--
DROP TABLE IF EXISTS `sticky_notes`;
CREATE TABLE `sticky_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `picture_id` int(11) DEFAULT NULL,
  `external_video_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `sticky_notes_users`
--
DROP TABLE IF EXISTS `sticky_notes_users`;
CREATE TABLE `sticky_notes_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `sticky_note_id` int(11) DEFAULT NULL,
  `seen` tinyint(1) DEFAULT 0,
  `clicked` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `sticky_note_id` (`sticky_note_id`),
  CONSTRAINT `sticky_notes_users_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `sticky_notes_users_ibfk_2` FOREIGN KEY (`sticky_note_id`) REFERENCES `sticky_notes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `suggestions`
--
DROP TABLE IF EXISTS `suggestions`;
CREATE TABLE `suggestions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `suggested_user_id` int(11) DEFAULT NULL,
  `nb_time_displayed` int(11) DEFAULT 0,
  `rejected` tinyint(1) DEFAULT 0,
  `accepted` tinyint(1) DEFAULT 0,
  `match_score` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `index_suggestions_on_user_id` (`user_id`) USING BTREE,
  KEY `index_suggestions_on_suggested_user_id` (`suggested_user_id`) USING BTREE,
  KEY `user_id_suggested_user_id` (`user_id`,`suggested_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14373388521 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `suggestions_views`
--
DROP TABLE IF EXISTS `suggestions_views`;
CREATE TABLE `suggestions_views` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `suggested_user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `suggested_user_id` (`suggested_user_id`),
  KEY `user_id_suggested_user_id` (`user_id`,`suggested_user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `suggestions_views_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7144755 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `tagged_items`
--
DROP TABLE IF EXISTS `tagged_items`;
CREATE TABLE `tagged_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tagged_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tagged_id` int(11) DEFAULT NULL,
  `taggable_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `taggable_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `taggedid` (`tagged_id`)
) ENGINE=InnoDB AUTO_INCREMENT=939 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `tags`
--
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tagged_id` int(11) DEFAULT NULL,
  `tagged_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachment_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `texts`
--
DROP TABLE IF EXISTS `texts`;
CREATE TABLE `texts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachment_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=97 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `tokens`
--
DROP TABLE IF EXISTS `tokens`;
CREATE TABLE `tokens` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `token` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `application` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_token` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_token_expire` datetime DEFAULT NULL,
  `scope` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expire_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT '1970-01-01 00:00:00',
  PRIMARY KEY (`id`),
  KEY `index_token_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21936 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `travels`
--
DROP TABLE IF EXISTS `travels`;
CREATE TABLE `travels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `starts_at` datetime DEFAULT NULL,
  `ends_at` datetime DEFAULT NULL,
  `budget` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comment` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `firstname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `currency` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `responsible_id` int(11) DEFAULT NULL,
  `current_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comment_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` int(11) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `trending_categories`
--
DROP TABLE IF EXISTS `trending_categories`;
CREATE TABLE `trending_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `randomize` tinyint(1) DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ranking_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `virtual_type` int(11) DEFAULT -1,
  `parent_area_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parent_area_id` int(11) DEFAULT NULL,
  `sitemap` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `index_trending_categories_on_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=359 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `trending_categories_items`
--
DROP TABLE IF EXISTS `trending_categories_items`;
CREATE TABLE `trending_categories_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category_id` int(11) DEFAULT NULL,
  `item_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16027 DEFAULT CHARSET=latin1;
--
-- Table structure for table `unsent_pushes`
--
DROP TABLE IF EXISTS `unsent_pushes`;
CREATE TABLE `unsent_pushes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `push_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `data_id` int(11) DEFAULT NULL,
  `error_message` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `unsent_pushes_users`
--
DROP TABLE IF EXISTS `unsent_pushes_users`;
CREATE TABLE `unsent_pushes_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `unsent_push_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
--
-- Table structure for table `user_club_activities`
--
DROP TABLE IF EXISTS `user_club_activities`;
CREATE TABLE `user_club_activities` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `club_id` int(11) DEFAULT NULL,
  `review_score` int(11) DEFAULT NULL,
  `following` tinyint(1) DEFAULT NULL,
  `checked_in` tinyint(1) DEFAULT NULL,
  `played` int(11) DEFAULT 0,
  `wishing` tinyint(1) DEFAULT NULL,
  `wishing_membership` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `club_id` (`club_id`),
  KEY `user_id_idx` (`user_id`) USING BTREE,
  CONSTRAINT `user_club_activities_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_club_activities_ibfk_2` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=144550 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `user_products`
--
DROP TABLE IF EXISTS `user_products`;
CREATE TABLE `user_products` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `item_id` int(11) DEFAULT NULL,
  `product_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `year` int(11) DEFAULT NULL,
  `product_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_equipment_on_user_id` (`user_id`) USING BTREE,
  KEY `index_equipment_on_product_id` (`product_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6915 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `usernames`
--
DROP TABLE IF EXISTS `usernames`;
CREATE TABLE `usernames` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usernamable_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usernamable_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_usernames_on_usernamable_id_and_usernamable_type` (`usernamable_id`,`usernamable_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=14231 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `users_interactions`
--
DROP TABLE IF EXISTS `users_interactions`;
CREATE TABLE `users_interactions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `resource_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resource_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `platform` int(11) DEFAULT NULL,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `referrer` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=448632 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `users_roles`
--
DROP TABLE IF EXISTS `users_roles`;
CREATE TABLE `users_roles` (
  `user_id` int(11) DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `index_users_roles_on_user_id_and_role_id` (`user_id`,`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `versions`
--
DROP TABLE IF EXISTS `versions`;
CREATE TABLE `versions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_type` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_id` int(11) NOT NULL,
  `event` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `whodunnit` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `object` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_versions_on_item_type` (`item_type`) USING BTREE,
  KEY `index_versions_on_item_id` (`item_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=528658 DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `villas`
--
DROP TABLE IF EXISTS `villas`;
CREATE TABLE `villas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `visits`
--
DROP TABLE IF EXISTS `visits`;
CREATE TABLE `visits` (
  `id` binary(16) NOT NULL,
  `visitor_id` binary(16) DEFAULT NULL,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `referrer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `landing_page` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `referring_domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `search_keyword` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `browser` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `os` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `screen_height` int(11) DEFAULT NULL,
  `screen_width` int(11) DEFAULT NULL,
  `country` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `region` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `utm_source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `utm_medium` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `utm_term` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `utm_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `utm_campaign` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_visits_on_id` (`id`) USING BTREE,
  KEY `index_visits_on_visitor_id` (`visitor_id`) USING BTREE,
  KEY `index_visits_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- Table structure for table `votes`
--
DROP TABLE IF EXISTS `votes`;
CREATE TABLE `votes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `resource_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `value` int(11) DEFAULT NULL,
  `resource_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `review_id` (`resource_id`)
) ENGINE=InnoDB AUTO_INCREMENT=80 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `weathers`
--
DROP TABLE IF EXISTS `weathers`;
CREATE TABLE `weathers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` datetime DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `temp_min` int(11) DEFAULT NULL,
  `temp_max` int(11) DEFAULT NULL,
  `wind_speed` int(11) DEFAULT NULL,
  `weathering` int(11) DEFAULT NULL,
  `weatherable_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weatherable_id` int(11) DEFAULT NULL,
  `wind_direction` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30170480 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Table structure for table `wishes`
--
DROP TABLE IF EXISTS `wishes`;
CREATE TABLE `wishes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `wishable_id` int(11) DEFAULT NULL,
  `wishable_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_wishes_on_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=779 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Dump completed on 2019-06-05 17:52:38
