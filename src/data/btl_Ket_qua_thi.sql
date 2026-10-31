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
-- Table structure for table `Ket_qua_thi`
--

DROP TABLE IF EXISTS `Ket_qua_thi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Ket_qua_thi` (
  `ma_bai_lam` varchar(50) NOT NULL,
  `ma_sinh_vien` varchar(50) NOT NULL,
  `trinh_do` varchar(50) NOT NULL,
  `ma_de_thi` varchar(50) NOT NULL,
  `diem_doc_hieu` int NOT NULL,
  `diem_tu_vung` int NOT NULL,
  `diem_nghe` int NOT NULL,
  `tong_diem` int NOT NULL,
  `tinh_trang` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`ma_bai_lam`),
  KEY `ma_sinh_vien` (`ma_sinh_vien`),
  KEY `ma_de_thi` (`ma_de_thi`),
  CONSTRAINT `ket_qua_thi_ibfk_1` FOREIGN KEY (`ma_sinh_vien`) REFERENCES `Hoc_sinh` (`ma_sinh_vien`),
  CONSTRAINT `ket_qua_thi_ibfk_2` FOREIGN KEY (`ma_de_thi`) REFERENCES `De_thi` (`ma_de_thi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Ket_qua_thi`
--

LOCK TABLES `Ket_qua_thi` WRITE;
/*!40000 ALTER TABLE `Ket_qua_thi` DISABLE KEYS */;
INSERT INTO `Ket_qua_thi` VALUES ('BL01','SV01','N5','DT01',25,28,30,83,'ĐẠT'),('BL02','SV02','N5','DT01',15,18,12,45,'TRƯỢT'),('BL03','SV03','N4','DT02',28,30,25,83,'TRƯỢT'),('BL04','SV03','N4','DT02',35,35,28,98,'ĐẠT'),('BL05','SV04','N3','DT03',32,30,33,95,'ĐẠT'),('BL06','SV05','N2','DT04',30,28,32,90,'ĐẠT'),('BL07','SV06','N1','DT05',40,35,38,113,'ĐẠT'),('BL08','SV07','N5','DT01',30,32,28,90,'ĐẠT'),('BL09','HS09','N2','DT14',15,15,20,50,'TRƯỢT'),('BL10','HS10','N1','DT18',20,20,20,60,'TRƯỢT'),('BL11','HS11','N5','DT03',30,25,25,80,'ĐẠT'),('BL12','HS12','N4','DT07',20,20,30,70,'TRƯỢT'),('BL13','HS13','N3','DT11',25,25,25,75,'TRƯỢT'),('BL14','HS14','N2','DT15',30,30,30,90,'ĐẠT'),('BL15','HS15','N1','DT19',35,35,35,105,'ĐẠT'),('BL16','HS16','N5','DT04',10,10,15,35,'TRƯỢT'),('BL17','HS17','N4','DT08',30,25,30,85,'TRƯỢT'),('BL18','HS18','N3','DT12',40,35,35,110,'ĐẠT'),('BL19','HS19','N2','DT16',25,25,25,75,'TRƯỢT'),('BL20','HS20','N1','DT20',40,40,40,120,'ĐẠT');
/*!40000 ALTER TABLE `Ket_qua_thi` ENABLE KEYS */;
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
