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
-- Table structure for table `Lop`
--

DROP TABLE IF EXISTS `Lop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Lop` (
  `id_lop` varchar(50) NOT NULL,
  `ma_khoa_hoc` varchar(50) NOT NULL,
  `ma_nhan_vien` varchar(50) NOT NULL,
  `nien_khoa` varchar(50) NOT NULL,
  `ten_lop` varchar(50) NOT NULL,
  `si_so` int DEFAULT NULL,
  PRIMARY KEY (`id_lop`),
  KEY `ma_khoa_hoc` (`ma_khoa_hoc`),
  KEY `ma_nhan_vien` (`ma_nhan_vien`),
  CONSTRAINT `lop_ibfk_1` FOREIGN KEY (`ma_khoa_hoc`) REFERENCES `Khoa_hoc` (`ma_khoa_hoc`),
  CONSTRAINT `lop_ibfk_2` FOREIGN KEY (`ma_nhan_vien`) REFERENCES `Nhan_vien` (`ma_nhan_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Lop`
--

LOCK TABLES `Lop` WRITE;
/*!40000 ALTER TABLE `Lop` DISABLE KEYS */;
INSERT INTO `Lop` VALUES ('L01','KH01','NV03','2026-2027','N5-A1',20),('L02','KH02','NV03','2026-2027','N5-A2',18),('L03','KH03','NV03','2026-2027','N4-B1',15),('L04','KH04','NV01','2026-2027','N3-C1',12),('L05','KH06','NV01','2026-2027','N2-D1',10),('L06','KH08','NV01','2026-2027','N1-E1',8),('L07','KH07','NV07','2025-2026','GT-B',12),('L08','KH08','NV08','2025-2026','LT-N5',14),('L09','KH09','NV09','2025-2026','LT-N4',10),('L10','KH10','NV10','2025-2026','TE-A',11),('L11','KH11','NV11','2025-2026','DN-A',25),('L12','KH12','NV12','2025-2026','DH-A',20),('L13','KH13','NV13','2025-2026','NP-N5',18),('L14','KH14','NV14','2025-2026','NP-N4',16),('L15','KH15','NV15','2025-2026','DH-N3',15),('L16','KH16','NV16','2025-2026','NG-N3',12),('L17','KH17','NV17','2025-2026','KJ-N2',20),('L18','KH18','NV18','2025-2026','KJ-N1',22),('L19','KH19','NV19','2025-2026','IT-JP',13),('L20','KH20','NV20','2025-2026','GT-C',24);
/*!40000 ALTER TABLE `Lop` ENABLE KEYS */;
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
