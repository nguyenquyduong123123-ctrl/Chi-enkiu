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
-- Table structure for table `Giao_vien`
--

DROP TABLE IF EXISTS `Giao_vien`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Giao_vien` (
  `id_giao_vien` varchar(50) NOT NULL,
  `so_dien_thoai` varchar(50) NOT NULL,
  `chung_chi` varchar(50) NOT NULL,
  `quoc_tich` varchar(50) NOT NULL,
  `ho_ten` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_giao_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Giao_vien`
--

LOCK TABLES `Giao_vien` WRITE;
/*!40000 ALTER TABLE `Giao_vien` DISABLE KEYS */;
INSERT INTO `Giao_vien` VALUES ('GV01','0901000001','JLPT N1','Nhật Bản','Tanaka Hiroshi'),('GV02','0901000002','JLPT N1','Việt Nam','Nguyễn Thanh Mai'),('GV03','0901000003','JLPT N2','Việt Nam','Đỗ Quang Huy'),('GV04','0901000004','JLPT N1','Nhật Bản','Sato Yuki'),('GV05','0901000005','JLPT N2','Việt Nam','Vũ Ngọc Ánh'),('GV06','0906666666','JLPT N1','Viet Nam','Pham Van Long'),('GV07','0907777777','JLPT N2','Nhat Ban','Suzuki Ken'),('GV08','0908888888','JLPT N3','Viet Nam','Do Thi Linh'),('GV09','0909999999','JLPT N1','Nhat Ban','Tanaka Yuki'),('GV10','0910000000','JLPT N2','Viet Nam','Hoang Thi Thu'),('GV11','0911111111','JLPT N3','Viet Nam','Bui Van Tuan'),('GV12','0912222222','JLPT N1','Nhat Ban','Kobayashi Mika'),('GV13','0913333333','JLPT N2','Viet Nam','Nguyen Van Hai'),('GV14','0914444444','JLPT N3','Nhat Ban','Ito Sakura'),('GV15','0915555555','JLPT N1','Viet Nam','Tran Thi Lan'),('GV16','0916666666','JLPT N2','Viet Nam','Pham Thi Yen'),('GV17','0917777777','JLPT N1','Nhat Ban','Kato Hiroshi'),('GV18','0918888888','JLPT N3','Viet Nam','Le Van Thanh'),('GV19','0919999999','JLPT N2','Nhat Ban','Mori Akira'),('GV20','0920000000','JLPT N1','Viet Nam','Do Thi Ngoc');
/*!40000 ALTER TABLE `Giao_vien` ENABLE KEYS */;
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

-- Dump completed on 2026-10-31 10:08:57
