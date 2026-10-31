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
-- Table structure for table `Khoa_hoc`
--

DROP TABLE IF EXISTS `Khoa_hoc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Khoa_hoc` (
  `ma_khoa_hoc` varchar(50) NOT NULL,
  `thoi_gian_dang_ki` timestamp NOT NULL,
  `ten_khoa_hoc` varchar(50) NOT NULL,
  `trinh_do` varchar(50) NOT NULL,
  `chuyen_de` varchar(50) NOT NULL,
  `giao_trinh` varchar(50) NOT NULL,
  `hoc_phi` int NOT NULL,
  PRIMARY KEY (`ma_khoa_hoc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Khoa_hoc`
--

LOCK TABLES `Khoa_hoc` WRITE;
/*!40000 ALTER TABLE `Khoa_hoc` DISABLE KEYS */;
INSERT INTO `Khoa_hoc` VALUES ('KH01','2026-09-01 01:00:00','Tiếng Nhật sơ cấp N5','N5','Ngữ pháp - Từ vựng','Minna no Nihongo I',2800000),('KH02','2026-09-01 08:00:00','Hán tự Kanji N5','N5','Kanji','Kanji Look and Learn N5',2000000),('KH03','2026-09-05 01:00:00','Chokai N5 (Nghe hiểu)','N5','Nghe hiểu','Nihongo Soumatome N5 Chokai',2200000),('KH04','2026-09-10 01:00:00','Luyện thi JLPT N5','N5','Luyện thi','Soumatome N5 + đề thi thử',2500000),('KH05','2026-09-10 01:00:00','Tiếng Nhật sơ cấp N4','N4','Ngữ pháp - Từ vựng','Minna no Nihongo II',3200000),('KH06','2026-09-15 01:00:00','Dokkai N4 (Đọc hiểu)','N4','Đọc hiểu','Nihongo Soumatome N4 Dokkai',2800000),('KH07','2026-09-20 01:00:00','Chokai N4 (Nghe hiểu)','N4','Nghe hiểu','Nihongo Soumatome N4 Chokai',2800000),('KH08','2026-09-25 01:00:00','Luyện thi JLPT N4','N4','Luyện thi','Shin Kanzen Master N4',3000000),('KH09','2026-01-09 01:00:00','Tiếng Nhật trung cấp N3','N3','Ngữ pháp - Từ vựng','Try! N3',3800000),('KH10','2026-01-10 01:00:00','Dokkai N3 (Đọc hiểu)','N3','Đọc hiểu','Shin Kanzen Master N3 Dokkai',3500000),('KH11','2026-01-11 01:00:00','Chokai N3 (Nghe hiểu)','N3','Nghe hiểu','Shin Kanzen Master N3 Chokai',3500000),('KH12','2026-01-12 01:00:00','Tiếng Nhật IT N3','N3','Tiếng Nhật CNTT','IT Nihongo - tài liệu nội bộ',4200000),('KH13','2026-01-13 01:00:00','Tiếng Nhật trung cao cấp N2','N2','Ngữ pháp - Từ vựng','Try! N2',4800000),('KH14','2026-01-14 01:00:00','Dokkai N2 (Đọc hiểu)','N2','Đọc hiểu','Shin Kanzen Master N2 Dokkai',4500000),('KH15','2026-01-15 01:00:00','Chokai N2 (Nghe hiểu)','N2','Nghe hiểu','Shin Kanzen Master N2 Chokai',4500000),('KH16','2026-01-16 01:00:00','Tiếng Nhật IT N2 - BrSE','N2','Tiếng Nhật CNTT','IT Nihongo - BrSE',5400000),('KH17','2026-01-17 01:00:00','Tiếng Nhật cao cấp N1','N1','Ngữ pháp - Từ vựng','Try! N1',5800000),('KH18','2026-01-18 01:00:00','Dokkai N1 (Đọc hiểu)','N1','Đọc hiểu','Shin Kanzen Master N1 Dokkai',5500000),('KH19','2026-01-19 01:00:00','Chokai N1 (Nghe hiểu)','N1','Nghe hiểu','Shin Kanzen Master N1 Chokai',5500000),('KH20','2026-01-20 01:00:00','Luyện thi JLPT N1','N1','Luyện thi','Soumatome N1 + đề thi thử',6200000);
/*!40000 ALTER TABLE `Khoa_hoc` ENABLE KEYS */;
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
