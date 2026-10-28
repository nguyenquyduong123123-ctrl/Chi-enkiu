-- 1. THỐNG KÊ LỜI / LỖ THEO THÁNG
SELECT 
    thang_nam.nam,
    thang_nam.thang,
    COALESCE(SUM(hd.khoan_thu), 0) AS tong_thu_hoc_sinh,
    COALESCE(pl.tong_chi_luong_gv, 0) AS tong_chi_luong_gv,
    (COALESCE(SUM(hd.khoan_thu), 0) - COALESCE(pl.tong_chi_luong_gv, 0)) AS loi_nhuan_rong,
    CASE 
        WHEN (COALESCE(SUM(hd.khoan_thu), 0) - COALESCE(pl.tong_chi_luong_gv, 0)) > 0 THEN 'LỜI'
        WHEN (COALESCE(SUM(hd.khoan_thu), 0) - COALESCE(pl.tong_chi_luong_gv, 0)) < 0 THEN 'LỖ'
        ELSE 'HÒA VỐN'
    END AS trang_thai
FROM (
    SELECT DISTINCT YEAR(ngay_lap) AS nam, MONTH(ngay_lap) AS thang FROM hoa_don
    UNION
    SELECT DISTINCT YEAR(ngay_tra) AS nam, MONTH(ngay_tra) AS thang FROM Phieu_luong
) thang_nam
LEFT JOIN hoa_don hd 
    ON YEAR(hd.ngay_lap) = thang_nam.nam AND MONTH(hd.ngay_lap) = thang_nam.thang
LEFT JOIN (
    SELECT YEAR(ngay_tra) AS nam, MONTH(ngay_tra) AS thang, SUM(tong_luong) AS tong_chi_luong_gv
    FROM Phieu_luong
    GROUP BY YEAR(ngay_tra), MONTH(ngay_tra)
) pl 
    ON pl.nam = thang_nam.nam AND pl.thang = thang_nam.thang
GROUP BY thang_nam.nam, thang_nam.thang, pl.tong_chi_luong_gv
ORDER BY thang_nam.nam DESC, thang_nam.thang DESC;