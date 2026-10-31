
USE btl;

-- =====================================
-- 1. NHAN_VIEN (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Nhan_vien VALUES
('NV01','Nguyen Van An','Quan ly',15000000),
('NV02','Tran Thi Binh','Ke toan',12000000),
('NV03','Le Van Cuong','Tu van',10000000),
('NV04','Pham Thi Dung','Thu ngan',9000000),
('NV05','Hoang Van Duc','Quan ly',16000000),
('NV06','Do Thi Hanh','Tu van',11000000),
('NV07','Bui Van Hung','Ke toan',12500000),
('NV08','Vu Thi Lan','Thu ngan',9500000),
('NV09','Dang Van Minh','Nhan vien',8500000),
('NV10','Ngo Thi Nga','Quan ly',17000000),
('NV11','Mai Van Phuc','Tu van',10500000),
('NV12','Trinh Thi Hoa','Ke toan',13000000),
('NV13','Nguyen Van Khoa','Tu van',10000000),
('NV14','Pham Van Long','Nhan vien',8000000),
('NV15','Le Thi Mai','Thu ngan',9000000),
('NV16','Tran Van Nam','Quan ly',18000000),
('NV17','Do Van Phong','Ke toan',14000000),
('NV18','Hoang Thi Quynh','Tu van',11000000),
('NV19','Bui Thi Sen','Nhan vien',8500000),
('NV20','Vu Van Tuan','Quan ly',15500000);

-- =====================================
-- 2. KHOA_HOC (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Khoa_hoc VALUES
('KH01','2026-01-01 08:00:00','Tieng Nhat N5','So cap','Giao tiep','Minna 1',2500000),
('KH02','2026-01-02 08:00:00','Tieng Nhat N4','So cap','Ngu phap','Minna 2',3000000),
('KH03','2026-01-03 08:00:00','Tieng Nhat N3','Trung cap','Doc hieu','Soumatome N3',4500000),
('KH04','2026-01-04 08:00:00','Tieng Nhat N2','Cao cap','Tu vung','Soumatome N2',5500000),
('KH05','2026-01-05 08:00:00','Tieng Nhat N1','Cao cap','Luyen thi','Soumatome N1',6500000),
('KH06','2026-01-06 08:00:00','Giao tiep co ban','So cap','Giao tiep','Basic',2000000),
('KH07','2026-01-07 08:00:00','Giao tiep nang cao','Trung cap','Giao tiep','Advanced',3500000),
('KH08','2026-01-08 08:00:00','Luyen thi N5','So cap','Luyen thi','JLPT N5',2200000),
('KH09','2026-01-09 08:00:00','Luyen thi N4','So cap','Luyen thi','JLPT N4',2800000),
('KH10','2026-01-10 08:00:00','Tieng Nhat tre em','So cap','Giao tiep','Kodomo',1800000),
('KH11','2026-01-11 08:00:00','Tieng Nhat doanh nghiep','Trung cap','Thuong mai','Business',5000000),
('KH12','2026-01-12 08:00:00','Tieng Nhat du hoc','Trung cap','Du hoc','Study Japan',4000000),
('KH13','2026-01-13 08:00:00','Ngu phap N5','So cap','Ngu phap','Grammar N5',2100000),
('KH14','2026-01-14 08:00:00','Ngu phap N4','So cap','Ngu phap','Grammar N4',2700000),
('KH15','2026-01-15 08:00:00','Doc hieu N3','Trung cap','Doc hieu','Reading N3',3200000),
('KH16','2026-01-16 08:00:00','Nghe N3','Trung cap','Nghe','Listening N3',3300000),
('KH17','2026-01-17 08:00:00','Kanji N2','Cao cap','Kanji','Kanji N2',3600000),
('KH18','2026-01-18 08:00:00','Kanji N1','Cao cap','Kanji','Kanji N1',4200000),
('KH19','2026-01-19 08:00:00','Tieng Nhat IT','Trung cap','Chuyen nganh','IT Japanese',4800000),
('KH20','2026-01-20 08:00:00','Tieng Nhat phan xa','So cap','Giao tiep','Conversation',2300000);

-- =====================================
-- 3. GIAO_VIEN (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Giao_vien VALUES
('GV01','0901111111','JLPT N1','Viet Nam','Nguyen Thi Mai'),
('GV02','0902222222','JLPT N2','Viet Nam','Tran Van Nam'),
('GV03','0903333333','JLPT N1','Nhat Ban','Yamada Taro'),
('GV04','0904444444','JLPT N2','Nhat Ban','Sato Hanako'),
('GV05','0905555555','JLPT N3','Viet Nam','Le Thi Hoa'),
('GV06','0906666666','JLPT N1','Viet Nam','Pham Van Long'),
('GV07','0907777777','JLPT N2','Nhat Ban','Suzuki Ken'),
('GV08','0908888888','JLPT N3','Viet Nam','Do Thi Linh'),
('GV09','0909999999','JLPT N1','Nhat Ban','Tanaka Yuki'),
('GV10','0910000000','JLPT N2','Viet Nam','Hoang Thi Thu'),
('GV11','0911111111','JLPT N3','Viet Nam','Bui Van Tuan'),
('GV12','0912222222','JLPT N1','Nhat Ban','Kobayashi Mika'),
('GV13','0913333333','JLPT N2','Viet Nam','Nguyen Van Hai'),
('GV14','0914444444','JLPT N3','Nhat Ban','Ito Sakura'),
('GV15','0915555555','JLPT N1','Viet Nam','Tran Thi Lan'),
('GV16','0916666666','JLPT N2','Viet Nam','Pham Thi Yen'),
('GV17','0917777777','JLPT N1','Nhat Ban','Kato Hiroshi'),
('GV18','0918888888','JLPT N3','Viet Nam','Le Van Thanh'),
('GV19','0919999999','JLPT N2','Nhat Ban','Mori Akira'),
('GV20','0920000000','JLPT N1','Viet Nam','Do Thi Ngoc');

-- =====================================
-- 4. LOP (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Lop VALUES
('L01','KH01','NV01','2025-2026','N5-A',20),
('L02','KH02','NV02','2025-2026','N4-A',18),
('L03','KH03','NV03','2025-2026','N3-A',22),
('L04','KH04','NV04','2025-2026','N2-A',19),
('L05','KH05','NV05','2025-2026','N1-A',15),
('L06','KH06','NV06','2025-2026','GT-A',17),
('L07','KH07','NV07','2025-2026','GT-B',12),
('L08','KH08','NV08','2025-2026','LT-N5',14),
('L09','KH09','NV09','2025-2026','LT-N4',10),
('L10','KH10','NV10','2025-2026','TE-A',11),
('L11','KH11','NV11','2025-2026','DN-A',25),
('L12','KH12','NV12','2025-2026','DH-A',20),
('L13','KH13','NV13','2025-2026','NP-N5',18),
('L14','KH14','NV14','2025-2026','NP-N4',16),
('L15','KH15','NV15','2025-2026','DH-N3',15),
('L16','KH16','NV16','2025-2026','NG-N3',12),
('L17','KH17','NV17','2025-2026','KJ-N2',20),
('L18','KH18','NV18','2025-2026','KJ-N1',22),
('L19','KH19','NV19','2025-2026','IT-JP',13),
('L20','KH20','NV20','2025-2026','GT-C',24);

-- =====================================
-- 5. QUANLI (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Quanli VALUES
('L01','GV01'),('L02','GV02'),
('L03','GV03'),('L04','GV04'),
('L05','GV05'),('L06','GV06'),
('L07','GV07'),('L08','GV08'),
('L09','GV09'),('L10','GV10'),
('L11','GV11'),('L12','GV12'),
('L13','GV13'),('L14','GV14'),
('L15','GV15'),('L16','GV16'),
('L17','GV17'),('L18','GV18'),
('L19','GV19'),('L20','GV20');

-- =====================================
-- 6. HOC_SINH (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Hoc_sinh VALUES
('HS01','GV01','Nguyen Van A','2005-01-10 00:00:00','N5','0981000001'),
('HS02','GV02','Tran Thi B','2004-02-15 00:00:00','N4','0981000002'),
('HS03','GV03','Le Van C','2003-03-20 00:00:00','N3','0981000003'),
('HS04','GV04','Pham Thi D','2005-04-25 00:00:00','N2','0981000004'),
('HS05','GV05','Hoang Van E','2002-05-10 00:00:00','N1','0981000005'),
('HS06','GV06','Do Thi F','2004-06-15 00:00:00','N5','0981000006'),
('HS07','GV07','Bui Van G','2003-07-20 00:00:00','N4','0981000007'),
('HS08','GV08','Vu Thi H','2005-08-25 00:00:00','N3','0981000008'),
('HS09','GV09','Dang Van I','2002-09-10 00:00:00','N2','0981000009'),
('HS10','GV10','Ngo Thi K','2004-10-15 00:00:00','N1','0981000010'),
('HS11','GV11','Mai Van L','2003-11-20 00:00:00','N5','0981000011'),
('HS12','GV12','Trinh Thi M','2005-12-25 00:00:00','N4','0981000012'),
('HS13','GV13','Nguyen Van N','2002-01-15 00:00:00','N3','0981000013'),
('HS14','GV14','Tran Thi O','2004-02-20 00:00:00','N2','0981000014'),
('HS15','GV15','Le Van P','2003-03-25 00:00:00','N1','0981000015'),
('HS16','GV16','Pham Thi Q','2005-04-10 00:00:00','N5','0981000016'),
('HS17','GV17','Hoang Van R','2002-05-15 00:00:00','N4','0981000017'),
('HS18','GV18','Do Thi S','2004-06-20 00:00:00','N3','0981000018'),
('HS19','GV19','Bui Van T','2003-07-25 00:00:00','N2','0981000019'),
('HS20','GV20','Vu Thi U','2005-08-10 00:00:00','N1','0981000020');

-- =====================================
-- 7. DE_THI (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO De_thi VALUES
('DT01','N5',105),('DT02','N5',105),
('DT03','N5',105),('DT04','N5',105),
('DT05','N4',125),('DT06','N4',125),
('DT07','N4',125),('DT08','N4',125),
('DT09','N3',140),('DT10','N3',140),
('DT11','N3',140),('DT12','N3',140),
('DT13','N2',155),('DT14','N2',155),
('DT15','N2',155),('DT16','N2',155),
('DT17','N1',170),('DT18','N1',170),
('DT19','N1',170),('DT20','N1',170);

-- =====================================
-- 8. KET_QUA_THI (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Ket_qua_thi VALUES
('BL01','HS01','N5','DT01',25,20,30,90,NULL),
('BL02','HS02','N4','DT05',25,20,30,90,NULL),
('BL03','HS03','N3','DT09',30,30,30,90,NULL),
('BL04','HS04','N2','DT13',35,30,35,100,NULL),
('BL05','HS05','N1','DT17',40,40,40,120,NULL),
('BL06','HS06','N5','DT02',15,15,20,50,NULL),
('BL07','HS07','N4','DT06',20,15,25,60,NULL),
('BL08','HS08','N3','DT10',18,20,25,63,NULL),
('BL09','HS09','N2','DT14',15,15,20,50,NULL),
('BL10','HS10','N1','DT18',20,20,20,60,NULL),
('BL11','HS11','N5','DT03',30,25,25,80,NULL),
('BL12','HS12','N4','DT07',20,20,30,70,NULL),
('BL13','HS13','N3','DT11',25,25,25,75,NULL),
('BL14','HS14','N2','DT15',30,30,30,90,NULL),
('BL15','HS15','N1','DT19',35,35,35,105,NULL),
('BL16','HS16','N5','DT04',10,10,15,35,NULL),
('BL17','HS17','N4','DT08',30,25,30,85,NULL),
('BL18','HS18','N3','DT12',40,35,35,110,NULL),
('BL19','HS19','N2','DT16',25,25,25,75,NULL),
('BL20','HS20','N1','DT20',40,40,40,120,NULL);

-- =====================================
-- 9. HOA_DON (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO hoa_don
(ma_hoa_don, ma_nhan_vien, ma_ghi_danh, khoan_thu, ngay_lap, hinh_thuc)
VALUES
('HD01','NV02','HS01',2500000,'2026-01-05 09:00:00','Tien mat'),
('HD02','NV02','HS02',3000000,'2026-01-06 09:00:00','Chuyen khoan'),
('HD03','NV03','HS03',4500000,'2026-01-07 09:00:00','Tien mat'),
('HD04','NV04','HS04',5500000,'2026-01-08 09:00:00','Chuyen khoan'),
('HD05','NV05','HS05',6500000,'2026-01-09 09:00:00','Tien mat'),
('HD06','NV06','HS06',2000000,'2026-01-10 09:00:00','The'),
('HD07','NV07','HS07',3500000,'2026-01-11 09:00:00','Tien mat'),
('HD08','NV08','HS08',2200000,'2026-01-12 09:00:00','Chuyen khoan'),
('HD09','NV09','HS09',2800000,'2026-01-13 09:00:00','The'),
('HD10','NV10','HS10',1800000,'2026-01-14 09:00:00','Tien mat'),
('HD11','NV11','HS11',5000000,'2026-01-15 09:00:00','Chuyen khoan'),
('HD12','NV12','HS12',4000000,'2026-01-16 09:00:00','The'),
('HD13','NV13','HS13',2100000,'2026-01-17 09:00:00','Tien mat'),
('HD14','NV14','HS14',2700000,'2026-01-18 09:00:00','Chuyen khoan'),
('HD15','NV15','HS15',3200000,'2026-01-19 09:00:00','The'),
('HD16','NV16','HS16',3300000,'2026-01-20 09:00:00','Tien mat'),
('HD17','NV17','HS17',3600000,'2026-01-21 09:00:00','Chuyen khoan'),
('HD18','NV18','HS18',4200000,'2026-01-22 09:00:00','The'),
('HD19','NV19','HS19',4800000,'2026-01-23 09:00:00','Tien mat'),
('HD20','NV20','HS20',2300000,'2026-01-24 09:00:00','Chuyen khoan');

-- =====================================
-- 10. PHIEU_LUONG (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Phieu_luong VALUES
('PL01','GV01','NV02','2026-01-01','2026-01-31','2026-02-05',12000000),
('PL02','GV02','NV02','2026-01-01','2026-01-31','2026-02-05',11000000),
('PL03','GV03','NV03','2026-01-01','2026-01-31','2026-02-05',15000000),
('PL04','GV04','NV04','2026-01-01','2026-01-31','2026-02-05',13000000),
('PL05','GV05','NV05','2026-01-01','2026-01-31','2026-02-05',10000000),
('PL06','GV06','NV06','2026-01-01','2026-01-31','2026-02-05',14000000),
('PL07','GV07','NV07','2026-01-01','2026-01-31','2026-02-05',12500000),
('PL08','GV08','NV08','2026-01-01','2026-01-31','2026-02-05',9000000),
('PL09','GV09','NV09','2026-01-01','2026-01-31','2026-02-05',16000000),
('PL10','GV10','NV10','2026-01-01','2026-01-31','2026-02-05',11000000),
('PL11','GV11','NV11','2026-01-01','2026-01-31','2026-02-05',10500000),
('PL12','GV12','NV12','2026-01-01','2026-01-31','2026-02-05',17000000),
('PL13','GV13','NV13','2026-01-01','2026-01-31','2026-02-05',11500000),
('PL14','GV14','NV14','2026-01-01','2026-01-31','2026-02-05',9500000),
('PL15','GV15','NV15','2026-01-01','2026-01-31','2026-02-05',14500000),
('PL16','GV16','NV16','2026-01-01','2026-01-31','2026-02-05',13000000),
('PL17','GV17','NV17','2026-01-01','2026-01-31','2026-02-05',15500000),
('PL18','GV18','NV18','2026-01-01','2026-01-31','2026-02-05',10000000),
('PL19','GV19','NV19','2026-01-01','2026-01-31','2026-02-05',13500000),
('PL20','GV20','NV20','2026-01-01','2026-01-31','2026-02-05',18000000);

-- =====================================
-- 11. LICH_HOC (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Lich_hoc VALUES
('LH01','GV01','L01','2026-02-01 08:00:00','Ca 1','P101','Ngu am','Day'),
('LH02','GV02','L02','2026-02-01 10:00:00','Ca 2','P102','Ngu phap','Day'),
('LH03','GV03','L03','2026-02-02 08:00:00','Ca 1','P103','Doc hieu','Day'),
('LH04','GV04','L04','2026-02-02 10:00:00','Ca 2','P104','Tu vung','Nghi'),
('LH05','GV05','L05','2026-02-03 08:00:00','Ca 1','P105','Luyen de','Day'),
('LH06','GV06','L06','2026-02-03 10:00:00','Ca 2','P106','Hoi thoai','Day'),
('LH07','GV07','L07','2026-02-04 08:00:00','Ca 1','P107','Giao tiep','Day'),
('LH08','GV08','L08','2026-02-04 10:00:00','Ca 2','P108','Luyen thi','Nghi'),
('LH09','GV09','L09','2026-02-05 08:00:00','Ca 1','P109','Ngu phap','Day'),
('LH10','GV10','L10','2026-02-05 10:00:00','Ca 2','P110','Kanji','Day'),
('LH11','GV11','L11','2026-02-06 08:00:00','Ca 1','P111','Thuong mai','Day'),
('LH12','GV12','L12','2026-02-06 10:00:00','Ca 2','P112','Du hoc','Day'),
('LH13','GV13','L13','2026-02-07 08:00:00','Ca 1','P113','Ngu phap','Nghi'),
('LH14','GV14','L14','2026-02-07 10:00:00','Ca 2','P114','Luyen de','Day'),
('LH15','GV15','L15','2026-02-08 08:00:00','Ca 1','P115','Doc hieu','Day'),
('LH16','GV16','L16','2026-02-08 10:00:00','Ca 2','P116','Nghe','Day'),
('LH17','GV17','L17','2026-02-09 08:00:00','Ca 1','P117','Kanji','Day'),
('LH18','GV18','L18','2026-02-09 10:00:00','Ca 2','P118','Kanji','Day'),
('LH19','GV19','L19','2026-02-10 08:00:00','Ca 1','P119','Tieng Nhat IT','Nghi'),
('LH20','GV20','L20','2026-02-10 10:00:00','Ca 2','P120','Hoi thoai','Day');

-- =====================================
-- 12. DANH_GIA_VA_PHAN_HOI (20 BAN GHI)
-- =====================================
INSERT IGNORE INTO Danh_gia_va_phan_hoi VALUES
('DG01','HS01','L01',5,'Giao vien giang day rat de hieu','2026-02-11 09:00:00'),
('DG02','HS02','L02',4,'Bai giang tot','2026-02-11 10:00:00'),
('DG03','HS03','L03',3,'Can them bai tap','2026-02-11 11:00:00'),
('DG04','HS04','L04',2,'Lop hoc hoi nhanh','2026-02-11 12:00:00'),
('DG05','HS05','L05',5,'Rat hai long','2026-02-12 09:00:00'),
('DG06','HS06','L06',4,'Giao vien nhiet tinh','2026-02-12 10:00:00'),
('DG07','HS07','L07',3,'Noi dung binh thuong','2026-02-12 11:00:00'),
('DG08','HS08','L08',1,'Can cai thien tai lieu','2026-02-12 12:00:00'),
('DG09','HS09','L09',5,'Moi thu deu tot','2026-02-13 09:00:00'),
('DG10','HS10','L10',4,'Co nhieu vi du hay','2026-02-13 10:00:00'),
('DG11','HS11','L11',3,'Can them thoi gian','2026-02-13 11:00:00'),
('DG12','HS12','L12',2,'Phong hoc hoi chat','2026-02-13 12:00:00'),
('DG13','HS13','L13',5,'Rat tot','2026-02-14 09:00:00'),
('DG14','HS14','L14',4,'Chat luong tot','2026-02-14 10:00:00'),
('DG15','HS15','L15',3,'Tam duoc','2026-02-14 11:00:00'),
('DG16','HS16','L16',1,'Can cai thien bai giang','2026-02-14 12:00:00'),
('DG17','HS17','L17',5,'Giao vien rat tot','2026-02-15 09:00:00'),
('DG18','HS18','L18',4,'Noi dung de hieu','2026-02-15 10:00:00'),
('DG19','HS19','L19',2,'Can them thuc hanh','2026-02-15 11:00:00'),
('DG20','HS20','L20',5,'Rat hai long','2026-02-15 12:00:00');

-- =====================================
-- 13. CAP NHAT TINH TRANG KET QUA THI
-- =====================================
SET SQL_SAFE_UPDATES = 0;

UPDATE Ket_qua_thi
SET tinh_trang = CASE
    WHEN trinh_do = 'N5'
         AND (diem_doc_hieu + diem_tu_vung) >= 38
         AND diem_nghe >= 19 AND tong_diem >= 80 THEN 'ĐẠT'
    WHEN trinh_do = 'N4'
         AND (diem_doc_hieu + diem_tu_vung) >= 38
         AND diem_nghe >= 19 AND tong_diem >= 90 THEN 'ĐẠT'
    WHEN trinh_do = 'N3'
         AND diem_doc_hieu >= 19 AND diem_tu_vung >= 19
         AND diem_nghe >= 19 AND tong_diem >= 95 THEN 'ĐẠT'
    WHEN trinh_do = 'N2'
         AND diem_doc_hieu >= 19 AND diem_tu_vung >= 19
         AND diem_nghe >= 19 AND tong_diem >= 90 THEN 'ĐẠT'
    WHEN trinh_do = 'N1'
         AND diem_doc_hieu >= 19 AND diem_tu_vung >= 19
         AND diem_nghe >= 19 AND tong_diem >= 100 THEN 'ĐẠT'
    ELSE 'TRƯỢT'
END;

SET SQL_SAFE_UPDATES = 1;

-- =====================================
-- 14. KIEM TRA SO LUONG DU LIEU
-- =====================================
SELECT 'Nhan_vien' AS ten_bang, COUNT(*) AS so_luong FROM Nhan_vien
UNION ALL
SELECT 'Khoa_hoc', COUNT(*) FROM Khoa_hoc
UNION ALL
SELECT 'Giao_vien', COUNT(*) FROM Giao_vien
UNION ALL
SELECT 'Lop', COUNT(*) FROM Lop
UNION ALL
SELECT 'Quanli', COUNT(*) FROM Quanli
UNION ALL
SELECT 'Hoc_sinh', COUNT(*) FROM Hoc_sinh
UNION ALL
SELECT 'De_thi', COUNT(*) FROM De_thi
UNION ALL
SELECT 'Ket_qua_thi', COUNT(*) FROM Ket_qua_thi
UNION ALL
SELECT 'Hoa_don', COUNT(*) FROM hoa_don
UNION ALL
SELECT 'Phieu_luong', COUNT(*) FROM Phieu_luong
UNION ALL
SELECT 'Lich_hoc', COUNT(*) FROM Lich_hoc
UNION ALL
SELECT 'Danh_gia_va_phan_hoi', COUNT(*) FROM Danh_gia_va_phan_hoi;
