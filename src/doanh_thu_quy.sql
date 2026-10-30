-- 2. THỐNG KÊ LỜI / LỖ THEO QUÝ
SELECT 
    quy_nam.nam,
    quy_nam.quy,
    COALESCE(SUM(hd.khoan_thu), 0) AS tong_thu_hoc_sinh,
    COALESCE(pl.tong_chi_luong_gv, 0) AS tong_chi_luong_gv,
    (COALESCE(SUM(hd.khoan_thu), 0) - COALESCE(pl.tong_chi_luong_gv, 0)) AS loi_nhuan_rong,
    CASE 
        WHEN (COALESCE(SUM(hd.khoan_thu), 0) - COALESCE(pl.tong_chi_luong_gv, 0)) > 0 THEN 'LỜI'
        WHEN (COALESCE(SUM(hd.khoan_thu), 0) - COALESCE(pl.tong_chi_luong_gv, 0)) < 0 THEN 'LỖ'
        ELSE 'HÒA VỐN'
    END AS trang_thai
FROM (
    SELECT DISTINCT YEAR(ngay_lap) AS nam, QUARTER(ngay_lap) AS quy FROM hoa_don
    UNION
    SELECT DISTINCT YEAR(ngay_tra) AS nam, QUARTER(ngay_tra) AS quy FROM Phieu_luong
) quy_nam
LEFT JOIN hoa_don hd 
    ON YEAR(hd.ngay_lap) = quy_nam.nam AND QUARTER(hd.ngay_lap) = quy_nam.quy
LEFT JOIN (
    SELECT YEAR(ngay_tra) AS nam, QUARTER(ngay_tra) AS quy, SUM(tong_luong) AS tong_chi_luong_gv
    FROM Phieu_luong
    GROUP BY YEAR(ngay_tra), QUARTER(ngay_tra)
) pl 
    ON pl.nam = quy_nam.nam AND pl.quy = quy_nam.quy
GROUP BY quy_nam.nam, quy_nam.quy, pl.tong_chi_luong_gv
ORDER BY quy_nam.nam DESC, quy_nam.quy DESC;