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
-- Table structure for table `Lich_hoc`
--

DROP TABLE IF EXISTS `Lich_hoc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Lich_hoc` (
  `ma_lich_hoc` varchar(50) NOT NULL,
  `id_giao_vien` varchar(50) NOT NULL,
  `id_lop` varchar(50) NOT NULL,
  `ngay_day` timestamp NOT NULL,
  `ca_hoc` varchar(50) NOT NULL,
  `phong_hoc` varchar(50) NOT NULL,
  `noi_dung` varchar(50) NOT NULL,
  `trang_thai_buoi` varchar(50) NOT NULL,
  PRIMARY KEY (`ma_lich_hoc`),
  KEY `id_giao_vien` (`id_giao_vien`),
  KEY `id_lop` (`id_lop`),
  CONSTRAINT `lich_hoc_ibfk_1` FOREIGN KEY (`id_giao_vien`) REFERENCES `Giao_vien` (`id_giao_vien`),
  CONSTRAINT `lich_hoc_ibfk_2` FOREIGN KEY (`id_lop`) REFERENCES `Lop` (`id_lop`),
  CONSTRAINT `lich_hoc_chk_1` CHECK ((`trang_thai_buoi` in (_utf8mb4'nghi',_utf8mb4'day')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Lich_hoc`
--

LOCK TABLES `Lich_hoc` WRITE;
/*!40000 ALTER TABLE `Lich_hoc` DISABLE KEYS */;
INSERT INTO `Lich_hoc` VALUES ('LH01','GV01','L01','2026-11-02 11:00:00','Ca 1','P101','Bảng chữ cái Hiragana','day'),('LH02','GV02','L01','2026-11-04 11:00:00','Ca 1','P101','Bảng chữ cái Katakana','day'),('LH03','GV02','L02','2026-11-03 12:30:00','Ca 2','P102','Ngữ pháp bài 5','day'),('LH04','GV03','L03','2026-11-03 11:00:00','Ca 1','P201','Thể て và thể ない','day'),('LH05','GV04','L04','2026-11-05 12:30:00','Ca 2','P202','Đọc hiểu đoạn văn ngắn','nghi'),('LH06','GV05','L05','2026-11-06 11:00:00','Ca 1','P301','Ngữ pháp N2 bài 3','day'),('LH07','GV01','L06','2026-11-07 12:30:00','Ca 2','P302','Đọc hiểu N1','Dạy'),('LH08','GV01','L01','2026-11-09 11:00:00','Ca 1','P101','Từ vựng chủ đề gia đình','Dạy'),('LH09','GV09','L09','2026-02-05 01:00:00','Ca 1','P109','Ngu phap','Day'),('LH10','GV10','L10','2026-02-05 03:00:00','Ca 2','P110','Kanji','Day'),('LH11','GV11','L11','2026-02-06 01:00:00','Ca 1','P111','Thuong mai','Day'),('LH12','GV12','L12','2026-02-06 03:00:00','Ca 2','P112','Du hoc','Day'),('LH13','GV13','L13','2026-02-07 01:00:00','Ca 1','P113','Ngu phap','Nghi'),('LH14','GV14','L14','2026-02-07 03:00:00','Ca 2','P114','Luyen de','Day'),('LH15','GV15','L15','2026-02-08 01:00:00','Ca 1','P115','Doc hieu','Day'),('LH16','GV16','L16','2026-02-08 03:00:00','Ca 2','P116','Nghe','Day'),('LH17','GV17','L17','2026-02-09 01:00:00','Ca 1','P117','Kanji','Day'),('LH18','GV18','L18','2026-02-09 03:00:00','Ca 2','P118','Kanji','Day'),('LH19','GV19','L19','2026-02-10 01:00:00','Ca 1','P119','Tieng Nhat IT','Nghi'),('LH20','GV20','L20','2026-02-10 03:00:00','Ca 2','P120','Hoi thoai','Day');
/*!40000 ALTER TABLE `Lich_hoc` ENABLE KEYS */;
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
