-- MariaDB dump 10.19  Distrib 10.11.6-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: DiscordBotGiveaway
-- ------------------------------------------------------
-- Server version	10.11.6-MariaDB-0+deb12u1

DROP SEQUENCE IF EXISTS `sequence_id_auto_gen`;
CREATE SEQUENCE `sequence_id_auto_gen` start with 1 minvalue 1 maxvalue 9223372036854775806 increment by 100 cache 1000 nocycle ENGINE=InnoDB;
SELECT SETVAL(`sequence_id_auto_gen`, 1000, 0);

DROP TABLE IF EXISTS `active_giveaways`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `active_giveaways` (
  `guild_id` bigint(20) NOT NULL,
  `channel_id` bigint(20) NOT NULL,
  `count_winners` int(10) DEFAULT NULL,
  `date_end` datetime(6) DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'Giveaway',
  `message_id` bigint(20) NOT NULL,
  `is_for_specific_role` bit(1) DEFAULT NULL,
  `role_id` bigint(20) DEFAULT NULL,
  `created_user_id` bigint(20) NOT NULL,
  `url_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `min_participants` int(11) DEFAULT NULL,
  `finish` tinyint(1) NOT NULL DEFAULT 0,
  `forbidden_role` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`guild_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `list_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `list_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `giveaway_id` bigint(20) NOT NULL,
  `guild_id` bigint(20) NOT NULL,
  `created_user_id` bigint(20) NOT NULL,
  `nick_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=126 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `participants` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nick_name` varchar(255) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `guild_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9edsvtap9uhuikihdsv3c74rv` (`guild_id`),
  CONSTRAINT `FK9edsvtap9uhuikihdsv3c74rv` FOREIGN KEY (`guild_id`) REFERENCES `active_giveaways` (`guild_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=600002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

-- Table structure for table `scheduling`
--

DROP TABLE IF EXISTS `scheduling`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scheduling` (
  `guild_id` bigint(20) NOT NULL,
  `channel_id` bigint(20) NOT NULL,
  `count_winners` int(11) DEFAULT NULL,
  `create_giveaway` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `date_end` timestamp NULL DEFAULT NULL,
  `title` varchar(255) DEFAULT 'Giveaway',
  `is_for_specific_role` tinyint(1) DEFAULT NULL,
  `role_id` bigint(20) DEFAULT NULL,
  `created_user_id` bigint(20) NOT NULL,
  `url_image` varchar(255) DEFAULT NULL,
  `min_participants` int(11) DEFAULT NULL,
  `forbidden_role` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`guild_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `settings` (
  `server_id` bigint(20) NOT NULL,
  `language` varchar(255) NOT NULL,
  `color_hex` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;