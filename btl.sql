use BTL;
create table hoa_don(
ma_hoa_don varchar(50) not null primary key,
ma_nhan_vien varchar(50) not null,
ma_ghi_danh varchar(50) not null,
khoan_thu int not null,

ngay_lap timestamp not null,
hinh_thuc varchar(20) not null,
foreign key(ma_nhan_vien) referenclich_hoces Nhan_vien(ma_nhan_vien)

);
create table Nhan_vien(
ma_nhan_vien varchar(50) not null primary key,
ho_ten varchar(50) not null,
vai_tro varchar(50) not null,
luong int not null
);

create table Khoa_hoc(
ma_khoa_hoc varchar(50) not null primary key,
thoi_gian_dang_ki timestamp not null,
ten_khoa_hoc varchar(50) not null ,
trinh_do varchar(50) not null ,
chuyen_de varchar(50) not null ,
giao_trinh varchar(50) not null,
hoc_phi int not null
);

create table Lop(
id_lop varchar(50) not null primary key,
ma_khoa_hoc varchar(50) not null,
ma_nhan_vien varchar(50) not null,

nien_khoa varchar(50) not null,
ten_lop varchar(50) not null,
si_so int,
foreign key (ma_khoa_hoc) references Khoa_hoc(ma_khoa_hoc),
foreign key(ma_nhan_vien) references Nhan_vien(ma_nhan_vien)

);
create table Giao_vien(
id_giao_vien varchar(50) not null primary key,
so_dien_thoai varchar(50) not null,
chung_chi varchar(50) not null,
quoc_tich varchar(50) not null,
ho_ten varchar(50)
);
create table Quanli(
id_lop varchar(50) not null,
id_giao_vien varchar(50) not null,
primary key(id_lop, id_giao_vien),
foreign key(id_lop) references Lop(id_lop),
foreign key(id_giao_vien) references Giao_vien(id_giao_vien)
);

create table Phieu_luong(
ma_phieu_luong varchar(50) primary key,
id_giao_vien varchar(50) not null,
ma_nhan_vien varchar(50) not null,
tu_ngay timestamp not null,
den_ngay timestamp not null,
ngay_tra timestamp not null,
tong_luong int not null,
foreign key (id_giao_vien) references Giao_vien(id_giao_vien),
foreign key(ma_nhan_vien) references Nhan_vien(ma_nhan_vien)
);

create table Lich_hoc(
ma_lich_hoc varchar(50) not null primary key,
id_giao_vien varchar(50) not null,
id_lop varchar(50) not null,

ngay_day timestamp not null,
ca_hoc varchar(50) not null,
phong_hoc varchar(50) not null,
noi_dung varchar(50) not null,
trang_thai_buoi varchar(50) check(trang_thai_buoi in ('nghi', 'day')) not null,
foreign key (id_giao_vien) references Giao_vien(id_giao_vien),
foreign key(id_lop) references Lop(id_lop)
);

create table Hoc_sinh(
ma_sinh_vien varchar(50) not null primary key,
id_giao_vien varchar(50) not null,
ho_ten varchar(50) not null,
ngay_sinh timestamp not null,
trinh_do varchar(50) not null default 'khong biet',
so_dien_thoai varchar(50) not null,
foreign key(id_giao_vien) references Giao_vien(id_giao_vien)
);

create table Danh_gia_va_phan_hoi(
ma_danh_gia varchar(50) not null primary key,
ma_sinh_vien varchar(50) not null,
id_lop varchar(50) not null,
rating int not null check(rating >= 1 and rating <= 5),
mo_ta text not null,
ngay_gui timestamp not null,
foreign key(ma_sinh_vien) references Hoc_sinh(ma_sinh_vien),
foreign key(id_lop) references Lop(id_lop)
);

create table Ket_qua_thi(
ma_bai_lam varchar(50) not null primary key,
ma_sinh_vien varchar(50) not null,
trinh_do varchar(50) not null,
ma_de_thi varchar(50) not null,
diem_doc_hieu int not null,
diem_tu_vung int not null,
diem_nghe int not null,
tong_diem int not null,
tinh_trang varchar(50) not null,
foreign key (ma_sinh_vien) references Hoc_sinh(ma_sinh_vien),
foreign key (ma_de_thi) references De_thi(ma_de_thi)
);

create table De_thi(
ma_de_thi varchar(50) not null primary key,
cap_do varchar(50) not null,
thoi_gian int not null
);
SET SQL_SAFE_UPDATES = 0;

UPDATE Ket_qua_thi  
SET tinh_trang = CASE      
    WHEN trinh_do = 'N5' AND (diem_doc_hieu + diem_tu_vung) >= 38 AND diem_nghe >= 19 and tong_diem >= 80  THEN 'dat'      
     WHEN trinh_do = 'N4' AND (diem_doc_hieu + diem_tu_vung) >= 38 AND diem_nghe >= 19   and tong_diem >= 90 THEN 'dat'    
      WHEN trinh_do = 'N3' AND diem_doc_hieu >= 19 AND diem_tu_vung >= 19 AND diem_nghe >= 19  and tong_diem >= 95 THEN 'dat'    
       WHEN trinh_do = 'N2' AND diem_doc_hieu >= 19 AND diem_tu_vung >= 19 AND diem_nghe >= 19  and tong_diem >= 90 THEN 'dat'    
        WHEN trinh_do = 'N1' AND diem_doc_hieu >= 19 AND diem_tu_vung >= 19 AND diem_nghe >= 19  and tong_diem >= 100 THEN 'dat'    
    ELSE 'truot' 
END;
SET SQL_SAFE_UPDATES = 1;
