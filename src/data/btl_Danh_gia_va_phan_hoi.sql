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
-- Table structure for table `Danh_gia_va_phan_hoi`
--

DROP TABLE IF EXISTS `Danh_gia_va_phan_hoi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Danh_gia_va_phan_hoi` (
  `ma_danh_gia` varchar(50) NOT NULL,
  `ma_sinh_vien` varchar(50) NOT NULL,
  `id_lop` varchar(50) NOT NULL,
  `rating` int NOT NULL,
  `mo_ta` text NOT NULL,
  `ngay_gui` timestamp NOT NULL,
  PRIMARY KEY (`ma_danh_gia`),
  KEY `ma_sinh_vien` (`ma_sinh_vien`),
  KEY `id_lop` (`id_lop`),
  CONSTRAINT `danh_gia_va_phan_hoi_ibfk_1` FOREIGN KEY (`ma_sinh_vien`) REFERENCES `Hoc_sinh` (`ma_sinh_vien`),
  CONSTRAINT `danh_gia_va_phan_hoi_ibfk_2` FOREIGN KEY (`id_lop`) REFERENCES `Lop` (`id_lop`),
  CONSTRAINT `danh_gia_va_phan_hoi_chk_1` CHECK (((`rating` >= 1) and (`rating` <= 5)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Danh_gia_va_phan_hoi`
--

LOCK TABLES `Danh_gia_va_phan_hoi` WRITE;
/*!40000 ALTER TABLE `Danh_gia_va_phan_hoi` DISABLE KEYS */;
INSERT INTO `Danh_gia_va_phan_hoi` VALUES ('DG01','SV01','L01',5,'Giáo viên dạy dễ hiểu, lớp vui.','2026-10-10 13:00:00'),('DG02','SV02','L02',4,'Nội dung hay nhưng bài tập hơi nhiều.','2026-10-11 14:00:00'),('DG03','SV03','L03',5,'Phòng học thoải mái, giáo viên nhiệt tình.','2026-10-12 12:30:00'),('DG04','SV04','L04',3,'Tốc độ dạy hơi nhanh.','2026-10-13 13:15:00'),('DG05','SV05','L05',5,'Tài liệu rất chất lượng.','2026-10-14 15:00:00'),('DG06','SV06','L06',4,'Cần thêm giờ luyện nghe.','2026-10-15 11:45:00'),('DG07','HS07','L07',3,'Noi dung binh thuong','2026-02-12 04:00:00'),('DG08','HS08','L08',1,'Can cai thien tai lieu','2026-02-12 05:00:00'),('DG09','HS09','L09',5,'Moi thu deu tot','2026-02-13 02:00:00'),('DG10','HS10','L10',4,'Co nhieu vi du hay','2026-02-13 03:00:00'),('DG11','HS11','L11',3,'Can them thoi gian','2026-02-13 04:00:00'),('DG12','HS12','L12',2,'Phong hoc hoi chat','2026-02-13 05:00:00'),('DG13','HS13','L13',5,'Rat tot','2026-02-14 02:00:00'),('DG14','HS14','L14',4,'Chat luong tot','2026-02-14 03:00:00'),('DG15','HS15','L15',3,'Tam duoc','2026-02-14 04:00:00'),('DG16','HS16','L16',1,'Can cai thien bai giang','2026-02-14 05:00:00'),('DG17','HS17','L17',5,'Giao vien rat tot','2026-02-15 02:00:00'),('DG18','HS18','L18',4,'Noi dung de hieu','2026-02-15 03:00:00'),('DG19','HS19','L19',2,'Can them thuc hanh','2026-02-15 04:00:00'),('DG20','HS20','L20',5,'Rat hai long','2026-02-15 05:00:00');
/*!40000 ALTER TABLE `Danh_gia_va_phan_hoi` ENABLE KEYS */;
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
