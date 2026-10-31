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
-- Table structure for table `Nhan_vien`
--

DROP TABLE IF EXISTS `Nhan_vien`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Nhan_vien` (
  `ma_nhan_vien` varchar(50) NOT NULL,
  `ho_ten` varchar(50) NOT NULL,
  `vai_tro` varchar(50) NOT NULL,
  `luong` int NOT NULL,
  PRIMARY KEY (`ma_nhan_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Nhan_vien`
--

LOCK TABLES `Nhan_vien` WRITE;
/*!40000 ALTER TABLE `Nhan_vien` DISABLE KEYS */;
INSERT INTO `Nhan_vien` VALUES ('NV01','Nguyễn Thị Lan','Quản lý',15000000),('NV02','Trần Văn Hùng','Kế toán',12000000),('NV03','Lê Thu Hà','Tư vấn',9000000),('NV04','Phạm Minh Tuấn','Thu ngân',8500000),('NV05','Hoang Van Duc','Quan ly',16000000),('NV06','Do Thi Hanh','Tu van',11000000),('NV07','Bui Van Hung','Ke toan',12500000),('NV08','Vu Thi Lan','Thu ngan',9500000),('NV09','Dang Van Minh','Nhan vien',8500000),('NV10','Ngo Thi Nga','Quan ly',17000000),('NV11','Mai Van Phuc','Tu van',10500000),('NV12','Trinh Thi Hoa','Ke toan',13000000),('NV13','Nguyen Van Khoa','Tu van',10000000),('NV14','Pham Van Long','Nhan vien',8000000),('NV15','Le Thi Mai','Thu ngan',9000000),('NV16','Tran Van Nam','Quan ly',18000000),('NV17','Do Van Phong','Ke toan',14000000),('NV18','Hoang Thi Quynh','Tu van',11000000),('NV19','Bui Thi Sen','Nhan vien',8500000),('NV20','Vu Van Tuan','Quan ly',15500000);
/*!40000 ALTER TABLE `Nhan_vien` ENABLE KEYS */;
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
