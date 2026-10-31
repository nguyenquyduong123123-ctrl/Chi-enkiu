-- MySQL dump 10.13  Distrib 8.0.46, for macos15 (x86_64)
--
-- Host: localhost    Database: btl
-- ------------------------------------------------------
-- Server version	9.7.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '1975bde0-b6f1-11f1-b8f7-d409ac4452cb:1-181';

--
-- Table structure for table `Quanli`
--

DROP TABLE IF EXISTS `Quanli`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Quanli` (
  `id_lop` varchar(50) NOT NULL,
  `id_giao_vien` varchar(50) NOT NULL,
  PRIMARY KEY (`id_lop`,`id_giao_vien`),
  KEY `id_giao_vien` (`id_giao_vien`),
  CONSTRAINT `quanli_ibfk_1` FOREIGN KEY (`id_lop`) REFERENCES `Lop` (`id_lop`),
  CONSTRAINT `quanli_ibfk_2` FOREIGN KEY (`id_giao_vien`) REFERENCES `Giao_vien` (`id_giao_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Quanli`
--

LOCK TABLES `Quanli` WRITE;
/*!40000 ALTER TABLE `Quanli` DISABLE KEYS */;
INSERT INTO `Quanli` VALUES ('L01','GV01'),('L06','GV01'),('L01','GV02'),('L02','GV02'),('L03','GV03'),('L04','GV04'),('L05','GV05'),('L06','GV06'),('L07','GV07'),('L08','GV08'),('L09','GV09'),('L10','GV10'),('L11','GV11'),('L12','GV12'),('L13','GV13'),('L14','GV14'),('L15','GV15'),('L16','GV16'),('L17','GV17'),('L18','GV18'),('L19','GV19'),('L20','GV20');
/*!40000 ALTER TABLE `Quanli` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-31 10:08:56
