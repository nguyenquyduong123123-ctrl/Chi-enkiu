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

--
-- Table structure for table `De_thi`
--

DROP TABLE IF EXISTS `De_thi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `De_thi` (
  `ma_de_thi` varchar(50) NOT NULL,
  `cap_do` varchar(50) NOT NULL,
  `thoi_gian` int NOT NULL,
  PRIMARY KEY (`ma_de_thi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `De_thi`
--

LOCK TABLES `De_thi` WRITE;
/*!40000 ALTER TABLE `De_thi` DISABLE KEYS */;
INSERT INTO `De_thi` VALUES ('DT01','N5',105),('DT02','N4',125),('DT03','N3',140),('DT04','N2',155),('DT05','N1',170),('DT06','N4',125),('DT07','N4',125),('DT08','N4',125),('DT09','N3',140),('DT10','N3',140),('DT11','N3',140),('DT12','N3',140),('DT13','N2',155),('DT14','N2',155),('DT15','N2',155),('DT16','N2',155),('DT17','N1',170),('DT18','N1',170),('DT19','N1',170),('DT20','N1',170);
/*!40000 ALTER TABLE `De_thi` ENABLE KEYS */;
UNLOCK TABLES;

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

--
-- Table structure for table `hoa_don`
--

DROP TABLE IF EXISTS `hoa_don`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hoa_don` (
  `ma_hoa_don` varchar(50) NOT NULL,
  `ma_nhan_vien` varchar(50) NOT NULL,
  `ma_ghi_danh` varchar(50) NOT NULL,
  `khoan_thu` int NOT NULL,
  `ngay_lap` timestamp NOT NULL,
  `hinh_thuc` varchar(20) DEFAULT NULL,
  `trang_thai` varchar(30) NOT NULL DEFAULT 'Chưa nộp',
  `ngay_nop` timestamp NULL DEFAULT NULL,
  `nv_xac_nhan` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`ma_hoa_don`),
  KEY `ma_nhan_vien` (`ma_nhan_vien`),
  CONSTRAINT `hoa_don_ibfk_1` FOREIGN KEY (`ma_nhan_vien`) REFERENCES `Nhan_vien` (`ma_nhan_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hoa_don`
--

LOCK TABLES `hoa_don` WRITE;
/*!40000 ALTER TABLE `hoa_don` DISABLE KEYS */;
INSERT INTO `hoa_don` VALUES ('HD_1793344809','NV01','NV01:KH02',3200000,'2026-10-30 07:20:09','Chuyển khoản','Đã nộp','2026-10-30 09:16:33','NV01'),('HD01','NV04','SV01:KH01',3000000,'2026-09-02 02:00:00','Tiền mặt','Đã nộp','2026-09-02 02:00:00','NV04'),('HD02','NV04','SV02:KH02',3200000,'2026-09-03 03:30:00','Chuyển khoản','Đã nộp','2026-09-03 03:30:00','NV04'),('HD03','NV04','SV03:KH03',3800000,'2026-09-06 07:00:00','Tiền mặt','Đã nộp','2026-09-06 07:00:00','NV04'),('HD04','NV04','SV04:KH04',4500000,'2026-09-11 02:15:00','Chuyển khoản','Đã nộp','2026-09-11 02:15:00','NV04'),('HD05','NV04','SV05:KH06',5500000,'2026-09-16 04:00:00','Chuyển khoản','Đã nộp','2026-09-16 04:00:00','NV04'),('HD06','NV04','SV06:KH08',7000000,'2026-09-26 08:45:00','Tiền mặt','Đã nộp','2026-09-26 08:45:00','NV04'),('HD07','NV04','SV07:KH01',3000000,'2026-10-02 02:30:00','Tiền mặt','Đã nộp','2026-10-30 11:06:35','NV01'),('HD08','NV04','SV08:KH03',3800000,'2026-10-08 06:20:00','Tiền mặt','Đã nộp','2026-10-08 06:20:00','NV04'),('HD09','NV04','SV01:KH05',4200000,'2026-10-20 03:00:00','Tiền mặt','Đã nộp','2026-10-30 11:06:22','NV01'),('HD10','NV10','HS10',1800000,'2026-01-14 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD11','NV11','HS11',5000000,'2026-01-15 02:00:00','Chuyển khoản','Đã nộp','2026-10-30 11:06:42','NV01'),('HD12','NV12','HS12',4000000,'2026-01-16 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD13','NV13','HS13',2100000,'2026-01-17 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD14','NV14','HS14',2700000,'2026-01-18 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD15','NV15','HS15',3200000,'2026-01-19 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD16','NV16','HS16',3300000,'2026-01-20 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD17','NV17','HS17',3600000,'2026-01-21 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD18','NV18','HS18',4200000,'2026-01-22 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD19','NV19','HS19',4800000,'2026-01-23 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD20','NV20','HS20',2300000,'2026-01-24 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD21','NV01','HS01',2500000,'2026-05-05 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD22','NV02','HS02',3000000,'2026-05-12 03:00:00','Chuyển khoản','Đã nộp','2026-10-30 09:16:41','NV01'),('HD23','NV03','HS03',4000000,'2026-05-20 07:00:00',NULL,'Chưa nộp',NULL,NULL),('HD24','NV04','HS04',3500000,'2026-06-03 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD25','NV05','HS05',4200000,'2026-06-15 03:00:00',NULL,'Chưa nộp',NULL,NULL),('HD26','NV06','HS06',5000000,'2026-06-25 07:00:00',NULL,'Chưa nộp',NULL,NULL),('HD27','NV07','HS07',4000000,'2026-07-04 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD28','NV08','HS08',5500000,'2026-07-14 03:00:00',NULL,'Chưa nộp',NULL,NULL),('HD29','NV09','HS09',6000000,'2026-07-25 07:00:00',NULL,'Chưa nộp',NULL,NULL),('HD30','NV10','HS10',5000000,'2026-08-02 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD31','NV11','HS11',6000000,'2026-08-12 03:00:00',NULL,'Chưa nộp',NULL,NULL),('HD32','NV12','HS12',7000000,'2026-08-22 07:00:00',NULL,'Chưa nộp',NULL,NULL),('HD33','NV13','HS13',6000000,'2026-09-03 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD34','NV14','HS14',7000000,'2026-09-10 03:00:00',NULL,'Chưa nộp',NULL,NULL),('HD35','NV15','HS15',8000000,'2026-09-18 07:00:00',NULL,'Chưa nộp',NULL,NULL),('HD36','NV16','HS16',6000000,'2026-09-25 08:00:00','Tiền mặt','Đã nộp','2026-10-30 11:06:45','NV01'),('HD37','NV17','HS17',2300000,'2026-10-05 02:00:00',NULL,'Chưa nộp',NULL,NULL),('HD38','NV18','HS18',3200000,'2026-10-12 03:00:00',NULL,'Chưa nộp',NULL,NULL),('HD39','NV19','HS19',4200000,'2026-10-20 07:00:00','Thẻ','Đã nộp','2026-10-30 09:16:53','NV01'),('HD40','NV20','HS20',2500000,'2026-10-28 08:00:00','Tiền mặt','Đã nộp','2026-10-30 11:06:40','NV01'),('HD41','NV04','SV01:KH01',3000000,'2026-05-04 02:00:00','Tiền mặt','Đã nộp','2026-05-04 02:00:00','NV01'),('HD42','NV04','SV02:KH02',3200000,'2026-05-10 03:30:00','Chuyển khoản','Đã nộp','2026-05-10 03:30:00','NV01'),('HD43','NV04','SV03:KH03',3800000,'2026-05-17 07:00:00','Tiền mặt','Đã nộp','2026-05-17 07:00:00','NV01'),('HD44','NV04','SV04:KH04',4500000,'2026-05-24 02:15:00','Chuyển khoản','Đã nộp','2026-05-24 02:15:00','NV01'),('HD45','NV04','SV05:KH06',5500000,'2026-05-29 04:00:00','Thẻ','Đã nộp','2026-05-29 04:00:00','NV01'),('HD46','NV04','SV06:KH08',7000000,'2026-06-03 02:00:00','Chuyển khoản','Đã nộp','2026-06-03 02:00:00','NV01'),('HD47','NV04','SV07:KH01',3000000,'2026-06-09 03:00:00','Tiền mặt','Đã nộp','2026-06-09 03:00:00','NV01'),('HD48','NV04','SV08:KH03',3800000,'2026-06-15 07:30:00','Thẻ','Đã nộp','2026-06-15 07:30:00','NV01'),('HD49','NV04','SV01:KH05',4200000,'2026-06-21 02:45:00','Chuyển khoản','Đã nộp','2026-06-21 02:45:00','NV01'),('HD50','NV04','SV02:KH02',3200000,'2026-06-27 08:00:00','Tiền mặt','Đã nộp','2026-06-27 08:00:00','NV01'),('HD51','NV04','SV03:KH03',3800000,'2026-07-02 02:00:00','Tiền mặt','Đã nộp','2026-07-02 02:00:00','NV01'),('HD52','NV04','SV04:KH04',4500000,'2026-07-08 03:30:00','Chuyển khoản','Đã nộp','2026-07-08 03:30:00','NV01'),('HD53','NV04','SV05:KH06',5500000,'2026-07-14 06:00:00','Thẻ','Đã nộp','2026-07-14 06:00:00','NV01'),('HD54','NV04','SV06:KH08',7000000,'2026-07-20 04:15:00','Chuyển khoản','Đã nộp','2026-07-20 04:15:00','NV01'),('HD55','NV04','SV07:KH01',3000000,'2026-07-27 09:00:00','Tiền mặt','Đã nộp','2026-07-27 09:00:00','NV01'),('HD56','NV04','SV08:KH03',3800000,'2026-08-03 02:30:00','Tiền mặt','Đã nộp','2026-08-03 02:30:00','NV01'),('HD57','NV04','SV01:KH01',3000000,'2026-08-09 03:00:00','Thẻ','Đã nộp','2026-08-09 03:00:00','NV01'),('HD58','NV04','SV02:KH02',3200000,'2026-08-14 07:00:00','Chuyển khoản','Đã nộp','2026-08-14 07:00:00','NV01'),('HD59','NV04','SV03:KH03',3800000,'2026-08-19 02:15:00','Tiền mặt','Đã nộp','2026-08-19 02:15:00','NV01'),('HD60','NV04','SV04:KH04',4500000,'2026-08-24 04:00:00','Chuyển khoản','Đã nộp','2026-08-24 04:00:00','NV01'),('HD61','NV04','SV05:KH06',5500000,'2026-08-29 08:30:00','Thẻ','Đã nộp','2026-08-29 08:30:00','NV01');
/*!40000 ALTER TABLE `hoa_don` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Hoc_sinh`
--

DROP TABLE IF EXISTS `Hoc_sinh`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Hoc_sinh` (
  `ma_sinh_vien` varchar(50) NOT NULL,
  `id_giao_vien` varchar(50) NOT NULL,
  `ho_ten` varchar(50) NOT NULL,
  `ngay_sinh` timestamp NOT NULL,
  `trinh_do` varchar(50) NOT NULL DEFAULT 'khong biet',
  `so_dien_thoai` varchar(50) NOT NULL,
  PRIMARY KEY (`ma_sinh_vien`),
  KEY `id_giao_vien` (`id_giao_vien`),
  CONSTRAINT `hoc_sinh_ibfk_1` FOREIGN KEY (`id_giao_vien`) REFERENCES `Giao_vien` (`id_giao_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Hoc_sinh`
--

LOCK TABLES `Hoc_sinh` WRITE;
/*!40000 ALTER TABLE `Hoc_sinh` DISABLE KEYS */;
INSERT INTO `Hoc_sinh` VALUES ('HS01','GV01','Nguyen Van A','2005-01-09 17:00:00','N5','0981000001'),('HS02','GV02','Tran Thi B','2004-02-14 17:00:00','N4','0981000002'),('HS03','GV03','Le Van C','2003-03-19 17:00:00','N3','0981000003'),('HS04','GV04','Pham Thi D','2005-04-24 17:00:00','N2','0981000004'),('HS05','GV05','Hoang Van E','2002-05-09 17:00:00','N1','0981000005'),('HS06','GV06','Do Thi F','2004-06-14 17:00:00','N5','0981000006'),('HS07','GV07','Bui Van G','2003-07-19 17:00:00','N4','0981000007'),('HS08','GV08','Vu Thi H','2005-08-24 17:00:00','N3','0981000008'),('HS09','GV09','Dang Van I','2002-09-09 17:00:00','N2','0981000009'),('HS10','GV10','Ngo Thi K','2004-10-14 17:00:00','N1','0981000010'),('HS11','GV11','Mai Van L','2003-11-19 17:00:00','N5','0981000011'),('HS12','GV12','Trinh Thi M','2005-12-24 17:00:00','N4','0981000012'),('HS13','GV13','Nguyen Van N','2002-01-14 17:00:00','N3','0981000013'),('HS14','GV14','Tran Thi O','2004-02-19 17:00:00','N2','0981000014'),('HS15','GV15','Le Van P','2003-03-24 17:00:00','N1','0981000015'),('HS16','GV16','Pham Thi Q','2005-04-09 17:00:00','N5','0981000016'),('HS17','GV17','Hoang Van R','2002-05-14 17:00:00','N4','0981000017'),('HS18','GV18','Do Thi S','2004-06-19 17:00:00','N3','0981000018'),('HS19','GV19','Bui Van T','2003-07-24 17:00:00','N2','0981000019'),('HS20','GV20','Vu Thi U','2005-08-09 17:00:00','N1','0981000020'),('SV01','GV01','Nguyễn Văn An','2003-03-11 17:00:00','N5','0912000001'),('SV02','GV02','Trần Thị Bình','2002-07-24 17:00:00','N5','0912000002'),('SV03','GV03','Lê Hoàng Cường','2001-11-01 17:00:00','N4','0912000003'),('SV04','GV04','Phạm Thị Dung','2000-05-17 17:00:00','N3','0912000004'),('SV05','GV05','Hoàng Văn Em','1999-09-29 17:00:00','N2','0912000005'),('SV06','GV01','Đặng Thu Giang','2004-01-07 17:00:00','N1','0912000006'),('SV07','GV02','Bùi Minh Hải','2003-12-20 17:00:00','N5','0912000007'),('SV08','GV03','Vũ Thị Hương','2002-04-13 17:00:00','N4','0912000008');
/*!40000 ALTER TABLE `Hoc_sinh` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Hoc_sinh_lop`
--

DROP TABLE IF EXISTS `Hoc_sinh_lop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Hoc_sinh_lop` (
  `id_lop` varchar(50) NOT NULL,
  `ma_sinh_vien` varchar(50) NOT NULL,
  `ngay_vao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_lop`,`ma_sinh_vien`),
  KEY `ma_sinh_vien` (`ma_sinh_vien`),
  CONSTRAINT `hoc_sinh_lop_ibfk_1` FOREIGN KEY (`id_lop`) REFERENCES `Lop` (`id_lop`),
  CONSTRAINT `hoc_sinh_lop_ibfk_2` FOREIGN KEY (`ma_sinh_vien`) REFERENCES `Hoc_sinh` (`ma_sinh_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Hoc_sinh_lop`
--

LOCK TABLES `Hoc_sinh_lop` WRITE;
/*!40000 ALTER TABLE `Hoc_sinh_lop` DISABLE KEYS */;
/*!40000 ALTER TABLE `Hoc_sinh_lop` ENABLE KEYS */;
UNLOCK TABLES;

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

--
-- Table structure for table `Phieu_luong`
--

DROP TABLE IF EXISTS `Phieu_luong`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Phieu_luong` (
  `ma_phieu_luong` varchar(50) NOT NULL,
  `id_giao_vien` varchar(50) NOT NULL,
  `ma_nhan_vien` varchar(50) NOT NULL,
  `tu_ngay` timestamp NOT NULL,
  `den_ngay` timestamp NOT NULL,
  `ngay_tra` timestamp NOT NULL,
  `tong_luong` int NOT NULL,
  PRIMARY KEY (`ma_phieu_luong`),
  KEY `id_giao_vien` (`id_giao_vien`),
  KEY `ma_nhan_vien` (`ma_nhan_vien`),
  CONSTRAINT `phieu_luong_ibfk_1` FOREIGN KEY (`id_giao_vien`) REFERENCES `Giao_vien` (`id_giao_vien`),
  CONSTRAINT `phieu_luong_ibfk_2` FOREIGN KEY (`ma_nhan_vien`) REFERENCES `Nhan_vien` (`ma_nhan_vien`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Phieu_luong`
--

LOCK TABLES `Phieu_luong` WRITE;
/*!40000 ALTER TABLE `Phieu_luong` DISABLE KEYS */;
INSERT INTO `Phieu_luong` VALUES ('PL01','GV01','NV02','2026-08-31 17:00:00','2026-09-29 17:00:00','2026-10-04 17:00:00',18000000),('PL02','GV02','NV02','2026-08-31 17:00:00','2026-09-29 17:00:00','2026-10-04 17:00:00',14000000),('PL03','GV03','NV03','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',15000000),('PL04','GV04','NV02','2026-08-31 17:00:00','2026-09-29 17:00:00','2026-10-04 17:00:00',17000000),('PL05','GV05','NV02','2026-08-31 17:00:00','2026-09-29 17:00:00','2026-10-04 17:00:00',13000000),('PL06','GV01','NV02','2026-09-30 17:00:00','2026-10-30 17:00:00','2026-11-04 17:00:00',18500000),('PL07','GV07','NV07','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',12500000),('PL08','GV08','NV08','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',9000000),('PL09','GV09','NV09','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',16000000),('PL10','GV10','NV10','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',11000000),('PL11','GV11','NV11','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',10500000),('PL12','GV12','NV12','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',17000000),('PL13','GV13','NV13','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',11500000),('PL14','GV14','NV14','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',9500000),('PL15','GV15','NV15','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',14500000),('PL16','GV16','NV16','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',13000000),('PL17','GV17','NV17','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',15500000),('PL18','GV18','NV18','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',10000000),('PL19','GV19','NV19','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',13500000),('PL20','GV20','NV20','2025-12-31 17:00:00','2026-01-30 17:00:00','2026-02-04 17:00:00',18000000);
/*!40000 ALTER TABLE `Phieu_luong` ENABLE KEYS */;
UNLOCK TABLES;

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

-- Dump completed on 2026-10-31 10:15:08
