-- MySQL dump 10.13  Distrib 5.7.21, for osx10.13 (x86_64)
--
-- Host: localhost    Database: bodyfat
-- ------------------------------------------------------
-- Server version	5.7.21

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `bodyfat`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `bodyfat` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `bodyfat`;

--
-- Table structure for table `x`
--

DROP TABLE IF EXISTS `x`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `x1` tinyint(1) NOT NULL,
  `x2` tinyint(1) NOT NULL,
  `x3` tinyint(1) NOT NULL,
  `x4` tinyint(1) NOT NULL,
  `x5` tinyint(1) NOT NULL,
  `x6` tinyint(1) NOT NULL,
  `x7` tinyint(1) NOT NULL,
  `x8` tinyint(1) NOT NULL,
  `x9` tinyint(1) NOT NULL,
  `x10` tinyint(1) NOT NULL,
  `x11` tinyint(1) NOT NULL,
  `x12` tinyint(1) NOT NULL,
  `x13` tinyint(1) NOT NULL,
  `x14` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=256 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x`
--

LOCK TABLES `x` WRITE;
/*!40000 ALTER TABLE `x` DISABLE KEYS */;
INSERT INTO `x` VALUES (1,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(2,0,0,0,1,1,0,0,0,0,0,1,0,1,0),(3,1,0,0,0,0,0,0,0,1,1,1,0,0,0),(4,0,0,1,1,0,1,0,1,1,0,0,1,1,0),(5,1,0,1,1,0,0,1,1,1,1,1,1,0,0),(6,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(7,0,0,1,0,0,1,0,1,0,0,1,0,0,0),(8,0,0,0,1,0,0,0,0,1,1,1,0,1,1),(9,0,0,1,1,1,1,0,1,1,0,1,1,1,0),(10,0,0,1,1,1,0,0,1,1,1,1,1,1,1),(11,0,0,1,1,1,1,0,0,1,1,1,1,1,1),(12,0,0,1,1,1,1,0,1,1,1,1,1,1,1),(13,1,0,1,0,1,1,1,1,1,0,0,1,0,0),(14,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(15,1,0,1,0,1,1,1,1,1,1,1,1,1,0),(16,1,0,0,0,0,0,1,0,1,1,0,0,0,0),(17,1,0,1,1,1,1,1,1,1,1,1,1,1,0),(18,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(19,0,0,1,0,0,1,0,1,1,1,1,1,1,1),(20,0,0,1,1,1,1,1,1,1,1,1,1,1,0),(21,0,0,1,0,1,1,1,1,1,0,0,1,1,1),(22,0,0,1,0,1,1,1,1,1,1,1,1,1,1),(23,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(24,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(25,0,0,0,0,0,0,0,0,0,0,1,0,0,0),(26,0,0,0,1,0,0,0,0,0,0,0,0,0,0),(27,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(28,1,0,0,0,1,0,0,0,0,0,0,0,0,0),(29,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(30,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(31,0,0,1,1,1,1,0,1,0,1,1,1,0,1),(32,0,0,0,1,0,0,0,1,0,1,0,0,0,0),(33,0,0,0,1,1,0,0,0,0,0,1,0,1,1),(34,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(35,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(36,1,1,1,0,1,1,1,1,1,0,0,0,1,0),(37,1,0,1,0,1,1,1,1,1,1,0,1,1,1),(38,1,1,1,0,1,1,1,1,1,1,1,1,1,1),(39,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(40,1,1,1,0,1,1,1,1,1,1,1,1,1,0),(41,1,1,1,0,1,1,1,1,1,1,1,1,1,1),(42,1,1,1,0,0,1,1,1,1,1,1,1,0,0),(43,1,1,1,0,0,1,1,1,1,1,1,1,1,1),(44,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(45,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(46,0,0,0,1,0,0,0,0,0,0,1,0,0,0),(47,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(48,0,0,0,1,0,0,0,0,0,0,0,0,0,0),(49,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(50,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(51,0,1,0,1,0,0,0,0,0,0,0,0,0,0),(52,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(53,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(54,0,1,0,1,0,0,0,0,0,0,0,1,0,1),(55,0,0,0,0,0,0,0,0,0,0,0,0,0,1),(56,1,1,1,1,1,1,1,1,0,0,0,1,1,1),(57,1,1,1,0,1,1,1,1,1,1,0,1,0,1),(58,1,1,1,0,1,1,1,1,1,1,0,1,1,1),(59,1,1,1,1,1,1,1,0,1,0,0,0,1,0),(60,1,1,1,0,1,1,1,1,1,0,1,1,1,1),(61,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(62,1,1,1,0,0,1,1,1,1,0,0,1,1,0),(63,1,1,1,1,0,1,1,1,1,1,1,1,1,1),(64,1,1,1,0,0,1,1,1,1,1,0,1,1,1),(65,1,1,1,0,1,1,1,1,1,1,1,1,1,1),(66,1,1,1,0,1,1,1,1,1,1,0,1,1,1),(67,1,1,0,1,0,0,0,0,0,0,0,0,0,0),(68,0,1,0,1,0,0,0,0,0,0,0,1,0,1),(69,0,1,0,0,0,0,0,0,0,0,0,1,0,0),(70,0,1,0,1,0,0,0,0,0,0,0,0,0,0),(71,1,1,0,1,0,0,1,0,0,1,0,0,0,1),(72,0,1,0,0,1,0,0,0,0,0,0,0,0,0),(73,0,1,0,1,0,0,0,0,0,0,1,0,0,0),(74,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(75,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(76,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(77,0,1,0,0,1,0,0,0,0,1,1,0,1,0),(78,1,1,1,0,1,1,1,0,0,0,0,0,0,1),(79,1,1,0,1,0,0,1,0,0,0,0,0,0,0),(80,0,1,0,0,0,1,1,1,0,1,0,0,0,1),(81,1,1,0,0,1,0,1,0,0,0,1,0,0,1),(82,1,1,0,0,1,0,0,0,0,0,0,0,0,0),(83,0,1,1,1,1,1,1,1,0,1,1,1,1,1),(84,1,1,0,0,1,1,1,0,0,0,1,0,0,1),(85,1,1,0,0,1,1,1,0,0,0,0,0,0,0),(86,1,1,0,0,0,0,0,0,0,0,1,1,0,0),(87,0,1,0,0,0,0,0,0,0,0,0,1,0,1),(88,1,1,0,0,0,1,0,0,0,1,1,0,0,0),(89,0,1,1,1,0,0,0,0,1,0,1,0,1,1),(90,0,1,0,1,0,0,0,0,1,1,1,0,1,1),(91,1,1,1,0,0,1,1,1,0,0,0,0,0,0),(92,0,1,1,0,1,1,1,1,0,1,1,0,0,1),(93,0,1,0,1,0,0,0,0,0,0,0,0,0,1),(94,1,1,1,1,0,1,1,1,0,1,1,1,1,1),(95,0,1,1,1,0,0,0,1,0,1,1,0,0,0),(96,0,1,1,1,1,1,1,1,1,1,1,1,1,1),(97,0,0,1,1,0,0,1,1,1,1,1,0,1,1),(98,0,1,0,0,1,0,0,0,1,1,1,0,0,0),(99,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(100,1,1,1,1,1,1,1,1,1,1,0,1,1,0),(101,1,1,1,1,1,1,1,1,0,1,0,1,1,1),(102,1,1,0,1,0,0,1,0,1,0,0,0,0,0),(103,1,0,0,1,0,0,0,0,1,0,1,0,1,1),(104,1,1,1,1,1,1,1,1,1,1,1,0,1,1),(105,1,0,1,0,1,1,1,1,1,0,0,0,0,0),(106,0,0,0,0,0,0,0,0,0,1,1,0,1,1),(107,1,0,1,1,1,1,1,1,1,1,1,0,0,0),(108,0,1,1,1,1,1,1,1,0,1,1,1,1,1),(109,0,0,1,1,1,1,0,1,0,1,1,1,1,1),(110,1,0,0,0,0,0,1,1,0,0,0,1,0,0),(111,1,0,0,0,0,0,0,0,1,0,0,1,0,0),(112,1,0,1,0,0,1,1,1,1,1,1,1,0,0),(113,1,1,1,0,1,1,1,0,1,0,1,1,1,1),(114,1,0,0,1,0,0,0,1,1,0,0,0,0,0),(115,1,1,0,1,0,1,1,0,1,0,0,0,0,0),(116,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(117,1,1,1,1,0,0,0,1,0,0,0,0,0,0),(118,0,1,1,1,1,0,0,0,0,1,1,1,1,1),(119,1,0,1,1,1,0,1,1,1,1,1,1,1,1),(120,0,1,1,1,0,1,0,1,0,1,1,1,0,0),(121,1,1,1,1,1,1,1,1,0,1,1,1,1,1),(122,1,1,1,1,1,0,1,1,0,1,1,1,1,1),(123,0,0,0,0,0,0,0,0,1,1,0,1,0,0),(124,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(125,0,1,0,0,0,0,0,0,0,0,1,1,1,0),(126,0,1,0,0,0,1,0,1,1,0,0,1,1,0),(127,1,0,1,0,1,0,1,0,1,0,0,1,1,0),(128,0,0,0,0,0,0,0,0,0,0,0,1,0,0),(129,1,0,1,1,1,1,1,1,1,1,0,1,1,0),(130,0,0,0,0,1,0,0,0,0,0,0,0,0,0),(131,0,1,0,1,0,0,0,0,0,1,1,0,0,0),(132,1,0,0,1,0,0,0,0,1,0,1,0,0,0),(133,1,1,1,1,0,1,1,1,1,0,0,1,0,0),(134,1,1,0,0,0,1,0,0,0,0,0,1,1,0),(135,1,0,0,0,0,0,0,0,0,0,1,1,1,0),(136,1,1,1,0,0,1,1,1,0,0,0,0,1,0),(137,1,0,0,1,0,0,0,0,0,0,0,1,0,0),(138,1,0,1,1,0,0,1,1,1,1,1,1,0,0),(139,1,0,0,1,0,0,0,0,0,0,0,0,0,0),(140,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(141,1,0,1,1,0,0,1,1,1,1,0,0,0,0),(142,0,0,0,0,0,0,1,0,1,0,0,0,0,0),(143,1,1,0,0,0,1,1,0,0,0,0,0,0,0),(144,0,0,0,1,0,0,0,0,0,0,0,0,0,0),(145,0,0,1,1,0,0,0,1,1,0,1,0,1,0),(146,0,0,0,1,0,0,0,0,0,0,0,1,0,0),(147,0,0,1,1,1,1,1,1,1,1,1,1,1,1),(148,1,0,1,0,1,1,1,1,1,1,1,1,1,1),(149,0,0,0,1,0,0,0,0,0,0,0,0,0,0),(150,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(151,0,0,0,0,0,0,0,0,0,0,0,0,1,0),(152,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(153,0,0,0,1,0,0,0,0,0,0,1,0,0,0),(154,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(155,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(156,0,0,0,1,0,0,0,0,0,0,0,1,0,0),(157,1,0,1,0,1,1,1,1,1,1,1,1,1,1),(158,0,0,1,1,0,0,0,1,1,0,1,0,0,1),(159,0,0,0,0,0,0,0,0,0,0,0,0,1,0),(160,1,0,1,1,0,1,1,0,1,1,1,0,0,0),(161,0,0,0,1,0,0,0,0,0,0,0,0,0,0),(162,0,0,1,1,1,1,1,1,1,1,1,0,1,1),(163,0,0,1,0,1,0,1,1,1,0,1,1,1,1),(164,0,0,0,1,0,0,0,0,0,0,0,0,0,0),(165,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(166,0,0,1,1,1,1,1,1,1,1,1,1,1,1),(167,1,0,0,0,1,0,0,0,0,0,0,0,0,0),(168,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(169,1,0,1,0,1,1,1,1,1,1,1,1,1,1),(170,0,0,0,0,0,0,0,0,1,1,1,1,1,0),(171,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(172,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(173,1,0,1,1,1,1,0,0,0,0,0,0,0,0),(174,0,0,0,1,1,0,0,1,1,0,0,1,0,0),(175,1,0,1,1,1,1,1,1,1,1,1,1,0,1),(176,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(177,0,0,0,0,0,0,0,0,1,0,0,0,0,0),(178,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(179,1,0,1,0,0,1,1,1,1,1,1,1,1,0),(180,0,0,1,1,1,1,1,1,1,1,1,1,1,1),(181,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(182,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(183,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(184,0,0,0,0,0,0,0,0,0,1,0,0,0,0),(185,0,0,0,1,0,0,0,0,0,1,0,0,1,0),(186,0,0,0,1,1,0,0,0,0,1,0,1,1,0),(187,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(188,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(189,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(190,1,0,1,0,0,1,1,1,1,1,1,1,1,1),(191,0,0,0,0,0,0,0,0,0,0,0,0,0,0),(192,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(193,0,0,1,1,1,1,1,1,1,1,1,1,1,1),(194,1,0,1,1,1,1,1,1,1,1,1,1,1,1),(195,1,0,0,1,0,0,0,0,1,1,0,0,0,0),(196,1,0,1,0,1,1,1,1,1,0,1,1,1,1),(197,1,0,0,0,0,0,0,0,0,1,1,0,0,0),(198,0,0,0,1,0,0,0,1,1,0,0,0,0,0),(199,0,0,0,1,0,0,0,0,0,1,0,0,1,1),(200,1,0,0,0,0,1,0,0,0,1,1,1,1,1),(201,0,0,1,1,0,1,0,0,1,1,1,0,0,1),(202,1,0,0,0,0,0,0,0,0,0,0,0,0,0),(203,1,0,1,1,0,1,1,1,1,0,1,1,1,1),(204,0,1,1,1,0,1,0,1,1,1,1,1,1,1),(205,1,1,1,0,1,1,1,1,1,1,0,1,1,0),(206,0,1,1,1,1,1,1,1,1,1,1,1,0,1),(207,1,1,0,0,1,1,1,1,0,0,0,1,1,0),(208,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(209,0,1,0,1,0,1,0,0,0,0,0,0,0,0),(210,0,1,0,1,0,0,0,0,0,0,0,0,0,0),(211,0,1,0,0,0,0,0,0,0,0,0,0,1,0),(212,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(213,1,1,0,1,1,0,0,0,0,1,1,0,0,1),(214,0,1,1,1,1,1,1,1,1,1,1,1,1,1),(215,1,1,0,1,0,0,0,0,0,0,0,0,0,0),(216,1,1,1,0,1,1,1,1,1,0,1,1,1,1),(217,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(218,0,1,0,0,0,0,0,0,0,1,0,0,0,1),(219,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(220,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(221,0,1,0,1,1,0,1,0,0,0,1,0,1,1),(222,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(223,0,1,0,0,0,0,0,0,1,1,1,0,0,0),(224,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(225,0,1,1,0,1,1,1,0,0,0,0,1,1,1),(226,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(227,0,1,0,0,0,1,1,0,0,0,0,1,1,1),(228,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(229,0,1,0,0,1,1,0,0,0,0,0,1,1,1),(230,0,1,0,0,0,0,1,0,0,1,1,1,1,1),(231,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(232,0,1,1,1,1,1,1,1,1,1,0,0,1,1),(233,0,1,0,1,0,1,0,0,0,1,1,0,1,0),(234,1,1,0,0,0,0,1,1,0,0,0,0,0,0),(235,1,1,0,0,1,0,1,0,0,0,0,0,0,0),(236,0,1,0,0,1,1,1,0,0,0,0,0,0,0),(237,1,1,1,1,1,1,1,1,1,1,1,1,0,1),(238,1,1,1,0,1,1,1,1,1,1,0,1,1,1),(239,0,1,0,0,0,0,0,0,1,1,0,0,1,0),(240,1,1,1,0,1,1,1,1,1,0,1,1,1,1),(241,0,1,0,0,0,0,0,0,0,0,0,0,0,0),(242,1,1,1,0,1,1,1,1,1,1,1,1,1,1),(243,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(244,1,1,1,1,1,1,1,1,1,1,1,1,1,1),(245,1,1,1,0,1,1,1,1,0,1,1,1,1,1),(246,0,1,0,0,0,0,0,0,0,0,0,0,0,1),(247,1,1,1,1,1,1,1,1,1,1,0,1,1,1),(248,0,1,0,0,0,0,0,0,0,0,0,0,0,1),(249,1,1,1,0,1,1,1,1,1,1,1,1,0,1),(250,1,1,1,0,1,1,1,1,1,0,0,0,0,0),(251,1,1,1,1,1,1,1,0,0,1,0,0,1,1),(252,1,1,1,0,1,1,1,1,1,1,1,1,1,1);
/*!40000 ALTER TABLE `x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `yz`
--

DROP TABLE IF EXISTS `yz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yz` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `y1` double NOT NULL,
  `y2` double NOT NULL,
  `y3` double NOT NULL,
  `y4` double NOT NULL,
  `y5` double NOT NULL,
  `y6` double NOT NULL,
  `y7` double NOT NULL,
  `y8` double NOT NULL,
  `y9` double NOT NULL,
  `y10` double NOT NULL,
  `y11` double NOT NULL,
  `y12` double NOT NULL,
  `y13` double NOT NULL,
  `y14` double NOT NULL,
  `z` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=256 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `yz`
--

LOCK TABLES `yz` WRITE;
/*!40000 ALTER TABLE `yz` DISABLE KEYS */;
INSERT INTO `yz` VALUES (1,12.3,23,154.25,67.75,36.2,93.1,85.2,94.5,59,37.3,21.9,32,27.4,17.1,1.0708),(2,6.1,22,173.25,72.25,38.5,93.6,83,98.7,58.7,37.3,23.4,30.5,28.9,18.2,1.0853),(3,25.3,22,154,66.25,34,95.8,87.9,99.2,59.6,38.9,24,28.8,25.2,16.6,1.0414),(4,10.4,26,184.75,72.25,37.4,101.8,86.4,101.2,60.1,37.3,22.8,32.4,29.4,18.2,1.0751),(5,28.7,24,184.25,71.25,34.4,97.3,100,101.9,63.2,42.2,24,32.2,27.7,17.7,1.034),(6,20.9,24,210.25,74.75,39,104.5,94.4,107.8,66,42,25.6,35.7,30.6,18.8,1.0502),(7,19.2,26,181,69.75,36.4,105.1,90.7,100.3,58.4,38.3,22.9,31.9,27.8,17.7,1.0549),(8,12.4,25,176,72.5,37.8,99.6,88.5,97.1,60,39.4,23.2,30.5,29,18.8,1.0704),(9,4.1,25,191,74,38.1,100.9,82.5,99.9,62.9,38.3,23.8,35.9,31.1,18.2,1.09),(10,11.7,23,198.25,73.5,42.1,99.6,88.6,104.1,63.1,41.7,25,35.6,30,19.2,1.0722),(11,7.1,26,186.25,74.5,38.5,101.5,83.6,98.2,59.7,39.7,25.2,32.8,29.4,18.5,1.083),(12,7.8,27,216,76,39.4,103.6,90.9,107.7,66.2,39.2,25.9,37.2,30.2,19,1.0812),(13,20.8,32,180.5,69.5,38.4,102,91.6,103.9,63.4,38.3,21.5,32.5,28.6,17.7,1.0513),(14,21.2,30,205.25,71.25,39.4,104.1,101.8,108.6,66,41.5,23.7,36.9,31.6,18.8,1.0505),(15,22.1,35,187.75,69.5,40.5,101.3,96.4,100.1,69,39,23.1,36.1,30.5,18.2,1.0484),(16,20.9,35,162.75,66,36.4,99.1,92.8,99.2,63.1,38.7,21.7,31.1,26.4,16.9,1.0512),(17,29,34,195.75,71,38.9,101.9,96.4,105.2,64.8,40.8,23.1,36.2,30.8,17.3,1.0333),(18,22.9,32,209.25,71,42.1,107.6,97.5,107,66.9,40,24.4,38.2,31.6,19.3,1.0468),(19,16,28,183.75,67.75,38,106.8,89.6,102.4,64.2,38.7,22.9,37.2,30.5,18.5,1.0622),(20,16.5,33,211.75,73.5,40,106.2,100.5,109,65.8,40.6,24,37.1,30.1,18.2,1.061),(21,19.1,28,179,68,39.1,103.3,95.9,104.9,63.5,38,22.1,32.5,30.3,18.4,1.0551),(22,15.2,28,200.5,69.75,41.3,111.4,98.8,104.8,63.4,40.6,24.6,33,32.8,19.9,1.064),(23,15.6,31,140.25,68.25,33.9,86,76.4,94.6,57.4,35.3,22.2,27.9,25.9,16.7,1.0631),(24,17.7,32,148.75,70,35.5,86.7,80,93.4,54.9,36.2,22.1,29.8,26.7,17.1,1.0584),(25,14,28,151.25,67.75,34.5,90.2,76.3,95.8,58.4,35.5,22.9,31.1,28,17.6,1.0668),(26,3.7,27,159.25,71.5,35.7,89.6,79.7,96.5,55,36.7,22.5,29.9,28.2,17.7,1.0911),(27,7.9,34,131.5,67.5,36.2,88.6,74.6,85.3,51.7,34.7,21.4,28.7,27,16.5,1.0811),(28,22.9,31,148,67.5,38.8,97.4,88.7,94.7,57.5,36,21,29.2,26.6,17,1.0468),(29,3.7,27,133.25,64.75,36.4,93.5,73.9,88.5,50.1,34.5,21.3,30.5,27.9,17.2,1.091),(30,8.8,29,160.75,69,36.7,97.4,83.5,98.7,58.9,35.3,22.6,30.1,26.7,17.6,1.079),(31,11.9,32,182,73.75,38.7,100.5,88.7,99.8,57.5,38.7,33.9,32.5,27.7,18.4,1.0716),(32,5.7,29,160.25,71.25,37.3,93.5,84.5,100.6,58.5,38.8,21.5,30.1,26.4,17.9,1.0862),(33,11.8,27,168,71.25,38.1,93,79.1,94.5,57.3,36.2,24.5,29,30,18.8,1.0719),(34,21.3,41,218.5,71,39.8,111.7,100.5,108.3,67.1,44.2,25.2,37.5,31.5,18.7,1.0502),(35,32.3,41,247.25,73.5,42.1,117,115.6,116.1,71.2,43.3,26.3,37.3,31.7,19.7,1.0263),(36,40.1,49,191.75,65,38.4,118.5,113.1,113.8,61.9,38.3,21.9,32,29.8,17,1.0101),(37,24.2,40,202.25,70,38.5,106.5,100.9,106.2,63.5,39.9,22.6,35.1,30.6,19,1.0438),(38,28.4,50,196.75,68.25,42.1,105.6,98.8,104.8,66,41.5,24.7,33.2,30.5,19.4,1.0346),(39,35.2,46,363.15,72.25,51.2,136.2,148.1,147.7,87.3,49.1,29.6,45,29,21.4,1.0202),(40,32.6,50,203,67,40.2,114.8,108.1,102.5,61.3,41.1,24.7,34.1,31,18.3,1.0258),(41,34.5,45,262.75,68.75,43.2,128.3,126.2,125.6,72.5,39.6,26.6,36.4,32.7,21.4,1.0217),(42,32.9,44,205,29.5,36.6,106,104.3,115.5,70.6,42.5,23.7,33.6,28.7,17.4,1.025),(43,31.6,48,217,70,37.3,113.3,111.2,114.1,67.7,40.9,25,36.7,29.8,18.4,1.0279),(44,32,41,212,71.5,41.5,106.6,104.3,106,65,40.2,23,35.8,31.5,18.8,1.0269),(45,7.7,39,125.25,68,31.5,85.1,76,88.2,50,34.7,21,26.1,23.1,16.1,1.0814),(46,13.9,43,164.25,73.25,35.7,96.6,81.5,97.2,58.4,38.2,23.4,29.7,27.4,18.3,1.067),(47,10.8,40,133.5,67.5,33.6,88.2,73.7,88.5,53.3,34.5,22.5,27.9,26.2,17.3,1.0742),(48,5.6,39,148.5,71.25,34.6,89.8,79.5,92.7,52.7,37.5,21.9,28.8,26.8,17.9,1.0665),(49,13.6,45,135.75,68.5,32.8,92.3,83.4,90.4,52,35.8,20.6,28.8,25.5,16.3,1.0678),(50,4,47,127.5,66.75,34,83.4,70.4,87.2,50.6,34.4,21.9,26.8,25.8,16.8,1.0903),(51,10.2,47,158.25,72.25,34.9,90.2,86.7,98.3,52.6,37.2,22.4,26,25.8,17.3,1.0756),(52,6.6,40,139.25,69,34.3,89.2,77.9,91,51.4,34.9,21,26.7,26.1,17.2,1.084),(53,8,51,137.25,67.75,36.5,89.7,82,89.1,49.3,33.7,21.4,29.6,26,16.9,1.0807),(54,6.3,49,152.75,73.5,35.1,93.3,79.6,91.6,52.6,37.6,22.6,38.5,27.4,18.5,1.0848),(55,3.9,42,136.25,67.5,37.8,87.6,77.6,88.6,51.9,34.9,22.5,27.7,27.5,18.5,1.0906),(56,22.6,54,198,72,39.9,107.6,100,99.6,57.2,38,22,35.9,30.2,18.9,1.0473),(57,20.4,58,181.5,68,39.1,100,99.8,102.5,62.1,39.6,22.5,33.1,28.3,18.5,1.0524),(58,28,62,201.25,69.5,40.5,111.5,104.2,105.8,61.8,39.8,22.7,37.7,30.9,19.2,1.0356),(59,31.5,54,202.5,70.75,40.5,115.4,105.3,97,59.1,38,22.5,31.6,28.8,18.2,1.028),(60,24.6,61,179.75,65.75,38.4,104.8,98.3,99.6,60.6,37.7,22.9,34.5,29.6,18.5,1.043),(61,26.1,62,216,73.25,41.4,112.3,104.8,103.1,61.6,40.9,23.1,36.2,31.8,20.2,1.0396),(62,29.8,56,178.75,68.5,35.6,102.9,94.7,100.8,60.9,38,22.1,32.5,29.8,18.3,1.0317),(63,30.7,54,193.25,70.25,38,107.6,102.4,99.4,61,39.4,23.6,32.7,29.9,19.1,1.0298),(64,25.8,61,178,67,37.4,105.3,99.7,99.7,60.8,40.1,22.7,33.6,29,18.8,1.0403),(65,32.3,57,205.5,70,40.1,105.3,105.5,108.3,65,41.2,24.7,35.3,31.1,18.4,1.0264),(66,30,55,183.5,67.5,40.9,103,100.3,104.2,64.8,40.2,22.7,34.8,30.1,18.7,1.0313),(67,21.5,54,151.5,70.75,35.6,90,83.9,93.9,55,36.1,21.7,29.6,27.4,17.4,1.0499),(68,13.8,55,154.75,71.5,36.9,95.4,86.6,91.8,54.3,35.4,21.5,32.8,27.4,18.7,1.0673),(69,6.3,54,155.25,69.25,37.5,89.3,78.4,96.1,56,37.4,22.4,32.6,28.1,18.1,1.0847),(70,12.9,55,156.75,71.5,36.3,94.4,84.6,94.3,51.2,37.4,21.6,27.3,27.1,17.3,1.0693),(71,24.3,62,167.5,71.5,35.5,97.6,91.5,98.5,56.6,38.6,22.4,31.5,27.3,18.6,1.0439),(72,8.8,55,146.75,68.75,38.7,88.5,82.8,95.5,58.9,37.6,21.6,30.3,27.3,18.3,1.0788),(73,8.5,56,160.75,73.75,36.4,93.6,82.9,96.3,52.9,37.5,23.1,29.7,27.3,18.2,1.0796),(74,13.5,55,125,64,33.2,87.7,76,88.6,50.9,35.4,19.1,29.3,25.7,16.9,1.068),(75,11.8,61,143,65.75,36.5,93.4,83.3,93,55.5,35.2,20.9,29.4,27,16.8,1.072),(76,18.5,61,148.25,67.5,36,91.6,81.8,94.8,54.5,37,21.4,29.3,27,18.3,1.0666),(77,8.8,57,162.5,69.5,38.7,91.6,78.8,94.3,56.7,39.7,24.2,30.2,29.2,18.1,1.079),(78,22.2,69,177.75,68.5,38.7,102,95,98.3,55,38.3,21.8,30.8,25.7,18.8,1.0483),(79,21.5,81,161.25,70.25,37.8,96.4,95.4,99.3,53.5,37.5,21.5,31.4,26.8,18.3,1.0498),(80,18.8,66,171.25,69.25,37.4,102.7,98.6,100.2,56.5,39.3,22.7,30.3,28.7,19,1.056),(81,31.4,67,163.75,67.75,38.4,97.7,95.8,97.1,54.8,38.2,23.7,29.4,27.2,19,1.0283),(82,26.8,64,150.25,67.25,38.1,97.1,89,96.9,54.8,38,22,29.9,25.2,17.7,1.0382),(83,18.4,64,190.25,72.75,39.3,103.1,97.8,99.6,58.9,39,23,34.3,29.6,19,1.0568),(84,27,70,170.75,70,38.7,101.8,94.9,95,56,36.5,24.1,31.2,27.3,19.2,1.0377),(85,27,72,168,69.25,38.5,101.4,99.8,96.2,56.3,36.6,22,29.7,26.3,18,1.0378),(86,26.6,67,167,67.5,36.5,98.9,89.7,96.2,54.7,37.8,33.7,32.4,27.7,18.2,1.0386),(87,14.9,72,157.75,67.25,37.7,97.5,88.1,96.9,57.2,37.7,21.8,32.6,28,18.8,1.0648),(88,23.1,64,160,65.75,36.5,104.3,90.9,93.8,57.8,39.5,23.3,29.2,28.4,18.1,1.0462),(89,8.3,46,176.75,72.5,38,97.3,86,99.3,61,38.4,23.8,30.2,29.3,18.8,1.08),(90,14.1,48,176,73,36.7,96.7,86.5,98.3,60.4,39.9,24.4,28.8,29.6,18.7,1.0666),(91,20.5,46,177,70,37.2,99.7,95.6,102.2,58.3,38.2,22.5,29.1,27.7,17.7,1.052),(92,18.2,44,179.75,69.5,39.2,101.9,93.2,100.6,58.9,39.7,23.1,31.4,28.4,18.8,1.0573),(93,8.5,47,165.25,70.5,37.5,97.2,83.1,95.4,56.9,38.3,22.1,30.1,28.2,18.4,1.0795),(94,24.9,46,192.5,71.75,38,106.6,97.5,100.6,58.9,40.5,24.5,33.3,29.6,19.1,1.0424),(95,9,47,184.25,74.5,37.3,99.6,88.8,101.4,57.4,39.6,24.6,30.3,27.9,17.8,1.0785),(96,17.4,53,224.5,77.75,41.1,113.2,99.2,107.5,61.7,42.3,23.2,32.9,30.8,20.4,1.0991),(97,9.6,38,188.75,73.25,37.5,99.1,91.6,102.4,60.6,39.4,22.9,31.6,30.1,18.5,1.077),(98,11.3,50,162.5,66.5,38.7,99.4,86.7,96.2,62.1,39.3,23.3,30.6,27.8,18.2,1.073),(99,17.8,46,156.5,68.25,35.9,95.1,88.2,92.8,54.7,37.3,21.9,31.6,27.5,18.2,1.0582),(100,22.2,47,197,72,40,107.5,94,103.7,62.7,39,22.3,35.3,30.9,18.3,1.0484),(101,21.2,49,198.5,73.5,40.1,106.5,95,101.7,59,39.4,22.3,32.2,31,18.6,1.0506),(102,20.4,48,173.75,72,37,99.1,92,98.3,59.3,38.4,22.4,27.9,26.2,17,1.0524),(103,20.1,41,172.75,71.25,36.3,96.7,89.2,98.3,60,38.4,23.2,31,29.2,18.4,1.053),(104,22.3,49,196.75,73.75,40.7,103.5,95.5,101.6,59.1,39.8,25.4,31,30.3,19.7,1.048),(105,25.4,43,177,69.25,39.6,104,98.6,99.5,59.5,36.1,22,30.1,27.2,17.7,1.0412),(106,18,43,165.5,68.5,31.1,93.1,87.3,96.6,54.7,39,24.8,31,29.4,18.8,1.0578),(107,19.3,43,200.25,73.5,38.6,105.2,102.8,103.6,61.2,39.3,23.5,30.5,28.5,18.1,1.0547),(108,18.3,52,203.25,74.25,42,110,101.6,100.7,55.8,38.7,23.4,35.1,29.6,19.1,1.0569),(109,17.3,43,194,75.5,38.5,110.1,88.7,102.1,57.5,40,24.8,35.1,30.7,19.2,1.0593),(110,21.4,40,168.5,69.25,34.2,97.8,92.3,100.6,57.5,36.8,22.8,32.1,26,17.3,1.05),(111,19.7,43,170.75,68.5,37.2,96.3,90.6,99.3,61.9,38,22.3,33.3,28.2,18.1,1.0538),(112,28,43,183.25,70,37.1,108,105,103,63.7,40,23.6,33.5,27.8,17.4,1.0355),(113,22.1,47,178.25,70,40.2,99.7,95,98.6,62.3,38.1,23.9,35.3,31.1,19.8,1.0486),(114,21.3,42,163,70.25,35.3,93.5,89.6,99.8,61.5,37.8,21.9,30.7,27.6,17.4,1.0503),(115,26.7,48,175.25,71.75,38,100.7,92.4,97.5,59.3,38.1,21.8,31.8,27.3,17.5,1.0384),(116,16.7,40,158,69.25,36.3,97,86.6,92.6,55.9,36.3,22.1,29.8,26.3,17.3,1.0607),(117,20.1,48,177.25,72.75,36.8,96,90,99.7,58.8,38.4,22.8,29.9,28,18.1,1.0529),(118,13.9,51,179,72,41,99.2,90,96.4,56.8,38.8,23.3,33.4,29.8,19.5,1.0671),(119,25.8,40,191,74,38.3,95.4,92.4,104.3,64.6,41.1,24.8,33.6,29.5,18.5,1.0404),(120,18.1,44,187.5,72.25,38,101.8,87.5,101,58.5,39.2,24.5,32.1,28.6,18,1.0575),(121,27.9,52,206.5,74.5,40.8,104.3,99.2,104.1,58.5,39.3,24.6,33.9,31.2,19.5,1.0358),(122,25.3,44,185.25,71.5,39.5,99.2,98.1,101.4,57.1,40.5,23.2,33,29.6,18.4,1.0414),(123,14.7,40,160.25,68.75,36.9,99.3,83.3,97.5,60.5,38.7,22.6,34.4,28,17.6,1.0652),(124,16,47,151.5,66.75,36.9,94,86.1,95.2,58.1,36.5,22.1,30.6,27.5,17.6,1.0623),(125,13.8,50,161,66.5,37.7,98.9,84.1,94,58.5,36.6,23.5,34.4,29.2,18,1.0674),(126,17.5,46,167,67,36.6,101,89.9,100,60.7,36,21.9,35.6,30.2,17.6,1.0587),(127,27.2,42,177.5,68.75,38.9,98.7,92.1,98.5,60.7,36.8,22.2,33.8,30.3,17.2,1.0373),(128,17.4,43,152.25,67.75,37.5,95.9,78,93.2,53.5,35.8,20.8,33.9,28.2,17.4,1.059),(129,20.8,40,192.25,73.25,39.8,103.9,93.5,99.5,61.7,39,21.8,33.3,29.6,18.1,1.0515),(130,14.9,42,165.25,69.75,38.3,96.2,87,97.8,57.4,36.9,22.2,31.6,27.8,17.7,1.0648),(131,18.1,49,171.75,71.5,35.5,97.8,90.1,95.8,57,38.7,23.2,27.5,26.5,17.6,1.0575),(132,22.7,40,171.25,70.5,36.3,94.6,90.3,99.1,60.3,38.5,23,31.2,28.4,17.1,1.0472),(133,23.6,47,197,73.25,37.8,103.6,99.8,103.2,61.2,38.1,22.6,33.5,28.6,17.9,1.0452),(134,26.1,50,157,66.75,37.8,100.4,89.4,92.3,56.1,35.6,20.5,33.6,29.3,17.3,1.0398),(135,24.4,41,168.25,69.5,36.5,98.4,87.2,98.4,56,36.9,23,34,29.8,18.1,1.0435),(136,27.1,44,186,69.75,37.8,104.6,101.1,102.1,58.9,37.9,22.7,30.9,28.8,17.6,1.0374),(137,21.8,39,166.75,70.75,37,92.9,86.1,95.6,58.8,36.1,22.4,32.7,28.3,17.1,1.0491),(138,29.4,43,187.75,74,37.7,97.8,98.6,100.6,63.6,39.2,23.8,34.3,28.4,17.7,1.0325),(139,22.4,40,168.25,71.25,34.3,98.3,88.5,98.3,58.1,38.4,22.5,31.7,27.4,17.6,1.0481),(140,20.4,49,212.75,75,40.8,104.7,106.6,107.7,66.5,42.5,24.5,35.5,29.8,18.7,1.0522),(141,24.9,40,176.75,71,37.4,98.6,93.1,101.6,59.1,39.6,21.6,30.8,27.9,16.6,1.0422),(142,18.3,40,173.25,69.5,36.5,99.5,93,99.3,60.4,38.2,22,32,28.5,17.8,1.0571),(143,23.3,52,167,67.75,37.5,102.7,91,98.9,57.1,36.7,22.3,31.6,27.5,17.9,1.0459),(144,9.4,23,159.75,72.25,35.5,92.1,77.1,93.9,56.1,36.1,22.7,30.5,27.2,18.2,1.0775),(145,10.3,23,188.15,77.5,38,96.6,85.3,102.5,59.1,37.6,23.2,31.8,29.7,18.3,1.0754),(146,14.2,24,156,70.75,35.7,92.7,81.9,95.3,56.4,36.5,22,33.5,28.3,17.3,1.0664),(147,19.2,24,208.5,72.75,39.2,102,99.1,110.1,71.2,43.5,25.2,36.1,30.3,18.7,1.055),(148,29.6,25,206.5,69.75,40.9,110.9,100.5,106.2,68.4,40.8,24.6,33.3,29.7,18.4,1.0322),(149,5.3,25,143.75,72.5,35.2,92.3,76.5,92.1,51.9,35.7,22,25.8,25.2,16.9,1.0873),(150,25.2,26,223,70.25,40.6,114.1,106.8,113.9,67.6,42.7,24.7,36,30.4,18.4,1.0416),(151,9.4,26,152.25,69,35.4,92.9,77.6,93.5,56.9,35.9,20.4,31.6,29,17.8,1.0776),(152,19.6,26,241.75,74.5,41.8,108.3,102.9,114.4,72.9,43.5,25.1,38.5,33.8,19.6,1.0542),(153,10.1,27,146,72.25,34.1,88.5,72.8,91.1,53.6,36.8,23.8,27.8,26.3,17.4,1.0758),(154,16.5,27,156.75,67.25,37.9,94,88.2,95.2,56.8,37.4,22.8,30.6,28.3,17.9,1.061),(155,21,27,200.25,73.5,38.2,101.1,100.1,105,62.1,40,24.9,33.7,29.2,19.4,1.051),(156,17.3,28,171.5,75.25,35.6,92.1,83.5,98.3,57.3,37.8,21.7,32.2,27.7,17.7,1.0594),(157,31.2,28,205.75,69,38.5,105.6,105,106.4,68.6,40,25.2,35.2,30.7,19.1,1.0287),(158,10,28,182.5,72.25,37,98.5,90.8,102.5,60.8,38.5,25,31.6,28,18.6,1.0761),(159,12.5,30,136.5,68.75,35.9,88.7,76.6,89.8,50.1,34.8,21.8,27,34.9,16.9,1.0704),(160,22.5,31,177.25,71.5,36.2,101.1,92.4,99.3,59.4,39,24.6,30.1,28.2,18.2,1.0477),(161,9.4,31,151.25,72.25,35,94,81.2,91.5,52.5,36.6,21,27,26.3,16.5,1.0775),(162,14.6,33,196,73,38.5,103.8,95.6,105.1,61.4,40.6,25,31.3,29.2,19.1,1.0653),(163,13,33,184.25,68.75,40.7,98.9,92.1,103.5,64,37.3,23.5,33.5,30.6,19.7,1.069),(164,15.1,34,140,70.5,36,89.2,83.4,89.6,52.4,35.6,20.4,28.3,26.2,16.5,1.0644),(165,27.3,34,218.75,72,39.5,111.4,106,108.8,63.8,42,23.4,34,31.2,18.5,1.037),(166,19.2,35,217,73.75,40.5,107.5,95.1,104.5,64.8,41.3,25.6,36.4,33.7,19.4,1.0549),(167,21.8,35,166.25,68,38.5,99.1,90.4,95.6,55.5,34.2,21.9,30.2,28.7,17.7,1.0492),(168,20.3,35,224.75,72.25,43.9,108.2,100.4,106.8,63.3,41.7,24.6,37.2,33.1,19.8,1.0525),(169,34.3,35,228.25,69.5,40.4,114.9,115.9,111.9,74.4,40.6,24,36.1,31.8,18.8,1.018),(170,16.5,35,172.75,69.5,37.6,99.1,90.8,98.1,60.1,39.1,23.4,32.5,29.8,17.4,1.061),(171,3,35,152.25,67.75,37,92.2,81.9,92.8,54.7,36.2,22.1,30.4,27.4,17.7,1.0926),(172,0.7,35,125.75,65.5,34,90.8,75,89.2,50,34.8,22,24.8,25.9,16.9,1.0983),(173,20.5,35,177.25,71,38.4,100.5,90.3,98.7,57.8,37.3,22.4,31,28.7,17.7,1.0521),(174,16.9,36,176.25,71.5,38.7,98.2,90.3,99.9,59.2,37.7,21.5,32.4,28.4,17.8,1.0603),(175,25.3,36,226.75,71.75,41.5,115.3,108.8,114.4,69.2,42.4,24,35.4,21,20.1,1.0414),(176,9.9,37,145.25,69.25,36,96.8,79.4,89.2,50.3,34.8,22.2,31,26.9,16.9,1.0763),(177,13.1,37,151,67,35.3,92.6,83.2,96.4,60,38.1,22,31.5,26.6,16.7,1.0689),(178,29.9,37,241.25,71.5,42.1,119.2,110.3,113.9,69.8,42.6,24.8,34.4,29.5,18.4,1.0316),(179,22.5,38,187.25,69.25,38,102.7,92.7,101.9,64.7,39.5,24.7,34.8,30.3,18.1,1.0477),(180,16.9,39,234.75,74.5,42.8,109.5,104.5,109.9,69.5,43.1,25.8,39.1,32.5,19.9,1.0603),(181,26.6,39,219.25,74.25,40,108.5,104.6,109.8,68.1,42.8,24.1,35.6,29,19,1.0387),(182,0,40,118.5,68,33.8,79.3,69.4,85,47.2,33.5,20.2,27.7,24.6,16.5,1.1089),(183,11.5,40,145.75,67.25,35.5,95.5,83.6,91.6,54.1,36.2,21.8,31.4,28.3,17.2,1.0725),(184,12.1,40,159.25,69.75,35.3,92.3,86.8,96.1,58,39.4,22.7,30,26.4,17.4,1.0713),(185,17.5,40,170.5,74.25,37.7,98.9,90.4,95.5,55.4,38.9,22.4,30.5,28.9,17.7,1.0587),(186,8.6,40,167.5,71.5,39.4,89.5,83.7,98.1,57.3,39.7,22.6,32.9,29.3,18.2,1.0794),(187,23.6,41,232.75,74.25,41.9,117.5,109.3,108.8,67.7,41.3,24.7,37.2,31.8,20,1.0453),(188,20.4,41,210.5,72,38.5,107.4,98.9,104.1,63.5,39.8,23.5,36.4,30.4,19.1,1.0524),(189,20.5,41,202.25,72.5,40.8,109.2,98,101.8,62.8,41.3,24.8,36.6,32.4,18.8,1.052),(190,24.4,41,185,68.25,38,103.4,101.2,103.1,61.5,40.4,22.9,33.4,29.2,18.5,1.0434),(191,11.4,41,153,69.25,36.4,91.4,80.6,92.3,54.3,36.3,21.8,29.6,27.3,17.9,1.0728),(192,38.1,42,244.25,76,41.8,115.2,113.7,112.4,68.5,45,25.5,37.1,31.2,19.9,1.014),(193,15.9,42,193.5,70.5,40.7,104.9,94.1,102.7,60.6,38.6,24.7,34,30.1,18.7,1.0624),(194,24.7,42,224.75,74.75,38.5,106.7,105.7,111.8,65.3,43.3,26,33.7,29.9,18.5,1.0429),(195,22.8,42,162.75,72.75,35.4,92.2,85.6,96.5,60.2,38.9,22.4,31.7,27.1,17.1,1.047),(196,25.5,42,180,68.25,38.5,101.6,96.6,100.6,61.1,38.4,24.1,32.9,29.8,18.8,1.0411),(197,22,42,156.25,69,35.5,97.8,86,96.2,57.7,38.6,24,31.2,27.3,17.4,1.0488),(198,17.7,42,168,71.5,36.5,92,89.7,101,62.3,38,22.3,30.8,27.8,16.9,1.0583),(199,6.6,42,167.25,72.75,37.6,94,78,99,57.5,40,22.5,30.6,30,18.5,1.0841),(200,23.6,43,170.75,67.5,37.4,103.7,89.7,94.2,58.5,39,24.1,33.8,28.8,18.8,1.0462),(201,12.2,43,178.25,70.25,37.8,102.7,89.2,99.2,60.2,39.2,23.8,31.7,28.4,18.6,1.0709),(202,22.1,43,150,69.25,35.2,91.1,85.7,96.9,55.5,35.7,22,29.4,26.6,17.4,1.0484),(203,28.7,43,200.5,71.5,37.9,107.2,103.1,105.5,68.8,38.3,23.7,32.1,28.9,18.7,1.034),(204,6,44,184,74,37.9,100.8,89.1,102.6,60.6,39,24,32.9,29.2,18.4,1.0854),(205,34.8,44,223,69.75,40.9,121.6,113.9,107.1,63.5,40.3,21.8,34.8,30.7,17.4,1.0209),(206,16.6,44,208.75,73,41.9,105.6,96.3,102,63.3,39.8,24.1,37.3,23.1,19.4,1.061),(207,32.9,44,166,65.5,39.1,100.6,93.9,100.1,58.9,37.6,21.4,33.1,29.5,17.3,1.025),(208,32.8,47,195,72.5,40.2,102.7,101.3,101.7,60.7,39.4,23.3,36.7,31.6,18.4,1.0254),(209,9.6,47,160.5,70.25,36,99.8,83.9,91.8,53,36.2,22.5,31.4,27.5,17.7,1.0771),(210,10.8,47,159.75,70.75,34.5,92.9,84.4,94,56,38.2,22.6,29,26.2,17.6,1.0742),(211,7.1,49,140.5,68,35.8,91.2,79.4,89,51.1,35,21.7,30.9,28.8,17.4,1.0829),(212,27.2,49,216.25,74.5,40.2,115.6,104,109,63.7,40.3,23.2,36.8,31,18.9,1.0373),(213,19.5,49,168.25,71.75,38.3,98.3,89.7,99.1,56.3,38.8,23,29.5,27.9,18.6,1.0543),(214,18.7,50,194.75,70.75,39,103.7,97.6,104.2,60,40.9,25.5,32.7,30,19,1.0561),(215,19.5,50,172.75,73,37.4,98.7,87.6,96.1,57.1,38.1,21.8,28.6,26.7,18,1.0543),(216,47.5,51,219,64,41.2,119.8,122.1,112.8,62.5,36.9,23.6,34.7,29.1,18.4,0.995),(217,13.6,51,149.25,69.75,34.8,92.8,81.1,96.3,53.8,36.5,21.5,31.3,26.3,17.8,1.0678),(218,7.5,51,154.5,70,36.9,93.3,81.5,94.4,54.7,39,22.6,27.5,25.9,18.6,1.0819),(219,24.5,52,199.25,71.75,39.4,106.8,100,105,63.9,39.2,22.9,35.7,30.4,19.2,1.0433),(220,15,53,154.5,69.25,37.6,93.9,88.7,94.5,53.7,36.2,22,28.5,25.7,17.1,1.0646),(221,12.4,54,153.25,70.5,38.5,99,91.8,96.2,57.7,38.1,23.9,31.4,29.9,18.9,1.0706),(222,26,54,230,72.25,42.5,119.9,110.4,105.5,64.2,42.7,27,38.4,32,19.6,1.0399),(223,11.5,54,161.75,67.5,37.4,94.2,87.6,95.6,59.7,40.2,23.4,27.9,27,17.8,1.0726),(224,5.2,55,142.25,67.25,35.2,92.7,82.8,91.9,54.4,35.2,22.5,29.4,26.8,17,1.0874),(225,10.9,55,179.75,68.75,41.1,106.9,95.3,98.2,57.4,37.1,21.8,34.1,31.1,19.2,1.074),(226,12.5,55,126.5,66.75,33.4,88.8,78.2,87.5,50.8,33,19.7,25.3,22,15.8,1.0703),(227,14.8,55,169.5,68.25,37.2,101.7,91.1,97.1,56.6,38.5,22.6,33.4,29.3,18.8,1.065),(228,25.2,55,198.5,74.25,38.3,105.3,96.7,106.6,64,42.6,23.4,33.2,30,18.4,1.0418),(229,14.9,56,174.5,69.5,38.1,104,89.4,98.4,58.4,37.4,22.5,34.6,30.1,18.8,1.0647),(230,17,56,167.75,68.5,37.4,98.6,93,97,55.4,38.8,23.2,32.4,29.7,19,1.0601),(231,10.6,57,147.75,65.75,35.2,99.6,86.4,90.1,53,35,21.3,31.7,27.3,16.9,1.0745),(232,16.1,57,182.25,71.75,39.4,103.4,96.7,100.7,59.3,38.6,22.8,31.8,29.1,19,1.062),(233,15.4,58,175.5,71.5,38,100.2,88.1,97.8,57.1,38.9,23.6,30.9,29.6,18,1.0636),(234,26.7,58,161.75,67.25,35.1,94.9,94.9,100.2,56.8,35.9,21,27.8,26.1,17.6,1.0384),(235,25.8,60,157.75,67.5,40.4,97.2,93.3,94,54.3,35.7,21,31.3,28.7,18.3,1.0403),(236,18.6,62,168.75,67.5,38.3,104.7,95.6,93.7,54.4,37.1,22.7,30.3,26.3,18.3,1.0563),(237,24.8,62,191.5,72.25,40.6,104,98.2,101.1,59.3,40.3,23,32.6,28.5,19,1.0424),(238,27.3,63,219.15,69.5,40.2,117.6,113.8,111.8,63.4,41.1,22.3,35.1,29.6,18.5,1.0372),(239,12.4,64,155.25,69.5,37.9,95.8,82.8,94.5,61.2,39.1,22.3,29.8,28.9,18.3,1.0705),(240,29.9,65,189.75,65.75,40.8,106.4,100.5,100.5,59.2,38.1,24,35.9,30.5,19.1,1.0316),(241,17,65,127.5,65.75,34.7,93,79.7,87.6,50.7,33.4,20.1,28.5,24.8,16.5,1.0599),(242,35,65,224.5,68.25,38.8,119.6,118,114.3,61.3,42.1,23.4,34.9,30.1,19.4,1.0207),(243,30.4,66,234.25,72,41.4,119.7,109,109.1,63.7,42.4,24.6,35.6,30.7,19.5,1.0304),(244,32.6,67,227.75,72.75,41.3,115.8,113.4,109.8,65.6,46,25.4,35.3,29.8,19.5,1.0256),(245,29,67,199.5,68.5,40.7,118.3,106.1,101.6,58.2,38.8,24.1,32.1,29.3,18.5,1.0334),(246,15.2,68,155.5,69.25,36.3,97.4,84.3,94.4,54.3,37.5,22.6,29.2,27.3,18.5,1.0641),(247,30.2,69,215.5,70.5,40.8,113.7,107.6,110,63.3,44,22.6,37.5,32.6,18.8,1.0308),(248,11,70,134.25,67,34.9,89.2,83.6,88.8,49.6,34.8,21.5,25.6,25.7,18.5,1.0736),(249,33.6,72,201,69.75,40.9,108.5,105,104.5,59.6,40.8,23.2,35.2,28.6,20.1,1.0236),(250,29.3,72,186.75,66,38.9,111.1,111.5,101.7,60.3,37.3,21.5,31.3,27.2,18,1.0328),(251,26,72,190.75,70.5,38.9,108.3,101.3,97.8,56,41.6,22.7,30.5,29.4,19.8,1.0399),(252,31.9,74,207.5,70,40.8,112.4,108.5,107.1,59.3,42.2,24.6,33.7,30,20.9,1.0271);
/*!40000 ALTER TABLE `yz` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-05-18  1:42:50
