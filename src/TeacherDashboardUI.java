import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * CỔNG GIÁO VIÊN S-LINK  (bản làm lại hoàn toàn - Java Swing / JFrame, chạy độc lập, không cần PortalFrame)
 *
 * Cách mở:   new TeacherDashboardUI("GV01").setVisible(true);
 * Yêu cầu:   MySQL Connector/J trong classpath, CSDL "btl". Chạy được trên JDK 8+.
 * Kết nối:   sửa DB_URL / DB_USER / DB_PASS bên dưới, hoặc chạy với
 *            -Ddb.url=... -Ddb.user=... -Ddb.pass=...  (hoặc biến môi trường DB_URL, DB_USER, DB_PASS)
 *
 * CÁC TRANG:
 *   1. Tổng quan        - banner, thẻ thống kê, lịch hôm nay, biểu đồ buổi dạy, tỉ lệ đạt, phân bố đánh giá
 *   2. Lớp phụ trách    - bảng lớp + cửa sổ chi tiết (danh sách học sinh, buổi sắp tới, đánh giá của lớp)
 *   3. Lịch dạy         - 3 chế độ xem: Danh sách / Lịch tuần / Lịch tháng; báo nghỉ & khôi phục buổi dạy
 *   4. Học sinh         - bảng học sinh + hồ sơ chi tiết (kết quả thi, xu hướng điểm, phản hồi)
 *   5. Điểm thi         - thống kê đạt/trượt, phổ điểm, chi tiết từng bài làm so với điều kiện đỗ
 *   6. Phản hồi         - phân bố số sao, lọc theo mức đánh giá, xem toàn văn góp ý
 *   7. Phiếu lương      - biểu đồ lương theo kỳ, chi tiết kỳ lương (số buổi dạy, lương/buổi)
 *   8. Cá nhân          - hồ sơ, chỉ số giảng dạy, cập nhật liên hệ
 *   + Thông báo (chuông), phím tắt Ctrl+1..8 / F5, xuất CSV cho mọi bảng.
 *
 * CÁC LỖI NGHIỆP VỤ ĐÃ XỬ LÝ (kế thừa từ bản cũ):
 *   - Trạng thái buổi học dùng đúng giá trị CSDL: 'Dạy' / 'Nghỉ'.
 *   - Không đổi trạng thái buổi đã diễn ra; chỉ đổi buổi của chính giáo viên (kiểm tra cả ở câu UPDATE).
 *   - "Buổi đã dạy" chỉ đếm buổi 'Dạy' trước thời điểm hiện tại.
 *   - Kết quả thi thiếu cột tinh_trang thì tự tính ĐẠT/TRƯỢT theo đúng quy tắc từng trình độ.
 *   - Sắp xếp bảng theo giá trị thật (ngày, số, tiền), không theo chuỗi đã định dạng.
 *   - Mọi truy vấn chạy nền (SwingWorker), giao diện không bị đơ khi CSDL chậm.
 */
public class TeacherDashboardUI extends JFrame {

    /* ============================ CẤU HÌNH ============================ */

    static final String DB_URL = prop("db.url", "DB_URL",
            "jdbc:mysql://localhost:3306/btl?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh");
    static final String DB_USER = prop("db.user", "DB_USER", "root");
    static final String DB_PASS = prop("db.pass", "DB_PASS", "123456");

    /** Hai giá trị hợp lệ của cột trang_thai_buoi (ràng buộc CHECK trong CSDL). */
    static final String DAY = "Dạy", OFF = "Nghỉ";
    static final String PASS = "ĐẠT", FAIL = "TRƯỢT";

    static String prop(String sys, String env, String def) {
        String v = System.getProperty(sys);
        if (v == null || v.isEmpty()) v = System.getenv(env);
        return v == null || v.isEmpty() ? def : v;
    }

    /* ============================ GIAO DIỆN ============================ */

    private static final String FAMILY = pickFamily();

    static final Color BG = new Color(0xF1F5F9);
    static final Color INK = new Color(0x0F172A);
    static final Color MUTED = new Color(0x64748B);
    static final Color LINE = new Color(0xE2E8F0);
    static final Color TEAL = new Color(0x0D9488);
    static final Color TEAL_D = new Color(0x0F766E);
    static final Color TEAL_L = new Color(0x2DD4BF);
    static final Color GREEN = new Color(0x16A34A);
    static final Color RED = new Color(0xDC2626);
    static final Color AMBER = new Color(0xF59E0B);
    static final Color BLUE = new Color(0x2563EB);
    static final Color VIOLET = new Color(0x7C3AED);
    static final Color PINK = new Color(0xEC4899);
    static final Color SLATE = new Color(0x94A3B8);

    /** Loại thông báo nổi. */
    static final int T_OK = 0, T_WARN = 1, T_ERR = 2, T_INFO = 3;

    static final Locale VI = Locale.forLanguageTag("vi");
    static final String[] DAYS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};
    static final String[] DAYS_S = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter DM_FMT = DateTimeFormatter.ofPattern("dd/MM");
    static final DateTimeFormatter IN_FMT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static String pickFamily() {
        Set<String> have = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI", "SF Pro Text", "Helvetica Neue", "Roboto", "Noto Sans", "Arial"}) {
            if (have.contains(f)) return f;
        }
        return Font.SANS_SERIF;
    }

    static Font font(int style, float size) {
        return new Font(FAMILY, Font.PLAIN, 12).deriveFont(style, size);
    }

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    static Color shade(Color c, double f) {
        return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f), c.getAlpha());
    }

    /** arc = đường kính bo góc */
    static RoundRectangle2D rr(double x, double y, double w, double h, double arc) {
        return new RoundRectangle2D.Double(x, y, w, h, arc, arc);
    }

    /** Bóng đổ mềm nhiều lớp quanh một hình chữ nhật bo góc. */
    static void shadow(Graphics2D g, double x, double y, double w, double h, double arc, double strength) {
        for (int i = 8; i >= 1; i--) {
            int a = (int) Math.max(0, Math.min(255, strength * (9 - i)));
            g.setColor(new Color(15, 23, 42, a));
            g.fill(rr(x - i, y - i + 4, w + 2 * i, h + 2 * i, arc + i * 2));
        }
    }

    static JLabel lbl(String t, Font f, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Vẽ chuỗi căn giữa theo chiều dọc quanh baseline hợp lý. */
    static void text(Graphics2D g, String s, double x, double cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (float) x, (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    /** Vẽ chuỗi căn giữa theo chiều ngang quanh cx. */
    static void textC(Graphics2D g, String s, double cx, double cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    /** Vẽ chuỗi căn phải tại x. */
    static void textR(Graphics2D g, String s, double x, double cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (float) (x - fm.stringWidth(s)), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    /** Cắt chuỗi cho vừa chiều rộng, thêm "…" nếu bị cắt. */
    static String fit(FontMetrics fm, String s, double maxW) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxW) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    static float ease(double t) {
        double u = 1 - Math.max(0, Math.min(1, t));
        return (float) (1 - u * u * u);
    }

    /* ============================ ĐỊNH DẠNG ============================ */

    static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    static String dash(String s) {
        return nz(s).isEmpty() ? "—" : nz(s);
    }

    static String vnd(long n) {
        return String.format(Locale.US, "%,d", n).replace(',', '.') + " ₫";
    }

    static String compact(long v) {
        if (v >= 1_000_000_000L) return trimNum(v / 1e9) + " tỷ";
        if (v >= 1_000_000L) return trimNum(v / 1e6) + " tr";
        if (v >= 1_000L) return trimNum(v / 1e3) + "k";
        return String.valueOf(v);
    }

    private static String trimNum(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.1f", d).replace('.', ',');
    }

    static String dec1(double d) {
        return String.format(Locale.US, "%.1f", d).replace('.', ',');
    }

    static String pct(double d) {
        return Math.round(d * 100) + "%";
    }

    static String stars(int n) {
        n = Math.max(0, Math.min(5, n));
        return "★★★★★".substring(0, n) + "☆☆☆☆☆".substring(0, 5 - n);
    }

    static String rootMsg(Throwable e) {
        while (e.getCause() != null && e.getCause() != e) e = e.getCause();
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String dayName(LocalDate d) {
        return DAYS[d.getDayOfWeek().getValue() - 1];
    }

    static String dateLong(LocalDate d) {
        return dayName(d) + ", " + d.format(D_FMT);
    }

    static String initialsOf(String name) {
        String[] w = nz(name).split("\\s+");
        if (w.length == 0 || w[0].isEmpty()) return "?";
        String a = w[0].substring(0, 1);
        String b = w.length > 1 ? w[w.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase(VI);
    }

    static String greeting() {
        int h = LocalDateTime.now().getHour();
        return h < 11 ? "Chào buổi sáng" : h < 14 ? "Chào buổi trưa" : h < 18 ? "Chào buổi chiều" : "Chào buổi tối";
    }

    static String nowText() {
        LocalDateTime n = LocalDateTime.now();
        return DAYS[n.getDayOfWeek().getValue() - 1] + ", " + n.format(DateTimeFormatter.ofPattern("dd/MM/yyyy  •  HH:mm:ss"));
    }

    static LocalDate ld(Timestamp t) {
        return t == null ? null : t.toLocalDateTime().toLocalDate();
    }

    static Set<Integer> cols(Integer... c) {
        return new HashSet<>(Arrays.asList(c));
    }

    static DocumentListener doc(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }

    static String stripAccents(String s) {
        return Normalizer.normalize(nz(s), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }

    /* ============================ GIÁ TRỊ MIỀN (có thể sắp xếp & xuất CSV) ============================ */

    /** Giá trị hiển thị trong bảng nhưng có dạng khác khi xuất CSV. */
    interface CsvValue {
        String csv();
    }

    /** Tiền - giữ số để sắp xếp đúng, hiển thị dạng 1.200.000 ₫ */
    static final class Money implements Comparable<Money>, CsvValue {
        final long v;
        Money(long v) { this.v = v; }
        public int compareTo(Money o) { return Long.compare(v, o.v); }
        public String toString() { return vnd(v); }
        public String csv() { return String.valueOf(v); }
    }

    /** Ngày giờ - giữ Timestamp để sắp xếp đúng thay vì sắp xếp chuỗi dd/MM/yyyy */
    static final class When implements Comparable<When>, CsvValue {
        final Timestamp t;
        final boolean time;
        When(Timestamp t, boolean time) { this.t = t; this.time = time; }
        public int compareTo(When o) {
            if (t == null) return o.t == null ? 0 : -1;
            if (o.t == null) return 1;
            return t.compareTo(o.t);
        }
        public String toString() {
            return t == null ? "—" : t.toLocalDateTime().format(time ? DT_FMT : D_FMT);
        }
        public String csv() { return toString(); }
        LocalDate date() { return ld(t); }
    }

    /** Số sao 0..5 - vẽ thành 5 ngôi sao trong bảng. */
    static final class Stars implements Comparable<Stars>, CsvValue {
        final int n;
        Stars(int n) { this.n = Math.max(0, Math.min(5, n)); }
        public int compareTo(Stars o) { return Integer.compare(n, o.n); }
        public String toString() { return stars(n); }
        public String csv() { return String.valueOf(n); }
    }

    /** Điểm số kèm thang điểm tối đa - vẽ kèm thanh tiến độ nhỏ. */
    static final class Score implements Comparable<Score>, CsvValue {
        final int v, max;
        Score(int v, int max) { this.v = v; this.max = max; }
        public int compareTo(Score o) { return Integer.compare(v, o.v); }
        public String toString() { return v < 0 ? "—" : String.valueOf(v); }
        public String csv() { return v < 0 ? "" : String.valueOf(v); }
    }

    /** Trình độ JLPT: N1 là cao nhất nên sắp xếp N1 > N2 > ... > N5. */
    static final class Level implements Comparable<Level>, CsvValue {
        final String text;
        final int rank;
        Level(String text) {
            this.text = nz(text).isEmpty() ? "Chưa xác định" : nz(text).toUpperCase(Locale.ROOT).startsWith("N") ? nz(text).toUpperCase(Locale.ROOT) : nz(text);
            this.rank = rankOf(this.text);
        }
        static int rankOf(String t) {
            switch (t) {
                case "N1": return 6;
                case "N2": return 5;
                case "N3": return 4;
                case "N4": return 3;
                case "N5": return 2;
                default: return 1;
            }
        }
        public int compareTo(Level o) { return Integer.compare(rank, o.rank); }
        public String toString() { return text; }
        public String csv() { return text; }
    }

    /** Quy tắc đỗ JLPT theo từng trình độ - khớp với lệnh UPDATE trong btl.sql. */
    static final class Rule {
        static final int SECTION_MAX = 60, TOTAL_MAX = 180, SECTION_MIN = 19;
        final String level;
        final boolean known;
        /** true: Đọc hiểu và Từ vựng được tính riêng (N3-N1); false: gộp Đọc + Từ vựng (N5-N4). */
        final boolean split;
        final int combinedMin, listenMin, totalMin;

        private Rule(String level, boolean known, boolean split, int combinedMin, int listenMin, int totalMin) {
            this.level = level;
            this.known = known;
            this.split = split;
            this.combinedMin = combinedMin;
            this.listenMin = listenMin;
            this.totalMin = totalMin;
        }

        static Rule of(String lv) {
            switch (nz(lv).toUpperCase(Locale.ROOT)) {
                case "N5": return new Rule("N5", true, false, 38, 19, 80);
                case "N4": return new Rule("N4", true, false, 38, 19, 90);
                case "N3": return new Rule("N3", true, true, 19, 19, 95);
                case "N2": return new Rule("N2", true, true, 19, 19, 90);
                case "N1": return new Rule("N1", true, true, 19, 19, 100);
                default: return new Rule(nz(lv), false, true, 19, 19, 100);
            }
        }

        boolean pass(int dh, int tv, int ng, int tong) {
            return unmet(dh, tv, ng, tong).isEmpty();
        }

        /** Danh sách điều kiện chưa đạt (rỗng nếu đỗ). */
        List<String> unmet(int dh, int tv, int ng, int tong) {
            List<String> out = new ArrayList<>();
            if (!known) {
                out.add("Trình độ \"" + dash(level) + "\" không xác định nên không thể chấm đạt.");
                return out;
            }
            if (split) {
                if (dh < combinedMin) out.add("Đọc hiểu " + dh + " < " + combinedMin);
                if (tv < combinedMin) out.add("Từ vựng " + tv + " < " + combinedMin);
            } else if (dh + tv < combinedMin) {
                out.add("Đọc hiểu + Từ vựng " + (dh + tv) + " < " + combinedMin);
            }
            if (ng < listenMin) out.add("Nghe " + ng + " < " + listenMin);
            if (tong < totalMin) out.add("Tổng điểm " + tong + " < " + totalMin);
            return out;
        }

        String describe() {
            if (!known) return "Chưa có quy tắc cho trình độ này";
            return (split ? "Đọc hiểu ≥ " + combinedMin + ", Từ vựng ≥ " + combinedMin
                    : "Đọc hiểu + Từ vựng ≥ " + combinedMin)
                    + ", Nghe ≥ " + listenMin + ", Tổng ≥ " + totalMin;
        }
    }

    static String result(String lv, int dh, int tv, int ng, int tong) {
        return Rule.of(lv).pass(dh, tv, ng, tong) ? PASS : FAIL;
    }

    /** Một buổi dạy trong bảng Lich_hoc. */
    static final class Session {
        final String ma, lopId, lop, ca, phong, noiDung;
        final LocalDate ngay;
        final Timestamp raw;
        String status;

        Session(String ma, String lopId, String lop, Timestamp raw, String ca, String phong, String noiDung, String status) {
            this.ma = nz(ma);
            this.lopId = nz(lopId);
            this.lop = nz(lop);
            this.raw = raw;
            this.ngay = ld(raw);
            this.ca = nz(ca);
            this.phong = nz(phong);
            this.noiDung = nz(noiDung);
            this.status = nz(status);
        }

        boolean off() { return OFF.equals(status); }

        boolean past() { return ngay != null && ngay.isBefore(LocalDate.now()); }

        boolean today() { return ngay != null && ngay.equals(LocalDate.now()); }

        String label() { return ma + "  •  " + lop + "  •  " + (ngay == null ? "—" : ngay.format(D_FMT)); }
    }

    /* ============================ TRUY CẬP DỮ LIỆU ============================ */

    interface RowMapper {
        Object[] map(ResultSet rs) throws SQLException;
    }

    static final class Db {
        /** Cho biết bảng Hoc_sinh_lop có dùng được không (bảng này do cổng nhân viên tạo). */
        static volatile boolean hasStudentClass = true;

        static Connection open() throws SQLException {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }

        private static void bind(PreparedStatement ps, Object[] p) throws SQLException {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
        }

        static List<Object[]> query(String sql, RowMapper m, Object... p) throws SQLException {
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, p);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Object[]> out = new ArrayList<>();
                    while (rs.next()) out.add(m.map(rs));
                    return out;
                }
            }
        }

        static long scalar(String sql, Object... p) throws SQLException {
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, p);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            }
        }

        /** Trung bình / số thực; trả về -1 nếu không có dữ liệu (AVG của tập rỗng là NULL). */
        static double decimal(String sql, Object... p) throws SQLException {
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, p);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return -1;
                    double v = rs.getDouble(1);
                    return rs.wasNull() ? -1 : v;
                }
            }
        }

        static String string(String sql, Object... p) throws SQLException {
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, p);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        }

        static int update(String sql, Object... p) throws SQLException {
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, p);
                return ps.executeUpdate();
            }
        }

        /** Bảo đảm bảng Hoc_sinh_lop tồn tại (giống cổng nhân viên). Lỗi thì chỉ tắt tính năng liên quan. */
        static void ensureSchema() {
            try (Connection c = open(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS Hoc_sinh_lop (id_lop VARCHAR(50) NOT NULL, "
                        + "ma_sinh_vien VARCHAR(50) NOT NULL, ngay_vao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "PRIMARY KEY (id_lop, ma_sinh_vien), FOREIGN KEY (id_lop) REFERENCES Lop(id_lop), "
                        + "FOREIGN KEY (ma_sinh_vien) REFERENCES Hoc_sinh(ma_sinh_vien))");
                hasStudentClass = true;
            } catch (SQLException e) {
                hasStudentClass = false;
            }
        }
    }

    static Object when(ResultSet rs, int i, boolean time) throws SQLException {
        return new When(rs.getTimestamp(i), time);
    }

    static Integer intObj(ResultSet rs, int i) throws SQLException {
        int v = rs.getInt(i);
        return rs.wasNull() ? null : v;
    }

    static int ni(ResultSet rs, int i) throws SQLException {
        return rs.getInt(i);
    }

    static final String SESSION_SELECT =
            "SELECT lh.ma_lich_hoc, lh.id_lop, COALESCE(l.ten_lop, lh.id_lop), lh.ngay_day, lh.ca_hoc, lh.phong_hoc, "
                    + "lh.noi_dung, lh.trang_thai_buoi FROM Lich_hoc lh LEFT JOIN Lop l ON lh.id_lop = l.id_lop ";

    static Object[] sessionRow(ResultSet rs) throws SQLException {
        return new Object[]{new Session(rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8))};
    }

    /** Chạy truy vấn nền, trả kết quả về luồng giao diện. */
    static <T> void async(Callable<T> job, Consumer<T> ok) {
        async(job, ok, null);
    }

    static <T> void async(Callable<T> job, Consumer<T> ok, Consumer<Exception> fail) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override protected void done() {
                T val;
                try {
                    val = get();
                } catch (Exception e) {
                    if (fail != null) fail.accept(e);
                    else toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR);
                    return;
                }
                ok.accept(val);
            }
        }.execute();
    }

    /* ============================ HIỆU ỨNG ============================ */

    /** Giá trị số chuyển động mượt (ease-out) - dùng cho hover, fade, đếm số... */
    static final class Tween {
        private double v;
        private Timer timer;
        private final Runnable change;

        Tween(double init, Runnable change) {
            this.v = init;
            this.change = change;
        }

        double get() {
            return v;
        }

        void set(double x) {
            if (timer != null) timer.stop();
            v = x;
            change.run();
        }

        void to(double target, int ms) {
            if (timer != null) timer.stop();
            final double from = v;
            final long t0 = System.nanoTime();
            timer = new Timer(14, null);
            timer.addActionListener(e -> {
                double t = Math.min(1.0, (System.nanoTime() - t0) / (ms * 1_000_000.0));
                v = from + (target - from) * (1 - Math.pow(1 - t, 3));
                change.run();
                if (t >= 1) timer.stop();
            });
            timer.start();
        }
    }

    /** Chỉ chạy Timer khi component đang hiển thị (tiết kiệm CPU khi chuyển trang). */
    static void runWhileShowing(JComponent c, Timer t) {
        c.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (c.isShowing()) t.start();
                else t.stop();
            }
        });
    }

    /** Thêm hiệu ứng hover mượt cho một component và trả về Tween 0..1. */
    static Tween hoverOf(JComponent c) {
        Tween tw = new Tween(0, c::repaint);
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { tw.to(1, 160); }
            @Override public void mouseExited(MouseEvent e) { tw.to(0, 240); }
        });
        return tw;
    }

    /* ============================ ICON VECTOR ============================ */

    static final class Icons {
        private static final Pattern TOK = Pattern.compile("([MLHVCZ])([^MLHVCZ]*)");

        static Path2D path(String d) {
            Path2D.Double p = new Path2D.Double();
            java.util.regex.Matcher m = TOK.matcher(d);
            double cx = 0, cy = 0;
            while (m.find()) {
                char cmd = m.group(1).charAt(0);
                String a = m.group(2).trim();
                double[] n = a.isEmpty() ? new double[0]
                        : Arrays.stream(a.split("[ ,]+")).mapToDouble(Double::parseDouble).toArray();
                switch (cmd) {
                    case 'M': p.moveTo(n[0], n[1]); cx = n[0]; cy = n[1]; break;
                    case 'L': p.lineTo(n[0], n[1]); cx = n[0]; cy = n[1]; break;
                    case 'H': p.lineTo(n[0], cy); cx = n[0]; break;
                    case 'V': p.lineTo(cx, n[0]); cy = n[0]; break;
                    case 'C': p.curveTo(n[0], n[1], n[2], n[3], n[4], n[5]); cx = n[4]; cy = n[5]; break;
                    default: p.closePath();
                }
            }
            return p;
        }

        /** Ngôi sao 5 cánh tâm (cx, cy) bán kính ngoài r. */
        static Path2D star(double cx, double cy, double r) {
            Path2D.Double p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5, rad = i % 2 == 0 ? r : r * 0.45;
                double px = cx + rad * Math.cos(a), py = cy + rad * Math.sin(a);
                if (i == 0) p.moveTo(px, py);
                else p.lineTo(px, py);
            }
            p.closePath();
            return p;
        }

        static void paint(Graphics2D g0, String t, double x, double y, double s, Color c) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.translate(x, y);
            g.scale(s / 24.0, s / 24.0);
            g.setColor(c);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (t) {
                case "home":
                    g.draw(path("M3 11L12 3L21 11M5 9.5V20H19V9.5M10 20V14H14V20")); break;
                case "wallet":
                    g.draw(rr(3, 6, 18, 14, 8)); g.draw(path("M3 10H21M15.5 15H17.5")); break;
                case "group":
                    g.draw(new Ellipse2D.Double(6, 4.5, 6.5, 6.5)); g.draw(path("M3 20C3 15 15.5 15 15.5 20"));
                    g.draw(new Ellipse2D.Double(14.5, 6.5, 5, 5)); g.draw(path("M17 14.5C19.5 14 21.5 16 21.5 19")); break;
                case "book":
                    g.draw(path("M4 5C8 4 10 5 12 7C14 5 16 4 20 5V19C16 18 14 19 12 21C10 19 8 18 4 19ZM12 7V21")); break;
                case "list":
                    g.draw(path("M8 6H20M8 12H20M8 18H20"));
                    for (int i = 0; i < 3; i++) g.fill(new Ellipse2D.Double(2.8, 4.8 + i * 6, 2.4, 2.4));
                    break;
                case "calendar":
                    g.draw(rr(3, 5, 18, 16, 6)); g.draw(path("M3 10H21M8 3V7M16 3V7"));
                    g.fill(new Ellipse2D.Double(7, 13, 2, 2)); g.fill(new Ellipse2D.Double(11, 13, 2, 2));
                    g.fill(new Ellipse2D.Double(15, 13, 2, 2)); g.fill(new Ellipse2D.Double(7, 16.5, 2, 2));
                    break;
                case "award":
                    g.draw(new Ellipse2D.Double(6, 2.5, 12, 12)); g.draw(path("M8.5 14L7 21L12 18.5L17 21L15.5 14")); break;
                case "star":
                    g.draw(star(12, 12.8, 10)); break;
                case "user":
                    g.draw(new Ellipse2D.Double(8, 3.5, 8, 8)); g.draw(path("M4 21C4 15 20 15 20 21")); break;
                case "search":
                    g.draw(new Ellipse2D.Double(4, 4, 12, 12)); g.draw(path("M14 14L20 20")); break;
                case "chevron":
                    g.draw(path("M6 9L12 15L18 9")); break;
                case "chevronL":
                    g.draw(path("M15 6L9 12L15 18")); break;
                case "chevronR":
                    g.draw(path("M9 6L15 12L9 18")); break;
                case "check":
                    g.draw(path("M5 12.5L10 17.5L19 7")); break;
                case "plus":
                    g.draw(path("M12 5V19M5 12H19")); break;
                case "x":
                    g.draw(path("M6 6L18 18M18 6L6 18")); break;
                case "undo":
                    g.draw(path("M9 14L4 9L9 4M4 9H14C18 9 20 11 20 14C20 17 18 19 14 19H9")); break;
                case "logout":
                    g.draw(path("M9 4H5V20H9M14 8L18 12L14 16M18 12H8")); break;
                case "clock":
                    g.draw(new Ellipse2D.Double(3, 3, 18, 18)); g.draw(path("M12 7V12L15.5 14")); break;
                case "inbox":
                    g.draw(path("M3 13L6 5H18L21 13V19H3ZM3 13H9C9 15 10.5 16 12 16C13.5 16 15 15 15 13H21")); break;
                case "alert":
                    g.draw(path("M12 3L22 20H2ZM12 10V14M12 17V17.2")); break;
                case "download":
                    g.draw(path("M12 4V15M7 11L12 16L17 11M5 20H19")); break;
                case "eye":
                    g.draw(path("M2 12C5 6 9 5 12 5C15 5 19 6 22 12C19 18 15 19 12 19C9 19 5 18 2 12Z"));
                    g.draw(new Ellipse2D.Double(9, 9, 6, 6)); break;
                case "edit":
                    g.draw(path("M4 20H8L19 9L15 5L4 16ZM13 7L17 11")); break;
                case "refresh":
                    g.draw(new Arc2D.Double(4, 4, 16, 16, 40, 270, Arc2D.OPEN));
                    g.draw(path("M18 3V8H13")); break;
                case "bell":
                    g.draw(path("M6 17V11C6 7.5 8.5 5 12 5C15.5 5 18 7.5 18 11V17L20 19H4ZM10 21H14")); break;
                case "pin":
                    g.draw(path("M12 21C7 15 5 12 5 9C5 5.5 8 3 12 3C16 3 19 5.5 19 9C19 12 17 15 12 21Z"));
                    g.draw(new Ellipse2D.Double(9.5, 6.5, 5, 5)); break;
                case "phone":
                    g.draw(path("M5 4H9L11 9L8.5 10.5C9.5 13 11 14.5 13.5 15.5L15 13L20 15V19C20 20 19 21 18 21C10 21 3 14 3 6C3 5 4 4 5 4Z")); break;
                case "globe":
                    g.draw(new Ellipse2D.Double(3, 3, 18, 18)); g.draw(path("M3 12H21M12 3C8 8 8 16 12 21M12 3C16 8 16 16 12 21")); break;
                case "trend":
                    g.draw(path("M3 17L9 11L13 15L21 7M15 7H21V13")); break;
                case "cert":
                    g.draw(rr(3, 4, 18, 13, 5)); g.draw(path("M7 9H17M7 12.5H12M15 17L14 21L16.5 19.5L19 21L18 17"));
                    break;
                case "filter":
                    g.draw(path("M3 5H21L14 13V20L10 18V13Z")); break;
                case "table":
                    g.draw(rr(3, 4, 18, 16, 6)); g.draw(path("M3 10H21M9 10V20")); break;
                case "grid":
                    g.draw(rr(3, 3, 8, 8, 3)); g.draw(rr(13, 3, 8, 8, 3));
                    g.draw(rr(3, 13, 8, 8, 3)); g.draw(rr(13, 13, 8, 8, 3)); break;
                case "door":
                    g.draw(path("M6 21V4H18V21M3 21H21")); g.fill(new Ellipse2D.Double(14, 12, 2, 2)); break;
                case "money":
                    g.draw(rr(2, 6, 20, 12, 6)); g.draw(new Ellipse2D.Double(9, 9, 6, 6)); break;
                case "info":
                    g.draw(new Ellipse2D.Double(3, 3, 18, 18)); g.draw(path("M12 11V17M12 7.5V7.7")); break;
                default:
                    g.draw(new Ellipse2D.Double(4, 4, 16, 16));
            }
            g.dispose();
        }
    }

    /* ============================ WIDGET CƠ BẢN ============================ */

    /** Nút bo tròn có hiệu ứng hover / nhấn; ghost = viền màu nền trắng. */
    static final class FlatButton extends JButton {
        private final Tween hover = new Tween(0, this::repaint);
        private final String icon;
        private final boolean ghost;
        private final Color base;

        FlatButton(String text, String icon, Color base, boolean ghost) {
            super(text);
            this.icon = icon;
            this.base = base;
            this.ghost = ghost;
            setFont(font(Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) hover.to(1, 160); }
                @Override public void mouseExited(MouseEvent e) { hover.to(0, 220); }
            });
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 44 + (icon != null ? 24 : 0), 42);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            double hv = hover.get();
            boolean en = isEnabled();
            int dy = getModel().isPressed() && en ? 1 : 0;
            Color fill, fg, border = null;
            if (!en) {
                fill = new Color(238, 240, 244);
                fg = new Color(160, 168, 180);
            } else if (ghost) {
                fill = lerp(Color.WHITE, alpha(base, 24), hv);
                fg = base;
                border = base;
            } else {
                fill = lerp(base, shade(base, 0.82), hv);
                fg = Color.WHITE;
                g.setColor(alpha(base, (int) (70 * hv)));
                g.fill(rr(3, 6 + dy, w - 6, h - 6, 28));
            }
            g.translate(0, dy - hv * 1.5);
            g.setColor(fill);
            g.fill(rr(0, 0, w, h - 3, 26));
            if (border != null) {
                g.setColor(alpha(border, 160));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(rr(.7, .7, w - 1.4, h - 4.4, 26));
            }
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(getText());
            int total = tw + (icon != null ? 24 : 0);
            int x = (w - total) / 2;
            if (icon != null) {
                Icons.paint(g, icon, x, (h - 3 - 17) / 2.0, 17, fg);
                x += 24;
            }
            g.setColor(fg);
            g.drawString(getText(), x, (h - 3 + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Nút tròn chỉ có icon (dùng cho chuông, điều hướng tuần / tháng...). */
    static final class IconButton extends JButton {
        private final Tween hover = new Tween(0, this::repaint);
        private final String icon;
        private int badge;

        IconButton(String icon, String tip) {
            this.icon = icon;
            setToolTipText(tip);
            setPreferredSize(new Dimension(42, 42));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover.to(1, 140); }
                @Override public void mouseExited(MouseEvent e) { hover.to(0, 200); }
            });
        }

        void setBadge(int n) {
            badge = n;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            double hv = hover.get();
            g.setColor(lerp(new Color(241, 245, 249), new Color(204, 251, 241), hv));
            g.fill(new Ellipse2D.Double(1, 1, w - 2, h - 2));
            Icons.paint(g, icon, (w - 20) / 2.0, (h - 20) / 2.0 + (getModel().isPressed() ? 1 : 0), 20,
                    lerp(MUTED, TEAL_D, hv));
            if (badge > 0) {
                String b = badge > 9 ? "9+" : String.valueOf(badge);
                g.setFont(font(Font.BOLD, 10));
                FontMetrics fm = g.getFontMetrics();
                int bw = Math.max(16, fm.stringWidth(b) + 8);
                g.setColor(Color.WHITE);
                g.fill(rr(w - bw - 1, 1, bw + 2, 18, 18));
                g.setColor(RED);
                g.fill(rr(w - bw, 2, bw, 16, 16));
                g.setColor(Color.WHITE);
                g.drawString(b, w - bw + (bw - fm.stringWidth(b)) / 2, 2 + (16 + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }
    }

    /** Ô nhập bo tròn, có viền sáng khi focus + placeholder + icon */
    static class StyledField extends JTextField {
        private final Tween focus = new Tween(0, this::repaint);
        private String placeholder = "";
        private String icon;

        StyledField(String text) {
            super(text);
            setOpaque(false);
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setCaretColor(TEAL);
            setSelectionColor(alpha(TEAL, 70));
            setBorder(new EmptyBorder(9, 14, 9, 14));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focus.to(1, 180); }
                @Override public void focusLost(FocusEvent e) { focus.to(0, 220); }
            });
        }

        StyledField withPlaceholder(String p, String icon) {
            this.placeholder = p;
            this.icon = icon;
            setBorder(new EmptyBorder(9, icon != null ? 38 : 14, 9, 14));
            return this;
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = 42;
            return d;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            double f = focus.get();
            g.setColor(new Color(248, 250, 252));
            g.fill(rr(0, 0, w, h, 26));
            if (f > 0.01) {
                g.setColor(alpha(TEAL, (int) (45 * f)));
                g.setStroke(new BasicStroke(4f));
                g.draw(rr(1.5, 1.5, w - 3, h - 3, 26));
            }
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(lerp(new Color(203, 213, 225), TEAL, f));
            g.draw(rr(.7, .7, w - 1.4, h - 1.4, 26));
            if (icon != null) Icons.paint(g, icon, 14, (h - 16) / 2.0, 16, lerp(new Color(148, 163, 184), TEAL, f));
            if (getText().isEmpty() && !placeholder.isEmpty()) {
                g.setFont(getFont());
                g.setColor(new Color(148, 163, 184));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(placeholder, getInsets().left, (h + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Ô nhập nhiều dòng bo tròn (dùng cho ghi chú / xem toàn văn). */
    static final class StyledArea extends JTextArea {
        StyledArea(String text, int rows) {
            super(text, rows, 30);
            setOpaque(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setCaretColor(TEAL);
            setBorder(new EmptyBorder(12, 14, 12, 14));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(new Color(248, 250, 252));
            g.fill(rr(0, 0, getWidth(), getHeight(), 22));
            g.setColor(new Color(203, 213, 225));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(rr(.6, .6, getWidth() - 1.2, getHeight() - 1.2, 22));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** ComboBox bo tròn */
    static final class RoundCombo extends JComboBox<String> {
        RoundCombo(String[] items) {
            super(items);
            setUI(new BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton b = new JButton() {
                        @Override protected void paintComponent(Graphics g0) {
                            Graphics2D g = (Graphics2D) g0.create();
                            Icons.paint(g, "chevron", getWidth() / 2.0 - 8, getHeight() / 2.0 - 8, 16, MUTED);
                            g.dispose();
                        }
                    };
                    b.setBorder(null);
                    b.setContentAreaFilled(false);
                    b.setFocusPainted(false);
                    b.setOpaque(false);
                    return b;
                }

                @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }
            });
            setOpaque(false);
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setBorder(new EmptyBorder(4, 8, 4, 4));
            setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                         boolean sel, boolean foc) {
                    list.setSelectionBackground(new Color(204, 251, 241));
                    list.setSelectionForeground(INK);
                    JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, sel, foc);
                    l.setOpaque(index >= 0);
                    l.setForeground(INK);
                    l.setFont(font(Font.PLAIN, 14));
                    l.setBorder(new EmptyBorder(8, 10, 8, 10));
                    return l;
                }
            });
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = 42;
            return d;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(new Color(248, 250, 252));
            g.fill(rr(0, 0, getWidth(), getHeight(), 26));
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(new Color(203, 213, 225));
            g.draw(rr(.7, .7, getWidth() - 1.4, getHeight() - 1.4, 26));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Bộ chọn dạng "viên thuốc" có hiệu ứng trượt */
    static final class Segmented extends JComponent {
        private final String[] labels;
        private final int[] left, wid;
        private final Tween pos = new Tween(0, this::repaint);
        private final IntConsumer cb;
        private int sel = 0;

        Segmented(String[] labels, IntConsumer cb) {
            this.labels = labels;
            this.cb = cb;
            FontMetrics fm = getFontMetrics(font(Font.BOLD, 13));
            left = new int[labels.length];
            wid = new int[labels.length];
            int x = 4;
            for (int i = 0; i < labels.length; i++) {
                wid[i] = fm.stringWidth(labels[i]) + 28;
                left[i] = x;
                x += wid[i];
            }
            setPreferredSize(new Dimension(x + 4, 42));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    for (int i = 0; i < labels.length; i++) {
                        if (e.getX() >= left[i] && e.getX() < left[i] + wid[i] && i != sel) {
                            select(i, true);
                        }
                    }
                }
            });
        }

        int selected() {
            return sel;
        }

        /** Chọn theo mã; fire = true thì gọi callback. */
        void select(int i, boolean fire) {
            if (i < 0 || i >= labels.length) return;
            sel = i;
            pos.to(i, 260);
            if (fire) cb.accept(i);
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(226, 240, 238));
            g.fill(rr(0, 0, w, h, 26));
            double p = pos.get();
            int lo = Math.max(0, Math.min(labels.length - 1, (int) Math.floor(p)));
            int hi = Math.min(labels.length - 1, lo + 1);
            double fr = p - lo;
            double px = left[lo] + (left[hi] - left[lo]) * fr;
            double pw = wid[lo] + (wid[hi] - wid[lo]) * fr;
            shadow(g, px, 4, pw, h - 8, 22, 0.5);
            g.setColor(Color.WHITE);
            g.fill(rr(px, 4, pw, h - 8, 22));
            g.setFont(font(Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < labels.length; i++) {
                double near = Math.max(0, 1 - Math.abs(p - i));
                g.setColor(lerp(MUTED, TEAL_D, near));
                g.drawString(labels[i], left[i] + (wid[i] - fm.stringWidth(labels[i])) / 2,
                        (h + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }
    }

    /** Thẻ trắng có bóng đổ - container chính của mọi khối nội dung */
    static class Card extends JPanel {
        static final int P = 12;

        Card(String title, String subtitle, JComponent right) {
            super(new BorderLayout(0, 14));
            setOpaque(false);
            setBorder(new EmptyBorder(P + 18, P + 22, P + 18, P + 22));
            if (title != null) {
                JPanel head = new JPanel(new BorderLayout(16, 0));
                head.setOpaque(false);
                JPanel t = new JPanel();
                t.setOpaque(false);
                t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
                t.add(lbl(title, font(Font.BOLD, 17), INK));
                if (subtitle != null) t.add(lbl(subtitle, font(Font.PLAIN, 12), MUTED));
                head.add(t, BorderLayout.WEST);
                if (right != null) head.add(right, BorderLayout.EAST);
                add(head, BorderLayout.NORTH);
            }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            double w = getWidth() - 2 * P, h = getHeight() - 2 * P;
            shadow(g, P, P, w, h, 28, 0.85);
            g.setColor(Color.WHITE);
            g.fill(rr(P, P, w, h, 28));
            g.dispose();
        }
    }

    /** Thẻ thống kê: đếm số chạy + nâng lên khi hover */
    static final class StatCard extends JComponent {
        private final String title, icon;
        private final Color color;
        private String sub = " ";
        private String fixed;
        private long target;
        private boolean loaded;
        private LongFunction<String> fmt = v -> String.valueOf(v);
        private final Tween hover = new Tween(0, this::repaint);
        private final Tween count = new Tween(0, this::repaint);

        StatCard(String title, String icon, Color color) {
            this.title = title;
            this.icon = icon;
            this.color = color;
            setPreferredSize(new Dimension(220, 140));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover.to(1, 180); }
                @Override public void mouseExited(MouseEvent e) { hover.to(0, 240); }
            });
        }

        /** Số đếm chạy từ 0 đến v; f định dạng số hiển thị. */
        void setValue(long v, LongFunction<String> f, String sub) {
            this.target = v;
            this.fmt = f;
            this.sub = sub;
            this.fixed = null;
            this.loaded = true;
            count.set(0);
            count.to(1, 1000);
        }

        /** Hiện chữ cố định (không hiệu ứng đếm) - dùng cho "Chưa có". */
        void setText(String value, String sub) {
            this.fixed = value;
            this.sub = sub;
            this.loaded = true;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int P = Card.P;
            double hv = hover.get();
            double x = P, y = P - hv * 4, w = getWidth() - 2 * P, h = getHeight() - 2 * P;
            shadow(g, x, y, w, h, 28, 0.85 + hv * 0.9);
            g.setColor(Color.WHITE);
            g.fill(rr(x, y, w, h, 28));
            Shape old = g.getClip();
            g.clip(rr(x, y, w, h, 28));
            g.setColor(alpha(color, 22));
            g.fill(new Ellipse2D.Double(x + w - 70, y - 40, 120, 120));
            g.setClip(old);
            double bs = 46, bx = x + w - bs - 18, by = y + 18;
            g.setPaint(new GradientPaint((float) bx, (float) by, lerp(color, Color.WHITE, 0.25),
                    (float) (bx + bs), (float) (by + bs), shade(color, 0.85)));
            g.fill(rr(bx, by, bs, bs, 28));
            Icons.paint(g, icon, bx + 12, by + 12, 22, Color.WHITE);
            g.setFont(font(Font.PLAIN, 13));
            g.setColor(MUTED);
            g.drawString(title, (int) x + 20, (int) y + 34);
            String val = !loaded ? "···" : fixed != null ? fixed : fmt.apply((long) (target * count.get()));
            float fs = 28f;
            Font vf = font(Font.BOLD, fs);
            double maxW = w - 40;
            while (g.getFontMetrics(vf).stringWidth(val) > maxW && fs > 14) vf = font(Font.BOLD, fs -= 1f);
            g.setFont(vf);
            g.setColor(INK);
            g.drawString(val, (int) x + 20, (int) y + 76);
            g.setFont(font(Font.PLAIN, 12));
            g.setColor(shade(color, 0.9));
            g.drawString(fit(g.getFontMetrics(), sub, w - 40), (int) x + 20, (int) y + 100);
            g.dispose();
        }
    }

    /** Banner chào mừng có các hình tròn chuyển động nhẹ */
    static final class Banner extends JComponent {
        private final String title, sub, chip;
        private String note = "";
        private double phase;

        Banner(String title, String sub, String chip) {
            this.title = title;
            this.sub = sub;
            this.chip = chip;
            setPreferredSize(new Dimension(100, 150));
            runWhileShowing(this, new Timer(40, e -> {
                phase += 0.025;
                repaint();
            }));
        }

        void setNote(String n) {
            note = n == null ? "" : n;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int P = Card.P;
            double x = P, y = P, w = getWidth() - 2 * P, h = getHeight() - 2 * P;
            shadow(g, x, y, w, h, 32, 1.0);
            Shape old = g.getClip();
            g.clip(rr(x, y, w, h, 32));
            g.setPaint(new GradientPaint((float) x, (float) y, TEAL_D, (float) (x + w), (float) (y + h), new Color(0x14B8A6)));
            g.fill(rr(x, y, w, h, 32));
            for (int i = 0; i < 4; i++) {
                double cx = x + w * (0.52 + 0.13 * i) + Math.sin(phase + i * 1.3) * 22;
                double cy = y + h * 0.5 + Math.cos(phase * 1.3 + i) * 16;
                double r = 55 + i * 26;
                g.setColor(new Color(255, 255, 255, 14 + i * 3));
                g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
            g.setClip(old);
            g.setFont(font(Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            String c = chip.isEmpty() ? "Giáo viên" : chip;
            int cw = fm.stringWidth(c) + 24;
            g.setColor(new Color(255, 255, 255, 40));
            g.fill(rr(x + 30, y + 22, cw, 26, 26));
            g.setColor(Color.WHITE);
            g.drawString(c, (int) x + 42, (int) y + 40);
            g.setFont(font(Font.BOLD, 26));
            g.drawString(fit(g.getFontMetrics(), title, w - 190), (int) x + 30, (int) y + 82);
            g.setFont(font(Font.PLAIN, 14));
            g.setColor(new Color(204, 251, 241));
            g.drawString(fit(g.getFontMetrics(), sub, w - 190), (int) x + 30, (int) y + 108);
            if (!note.isEmpty()) {
                g.setFont(font(Font.BOLD, 13));
                g.setColor(Color.WHITE);
                g.drawString(fit(g.getFontMetrics(), note, w - 190), (int) x + 30, (int) y + 130);
            }
            LocalDate d = LocalDate.now();
            int bw = 96, bh = 88;
            double bx = x + w - bw - 28, by = y + (h - bh) / 2;
            g.setColor(new Color(255, 255, 255, 36));
            g.fill(rr(bx, by, bw, bh, 26));
            g.setColor(Color.WHITE);
            g.setFont(font(Font.BOLD, 30));
            textC(g, String.valueOf(d.getDayOfMonth()), bx + bw / 2.0, by + 32);
            g.setFont(font(Font.PLAIN, 12));
            textC(g, "Tháng " + d.getMonthValue() + "/" + d.getYear(), bx + bw / 2.0, by + 58);
            g.setColor(new Color(204, 251, 241));
            textC(g, dayName(d), bx + bw / 2.0, by + 75);
            g.dispose();
        }
    }

    static final class Avatar extends JComponent {
        private final String initials;

        Avatar(String name, int size) {
            initials = initialsOf(name);
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        static void paint(Graphics2D g, String ini, double x, double y, double s) {
            g.setPaint(new GradientPaint((float) x, (float) y, TEAL_L, (float) (x + s), (float) (y + s), TEAL_D));
            g.fill(new Ellipse2D.Double(x, y, s, s));
            g.setColor(Color.WHITE);
            g.setFont(font(Font.BOLD, (float) (s * 0.38)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ini, (float) (x + (s - fm.stringWidth(ini)) / 2), (float) (y + (s + fm.getAscent() - fm.getDescent()) / 2));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            paint(g, initials, 0, 0, Math.min(getWidth(), getHeight()));
            g.dispose();
        }
    }

    /** Nhãn "viên thuốc" nhỏ */
    static final class Pill extends JComponent {
        private final String text;
        private final Color color;

        Pill(String text, Color color) {
            this.text = text;
            this.color = color;
            FontMetrics fm = getFontMetrics(font(Font.BOLD, 12));
            Dimension d = new Dimension(fm.stringWidth(text) + 24, 26);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(alpha(color, 30));
            g.fill(rr(0, 0, getWidth(), getHeight(), 26));
            g.setFont(font(Font.BOLD, 12));
            g.setColor(shade(color, 0.85));
            textC(g, text, getWidth() / 2.0, getHeight() / 2.0);
            g.dispose();
        }
    }

    static final class Spinner extends JComponent {
        private int angle;

        Spinner() {
            setPreferredSize(new Dimension(44, 44));
            runWhileShowing(this, new Timer(16, e -> {
                angle = (angle + 9) % 360;
                repaint();
            }));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int s = Math.min(getWidth(), getHeight()) - 8;
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(204, 251, 241));
            g.drawOval((getWidth() - s) / 2, (getHeight() - s) / 2, s, s);
            g.setColor(TEAL);
            g.drawArc((getWidth() - s) / 2, (getHeight() - s) / 2, s, s, -angle, 100);
            g.dispose();
        }
    }

    static final class LoadingView extends JPanel {
        LoadingView() {
            super(new GridBagLayout());
            setBackground(Color.WHITE);
            JPanel in = new JPanel();
            in.setOpaque(false);
            in.setLayout(new BoxLayout(in, BoxLayout.Y_AXIS));
            Spinner sp = new Spinner();
            sp.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel l = lbl("Đang tải dữ liệu...", font(Font.PLAIN, 13), MUTED);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            in.add(sp);
            in.add(Box.createVerticalStrut(8));
            in.add(l);
            add(in);
        }
    }

    static final class EmptyState extends JComponent {
        private String text;

        EmptyState(String text) {
            this.text = text;
        }

        void setText(String t) {
            text = t;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            int cx = getWidth() / 2, cy = getHeight() / 2 - 20;
            g.setColor(new Color(240, 253, 250));
            g.fill(new Ellipse2D.Double(cx - 50, cy - 50, 100, 100));
            Icons.paint(g, "inbox", cx - 24, cy - 24, 48, new Color(94, 234, 212));
            g.setFont(font(Font.PLAIN, 14));
            g.setColor(MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, cx - fm.stringWidth(text) / 2, cy + 82);
            g.dispose();
        }
    }

    /** Panel cuộn theo chiều dọc, luôn rộng bằng viewport. */
    static final class ScrollPanel extends JPanel implements Scrollable {
        ScrollPanel(LayoutManager l) {
            super(l);
            setOpaque(false);
        }

        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 28; }

        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 180; }

        public boolean getScrollableTracksViewportWidth() { return true; }

        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static final class SlimScrollBarUI extends BasicScrollBarUI {
        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override protected JButton createDecreaseButton(int o) { return zero(); }

        @Override protected JButton createIncreaseButton(int o) { return zero(); }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }

        @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(isThumbRollover() || isDragging ? new Color(94, 234, 212) : new Color(186, 230, 224));
            boolean v = scrollbar.getOrientation() == Adjustable.VERTICAL;
            g.fill(rr(r.x + (v ? 3 : 0), r.y + (v ? 0 : 3), r.width - (v ? 6 : 0), r.height - (v ? 0 : 6), 12));
            g.dispose();
        }

        @Override public Dimension getPreferredSize(JComponent c) { return new Dimension(12, 12); }
    }

    static void slim(JScrollPane sp) {
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    }

    /** Hàng "nhãn ... giá trị" dùng trong các thẻ thông tin. */
    static JComponent infoRow(String k, JComponent v) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(241, 245, 249)),
                new EmptyBorder(11, 2, 11, 2)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl(k, font(Font.PLAIN, 14), MUTED), BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        r.setOpaque(false);
        r.add(v);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    static JComponent infoRow(String k, String v) {
        return infoRow(k, lbl(dash(v), font(Font.BOLD, 14), INK));
    }

    /** Bọc component bằng khoảng đệm chuẩn của trang. */
    static JComponent pad(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(14, 20, 16, 20));
        p.add(c);
        return p;
    }

    /** Form dọc: nhãn nhỏ phía trên từng ô nhập. */
    static JPanel vform(String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        int y = 0;
        for (int i = 0; i < labels.length; i++) {
            c.gridy = y++;
            c.insets = new Insets(0, 0, 4, 0);
            p.add(lbl(labels[i], font(Font.BOLD, 12), MUTED), c);
            c.gridy = y++;
            c.insets = new Insets(0, 0, 12, 0);
            p.add(fields[i], c);
        }
        return p;
    }

    /* ============================ BIỂU ĐỒ ============================ */

    /** Làm tròn bước chia trục lên 1, 2, 5 x 10^k. */
    static double niceStep(double raw) {
        if (raw <= 0) return 1;
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double n = raw / mag;
        double nice = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10;
        return nice * mag;
    }

    /** Tính {giá trị lớn nhất của trục, số vạch chia}. */
    static double[] axis(double max, boolean integer) {
        if (max <= 0) return new double[]{integer ? 1 : 1, integer ? 1 : 4};
        if (integer && max <= 4) {
            double m = Math.ceil(max);
            return new double[]{m, m};
        }
        double step = niceStep(max / 4);
        if (integer) step = Math.max(1, Math.ceil(step));
        return new double[]{step * 4, 4};
    }

    /** Biểu đồ cột - cột mọc lên khi tải, rê chuột để xem giá trị. */
    static final class BarChart extends JComponent {
        private String[] labels = {};
        private double[] vals = {};
        private Function<Double, String> fmt = v -> String.valueOf(Math.round(v));
        private Function<Double, String> axisFmt = v -> String.valueOf(Math.round(v));
        private boolean integer = true;
        private int highlight = -1;
        private String unit = "";
        private final Tween grow = new Tween(0, this::repaint);
        private int hov = -1;

        BarChart() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int n = vals.length;
                    int old = hov;
                    hov = -1;
                    if (n > 0) {
                        double slot = (getWidth() - 46) / (double) n;
                        int i = (int) ((e.getX() - 46) / slot);
                        if (e.getX() >= 46 && i >= 0 && i < n) hov = i;
                    }
                    if (old != hov) repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hov = -1;
                    repaint();
                }
            });
        }

        void setData(String[] l, double[] v, boolean integer, Function<Double, String> fmt, Function<Double, String> axisFmt) {
            labels = l;
            vals = v;
            this.integer = integer;
            this.fmt = fmt;
            this.axisFmt = axisFmt;
            grow.set(0);
            grow.to(1, 1000);
        }

        /** Dữ liệu dạng đếm số (số buổi, số học sinh...). */
        void setCounts(String[] l, long[] v, String unit) {
            double[] d = new double[v.length];
            for (int i = 0; i < d.length; i++) d[i] = v[i];
            this.unit = unit;
            setData(l, d, true, x -> Math.round(x) + " " + unit, x -> String.valueOf(Math.round(x)));
        }

        void setHighlight(int i) {
            highlight = i;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            int left = 46, top = 22, bottom = 30;
            int plotH = h - top - bottom, baseY = top + plotH;
            double max = 0;
            for (double v : vals) max = Math.max(max, v);
            double[] ax = axis(max, integer);
            double nice = ax[0];
            int ticks = (int) ax[1];
            g.setFont(font(Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i <= ticks; i++) {
                int y = baseY - (int) (plotH * i / (double) ticks);
                g.setColor(i == 0 ? new Color(203, 213, 225) : new Color(241, 245, 249));
                g.drawLine(left, y, w, y);
                g.setColor(MUTED);
                String s = axisFmt.apply(nice * i / ticks);
                g.drawString(s, left - 8 - fm.stringWidth(s), y + 4);
            }
            int n = vals.length;
            if (n == 0) {
                g.dispose();
                return;
            }
            double slot = (w - left) / (double) n;
            double bw = Math.min(46, slot * 0.52);
            double e = grow.get();
            for (int i = 0; i < n; i++) {
                double bx = left + slot * i + (slot - bw) / 2;
                double bh = plotH * (vals[i] / nice) * e;
                boolean on = i == hov;
                boolean cur = i == highlight;
                Shape old = g.getClip();
                g.clipRect(0, 0, w, baseY);
                if (bh > 0.5) {
                    Color c1 = cur ? new Color(0x5EEAD4) : on ? new Color(0x5EEAD4) : new Color(0x99F6E4);
                    Color c2 = cur || on ? TEAL : new Color(0x2DD4BF);
                    g.setPaint(new GradientPaint(0, (float) (baseY - bh), c1, 0, baseY, c2));
                    g.fill(rr(bx, baseY - bh, bw, bh + 12, 16));
                }
                g.setClip(old);
                g.setColor(on || cur ? TEAL_D : MUTED);
                g.setFont(font(on || cur ? Font.BOLD : Font.PLAIN, 12));
                int lw = g.getFontMetrics().stringWidth(labels[i]);
                g.drawString(labels[i], (int) (bx + bw / 2 - lw / 2.0), baseY + 20);
                if (on) {
                    String tip = fmt.apply(vals[i]);
                    g.setFont(font(Font.BOLD, 12));
                    int tw = g.getFontMetrics().stringWidth(tip) + 20;
                    double tx = Math.max(left, Math.min(w - tw, bx + bw / 2 - tw / 2.0));
                    double ty = Math.max(0, baseY - bh - 38);
                    g.setColor(INK);
                    g.fill(rr(tx, ty, tw, 28, 14));
                    g.setColor(Color.WHITE);
                    g.drawString(tip, (int) tx + 10, (int) ty + 18);
                }
            }
            g.dispose();
        }
    }

    /** Biểu đồ cột ngang - dùng cho phân bố số sao. Bấm vào một hàng để lọc. */
    static final class HBarChart extends JComponent {
        private String[] labels = {};
        private long[] counts = {};
        private Color[] colors = {};
        private final Tween grow = new Tween(0, this::repaint);
        private int hov = -1;
        private IntConsumer onPick;

        HBarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(300, 200));
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = rowAt(e.getY());
                    if (i != hov) {
                        hov = i;
                        setCursor(Cursor.getPredefinedCursor(i >= 0 && onPick != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hov = -1;
                    repaint();
                }

                @Override public void mousePressed(MouseEvent e) {
                    int i = rowAt(e.getY());
                    if (i >= 0 && onPick != null) onPick.accept(i);
                }
            });
        }

        void setOnPick(IntConsumer c) {
            onPick = c;
        }

        void setData(String[] labels, long[] counts, Color[] colors) {
            this.labels = labels;
            this.counts = counts;
            this.colors = colors;
            grow.set(0);
            grow.to(1, 900);
        }

        private int rowAt(int y) {
            int n = labels.length;
            if (n == 0) return -1;
            double rh = getHeight() / (double) n;
            int i = (int) (y / rh);
            return i >= 0 && i < n ? i : -1;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight(), n = labels.length;
            if (n == 0) {
                g.dispose();
                return;
            }
            long max = 1, total = 0;
            for (long c : counts) {
                max = Math.max(max, c);
                total += c;
            }
            double rh = h / (double) n;
            int labW = 62, numW = 86;
            double trackX = labW, trackW = Math.max(40, w - labW - numW);
            double e = grow.get();
            for (int i = 0; i < n; i++) {
                double cy = rh * i + rh / 2;
                boolean on = i == hov;
                if (on) {
                    g.setColor(new Color(240, 253, 250));
                    g.fill(rr(0, rh * i + 2, w, rh - 4, 18));
                }
                g.setFont(font(Font.BOLD, 13));
                g.setColor(on ? TEAL_D : INK);
                text(g, labels[i], 10, cy);
                g.setColor(new Color(241, 245, 249));
                g.fill(rr(trackX, cy - 7, trackW, 14, 14));
                double bw = trackW * (counts[i] / (double) max) * e;
                if (bw > 1) {
                    g.setPaint(new GradientPaint((float) trackX, 0, lerp(colors[i], Color.WHITE, 0.3), (float) (trackX + trackW), 0, colors[i]));
                    g.fill(rr(trackX, cy - 7, Math.max(14, bw), 14, 14));
                }
                g.setFont(font(Font.BOLD, 13));
                g.setColor(INK);
                textR(g, String.valueOf(counts[i]), w - 40, cy);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(MUTED);
                textR(g, total == 0 ? "0%" : Math.round(100.0 * counts[i] / total) + "%", w - 6, cy);
            }
            g.dispose();
        }
    }

    /** Biểu đồ vành khuyên + chú giải. */
    static final class DonutChart extends JComponent {
        private String[] labels = {};
        private double[] vals = {};
        private Color[] colors = {};
        private String big = "", small = "";
        private final Tween sweep = new Tween(0, this::repaint);
        private int hov = -1;

        DonutChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 200));
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = sliceAt(e.getX(), e.getY());
                    if (i != hov) {
                        hov = i;
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hov = -1;
                    repaint();
                }
            });
        }

        void setData(String[] labels, double[] vals, Color[] colors, String big, String small) {
            this.labels = labels;
            this.vals = vals;
            this.colors = colors;
            this.big = big;
            this.small = small;
            sweep.set(0);
            sweep.to(1, 1000);
        }

        private double total() {
            double t = 0;
            for (double v : vals) t += v;
            return t;
        }

        private double diameter() {
            return Math.min(getHeight() - 16, getWidth() * 0.5);
        }

        private int sliceAt(int mx, int my) {
            double s = diameter(), cx = 8 + s / 2, cy = getHeight() / 2.0;
            double dx = mx - cx, dy = my - cy, d = Math.sqrt(dx * dx + dy * dy);
            if (d > s / 2 + 4 || d < s / 2 - 34) return -1;
            double ang = Math.toDegrees(Math.atan2(-dy, dx));
            double fromTop = ((90 - ang) % 360 + 360) % 360;
            double t = total();
            if (t <= 0) return -1;
            double acc = 0;
            for (int i = 0; i < vals.length; i++) {
                double span = 360 * vals[i] / t;
                if (fromTop >= acc && fromTop < acc + span) return i;
                acc += span;
            }
            return -1;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            double t = total();
            double s = diameter(), x0 = 8, y0 = (h - s) / 2;
            double thick = 26;
            if (t <= 0) {
                g.setStroke(new BasicStroke((float) thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(241, 245, 249));
                g.draw(new Ellipse2D.Double(x0 + thick / 2, y0 + thick / 2, s - thick, s - thick));
                g.setFont(font(Font.PLAIN, 13));
                g.setColor(MUTED);
                textC(g, "Chưa có dữ liệu", x0 + s / 2, y0 + s / 2);
                g.dispose();
                return;
            }
            double acc = 90;
            double e = sweep.get();
            for (int i = 0; i < vals.length; i++) {
                double span = 360 * vals[i] / t * e;
                boolean on = i == hov;
                double grow = on ? 4 : 0;
                g.setStroke(new BasicStroke((float) (thick + grow), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                g.setColor(colors[i]);
                double gap = vals.length > 1 ? Math.min(2.0, span) : 0;
                if (span - gap > 0.1) {
                    g.draw(new Arc2D.Double(x0 + thick / 2 - grow / 2, y0 + thick / 2 - grow / 2,
                            s - thick + grow, s - thick + grow, acc - gap / 2, -(span - gap), Arc2D.OPEN));
                }
                acc -= span;
            }
            g.setFont(font(Font.BOLD, 26));
            g.setColor(INK);
            textC(g, hov >= 0 ? String.valueOf(Math.round(vals[hov])) : big, x0 + s / 2, y0 + s / 2 - 6);
            g.setFont(font(Font.PLAIN, 12));
            g.setColor(MUTED);
            textC(g, hov >= 0 ? labels[hov] : small, x0 + s / 2, y0 + s / 2 + 16);
            // chú giải
            double lx = x0 + s + 28;
            int rows = labels.length;
            double rh = Math.min(34, (h - 20.0) / Math.max(1, rows));
            double ly = h / 2.0 - rh * rows / 2;
            for (int i = 0; i < rows; i++) {
                double cy = ly + rh * i + rh / 2;
                boolean on = i == hov;
                g.setColor(colors[i]);
                g.fill(rr(lx, cy - 6, 12, 12, 6));
                g.setFont(font(on ? Font.BOLD : Font.PLAIN, 13));
                g.setColor(INK);
                String lab = fit(g.getFontMetrics(), labels[i], Math.max(30, w - lx - 100));
                text(g, lab, lx + 22, cy);
                g.setFont(font(Font.BOLD, 13));
                textR(g, Math.round(vals[i]) + "  (" + Math.round(100 * vals[i] / t) + "%)", w - 4, cy);
            }
            g.dispose();
        }
    }

    /** Biểu đồ đường + vùng tô mờ, rê chuột để xem điểm gần nhất. */
    static final class LineChart extends JComponent {
        private String[] labels = {};
        private double[] vals = {};
        private Function<Double, String> fmt = v -> String.valueOf(Math.round(v));
        private Function<Double, String> axisFmt = v -> String.valueOf(Math.round(v));
        private boolean integer = true;
        private double fixedMax = -1;
        private Color color = TEAL;
        private String empty = "Chưa có dữ liệu";
        private final Tween grow = new Tween(0, this::repaint);
        private int hov = -1;

        LineChart() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int n = vals.length, old = hov;
                    hov = -1;
                    if (n > 0) {
                        double best = 1e9;
                        for (int i = 0; i < n; i++) {
                            double d = Math.abs(px(i) - e.getX());
                            if (d < best) {
                                best = d;
                                hov = i;
                            }
                        }
                        if (best > 40) hov = -1;
                    }
                    if (old != hov) repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hov = -1;
                    repaint();
                }
            });
        }

        void setColor(Color c) {
            color = c;
        }

        void setEmptyText(String s) {
            empty = s;
        }

        /** Trục cố định 0..max (ví dụ điểm thi 0..180). */
        void setFixedMax(double m) {
            fixedMax = m;
        }

        void setData(String[] l, double[] v, boolean integer, Function<Double, String> fmt, Function<Double, String> axisFmt) {
            labels = l;
            vals = v;
            this.integer = integer;
            this.fmt = fmt;
            this.axisFmt = axisFmt;
            grow.set(0);
            grow.to(1, 1100);
        }

        private double px(int i) {
            int left = 50, right = 22, n = vals.length;
            double span = getWidth() - left - right;
            return n <= 1 ? left + span / 2 : left + span * i / (n - 1);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            int left = 50, top = 24, bottom = 32;
            int plotH = h - top - bottom, baseY = top + plotH;
            int n = vals.length;
            if (n == 0) {
                g.setFont(font(Font.PLAIN, 13));
                g.setColor(MUTED);
                textC(g, empty, w / 2.0, h / 2.0);
                g.dispose();
                return;
            }
            double max = 0;
            for (double v : vals) max = Math.max(max, v);
            double[] ax = fixedMax > 0 ? new double[]{fixedMax, 4} : axis(max, integer);
            double nice = ax[0];
            int ticks = (int) ax[1];
            g.setFont(font(Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i <= ticks; i++) {
                int y = baseY - (int) (plotH * i / (double) ticks);
                g.setColor(i == 0 ? new Color(203, 213, 225) : new Color(241, 245, 249));
                g.drawLine(left, y, w - 14, y);
                g.setColor(MUTED);
                String s = axisFmt.apply(nice * i / ticks);
                g.drawString(s, left - 8 - fm.stringWidth(s), y + 4);
            }
            double e = grow.get();
            double[] xs = new double[n], ys = new double[n];
            for (int i = 0; i < n; i++) {
                xs[i] = px(i);
                ys[i] = baseY - plotH * (vals[i] / nice) * e;
            }
            // vùng tô
            Path2D.Double area = new Path2D.Double();
            Path2D.Double line = new Path2D.Double();
            line.moveTo(xs[0], ys[0]);
            for (int i = 1; i < n; i++) {
                double mx = (xs[i - 1] + xs[i]) / 2;
                line.curveTo(mx, ys[i - 1], mx, ys[i], xs[i], ys[i]);
            }
            area.append(line, false);
            area.lineTo(xs[n - 1], baseY);
            area.lineTo(xs[0], baseY);
            area.closePath();
            g.setPaint(new GradientPaint(0, top, alpha(color, 70), 0, baseY, alpha(color, 4)));
            g.fill(area);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(color);
            g.draw(line);
            // nhãn trục x (thưa bớt khi nhiều điểm)
            g.setFont(font(Font.PLAIN, 11));
            int every = Math.max(1, (int) Math.ceil(n / Math.max(1.0, (w - left) / 60.0)));
            for (int i = 0; i < n; i++) {
                if (i % every != 0 && i != hov) continue;
                g.setColor(i == hov ? TEAL_D : MUTED);
                textC(g, fit(g.getFontMetrics(), labels[i], 70), xs[i], baseY + 18);
            }
            // chấm điểm
            for (int i = 0; i < n; i++) {
                boolean on = i == hov;
                double r = on ? 6.5 : 4.5;
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(xs[i] - r - 1.5, ys[i] - r - 1.5, 2 * r + 3, 2 * r + 3));
                g.setColor(color);
                g.fill(new Ellipse2D.Double(xs[i] - r, ys[i] - r, 2 * r, 2 * r));
            }
            if (hov >= 0) {
                String tip = labels[hov] + ": " + fmt.apply(vals[hov]);
                g.setFont(font(Font.BOLD, 12));
                int tw = g.getFontMetrics().stringWidth(tip) + 22;
                double tx = Math.max(left, Math.min(w - tw - 6, xs[hov] - tw / 2.0));
                double ty = Math.max(2, ys[hov] - 44);
                g.setColor(INK);
                g.fill(rr(tx, ty, tw, 28, 14));
                g.setColor(Color.WHITE);
                g.drawString(tip, (int) tx + 11, (int) ty + 18);
            }
            g.dispose();
        }
    }

    /** Thanh điểm từng kỹ năng, có vạch "điểm sàn" để đối chiếu điều kiện đỗ. */
    static final class SkillBars extends JComponent {
        private String[] names = {};
        private int[] vals = {}, maxs = {}, mins = {};
        private final Tween grow = new Tween(0, this::repaint);

        SkillBars() {
            setOpaque(false);
        }

        /** mins[i] < 0 nghĩa là không có điểm sàn riêng cho thanh đó. */
        void setData(String[] names, int[] vals, int[] maxs, int[] mins) {
            this.names = names;
            this.vals = vals;
            this.maxs = maxs;
            this.mins = mins;
            setPreferredSize(new Dimension(300, 52 * names.length + 8));
            revalidate();
            grow.set(0);
            grow.to(1, 900);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth();
            double e = grow.get();
            for (int i = 0; i < names.length; i++) {
                double y = 4 + i * 52;
                boolean ok = mins[i] < 0 || vals[i] >= mins[i];
                Color c = mins[i] < 0 ? TEAL : ok ? GREEN : RED;
                g.setFont(font(Font.BOLD, 13));
                g.setColor(INK);
                text(g, names[i], 2, y + 10);
                g.setFont(font(Font.BOLD, 13));
                g.setColor(c);
                textR(g, vals[i] + " / " + maxs[i], w - 2, y + 10);
                double tx = 2, tw = w - 4, ty = y + 24;
                g.setColor(new Color(241, 245, 249));
                g.fill(rr(tx, ty, tw, 12, 12));
                double bw = tw * Math.min(1.0, vals[i] / (double) maxs[i]) * e;
                if (bw > 1) {
                    g.setPaint(new GradientPaint((float) tx, 0, lerp(c, Color.WHITE, 0.35), (float) (tx + tw), 0, c));
                    g.fill(rr(tx, ty, Math.max(12, bw), 12, 12));
                }
                if (mins[i] >= 0) {
                    double mx = tx + tw * mins[i] / (double) maxs[i];
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new Line2D.Double(mx, ty - 4, mx, ty + 16));
                    g.setFont(font(Font.PLAIN, 11));
                    g.setColor(MUTED);
                    String s = "tối thiểu " + mins[i];
                    FontMetrics fm = g.getFontMetrics();
                    double sx = Math.min(w - fm.stringWidth(s) - 2, Math.max(2, mx - fm.stringWidth(s) / 2.0));
                    g.drawString(s, (float) sx, (float) (ty + 30));
                }
            }
            g.dispose();
        }
    }

    /** Vòng tiến độ nhỏ - dùng cho các chỉ số phần trăm. */
    static final class ProgressRing extends JComponent {
        private double value;
        private String center = "", caption = "";
        private Color color = TEAL;
        private final Tween grow = new Tween(0, this::repaint);

        ProgressRing(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(120, 140));
            setOpaque(false);
        }

        void setValue(double v, String center, String caption) {
            this.value = Math.max(0, Math.min(1, v));
            this.center = center;
            this.caption = caption;
            grow.set(0);
            grow.to(1, 1000);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth();
            double s = Math.min(w - 12, 92), x = (w - s) / 2, y = 6;
            g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(alpha(color, 36));
            g.draw(new Ellipse2D.Double(x + 5, y + 5, s - 10, s - 10));
            g.setColor(color);
            g.draw(new Arc2D.Double(x + 5, y + 5, s - 10, s - 10, 90, -360 * value * grow.get(), Arc2D.OPEN));
            g.setFont(font(Font.BOLD, 18));
            g.setColor(INK);
            textC(g, center, x + s / 2, y + s / 2);
            g.setFont(font(Font.PLAIN, 12));
            g.setColor(MUTED);
            textC(g, fit(g.getFontMetrics(), caption, w - 4), w / 2.0, y + s + 18);
            g.dispose();
        }
    }

    /* ============================ XUẤT CSV ============================ */

    /** Ghi CSV UTF-8 có BOM để Excel mở đúng tiếng Việt. */
    static final class Csv {
        static String cell(Object v) {
            String s = v == null ? "" : v instanceof CsvValue ? ((CsvValue) v).csv() : v.toString();
            if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r") || s.contains(";")) {
                s = "\"" + s.replace("\"", "\"\"") + "\"";
            }
            return s;
        }

        static void write(File f, String[] head, List<Object[]> rows) throws IOException {
            try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                w.write('\uFEFF');
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < head.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(cell(head[i]));
                }
                w.write(sb.append("\r\n").toString());
                for (Object[] r : rows) {
                    sb.setLength(0);
                    for (int i = 0; i < r.length; i++) {
                        if (i > 0) sb.append(',');
                        sb.append(cell(r[i]));
                    }
                    w.write(sb.append("\r\n").toString());
                }
            }
        }
    }

    /* ============================ BẢNG DỮ LIỆU ============================ */

    static final class GridModel extends DefaultTableModel {
        GridModel(String[] c) {
            super(c, 0);
        }

        @Override public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override public Class<?> getColumnClass(int c) {
            for (int i = 0; i < getRowCount(); i++) {
                Object v = getValueAt(i, c);
                if (v != null) return v.getClass();
            }
            return Object.class;
        }
    }

    static Color pillColor(String s) {
        switch (s) {
            case DAY: case PASS: case "Đang học": return GREEN;
            case OFF: case FAIL: return RED;
            case "Chưa xác định": return SLATE;
            default: return TEAL;
        }
    }

    static Color levelColor(Level l) {
        switch (l.text) {
            case "N1": return VIOLET;
            case "N2": return BLUE;
            case "N3": return TEAL;
            case "N4": return AMBER;
            case "N5": return GREEN;
            default: return SLATE;
        }
    }

    static final Color SEL_BG = new Color(204, 251, 241);
    static final Color HOV_BG = new Color(240, 253, 250);

    static final class StyledTable extends JTable {
        int hoverRow = -1;
        private final Set<Integer> pills;
        private final Cell cell = new Cell();

        StyledTable(GridModel m, Set<Integer> pills, boolean compact) {
            super(m);
            this.pills = pills;
            setRowHeight(compact ? 42 : 48);
            setShowGrid(false);
            setIntercellSpacing(new Dimension(0, 0));
            setFillsViewportHeight(true);
            setFont(font(Font.PLAIN, 13));
            setSelectionBackground(SEL_BG);
            setSelectionForeground(INK);
            setFocusable(true);
            ToolTipManager.sharedInstance().registerComponent(this);
            HeaderStyle.apply(this, compact);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int r = rowAtPoint(e.getPoint());
                    if (r != hoverRow) {
                        hoverRow = r;
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hoverRow = -1;
                    repaint();
                }
            });
        }

        @Override public TableCellRenderer getCellRenderer(int row, int column) {
            return cell;
        }

        @Override public String getToolTipText(MouseEvent e) {
            int r = rowAtPoint(e.getPoint()), c = columnAtPoint(e.getPoint());
            if (r < 0 || c < 0) return null;
            Object v = getValueAt(r, c);
            String s = v == null ? "" : v.toString();
            if (s.length() < 28 || v instanceof Stars) return null;
            return "<html><div style='width:320px'>" + s.replace("&", "&amp;").replace("<", "&lt;") + "</div></html>";
        }

        /** Bộ vẽ ô: văn bản, pill trạng thái, sao, điểm kèm thanh, tiền, trình độ. */
        private final class Cell extends JComponent implements TableCellRenderer {
            private Object value;
            private int col;
            private Color bg = Color.WHITE;
            private boolean pill;

            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc,
                                                                    int row, int c) {
                value = v;
                col = c;
                bg = sel ? SEL_BG : row == hoverRow ? HOV_BG : Color.WHITE;
                String s = v == null ? "" : v.toString();
                pill = pills.contains(c) && !s.isEmpty() && !s.equals("—");
                return this;
            }

            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                int w = getWidth(), h = getHeight(), pad = 16;
                g.setColor(bg);
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(241, 245, 249));
                g.drawLine(0, h - 1, w, h - 1);
                String s = value == null ? "" : value.toString();
                if (value instanceof Stars) {
                    int n = ((Stars) value).n;
                    for (int i = 0; i < 5; i++) {
                        Path2D st = Icons.star(pad + 8 + i * 19, h / 2.0 + 0.5, 8);
                        g.setColor(i < n ? AMBER : new Color(226, 232, 240));
                        g.fill(st);
                    }
                } else if (pill) {
                    g.setFont(font(Font.BOLD, 12));
                    FontMetrics fm = g.getFontMetrics();
                    String t = fit(fm, s, w - pad * 2 - 22);
                    int pw = fm.stringWidth(t) + 22, ph = 26, y = (h - ph) / 2;
                    Color pc = pillColor(s);
                    g.setColor(alpha(pc, 30));
                    g.fill(rr(pad, y, pw, ph, 26));
                    g.setColor(shade(pc, 0.85));
                    g.drawString(t, pad + 11, y + (ph + fm.getAscent() - fm.getDescent()) / 2);
                } else if (value instanceof Score) {
                    Score sc = (Score) value;
                    if (sc.v < 0) {
                        g.setFont(font(Font.PLAIN, 13));
                        g.setColor(SLATE);
                        textR(g, "—", pad + 30, h / 2.0);
                        g.dispose();
                        return;
                    }
                    g.setFont(font(Font.BOLD, 13));
                    g.setColor(INK);
                    FontMetrics fm = g.getFontMetrics();
                    int numW = Math.max(30, fm.stringWidth("000"));
                    textR(g, s, pad + numW, h / 2.0);
                    double bx = pad + numW + 12, bw = Math.min(110, w - bx - pad);
                    if (bw > 20) {
                        g.setColor(new Color(241, 245, 249));
                        g.fill(rr(bx, h / 2.0 - 4, bw, 8, 8));
                        double ratio = Math.min(1.0, sc.v / (double) Math.max(1, sc.max));
                        g.setPaint(new GradientPaint((float) bx, 0, TEAL_L, (float) (bx + bw), 0, TEAL));
                        g.fill(rr(bx, h / 2.0 - 4, Math.max(8, bw * ratio), 8, 8));
                    }
                } else if (value instanceof Level) {
                    Level lv = (Level) value;
                    g.setFont(font(Font.BOLD, 12));
                    FontMetrics fm = g.getFontMetrics();
                    String t = fit(fm, lv.text, w - pad * 2 - 20);
                    int pw = fm.stringWidth(t) + 20, ph = 24, y = (h - ph) / 2;
                    Color lc = levelColor(lv);
                    g.setColor(alpha(lc, 28));
                    g.fill(rr(pad, y, pw, ph, 12));
                    g.setColor(shade(lc, 0.85));
                    g.drawString(t, pad + 10, y + (ph + fm.getAscent() - fm.getDescent()) / 2);
                } else if (value instanceof Money) {
                    g.setFont(font(Font.BOLD, 13));
                    g.setColor(TEAL_D);
                    textR(g, s, w - pad, h / 2.0);
                } else if (value instanceof Integer || value instanceof Long) {
                    g.setFont(font(Font.PLAIN, 13));
                    g.setColor(INK);
                    textR(g, s, w - pad, h / 2.0);
                } else {
                    boolean first = col == 0;
                    g.setFont(font(first ? Font.BOLD : Font.PLAIN, 13));
                    g.setColor(first ? TEAL_D : INK);
                    FontMetrics fm = g.getFontMetrics();
                    text(g, fit(fm, s.replace('\n', ' '), w - pad * 2), pad, h / 2.0);
                }
                g.dispose();
            }
        }
    }

    static final class HeaderStyle {
        static void apply(JTable t, boolean compact) {
            t.getTableHeader().setReorderingAllowed(false);
            t.getTableHeader().setPreferredSize(new Dimension(0, compact ? 38 : 44));
            t.getTableHeader().setDefaultRenderer((tbl, v, sel, foc, row, col) -> {
                String s = String.valueOf(v).toUpperCase(VI);
                List<? extends RowSorter.SortKey> keys = tbl.getRowSorter() == null ? null : tbl.getRowSorter().getSortKeys();
                if (keys != null && !keys.isEmpty() && keys.get(0).getColumn() == col) {
                    s += keys.get(0).getSortOrder() == SortOrder.ASCENDING ? "  ▲" : "  ▼";
                }
                JLabel l = new JLabel(s);
                l.setOpaque(true);
                l.setBackground(new Color(248, 250, 252));
                l.setForeground(new Color(100, 116, 139));
                l.setFont(font(Font.BOLD, 11));
                Class<?> c = tbl.getModel().getColumnClass(col);
                boolean right = c == Money.class || c == Integer.class || c == Long.class;
                l.setHorizontalAlignment(right ? SwingConstants.RIGHT : SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 0, 1, 0, LINE), new EmptyBorder(0, 16, 0, 16)));
                return l;
            });
        }
    }

    /** Bảng + tìm kiếm + lọc + trạng thái tải/rỗng + xuất CSV, nạp dữ liệu bất đồng bộ. */
    static final class DataGrid extends JPanel {
        final GridModel model;
        final StyledTable table;
        final TableRowSorter<GridModel> sorter;
        private final CardLayout cards = new CardLayout();
        private final JPanel stack = new JPanel(cards);
        private final EmptyState empty;
        private final JLabel count = new JLabel(" ");
        private final String emptyMsg;
        private final String[] headers;
        private String text = "", status;
        private int statusCol = -1;
        private final Map<Integer, String> colFilters = new HashMap<>();
        private boolean loading;
        private String lastSql;
        private RowMapper lastMap;
        private Object[] lastParams = new Object[0];
        private Runnable onLoaded;
        private IntConsumer onOpen;

        DataGrid(String[] cols, String emptyMsg, Set<Integer> pills, boolean compact) {
            super(new BorderLayout(0, 8));
            setOpaque(false);
            this.emptyMsg = emptyMsg;
            this.headers = cols;
            model = new GridModel(cols);
            table = new StyledTable(model, pills, compact);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            JScrollPane sp = new JScrollPane(table);
            slim(sp);
            sp.getViewport().setBackground(Color.WHITE);
            if (compact) sp.setWheelScrollingEnabled(false);
            JPanel framed = new JPanel(new BorderLayout());
            framed.setBorder(BorderFactory.createLineBorder(LINE));
            framed.add(sp);
            empty = new EmptyState(emptyMsg);
            stack.setOpaque(false);
            stack.add(framed, "table");
            stack.add(empty, "empty");
            stack.add(new LoadingView(), "loading");
            add(stack, BorderLayout.CENTER);
            if (!compact) {
                count.setFont(font(Font.PLAIN, 12));
                count.setForeground(MUTED);
                count.setBorder(new EmptyBorder(0, 4, 0, 0));
                add(count, BorderLayout.SOUTH);
            }
            sorter.addRowSorterListener(e -> refreshState());
            cards.show(stack, "loading");
            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && onOpen != null) {
                        int r = selectedModelRow();
                        if (r >= 0) onOpen.accept(r);
                    }
                }
            });
            table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
            table.getActionMap().put("open", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    int r = selectedModelRow();
                    if (r >= 0 && onOpen != null) onOpen.accept(r);
                }
            });
        }

        void setOnLoaded(Runnable r) {
            onLoaded = r;
        }

        /** Nạp dữ liệu có sẵn trong bộ nhớ (không truy vấn CSDL). */
        void setRows(List<Object[]> rows) {
            loading = false;
            model.setRowCount(0);
            for (Object[] r : rows) model.addRow(r);
            refreshState();
        }

        /** Gọi khi người dùng bấm đúp (hoặc Enter) vào một dòng; tham số là chỉ số dòng trong model. */
        void setOnOpen(IntConsumer c) {
            onOpen = c;
            if (c != null) table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void load(String sql, RowMapper map, Object... params) {
            lastSql = sql;
            lastMap = map;
            lastParams = params;
            reload();
        }

        void reload() {
            if (lastSql == null) return;
            loading = true;
            cards.show(stack, "loading");
            final String sql = lastSql;
            final RowMapper map = lastMap;
            final Object[] p = lastParams;
            new SwingWorker<List<Object[]>, Void>() {
                @Override protected List<Object[]> doInBackground() throws Exception {
                    return Db.query(sql, map, p);
                }

                @Override protected void done() {
                    loading = false;
                    try {
                        List<Object[]> rows = get();
                        model.setRowCount(0);
                        for (Object[] r : rows) model.addRow(r);
                        refreshState();
                        if (onLoaded != null) onLoaded.run();
                    } catch (Exception e) {
                        model.setRowCount(0);
                        empty.setText("Không tải được dữ liệu từ CSDL.");
                        cards.show(stack, "empty");
                        toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR);
                    }
                }
            }.execute();
        }

        private void refreshState() {
            if (loading) return;
            int total = model.getRowCount(), shown = table.getRowCount();
            if (total == 0) {
                empty.setText(emptyMsg);
                cards.show(stack, "empty");
            } else if (shown == 0) {
                empty.setText("Không tìm thấy kết quả phù hợp.");
                cards.show(stack, "empty");
            } else cards.show(stack, "table");
            count.setText("Hiển thị " + shown + " / " + total + " bản ghi" + (onOpen != null ? "   •   Bấm đúp để xem chi tiết" : ""));
        }

        void setText(String t) {
            text = t.trim();
            applyFilter();
        }

        void setStatus(String s, int col) {
            status = s;
            statusCol = col;
            applyFilter();
        }

        private void applyFilter() {
            List<RowFilter<GridModel, Integer>> fs = new ArrayList<>();
            if (!text.isEmpty()) fs.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
            if (status != null && statusCol >= 0) fs.add(RowFilter.regexFilter("^" + Pattern.quote(status) + "$", statusCol));
            for (Map.Entry<Integer, String> en : colFilters.entrySet())
                fs.add(RowFilter.regexFilter("^" + Pattern.quote(en.getValue()) + "$", en.getKey()));
            sorter.setRowFilter(fs.isEmpty() ? null : RowFilter.andFilter(fs));
        }

        void setColFilter(int col, String value) {
            if (value == null) colFilters.remove(col);
            else colFilters.put(col, value);
            applyFilter();
        }

        void sortBy(int col, boolean asc) {
            if (col < 0) {
                sorter.setSortKeys(null);
                return;
            }
            sorter.setSortKeys(Collections.singletonList(
                    new RowSorter.SortKey(col, asc ? SortOrder.ASCENDING : SortOrder.DESCENDING)));
        }

        int selectedModelRow() {
            int r = table.getSelectedRow();
            return r < 0 ? -1 : table.convertRowIndexToModel(r);
        }

        Object value(int modelRow, int col) {
            return model.getValueAt(modelRow, col);
        }

        String str(int modelRow, int col) {
            Object v = value(modelRow, col);
            return v == null ? "" : v.toString();
        }

        int rowCount() {
            return model.getRowCount();
        }

        /** Chọn dòng có giá trị cột col bằng key (sau khi tải lại). */
        void select(int col, String key) {
            for (int i = 0; i < table.getRowCount(); i++) {
                int mr = table.convertRowIndexToModel(i);
                if (key.equals(str(mr, col))) {
                    table.setRowSelectionInterval(i, i);
                    table.scrollRectToVisible(table.getCellRect(i, 0, true));
                    return;
                }
            }
        }

        /** Các dòng đang hiển thị (đã lọc + sắp xếp) dưới dạng mảng giá trị. */
        List<Object[]> visibleRows() {
            List<Object[]> out = new ArrayList<>();
            for (int i = 0; i < table.getRowCount(); i++) {
                int mr = table.convertRowIndexToModel(i);
                Object[] r = new Object[model.getColumnCount()];
                for (int c = 0; c < r.length; c++) r[c] = model.getValueAt(mr, c);
                out.add(r);
            }
            return out;
        }

        void exportCsv(String baseName) {
            List<Object[]> rows = visibleRows();
            if (rows.isEmpty()) {
                toast("Không có dữ liệu để xuất.", T_WARN);
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Xuất dữ liệu ra CSV");
            fc.setSelectedFile(new File(baseName + "_" + LocalDate.now() + ".csv"));
            if (fc.showSaveDialog(SwingUtilities.getWindowAncestor(this)) != JFileChooser.APPROVE_OPTION) return;
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase(Locale.ROOT).endsWith(".csv"))
                f = new File(f.getParentFile(), f.getName() + ".csv");
            if (f.exists() && JOptionPane.showConfirmDialog(this, "File " + f.getName() + " đã tồn tại. Ghi đè?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try {
                Csv.write(f, headers, rows);
                toast("Đã xuất " + rows.size() + " dòng ra " + f.getName(), T_OK);
            } catch (IOException e) {
                toast("Không ghi được file: " + rootMsg(e), T_ERR);
            }
        }
    }

    /* ============================ LỊCH TUẦN / THÁNG / TRONG NGÀY ============================ */

    static Color statusColor(Session s) {
        return s.off() ? RED : GREEN;
    }

    static int compareSession(Session a, Session b) {
        int c = a.ngay == null ? (b.ngay == null ? 0 : -1) : b.ngay == null ? 1 : a.ngay.compareTo(b.ngay);
        if (c != 0) return c;
        c = a.ca.compareTo(b.ca);
        return c != 0 ? c : a.ma.compareTo(b.ma);
    }

    /** Lịch tuần: 7 cột Thứ Hai → Chủ Nhật, mỗi buổi dạy là một thẻ nhỏ. */
    static final class WeekView extends JComponent implements Scrollable {
        static final int HEAD = 66, CHIP = 70, GAP = 8;
        private LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        private List<Session> all = new ArrayList<>();
        private Session selected, hovered;
        private Consumer<Session> onPick;
        private final List<Object[]> hits = new ArrayList<>();

        WeekView() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    Session s = at(e.getPoint());
                    if (s != hovered) {
                        hovered = s;
                        setCursor(Cursor.getPredefinedCursor(s != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hovered = null;
                    repaint();
                }

                @Override public void mousePressed(MouseEvent e) {
                    Session s = at(e.getPoint());
                    if (s != null) {
                        selected = s;
                        if (onPick != null) onPick.accept(s);
                        repaint();
                    }
                }
            });
        }

        void setOnPick(Consumer<Session> c) {
            onPick = c;
        }

        void setSelected(Session s) {
            selected = s;
            repaint();
        }

        LocalDate weekStart() {
            return start;
        }

        void setWeek(LocalDate any) {
            start = any.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            updateSize();
            repaint();
        }

        void setSessions(List<Session> l) {
            all = l;
            updateSize();
            repaint();
        }

        List<Session> on(int i) {
            LocalDate d = start.plusDays(i);
            List<Session> out = new ArrayList<>();
            for (Session s : all) if (d.equals(s.ngay)) out.add(s);
            out.sort(TeacherDashboardUI::compareSession);
            return out;
        }

        int countInWeek() {
            int n = 0;
            for (int i = 0; i < 7; i++) n += on(i).size();
            return n;
        }

        private void updateSize() {
            int max = 3;
            for (int i = 0; i < 7; i++) max = Math.max(max, on(i).size());
            setPreferredSize(new Dimension(700, HEAD + max * (CHIP + GAP) + 20));
            revalidate();
        }

        private Session at(Point p) {
            for (Object[] h : hits) if (((Rectangle) h[0]).contains(p)) return (Session) h[1];
            return null;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            hits.clear();
            double colW = w / 7.0;
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                double x = i * colW;
                LocalDate d = start.plusDays(i);
                boolean weekend = i >= 5, isToday = d.equals(today);
                if (weekend) {
                    g.setColor(new Color(248, 250, 252));
                    g.fillRect((int) x, 0, (int) Math.ceil(colW), h);
                }
                if (isToday) {
                    g.setColor(new Color(240, 253, 250));
                    g.fillRect((int) x, 0, (int) Math.ceil(colW), h);
                }
                g.setColor(new Color(241, 245, 249));
                g.drawLine((int) x, 0, (int) x, h);
                g.setFont(font(Font.BOLD, 12));
                g.setColor(isToday ? TEAL_D : MUTED);
                textC(g, DAYS_S[i], x + colW / 2, 16);
                if (isToday) {
                    g.setColor(TEAL);
                    g.fill(new Ellipse2D.Double(x + colW / 2 - 17, 28, 34, 34));
                    g.setColor(Color.WHITE);
                } else g.setColor(INK);
                g.setFont(font(Font.BOLD, 17));
                textC(g, String.valueOf(d.getDayOfMonth()), x + colW / 2, 45);
                List<Session> list = on(i);
                if (list.isEmpty()) {
                    g.setFont(font(Font.PLAIN, 12));
                    g.setColor(new Color(203, 213, 225));
                    textC(g, "Trống", x + colW / 2, HEAD + 30);
                }
                for (int j = 0; j < list.size(); j++) {
                    Session s = list.get(j);
                    double cy = HEAD + j * (CHIP + GAP);
                    Rectangle r = new Rectangle((int) x + 6, (int) cy, (int) colW - 12, CHIP);
                    hits.add(new Object[]{r, s});
                    paintChip(g, s, r);
                }
            }
            g.setColor(LINE);
            g.drawLine(0, HEAD - 2, w, HEAD - 2);
            g.dispose();
        }

        private void paintChip(Graphics2D g0, Session s, Rectangle r) {
            Graphics2D g = (Graphics2D) g0.create();
            boolean sel = s == selected, hov = s == hovered;
            if (s.past() && !sel) g.setComposite(AlphaComposite.SrcOver.derive(0.62f));
            Color sc = statusColor(s);
            if (sel || hov) shadow(g, r.x, r.y, r.width, r.height, 18, sel ? 1.4 : 0.9);
            g.setColor(sel ? SEL_BG : hov ? HOV_BG : Color.WHITE);
            g.fill(rr(r.x, r.y, r.width, r.height, 18));
            g.setColor(sel ? TEAL : LINE);
            g.setStroke(new BasicStroke(sel ? 2f : 1f));
            g.draw(rr(r.x + .5, r.y + .5, r.width - 1, r.height - 1, 18));
            g.setColor(sc);
            g.fill(rr(r.x + 7, r.y + 12, 4, r.height - 24, 4));
            int tx = r.x + 19, tw = r.width - 26;
            g.setFont(font(Font.BOLD, 12));
            g.setColor(s.off() ? MUTED : INK);
            FontMetrics fm = g.getFontMetrics();
            String ca = fit(fm, s.ca.isEmpty() ? "—" : s.ca, tw);
            text(g, ca, tx, r.y + 18);
            if (s.off()) {
                g.setStroke(new BasicStroke(1.2f));
                g.drawLine(tx, r.y + 18, tx + fm.stringWidth(ca), r.y + 18);
            }
            g.setFont(font(Font.PLAIN, 12));
            g.setColor(MUTED);
            text(g, fit(g.getFontMetrics(), s.lop, tw), tx, r.y + 36);
            g.setFont(font(Font.PLAIN, 11));
            g.setColor(s.off() ? RED : SLATE);
            text(g, fit(g.getFontMetrics(), s.off() ? "Nghỉ" + (s.phong.isEmpty() ? "" : " • " + s.phong) :
                    s.phong.isEmpty() ? "Chưa có phòng" : s.phong, tw), tx, r.y + 54);
            g.dispose();
        }

        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 28; }

        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 160; }

        public boolean getScrollableTracksViewportWidth() { return true; }

        public boolean getScrollableTracksViewportHeight() { return getParent() instanceof JViewport
                && getPreferredSize().height < getParent().getHeight(); }
    }

    /** Lịch tháng: lưới 7 cột, mỗi ngày hiện tối đa vài buổi, bấm vào ngày/buổi để chọn. */
    static final class MonthView extends JComponent {
        static final int HEAD = 34;
        private YearMonth month = YearMonth.now();
        private final Map<LocalDate, List<Session>> byDay = new HashMap<>();
        private Session selected;
        private LocalDate selDate;
        private Consumer<Session> onPick;
        private Consumer<LocalDate> onDay;
        private final List<Object[]> hits = new ArrayList<>();
        private final List<Object[]> dayHits = new ArrayList<>();

        MonthView() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    for (Object[] h : hits) {
                        if (((Rectangle) h[0]).contains(e.getPoint())) {
                            selected = (Session) h[1];
                            selDate = selected.ngay;
                            if (onPick != null) onPick.accept(selected);
                            repaint();
                            return;
                        }
                    }
                    for (Object[] h : dayHits) {
                        if (((Rectangle) h[0]).contains(e.getPoint())) {
                            selDate = (LocalDate) h[1];
                            if (onDay != null) onDay.accept(selDate);
                            repaint();
                            return;
                        }
                    }
                }
            });
        }

        void setOnPick(Consumer<Session> c) {
            onPick = c;
        }

        void setOnDay(Consumer<LocalDate> c) {
            onDay = c;
        }

        void setSelected(Session s) {
            selected = s;
            if (s != null) selDate = s.ngay;
            repaint();
        }

        YearMonth month() {
            return month;
        }

        void setMonth(YearMonth m) {
            month = m;
            repaint();
        }

        void setSessions(List<Session> l) {
            byDay.clear();
            for (Session s : l) {
                if (s.ngay == null) continue;
                List<Session> day = byDay.get(s.ngay);
                if (day == null) {
                    day = new ArrayList<>();
                    byDay.put(s.ngay, day);
                }
                day.add(s);
            }
            for (List<Session> day : byDay.values()) day.sort(TeacherDashboardUI::compareSession);
            repaint();
        }

        int countInMonth() {
            int n = 0;
            for (Map.Entry<LocalDate, List<Session>> en : byDay.entrySet())
                if (YearMonth.from(en.getKey()).equals(month)) n += en.getValue().size();
            return n;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            hits.clear();
            dayHits.clear();
            int w = getWidth(), h = getHeight();
            LocalDate first = month.atDay(1);
            LocalDate gridStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int offset = (int) ChronoUnit.DAYS.between(gridStart, first);
            int weeks = (int) Math.ceil((offset + month.lengthOfMonth()) / 7.0);
            double colW = w / 7.0, rowH = (h - HEAD) / (double) weeks;
            LocalDate today = LocalDate.now();
            g.setFont(font(Font.BOLD, 12));
            for (int i = 0; i < 7; i++) {
                g.setColor(i >= 5 ? RED : MUTED);
                textC(g, DAYS[i], i * colW + colW / 2, HEAD / 2.0);
            }
            g.setColor(LINE);
            g.drawLine(0, HEAD - 1, w, HEAD - 1);
            int maxChips = Math.max(0, (int) ((rowH - 34) / 21));
            for (int wk = 0; wk < weeks; wk++) {
                for (int i = 0; i < 7; i++) {
                    LocalDate d = gridStart.plusDays(wk * 7 + i);
                    double x = i * colW, y = HEAD + wk * rowH;
                    boolean inMonth = YearMonth.from(d).equals(month);
                    boolean isToday = d.equals(today), isSel = d.equals(selDate);
                    dayHits.add(new Object[]{new Rectangle((int) x, (int) y, (int) colW, (int) rowH), d});
                    g.setColor(inMonth ? (i >= 5 ? new Color(250, 251, 252) : Color.WHITE) : new Color(248, 250, 252));
                    g.fillRect((int) x, (int) y, (int) Math.ceil(colW), (int) Math.ceil(rowH));
                    g.setColor(new Color(241, 245, 249));
                    g.drawRect((int) x, (int) y, (int) colW, (int) rowH);
                    if (isSel) {
                        g.setColor(alpha(TEAL, 20));
                        g.fill(rr(x + 2, y + 2, colW - 4, rowH - 4, 14));
                        g.setColor(TEAL);
                        g.setStroke(new BasicStroke(1.6f));
                        g.draw(rr(x + 2, y + 2, colW - 4, rowH - 4, 14));
                    }
                    g.setFont(font(isToday ? Font.BOLD : Font.PLAIN, 12));
                    if (isToday) {
                        g.setColor(TEAL);
                        g.fill(new Ellipse2D.Double(x + 8, y + 6, 24, 24));
                        g.setColor(Color.WHITE);
                    } else g.setColor(inMonth ? INK : new Color(176, 186, 200));
                    textC(g, String.valueOf(d.getDayOfMonth()), x + 20, y + 18);
                    List<Session> list = byDay.get(d);
                    if (list == null) continue;
                    int shown = Math.min(list.size(), list.size() > maxChips ? Math.max(0, maxChips - 1) : maxChips);
                    for (int k = 0; k < shown; k++) {
                        Session s = list.get(k);
                        Rectangle r = new Rectangle((int) x + 5, (int) (y + 34 + k * 21), (int) colW - 10, 18);
                        hits.add(new Object[]{r, s});
                        Color sc = statusColor(s);
                        boolean sel = s == selected;
                        g.setColor(alpha(sc, sel ? 70 : 30));
                        g.fill(rr(r.x, r.y, r.width, r.height, 12));
                        if (sel) {
                            g.setColor(sc);
                            g.setStroke(new BasicStroke(1.4f));
                            g.draw(rr(r.x + .5, r.y + .5, r.width - 1, r.height - 1, 12));
                        }
                        g.setColor(sc);
                        g.fill(new Ellipse2D.Double(r.x + 6, r.y + 6, 6, 6));
                        g.setFont(font(Font.PLAIN, 11));
                        g.setColor(s.off() ? MUTED : INK);
                        String t = (s.ca.isEmpty() ? "" : s.ca + " · ") + s.lop;
                        text(g, fit(g.getFontMetrics(), t, r.width - 22), r.x + 17, r.y + 9);
                    }
                    if (list.size() > shown) {
                        g.setFont(font(Font.BOLD, 11));
                        g.setColor(TEAL_D);
                        text(g, "+" + (list.size() - shown) + " buổi nữa", x + 10, y + 34 + shown * 21 + 9);
                    }
                }
            }
            g.dispose();
        }
    }

    /** Dòng thời gian các buổi dạy trong một ngày (trang Tổng quan). */
    static final class AgendaView extends JComponent implements Scrollable {
        static final int ROW = 70;
        private List<Session> items = new ArrayList<>();
        private String emptyText = "Hôm nay không có buổi dạy nào";
        private int hov = -1;

        AgendaView() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = e.getY() / ROW;
                    if (i >= items.size()) i = -1;
                    if (i != hov) {
                        hov = i;
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hov = -1;
                    repaint();
                }
            });
        }

        void setSessions(List<Session> l, String emptyText) {
            items = l;
            this.emptyText = emptyText;
            setPreferredSize(new Dimension(300, Math.max(1, l.size()) * ROW));
            revalidate();
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth();
            if (items.isEmpty()) {
                g.setColor(new Color(240, 253, 250));
                g.fill(new Ellipse2D.Double(w / 2.0 - 34, 14, 68, 68));
                Icons.paint(g, "calendar", w / 2.0 - 16, 30, 32, new Color(94, 234, 212));
                g.setFont(font(Font.PLAIN, 13));
                g.setColor(MUTED);
                textC(g, emptyText, w / 2.0, 108);
                g.dispose();
                return;
            }
            int timeW = 96;
            for (int i = 0; i < items.size(); i++) {
                Session s = items.get(i);
                int y = i * ROW;
                boolean on = i == hov;
                Color sc = statusColor(s);
                g.setColor(new Color(226, 232, 240));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(timeW + 12, i == 0 ? y + ROW / 2 : y, timeW + 12, i == items.size() - 1 ? y + ROW / 2 : y + ROW);
                g.setFont(font(Font.BOLD, 12));
                g.setColor(s.off() ? MUTED : INK);
                textR(g, fit(g.getFontMetrics(), s.ca.isEmpty() ? "—" : s.ca, timeW - 4), timeW - 4, y + ROW / 2.0);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(timeW + 5, y + ROW / 2.0 - 7, 14, 14));
                g.setColor(sc);
                g.fill(new Ellipse2D.Double(timeW + 8, y + ROW / 2.0 - 4, 8, 8));
                double cx = timeW + 32, cw = w - cx - 4;
                g.setColor(on ? new Color(240, 253, 250) : new Color(248, 250, 252));
                g.fill(rr(cx, y + 6, cw, ROW - 12, 20));
                g.setColor(alpha(sc, 160));
                g.fill(rr(cx, y + 18, 4, ROW - 36, 4));
                g.setFont(font(Font.BOLD, 14));
                g.setColor(s.off() ? MUTED : INK);
                String pillText = s.off() ? OFF : DAY;
                g.setFont(font(Font.BOLD, 12));
                int pw = g.getFontMetrics().stringWidth(pillText) + 22;
                g.setFont(font(Font.BOLD, 14));
                text(g, fit(g.getFontMetrics(), s.lop, cw - pw - 40), cx + 18, y + 27);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(MUTED);
                String sub = (s.phong.isEmpty() ? "Chưa có phòng" : s.phong) + (s.noiDung.isEmpty() ? "" : "  •  " + s.noiDung);
                text(g, fit(g.getFontMetrics(), sub, cw - pw - 40), cx + 18, y + 47);
                g.setColor(alpha(sc, 30));
                g.fill(rr(cx + cw - pw - 14, y + ROW / 2.0 - 13, pw, 26, 26));
                g.setFont(font(Font.BOLD, 12));
                g.setColor(shade(sc, 0.85));
                textC(g, pillText, cx + cw - pw / 2.0 - 14, y + ROW / 2.0);
            }
            g.dispose();
        }
    
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 28; }

        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 140; }

        public boolean getScrollableTracksViewportWidth() { return true; }

        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /* ============================ SIDEBAR / TOAST / HERO ============================ */

    static final class NavItem extends JComponent {
        final String label, icon;
        private final Tween hover = new Tween(0, this::repaint), sel = new Tween(0, this::repaint);
        private int badge;

        NavItem(String label, String icon, Runnable click) {
            this.label = label;
            this.icon = icon;
            setPreferredSize(new Dimension(232, 46));
            setMinimumSize(new Dimension(100, 46));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover.to(1, 160); }

                @Override public void mouseExited(MouseEvent e) { hover.to(0, 240); }

                @Override public void mousePressed(MouseEvent e) { click.run(); }
            });
        }

        void setSelected(boolean s) {
            sel.to(s ? 1 : 0, 280);
        }

        void setBadge(int n) {
            badge = n;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            double s = sel.get(), hv = hover.get();
            if (hv > 0.01 && s < 0.99) {
                g.setColor(new Color(255, 255, 255, (int) (24 * hv * (1 - s))));
                g.fill(rr(0, 0, w, h, 28));
            }
            if (s > 0.01) {
                g.setPaint(new GradientPaint(0, 0, alpha(new Color(0x14B8A6), (int) (255 * s)),
                        w, 0, alpha(new Color(0x0D9488), (int) (255 * s))));
                g.fill(rr(0, 0, w, h, 28));
            }
            double k = Math.max(s, hv * 0.75);
            Icons.paint(g, icon, 16 + hv * 2, (h - 20) / 2.0, 20, lerp(new Color(153, 246, 228), Color.WHITE, k));
            g.setFont(font(s > 0.5 ? Font.BOLD : Font.PLAIN, 14));
            g.setColor(lerp(new Color(203, 213, 225), Color.WHITE, k));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, 50 + (int) (hv * 2), (h + fm.getAscent() - fm.getDescent()) / 2);
            if (badge > 0) {
                String b = badge > 99 ? "99+" : String.valueOf(badge);
                g.setFont(font(Font.BOLD, 11));
                fm = g.getFontMetrics();
                int bw = Math.max(22, fm.stringWidth(b) + 14), bx = w - bw - 12;
                g.setColor(new Color(239, 68, 68));
                g.fill(rr(bx, (h - 20) / 2.0, bw, 20, 20));
                g.setColor(Color.WHITE);
                g.drawString(b, bx + (bw - fm.stringWidth(b)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }
    }

    /** Lớp thông báo nổi (toast) - không chặn chuột */
    static final class ToastLayer extends JComponent {
        private String msg = "";
        private int kind;
        private final Tween show = new Tween(0, this::repaint);
        private Timer hide;

        ToastLayer() {
            setOpaque(false);
        }

        @Override public boolean contains(int x, int y) {
            return false;
        }

        void post(String m, int k) {
            msg = m;
            kind = k;
            show.to(1, 320);
            if (hide != null) hide.stop();
            hide = new Timer(3600, e -> show.to(0, 320));
            hide.setRepeats(false);
            hide.start();
        }

        @Override protected void paintComponent(Graphics g0) {
            double s = show.get();
            if (s < 0.01) return;
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            Color c = kind == T_OK ? GREEN : kind == T_WARN ? AMBER : kind == T_ERR ? RED : BLUE;
            String ic = kind == T_OK ? "check" : kind == T_INFO ? "info" : "alert";
            g.setFont(font(Font.PLAIN, 14));
            FontMetrics fm = g.getFontMetrics();
            int maxText = Math.min(560, getWidth() - 160);
            String t = fit(fm, msg, maxText);
            int w = fm.stringWidth(t) + 92, h = 56;
            double x = getWidth() - w - 28, y = getHeight() - h - 28 + (1 - s) * 36;
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, s)));
            shadow(g, x, y, w, h, 28, 1.4);
            g.setColor(Color.WHITE);
            g.fill(rr(x, y, w, h, 28));
            g.setColor(alpha(c, 32));
            g.fill(new Ellipse2D.Double(x + 12, y + 12, 32, 32));
            Icons.paint(g, ic, x + 19, y + 19, 18, c);
            g.setColor(INK);
            g.drawString(t, (int) x + 56, (int) y + (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Vùng nội dung: trang mới trượt lên + mờ dần vào */
    static final class FadePanel extends JPanel {
        private final Tween t = new Tween(1, this::repaint);

        FadePanel() {
            super(new BorderLayout());
            setOpaque(true);
            setBackground(BG);
        }

        void show(JComponent c, boolean animate) {
            removeAll();
            add(c, BorderLayout.CENTER);
            revalidate();
            if (animate) {
                t.set(0);
                t.to(1, 420);
            } else t.set(1);
        }

        @Override public void paint(Graphics g0) {
            double a = t.get();
            if (a >= 0.999) {
                super.paint(g0);
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, a))));
            g.translate(0, (1 - a) * 22);
            super.paint(g);
            g.dispose();
        }
    }

    /** Hồ sơ giáo viên (thẻ bên trái ở trang cá nhân). */
    static final class ProfileHero extends JComponent {
        private final String name, role, id, phone, nation;
        private final String[] labels;
        private final long[] nums;
        private final Tween grow = new Tween(0, this::repaint);

        ProfileHero(String name, String role, String id, String phone, String nation, String[] labels, long[] nums) {
            this.name = name;
            this.role = role;
            this.id = id;
            this.phone = phone;
            this.nation = nation;
            this.labels = labels;
            this.nums = nums;
            setPreferredSize(new Dimension(340, 510));
            grow.to(1, 900);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int P = Card.P;
            double x = P, y = P, w = getWidth() - 2 * P, h = getHeight() - 2 * P;
            shadow(g, x, y, w, h, 32, 0.85);
            Shape old = g.getClip();
            g.clip(rr(x, y, w, h, 32));
            g.setColor(Color.WHITE);
            g.fill(rr(x, y, w, h, 32));
            g.setPaint(new GradientPaint((float) x, (float) y, TEAL_D, (float) (x + w), (float) (y + 130), new Color(0x14B8A6)));
            g.fillRect((int) x, (int) y, (int) w, 130);
            g.setColor(new Color(255, 255, 255, 22));
            g.fill(new Ellipse2D.Double(x + w - 110, y - 60, 200, 200));
            g.fill(new Ellipse2D.Double(x - 50, y + 50, 140, 140));
            g.setClip(old);
            double cx = x + w / 2;
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(cx - 54, y + 130 - 54, 108, 108));
            Avatar.paint(g, initialsOf(name), cx - 48, y + 130 - 48, 96);
            g.setFont(font(Font.BOLD, 21));
            g.setColor(INK);
            textC(g, fit(g.getFontMetrics(), name, w - 30), cx, y + 214);
            g.setFont(font(Font.PLAIN, 13));
            g.setColor(MUTED);
            textC(g, id, cx, y + 238);
            String r = role.isEmpty() ? "Giáo viên" : "Chứng chỉ " + role;
            g.setFont(font(Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int pw = fm.stringWidth(r) + 28;
            g.setColor(alpha(TEAL, 28));
            g.fill(rr(cx - pw / 2.0, y + 254, pw, 28, 28));
            g.setColor(TEAL_D);
            textC(g, r, cx, y + 268);
            g.setColor(LINE);
            g.drawLine((int) x + 24, (int) y + 306, (int) (x + w - 24), (int) y + 306);
            double col = (w - 48) / 3.0;
            for (int i = 0; i < 3; i++) {
                double mx = x + 24 + col * i + col / 2;
                String n = String.valueOf(Math.round(nums[i] * grow.get()));
                g.setFont(font(Font.BOLD, 24));
                g.setColor(TEAL_D);
                textC(g, n, mx, y + 342);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(MUTED);
                textC(g, labels[i], mx, y + 366);
            }
            g.setColor(LINE);
            g.drawLine((int) x + 24, (int) y + 392, (int) (x + w - 24), (int) y + 392);
            g.setFont(font(Font.PLAIN, 13));
            Icons.paint(g, "phone", x + 30, y + 408, 18, TEAL);
            g.setColor(INK);
            text(g, dash(phone), x + 58, y + 417);
            Icons.paint(g, "globe", x + 30, y + 440, 18, TEAL);
            g.setColor(INK);
            text(g, dash(nation), x + 58, y + 449);
            Icons.paint(g, "cert", x + 30, y + 472, 18, TEAL);
            g.setColor(INK);
            text(g, role.isEmpty() ? "—" : role, x + 58, y + 481);
            g.dispose();
        }
    }

    /** Một dòng trong hộp thông báo. */
    static final class Notif {
        final String icon, title, sub, page;
        final Color color;

        Notif(String icon, Color color, String title, String sub, String page) {
            this.icon = icon;
            this.color = color;
            this.title = title;
            this.sub = sub;
            this.page = page;
        }
    }

    /* ============================ TRẠNG THÁI KHUNG ============================ */

    private static TeacherDashboardUI current;

    static void toast(String msg, int kind) {
        if (current != null) SwingUtilities.invokeLater(() -> current.toastLayer.post(msg, kind));
    }

    private static final class PageDef {
        final String key, title, subtitle, icon;
        final Supplier<JComponent> factory;
        NavItem nav;

        PageDef(String key, String title, String subtitle, String icon, Supplier<JComponent> f) {
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
            this.icon = icon;
            this.factory = f;
        }
    }

    private final String idGiaoVien;
    private String hoTen = "", soDienThoai = "", chungChi = "", quocTich = "";
    private boolean found = false;
    private String startupMsg;

    private final Map<String, PageDef> pages = new LinkedHashMap<>();
    private final FadePanel host = new FadePanel();
    private final ToastLayer toastLayer = new ToastLayer();
    private final JLabel pageTitle = lbl(" ", font(Font.BOLD, 22), INK);
    private final JLabel pageSub = lbl(" ", font(Font.PLAIN, 13), MUTED);
    private final JLabel clock = lbl(" ", font(Font.PLAIN, 13), MUTED);
    private final IconButton bell = new IconButton("bell", "Thông báo");
    private Timer clockTimer;
    private String currentKey = "";

    public TeacherDashboardUI() {
        this("GV01");
    }

    public TeacherDashboardUI(String idGiaoVien) {
        super("Cổng Giáo Viên S-Link");
        this.idGiaoVien = idGiaoVien;
        current = this;
        loadProfile();
        setTitle("Cổng Giáo Viên S-Link - " + hoTen);
        if (found) async(() -> {
            Db.ensureSchema();
            return Boolean.TRUE;
        }, ok -> { });

        pages.put("Dashboard", new PageDef("Dashboard", "Tổng quan", "Bức tranh nhanh về lịch dạy và kết quả giảng dạy của bạn",
                "home", this::createOverviewPage));
        pages.put("Lop", new PageDef("Lop", "Lớp phụ trách", "Các lớp bạn đang được phân công",
                "book", this::createClassPage));
        pages.put("Lich", new PageDef("Lich", "Lịch dạy", "Xem theo danh sách, tuần hoặc tháng; báo nghỉ / khôi phục buổi dạy",
                "calendar", this::createSchedulePage));
        pages.put("HocSinh", new PageDef("HocSinh", "Học sinh của tôi", "Học sinh do bạn phụ trách và hồ sơ học tập",
                "group", this::createStudentPage));
        pages.put("Thi", new PageDef("Thi", "Điểm thi học sinh", "Kết quả thi thử JLPT, tỉ lệ đạt và phổ điểm",
                "award", this::createExamPage));
        pages.put("PhanHoi", new PageDef("PhanHoi", "Phản hồi học sinh", "Đánh giá và góp ý từ các lớp bạn dạy",
                "star", this::createFeedbackPage));
        pages.put("Luong", new PageDef("Luong", "Phiếu lương", "Lịch sử nhận lương và chi tiết từng kỳ",
                "wallet", this::createPayslipPage));
        pages.put("HoSo", new PageDef("HoSo", "Thông tin cá nhân", "Hồ sơ giáo viên và chỉ số giảng dạy",
                "user", this::createProfilePage));
        for (PageDef p : pages.values()) {
            final String key = p.key;
            p.nav = new NavItem(p.title, p.icon, () -> navigate(key, true));
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(buildHeader(), BorderLayout.NORTH);
        right.add(host, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
        setGlassPane(toastLayer);
        toastLayer.setVisible(true);

        setIconImage(appIcon());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1340, 840);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (clockTimer != null) clockTimer.stop();
            }
        });
        installShortcuts();
        if (found) {
            navigate("Dashboard", true);
            refreshBadges();
        }
        SwingUtilities.invokeLater(() -> {
            if (startupMsg != null) JOptionPane.showMessageDialog(this, startupMsg, "Thông báo", JOptionPane.WARNING_MESSAGE);
        });
    }

    @Override public void setVisible(boolean b) {
        super.setVisible(b && found);
    }

    private void loadProfile() {
        try {
            List<Object[]> r = Db.query("SELECT ho_ten, so_dien_thoai, chung_chi, quoc_tich FROM Giao_vien "
                            + "WHERE id_giao_vien = ?",
                    rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)}, idGiaoVien);
            if (r.isEmpty()) {
                hoTen = idGiaoVien;
                startupMsg = "Không tìm thấy giáo viên có mã " + idGiaoVien + " trong CSDL.";
                JOptionPane.showMessageDialog(null, startupMsg, "Thông báo", JOptionPane.WARNING_MESSAGE);
                startupMsg = null;
                return;
            }
            found = true;
            hoTen = nz((String) r.get(0)[0]).isEmpty() ? idGiaoVien : nz((String) r.get(0)[0]);
            soDienThoai = nz((String) r.get(0)[1]);
            chungChi = nz((String) r.get(0)[2]);
            quocTich = nz((String) r.get(0)[3]);
        } catch (SQLException e) {
            hoTen = idGiaoVien;
            JOptionPane.showMessageDialog(null, "Không đọc được hồ sơ giáo viên: " + rootMsg(e)
                    + "\n\nKiểm tra DB_URL, DB_USER, DB_PASS và MySQL Connector/J.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Image appIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        aa(g);
        g.setPaint(new GradientPaint(0, 0, TEAL_L, 64, 64, TEAL_D));
        g.fill(rr(2, 2, 60, 60, 28));
        g.setColor(Color.WHITE);
        g.setFont(font(Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("S", (64 - fm.stringWidth("S")) / 2, (64 + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }

    private void installShortcuts() {
        JRootPane rp = getRootPane();
        String[] keys = pages.keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length && i < 9; i++) {
            final String k = keys[i];
            String name = "go" + i;
            rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, InputEvent.CTRL_DOWN_MASK), name);
            rp.getActionMap().put(name, new AbstractAction() {
                public void actionPerformed(ActionEvent e) { navigate(k, true); }
            });
        }
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        rp.getActionMap().put("refresh", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { reloadPage(); }
        });
    }

    /* ============================ KHUNG GIAO DIỆN ============================ */

    private JPanel buildSidebar() {
        JPanel sb = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setPaint(new GradientPaint(0, 0, new Color(0x0F172A), 0, getHeight(), new Color(0x134E4A)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(255, 255, 255, 10));
                g.fill(new Ellipse2D.Double(-90, getHeight() - 210, 280, 280));
                g.setColor(new Color(45, 212, 191, 26));
                g.fill(new Ellipse2D.Double(getWidth() - 100, -70, 220, 220));
                g.dispose();
            }
        };
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setPreferredSize(new Dimension(264, 0));
        sb.setBorder(new EmptyBorder(24, 16, 18, 16));

        JComponent logo = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setPaint(new GradientPaint(0, 0, TEAL_L, 44, 44, TEAL_D));
                g.fill(rr(4, 2, 44, 44, 28));
                g.setColor(Color.WHITE);
                g.setFont(font(Font.BOLD, 26));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("S", 4 + (44 - fm.stringWidth("S")) / 2, 2 + (44 + fm.getAscent() - fm.getDescent()) / 2);
                g.setFont(font(Font.BOLD, 20));
                g.drawString("S-Link", 62, 26);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(new Color(0x5EEAD4));
                g.drawString("Teacher Portal", 62, 43);
                g.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(232, 50));
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(logo);
        sb.add(Box.createVerticalStrut(28));
        JLabel menu = lbl("MENU", font(Font.BOLD, 11), new Color(0x5EEAD4));
        menu.setBorder(new EmptyBorder(0, 14, 8, 0));
        sb.add(menu);
        for (PageDef p : pages.values()) {
            sb.add(p.nav);
            sb.add(Box.createVerticalStrut(4));
        }
        sb.add(Box.createVerticalGlue());

        JComponent hint = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setFont(font(Font.PLAIN, 11));
                g.setColor(new Color(148, 163, 184));
                g.drawString("Ctrl + 1…8 chuyển trang   •   F5 tải lại", 14, 14);
                g.dispose();
            }
        };
        hint.setPreferredSize(new Dimension(232, 26));
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(hint);

        final String sub = idGiaoVien + (chungChi.isEmpty() ? "" : " • " + chungChi);
        JComponent chip = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setColor(new Color(255, 255, 255, 22));
                g.fill(rr(0, 0, getWidth(), getHeight(), 28));
                Avatar.paint(g, initialsOf(hoTen), 12, 11, 42);
                g.setFont(font(Font.BOLD, 13));
                g.setColor(Color.WHITE);
                g.drawString(fit(g.getFontMetrics(), hoTen, getWidth() - 76), 64, 29);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(new Color(0x99F6E4));
                g.drawString(fit(g.getFontMetrics(), sub, getWidth() - 76), 64, 46);
                g.dispose();
            }
        };
        chip.setPreferredSize(new Dimension(232, 64));
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(chip);
        sb.add(Box.createVerticalStrut(10));

        FlatButton btnLogout = new FlatButton("Đăng xuất", "logout", new Color(239, 68, 68), true);
        btnLogout.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogout.addActionListener(e -> {
            int r = JOptionPane.showOptionDialog(TeacherDashboardUI.this,
                    "Bạn có muốn đăng xuất khỏi hệ thống?", "Xác nhận đăng xuất",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    new Object[]{"Đăng xuất", "Ở lại"}, "Ở lại");
            if (r == 0) {
                dispose();
                new LoginForm().setVisible(true);
            }
        });
        sb.add(btnLogout);
        return sb;
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(LINE);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        h.setPreferredSize(new Dimension(0, 78));
        h.setBorder(new EmptyBorder(0, 32, 0, 28));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(Box.createVerticalGlue());
        left.add(pageTitle);
        left.add(Box.createVerticalStrut(2));
        left.add(pageSub);
        left.add(Box.createVerticalGlue());
        h.add(left, BorderLayout.WEST);

        IconButton refresh = new IconButton("refresh", "Tải lại trang (F5)");
        refresh.addActionListener(e -> reloadPage());
        bell.addActionListener(e -> showNotifications());
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 18));
        r.setOpaque(false);
        r.add(clock);
        r.add(refresh);
        r.add(bell);
        r.add(new Avatar(hoTen, 40));
        h.add(r, BorderLayout.EAST);
        clock.setText(nowText());
        clockTimer = new Timer(1000, e -> clock.setText(nowText()));
        clockTimer.start();
        return h;
    }

    void navigate(String key, boolean animate) {
        PageDef p = pages.get(key);
        if (p == null) return;
        currentKey = key;
        for (PageDef d : pages.values()) d.nav.setSelected(d == p);
        pageTitle.setText(p.title);
        pageSub.setText(p.subtitle);
        host.show(p.factory.get(), animate);
    }

    private void reloadPage() {
        if (!found) return;
        navigate(currentKey.isEmpty() ? "Dashboard" : currentKey, false);
        refreshBadges();
        toast("Đã tải lại dữ liệu.", T_INFO);
    }

    private void refreshBadges() {
        async(() -> Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                + "AND DATE(ngay_day) = CURDATE()", idGiaoVien, DAY), n -> {
            PageDef p = pages.get("Lich");
            if (p != null) p.nav.setBadge(n.intValue());
        });
        async(this::loadNotifs, list -> bell.setBadge(list.size()));
    }

    /* ============================ THÔNG BÁO ============================ */

    private List<Notif> loadNotifs() throws SQLException {
        List<Notif> out = new ArrayList<>();
        List<Object[]> today = Db.query("SELECT COUNT(*), COALESCE(SUM(trang_thai_buoi = ?), 0) FROM Lich_hoc "
                        + "WHERE id_giao_vien = ? AND DATE(ngay_day) = CURDATE()",
                rs -> new Object[]{rs.getLong(1), rs.getLong(2)}, DAY, idGiaoVien);
        long total = today.isEmpty() ? 0 : (Long) today.get(0)[0], teach = today.isEmpty() ? 0 : (Long) today.get(0)[1];
        if (teach > 0) out.add(new Notif("calendar", TEAL, "Hôm nay bạn có " + teach + " buổi dạy",
                "Mở lịch dạy để xem chi tiết ca học và phòng học", "Lich"));
        else if (total > 0) out.add(new Notif("calendar", AMBER, "Hôm nay bạn đang báo nghỉ",
                total + " buổi trong ngày đã được báo nghỉ", "Lich"));
        List<Object[]> next = Db.query("SELECT lh.ngay_day, lh.ca_hoc, COALESCE(l.ten_lop, lh.id_lop) FROM Lich_hoc lh "
                        + "LEFT JOIN Lop l ON lh.id_lop = l.id_lop WHERE lh.id_giao_vien = ? AND lh.trang_thai_buoi = ? "
                        + "AND DATE(lh.ngay_day) > CURDATE() ORDER BY lh.ngay_day, lh.ca_hoc LIMIT 1",
                rs -> new Object[]{rs.getTimestamp(1), rs.getString(2), rs.getString(3)}, idGiaoVien, DAY);
        if (!next.isEmpty()) {
            LocalDate d = ld((Timestamp) next.get(0)[0]);
            long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
            out.add(new Notif("clock", BLUE, "Buổi dạy kế tiếp: " + nz((String) next.get(0)[2]),
                    (days == 1 ? "Ngày mai" : "Sau " + days + " ngày") + " • " + d.format(D_FMT)
                            + (nz((String) next.get(0)[1]).isEmpty() ? "" : " • " + nz((String) next.get(0)[1])), "Lich"));
        }
        long off = Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                + "AND DATE(ngay_day) >= CURDATE()", idGiaoVien, OFF);
        if (off > 0) out.add(new Notif("alert", AMBER, "Bạn có " + off + " buổi đã báo nghỉ sắp tới",
                "Có thể khôi phục buổi dạy nếu kế hoạch thay đổi", "Lich"));
        List<Object[]> fb = Db.query("SELECT COUNT(*), COALESCE(MIN(rating), 0) FROM Danh_gia_va_phan_hoi "
                        + "WHERE id_lop IN (SELECT id_lop FROM Quanli WHERE id_giao_vien = ?) "
                        + "AND ngay_gui >= DATE_SUB(NOW(), INTERVAL 7 DAY)",
                rs -> new Object[]{rs.getLong(1), rs.getLong(2)}, idGiaoVien);
        if (!fb.isEmpty() && (Long) fb.get(0)[0] > 0) {
            long c = (Long) fb.get(0)[0], mn = (Long) fb.get(0)[1];
            out.add(new Notif("star", mn > 0 && mn <= 2 ? RED : AMBER, c + " phản hồi mới trong 7 ngày qua",
                    mn > 0 && mn <= 2 ? "Có đánh giá thấp (" + mn + " sao) cần xem lại" : "Xem góp ý từ học sinh của bạn", "PhanHoi"));
        }
        List<Object[]> pay = Db.query("SELECT ma_phieu_luong, tong_luong, ngay_tra FROM Phieu_luong "
                        + "WHERE id_giao_vien = ? ORDER BY ngay_tra DESC LIMIT 1",
                rs -> new Object[]{rs.getString(1), rs.getLong(2), rs.getTimestamp(3)}, idGiaoVien);
        if (!pay.isEmpty() && pay.get(0)[2] != null) {
            LocalDate d = ld((Timestamp) pay.get(0)[2]);
            if (!d.isBefore(LocalDate.now().minusDays(30)))
                out.add(new Notif("wallet", GREEN, "Phiếu lương " + pay.get(0)[0] + " đã được trả",
                        vnd((Long) pay.get(0)[1]) + " • " + d.format(D_FMT), "Luong"));
        }
        return out;
    }

    private void showNotifications() {
        if (!found) return;
        async(this::loadNotifs, list -> {
            JPopupMenu pm = new JPopupMenu();
            pm.setBorder(BorderFactory.createLineBorder(LINE));
            JPanel box = new JPanel();
            box.setBackground(Color.WHITE);
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            JLabel head = lbl("Thông báo", font(Font.BOLD, 16), INK);
            head.setBorder(new EmptyBorder(16, 18, 10, 18));
            box.add(head);
            if (list.isEmpty()) {
                JLabel none = lbl("Bạn đã xem hết thông báo.", font(Font.PLAIN, 13), MUTED);
                none.setBorder(new EmptyBorder(8, 18, 22, 18));
                box.add(none);
            }
            for (Notif n : list) box.add(notifRow(n, pm));
            box.setPreferredSize(new Dimension(380, box.getPreferredSize().height + 6));
            pm.add(box);
            pm.show(bell, bell.getWidth() - 380, bell.getHeight() + 8);
            bell.setBadge(list.size());
        });
    }

    private JComponent notifRow(Notif n, JPopupMenu pm) {
        JComponent row = new JComponent() {
            private final Tween hv = new Tween(0, this::repaint);

            {
                setPreferredSize(new Dimension(380, 66));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
                setAlignmentX(Component.LEFT_ALIGNMENT);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hv.to(1, 120); }

                    @Override public void mouseExited(MouseEvent e) { hv.to(0, 160); }

                    @Override public void mousePressed(MouseEvent e) {
                        pm.setVisible(false);
                        navigate(n.page, true);
                    }
                });
            }

            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setColor(lerp(Color.WHITE, new Color(240, 253, 250), hv.get()));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(alpha(n.color, 30));
                g.fill(new Ellipse2D.Double(16, 14, 38, 38));
                Icons.paint(g, n.icon, 25, 23, 20, n.color);
                g.setFont(font(Font.BOLD, 13));
                g.setColor(INK);
                g.drawString(fit(g.getFontMetrics(), n.title, getWidth() - 86), 68, 30);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(MUTED);
                g.drawString(fit(g.getFontMetrics(), n.sub, getWidth() - 86), 68, 49);
                g.dispose();
            }
        };
        return row;
    }

    /* ============================ HỘP THOẠI ============================ */

    /** Hộp thoại hiện đại. onOk trả về thông báo lỗi (giữ hộp thoại, rung) hoặc null (đóng, thành công). */
    boolean dialog(String title, String sub, JComponent body, String okText, Color okColor,
                   Supplier<String> onOk, boolean withCancel) {
        return dialog(title, sub, body, okText, okColor, onOk, withCancel, 540);
    }

    boolean dialog(String title, String sub, JComponent body, String okText, Color okColor,
                   Supplier<String> onOk, boolean withCancel, int minWidth) {
        JDialog d = new JDialog(this, true);
        d.setUndecorated(true);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createLineBorder(new Color(153, 246, 228)));

        JPanel head = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setPaint(new GradientPaint(0, 0, TEAL_D, getWidth(), getHeight(), new Color(0x14B8A6)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(255, 255, 255, 26));
                g.fill(new Ellipse2D.Double(getWidth() - 90, -60, 170, 170));
                g.dispose();
            }
        };
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(new EmptyBorder(20, 26, 18, 26));
        head.add(lbl(title, font(Font.BOLD, 19), Color.WHITE));
        if (sub != null) {
            head.add(Box.createVerticalStrut(3));
            head.add(lbl(sub, font(Font.PLAIN, 13), new Color(204, 251, 241)));
        }
        final Point[] drag = {null};
        head.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        head.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { drag[0] = e.getPoint(); }
        });
        head.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (drag[0] == null) return;
                Point p = d.getLocation();
                d.setLocation(p.x + e.getX() - drag[0].x, p.y + e.getY() - drag[0].y);
            }
        });
        root.add(head, BorderLayout.NORTH);

        JLabel err = lbl(" ", font(Font.BOLD, 12), RED);
        err.setBorder(new EmptyBorder(0, 26, 4, 26));
        body.setBorder(new EmptyBorder(22, 26, 8, 26));
        JPanel mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.add(body, BorderLayout.CENTER);
        mid.add(err, BorderLayout.SOUTH);
        root.add(mid, BorderLayout.CENTER);

        FlatButton ok = new FlatButton(okText, null, okColor, false);
        FlatButton cancel = new FlatButton("Hủy", null, MUTED, true);
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        foot.setOpaque(false);
        foot.setBorder(new EmptyBorder(6, 26, 22, 26));
        if (withCancel) foot.add(cancel);
        foot.add(ok);
        root.add(foot, BorderLayout.SOUTH);

        boolean[] result = {false};
        cancel.addActionListener(e -> d.dispose());
        ok.addActionListener(e -> {
            String m = onOk.get();
            if (m == null) {
                result[0] = true;
                d.dispose();
            } else {
                err.setText(m);
                Point p0 = d.getLocation();
                int[] n = {0};
                new Timer(16, ev -> {
                    n[0]++;
                    d.setLocation(p0.x + (int) (Math.sin(n[0] * 1.3) * 9 * Math.max(0, 1 - n[0] / 22.0)), p0.y);
                    if (n[0] > 22) {
                        ((Timer) ev.getSource()).stop();
                        d.setLocation(p0);
                    }
                }).start();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        root.getActionMap().put("esc", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { d.dispose(); }
        });
        d.setContentPane(root);
        d.getRootPane().setDefaultButton(ok);
        Dimension ps = root.getPreferredSize();
        int maxH = Math.max(420, getHeight() - 80);
        root.setPreferredSize(new Dimension(Math.max(minWidth, ps.width), Math.min(maxH, ps.height)));
        d.pack();
        d.setLocationRelativeTo(this);
        Tween[] tw = new Tween[1];
        try {
            d.setOpacity(0.05f);
            tw[0] = new Tween(0.05, () -> {
                try {
                    d.setOpacity((float) Math.max(0.05, Math.min(1, tw[0].get())));
                } catch (Exception ignored) { }
            });
            tw[0].to(1, 220);
        } catch (Exception ignored) { }
        d.setVisible(true);
        return result[0];
    }

    /** Hộp thoại chỉ để xem thông tin (có thanh cuộn khi nội dung dài). */
    void showInfo(String title, String sub, JComponent body, int width, int height) {
        JScrollPane sp = new JScrollPane(body);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setPreferredSize(new Dimension(width, height));
        dialog(title, sub, sp, "Đóng", TEAL, () -> null, false, width + 60);
    }

    private boolean confirm(String title, String html, String okText, Color okColor) {
        JLabel l = new JLabel("<html><div style='width:420px;line-height:1.5'>" + html + "</div></html>");
        l.setFont(font(Font.PLAIN, 14));
        l.setForeground(INK);
        return dialog(title, "Vui lòng kiểm tra kỹ trước khi xác nhận", l, okText, okColor, () -> null, true);
    }

    /* ============================ THANH CÔNG CỤ DÙNG CHUNG ============================ */

    /** Thanh phải của thẻ: (lọc trạng thái) + ô tìm kiếm + nút xuất CSV. */
    private JPanel toolbar(DataGrid g, String[] statuses, int statusCol, String placeholder, String exportName) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        p.setOpaque(false);
        StyledField s = new StyledField("").withPlaceholder(placeholder, "search");
        s.setPreferredSize(new Dimension(250, 42));
        s.getDocument().addDocumentListener(doc(() -> g.setText(s.getText())));
        if (statuses != null) {
            p.add(new Segmented(statuses, i -> g.setStatus(i == 0 ? null : statuses[i], statusCol)));
        }
        p.add(s);
        if (exportName != null) {
            IconButton ex = new IconButton("download", "Xuất CSV (các dòng đang hiển thị)");
            ex.addActionListener(e -> g.exportCsv(exportName));
            p.add(ex);
        }
        return p;
    }

    /** Thanh sắp xếp + lọc theo cột, đặt phía trên bảng. */
    private JPanel sortBar(DataGrid g, String[] sortLabels, int[] sortCols, boolean[] sortAsc,
                           String[] fLabels, int[] fCols, String[][] fValues) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        String[] sl = new String[sortLabels.length + 1];
        sl[0] = "Mặc định";
        System.arraycopy(sortLabels, 0, sl, 1, sortLabels.length);
        RoundCombo cbSort = new RoundCombo(sl);
        cbSort.setPreferredSize(new Dimension(230, 42));
        cbSort.addActionListener(e -> {
            int i = cbSort.getSelectedIndex();
            if (i <= 0) g.sortBy(-1, true);
            else g.sortBy(sortCols[i - 1], sortAsc[i - 1]);
        });
        p.add(lbl("Sắp xếp:", font(Font.BOLD, 12), MUTED));
        p.add(cbSort);
        List<RoundCombo> combos = new ArrayList<>();
        for (int k = 0; k < fLabels.length; k++) {
            final int col = fCols[k];
            final String[] vals = fValues[k];
            String[] items = new String[vals.length + 1];
            items[0] = fLabels[k] + ": Tất cả";
            for (int i = 0; i < vals.length; i++) items[i + 1] = fLabels[k] + ": " + vals[i];
            RoundCombo cb = new RoundCombo(items);
            cb.setPreferredSize(new Dimension(210, 42));
            cb.addActionListener(e -> {
                int i = cb.getSelectedIndex();
                g.setColFilter(col, i <= 0 ? null : vals[i - 1]);
            });
            combos.add(cb);
            p.add(cb);
        }
        FlatButton clear = new FlatButton("Xóa lọc", "undo", MUTED, true);
        clear.addActionListener(e -> {
            cbSort.setSelectedIndex(0);
            for (RoundCombo c : combos) c.setSelectedIndex(0);
        });
        p.add(clear);
        return p;
    }

    /** Ghép thanh phụ lên trên nội dung. */
    private static JComponent withBar(JComponent bar, JComponent body) {
        JPanel mid = new JPanel(new BorderLayout(0, 12));
        mid.setOpaque(false);
        mid.add(bar, BorderLayout.NORTH);
        mid.add(body, BorderLayout.CENTER);
        return mid;
    }

    /** Trang danh sách chuẩn: thẻ + thanh công cụ + bảng. */
    private JComponent listPage(String title, String subtitle, String[] cols, String sql, RowMapper map,
                                String empty, Set<Integer> pills, String[] statuses, int statusCol, Object... params) {
        DataGrid g = new DataGrid(cols, empty, pills, false);
        g.load(sql, map, params);
        Card card = new Card(title, subtitle, toolbar(g, statuses, statusCol, "Tìm kiếm trong danh sách...", title));
        card.add(g, BorderLayout.CENTER);
        return pad(card);
    }

    /** Khối tiêu đề nhỏ trong hộp thoại chi tiết. */
    static JComponent section(String title) {
        JLabel l = lbl(title, font(Font.BOLD, 14), TEAL_D);
        l.setBorder(new EmptyBorder(14, 0, 8, 0));
        return l;
    }

    /** Ô thông tin nhỏ (nhãn trên, giá trị dưới) dùng trong hộp thoại chi tiết. */
    static JComponent tile(String k, String v) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 3)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setColor(new Color(248, 250, 252));
                g.fill(rr(0, 0, getWidth(), getHeight(), 18));
                g.setColor(LINE);
                g.draw(rr(0, 0, getWidth() - 1, getHeight() - 1, 18));
                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(11, 16, 11, 16));
        JLabel a = lbl(k, font(Font.PLAIN, 12), MUTED);
        JLabel b = lbl(dash(v), font(Font.BOLD, 14), INK);
        p.add(a);
        p.add(b);
        return p;
    }

    /** Lưới các ô thông tin (cols cột). */
    static JPanel tiles(int cols, String[][] kv) {
        JPanel p = new JPanel(new GridLayout(0, cols, 10, 10));
        p.setOpaque(false);
        for (String[] r : kv) p.add(tile(r[0], r[1]));
        return p;
    }

    /* ============================ DÙNG CHUNG CHO CÁC TRANG ============================ */

    /** Lớp do giáo viên này phụ trách (dùng trong IN (...)). Tham số: id giáo viên. */
    static final String MY_CLASSES = "SELECT id_lop FROM Quanli WHERE id_giao_vien = ?";

    static final String EXAM_SELECT =
            "SELECT k.ma_bai_lam, hs.ho_ten, k.trinh_do, k.ma_de_thi, k.diem_doc_hieu, k.diem_tu_vung, "
                    + "k.diem_nghe, k.tong_diem, k.tinh_trang FROM Ket_qua_thi k "
                    + "JOIN Hoc_sinh hs ON k.ma_sinh_vien = hs.ma_sinh_vien ";

    static final Color[] STAR_COLORS = {GREEN, new Color(0x84CC16), AMBER, new Color(0xF97316), RED};

    /** Chuẩn hóa kết quả: dùng giá trị trong CSDL nếu có, thiếu thì tự tính theo quy tắc trình độ. */
    static String verdictOf(String raw, String lv, int dh, int tv, int ng, int tong) {
        String k = stripAccents(raw);
        if (!k.isEmpty()) {
            if (k.contains("truot") || k.contains("chua") || k.contains("khong")) return FAIL;
            if (k.contains("dat")) return PASS;
        }
        return result(lv, dh, tv, ng, tong);
    }

    /** Một dòng bảng kết quả thi (khớp EXAM_SELECT). */
    static Object[] examRow(ResultSet rs) throws SQLException {
        String lv = rs.getString(3);
        int dh = ni(rs, 5), tv = ni(rs, 6), ng = ni(rs, 7), tong = ni(rs, 8);
        return new Object[]{rs.getString(1), rs.getString(2), new Level(lv), rs.getString(4),
                new Score(dh, Rule.SECTION_MAX), new Score(tv, Rule.SECTION_MAX), new Score(ng, Rule.SECTION_MAX),
                new Score(tong, Rule.TOTAL_MAX), verdictOf(rs.getString(9), lv, dh, tv, ng, tong)};
    }

    /** Dòng bảng phản hồi: mã, học sinh, lớp, sao, nội dung, ngày. */
    static Object[] feedbackRow(ResultSet rs) throws SQLException {
        return new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), new Stars(rs.getInt(4)),
                nz(rs.getString(5)), when(rs, 6, true)};
    }

    static final String FEEDBACK_SELECT =
            "SELECT d.ma_danh_gia, hs.ho_ten, l.ten_lop, d.rating, d.mo_ta, d.ngay_gui "
                    + "FROM Danh_gia_va_phan_hoi d JOIN Hoc_sinh hs ON d.ma_sinh_vien = hs.ma_sinh_vien "
                    + "JOIN Lop l ON d.id_lop = l.id_lop ";

    /** Thanh tiêu đề có nút "Xem tất cả" chuyển sang trang khác. */
    private FlatButton gotoButton(String text, String page) {
        FlatButton b = new FlatButton(text, "chevronR", TEAL, true);
        b.addActionListener(e -> navigate(page, true));
        return b;
    }

    /** Làm mượt cuộn cho một component đặt trong thẻ. */
    private static JScrollPane scrolled(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    /* ============================ TỔNG QUAN ============================ */

    private JComponent createOverviewPage() {
        ScrollPanel body = new ScrollPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        Banner banner = new Banner(greeting() + ", " + hoTen + "!",
                "Theo dõi lịch dạy, học sinh và kết quả giảng dạy của bạn.",
                chungChi.isEmpty() ? "Giáo viên" : "Giáo viên • " + chungChi);
        body.add(banner, c);

        StatCard cLop = new StatCard("Lớp phụ trách", "book", TEAL);
        StatCard cHs = new StatCard("Học sinh phụ trách", "group", BLUE);
        StatCard cUp = new StatCard("Buổi dạy sắp tới", "calendar", AMBER);
        StatCard cAvg = new StatCard("Đánh giá trung bình", "star", VIOLET);
        JPanel stats = new JPanel(new GridLayout(1, 4, 0, 0));
        stats.setOpaque(false);
        stats.add(cLop);
        stats.add(cHs);
        stats.add(cUp);
        stats.add(cAvg);
        c.gridy = 1;
        body.add(stats, c);

        // --- hàng 2: lịch hôm nay + biểu đồ buổi dạy theo tháng
        AgendaView agenda = new AgendaView();
        LocalDate today = LocalDate.now();
        Card cardToday = new Card("Lịch dạy hôm nay", dateLong(today), gotoButton("Xem lịch", "Lich"));
        cardToday.add(scrolled(agenda), BorderLayout.CENTER);
        cardToday.setPreferredSize(new Dimension(100, 380));

        BarChart monthChart = new BarChart();
        Card cardMonth = new Card("Buổi dạy theo tháng", "6 tháng gần nhất, không tính buổi báo nghỉ", null);
        cardMonth.add(monthChart, BorderLayout.CENTER);
        cardMonth.setPreferredSize(new Dimension(100, 380));
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0.5;
        body.add(cardToday, c);
        c.gridx = 1;
        body.add(cardMonth, c);

        // --- hàng 3: tỉ lệ đạt + phân bố đánh giá
        DonutChart passChart = new DonutChart();
        Card cardPass = new Card("Tỉ lệ đạt kỳ thi thử", "Tổng hợp bài làm của học sinh bạn phụ trách",
                gotoButton("Chi tiết", "Thi"));
        cardPass.add(passChart, BorderLayout.CENTER);
        cardPass.setPreferredSize(new Dimension(100, 310));
        HBarChart starChart = new HBarChart();
        Card cardStar = new Card("Phân bố đánh giá", "Số lượt đánh giá theo mức sao",
                gotoButton("Xem phản hồi", "PhanHoi"));
        cardStar.add(starChart, BorderLayout.CENTER);
        cardStar.setPreferredSize(new Dimension(100, 310));
        c.gridx = 0;
        c.gridy = 3;
        body.add(cardPass, c);
        c.gridx = 1;
        body.add(cardStar, c);

        // --- hàng 4: trình độ học sinh + buổi sắp tới
        DonutChart levelChart = new DonutChart();
        Card cardLevel = new Card("Trình độ học sinh", "Phân bố theo cấp độ JLPT",
                gotoButton("Danh sách", "HocSinh"));
        cardLevel.add(levelChart, BorderLayout.CENTER);
        cardLevel.setPreferredSize(new Dimension(100, 340));

        DataGrid upcoming = new DataGrid(new String[]{"Ngày", "Ca", "Lớp", "Phòng", "Trạng thái"},
                "Chưa có lịch dạy sắp tới", cols(4), true);
        upcoming.load("SELECT lh.ngay_day, lh.ca_hoc, COALESCE(l.ten_lop, lh.id_lop), lh.phong_hoc, lh.trang_thai_buoi "
                        + "FROM Lich_hoc lh LEFT JOIN Lop l ON lh.id_lop = l.id_lop "
                        + "WHERE lh.id_giao_vien = ? AND DATE(lh.ngay_day) >= CURDATE() "
                        + "ORDER BY lh.ngay_day, lh.ca_hoc LIMIT 6",
                rs -> new Object[]{when(rs, 1, false), nz(rs.getString(2)), rs.getString(3), nz(rs.getString(4)),
                        rs.getString(5)}, idGiaoVien);
        Card cardUp = new Card("Buổi dạy sắp tới", "6 buổi gần nhất kể từ hôm nay", gotoButton("Lịch dạy", "Lich"));
        cardUp.add(upcoming, BorderLayout.CENTER);
        cardUp.setPreferredSize(new Dimension(100, 340));
        c.gridx = 0;
        c.gridy = 4;
        body.add(cardLevel, c);
        c.gridx = 1;
        body.add(cardUp, c);

        // --- hàng 5: phản hồi mới nhất
        DataGrid latest = new DataGrid(new String[]{"Học sinh", "Lớp", "Đánh Giá", "Nội dung", "Ngày gửi"},
                "Chưa có phản hồi nào", cols(), true);
        latest.load("SELECT hs.ho_ten, l.ten_lop, d.rating, d.mo_ta, d.ngay_gui FROM Danh_gia_va_phan_hoi d "
                        + "JOIN Hoc_sinh hs ON d.ma_sinh_vien = hs.ma_sinh_vien JOIN Lop l ON d.id_lop = l.id_lop "
                        + "WHERE d.id_lop IN (" + MY_CLASSES + ") ORDER BY d.ngay_gui DESC LIMIT 6",
                rs -> new Object[]{rs.getString(1), rs.getString(2), new Stars(rs.getInt(3)), nz(rs.getString(4)),
                        when(rs, 5, false)}, idGiaoVien);
        Card cardFb = new Card("Phản hồi mới nhất từ học sinh", "6 góp ý gần đây nhất", gotoButton("Xem tất cả", "PhanHoi"));
        cardFb.add(latest, BorderLayout.CENTER);
        cardFb.setPreferredSize(new Dimension(100, 350));
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        body.add(cardFb, c);

        loadOverviewData(banner, cLop, cHs, cUp, cAvg, agenda, monthChart, passChart, starChart, levelChart);

        JScrollPane sp = new JScrollPane(body);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return pad(sp);
    }

    /** Nạp toàn bộ dữ liệu của trang tổng quan (mỗi khối một luồng nền riêng). */
    private void loadOverviewData(Banner banner, StatCard cLop, StatCard cHs, StatCard cUp, StatCard cAvg,
                                  AgendaView agenda, BarChart monthChart, DonutChart passChart,
                                  HBarChart starChart, DonutChart levelChart) {
        // 1) thẻ thống kê
        async(() -> new double[]{
                Db.scalar("SELECT COUNT(*) FROM Quanli WHERE id_giao_vien = ?", idGiaoVien),
                Db.scalar("SELECT COUNT(*) FROM Hoc_sinh WHERE id_giao_vien = ?", idGiaoVien),
                Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                        + "AND DATE(ngay_day) >= CURDATE()", idGiaoVien, DAY),
                Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                        + "AND ngay_day < NOW()", idGiaoVien, DAY),
                Db.decimal("SELECT AVG(rating) FROM Danh_gia_va_phan_hoi WHERE id_lop IN (" + MY_CLASSES + ")", idGiaoVien),
                Db.scalar("SELECT COUNT(*) FROM Danh_gia_va_phan_hoi WHERE id_lop IN (" + MY_CLASSES + ")", idGiaoVien)
        }, v -> {
            cLop.setValue((long) v[0], n -> n + " lớp", "Mã giáo viên: " + idGiaoVien);
            cHs.setValue((long) v[1], n -> n + " học sinh", "Đang do bạn phụ trách");
            cUp.setValue((long) v[2], n -> n + " buổi", "Đã dạy từ trước đến nay: " + (long) v[3] + " buổi");
            if (v[4] < 0) cAvg.setText("Chưa có", "Chưa có lượt đánh giá nào");
            else cAvg.setValue(Math.round(v[4] * 10), n -> dec1(n / 10.0) + " / 5", (long) v[5] + " lượt đánh giá");
        });

        // 2) lịch hôm nay
        async(() -> Db.query(SESSION_SELECT + "WHERE lh.id_giao_vien = ? AND DATE(lh.ngay_day) = CURDATE() "
                + "ORDER BY lh.ca_hoc, lh.ma_lich_hoc", TeacherDashboardUI::sessionRow, idGiaoVien), rows -> {
            List<Session> list = new ArrayList<>();
            int teach = 0;
            for (Object[] r : rows) {
                Session s = (Session) r[0];
                list.add(s);
                if (!s.off()) teach++;
            }
            agenda.setSessions(list, "Hôm nay bạn không có buổi dạy nào");
            banner.setNote(list.isEmpty() ? "Hôm nay bạn không có lịch dạy - hãy nghỉ ngơi nhé!"
                    : teach == 0 ? "Hôm nay bạn đã báo nghỉ " + list.size() + " buổi."
                    : "Hôm nay bạn có " + teach + " buổi dạy" + (list.size() > teach ? " (" + (list.size() - teach) + " buổi nghỉ)." : "."));
        });

        // 3) buổi dạy theo tháng
        final YearMonth first = YearMonth.now().minusMonths(5);
        async(() -> Db.query("SELECT DATE_FORMAT(ngay_day, '%Y-%m'), COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? "
                        + "AND trang_thai_buoi = ? AND ngay_day >= ? GROUP BY 1",
                rs -> new Object[]{rs.getString(1), rs.getLong(2)}, idGiaoVien, DAY,
                Timestamp.valueOf(first.atDay(1).atStartOfDay())), rows -> {
            Map<String, Long> m = new HashMap<>();
            for (Object[] r : rows) m.put((String) r[0], (Long) r[1]);
            String[] lb = new String[6];
            long[] vals = new long[6];
            for (int i = 0; i < 6; i++) {
                YearMonth ym = first.plusMonths(i);
                lb[i] = "T" + ym.getMonthValue();
                vals[i] = m.getOrDefault(ym.toString(), 0L);
            }
            monthChart.setCounts(lb, vals, "buổi");
            monthChart.setHighlight(5);
        });

        // 4) tỉ lệ đạt
        async(() -> Db.query(EXAM_SELECT + "WHERE hs.id_giao_vien = ?", TeacherDashboardUI::examRow, idGiaoVien), rows -> {
            int pass = 0;
            for (Object[] r : rows) if (PASS.equals(r[8])) pass++;
            int total = rows.size();
            if (total == 0) {
                passChart.setData(new String[0], new double[0], new Color[0], "", "");
                return;
            }
            passChart.setData(new String[]{"Đạt", "Trượt"}, new double[]{pass, total - pass}, new Color[]{GREEN, RED},
                    pct(pass / (double) total), "tỉ lệ đạt");
        });

        // 5) phân bố đánh giá
        async(() -> Db.query("SELECT rating, COUNT(*) FROM Danh_gia_va_phan_hoi WHERE id_lop IN (" + MY_CLASSES
                        + ") GROUP BY rating", rs -> new Object[]{rs.getInt(1), rs.getLong(2)}, idGiaoVien),
                rows -> starChart.setData(STAR_LABELS, starCounts(rows), STAR_COLORS));

        // 6) trình độ học sinh
        async(() -> Db.query("SELECT COALESCE(NULLIF(trinh_do, ''), 'Chưa xác định'), COUNT(*) FROM Hoc_sinh "
                        + "WHERE id_giao_vien = ? GROUP BY 1", rs -> new Object[]{rs.getString(1), rs.getLong(2)}, idGiaoVien),
                rows -> {
                    List<Object[]> sorted = new ArrayList<>(rows);
                    sorted.sort((a, b) -> Integer.compare(new Level((String) b[0]).rank, new Level((String) a[0]).rank));
                    String[] lb = new String[sorted.size()];
                    double[] vv = new double[sorted.size()];
                    Color[] cc = new Color[sorted.size()];
                    long total = 0;
                    for (int i = 0; i < lb.length; i++) {
                        lb[i] = (String) sorted.get(i)[0];
                        vv[i] = (Long) sorted.get(i)[1];
                        cc[i] = levelColor(new Level(lb[i]));
                        total += (Long) sorted.get(i)[1];
                    }
                    levelChart.setData(lb, vv, cc, String.valueOf(total), "học sinh");
                });
    }

    static final String[] STAR_LABELS = {"5 sao", "4 sao", "3 sao", "2 sao", "1 sao"};

    /** Chuyển các dòng (rating, count) thành mảng đếm theo thứ tự 5 → 1 sao. */
    static long[] starCounts(List<Object[]> rows) {
        long[] out = new long[5];
        for (Object[] r : rows) {
            int star = (Integer) r[0];
            if (star >= 1 && star <= 5) out[5 - star] += (Long) r[1];
        }
        return out;
    }

    /* ============================ LỚP PHỤ TRÁCH ============================ */

    private JComponent createClassPage() {
        DataGrid g = new DataGrid(new String[]{"Mã Lớp", "Tên Lớp", "Khóa Học", "Trình Độ", "Niên Khóa", "Sĩ Số",
                "Học Phí", "Buổi Sắp Tới"}, "Bạn chưa được phân công lớp nào", cols(), false);
        g.load("SELECT l.id_lop, l.ten_lop, k.ten_khoa_hoc, k.trinh_do, l.nien_khoa, l.si_so, k.hoc_phi, "
                        + "(SELECT COUNT(*) FROM Lich_hoc lh WHERE lh.id_lop = l.id_lop AND lh.id_giao_vien = ? "
                        + "AND lh.trang_thai_buoi = ? AND DATE(lh.ngay_day) >= CURDATE()) "
                        + "FROM Quanli q JOIN Lop l ON q.id_lop = l.id_lop "
                        + "LEFT JOIN Khoa_hoc k ON l.ma_khoa_hoc = k.ma_khoa_hoc "
                        + "WHERE q.id_giao_vien = ? ORDER BY l.id_lop",
                rs -> new Object[]{rs.getString(1), nz(rs.getString(2)), nz(rs.getString(3)), new Level(rs.getString(4)),
                        nz(rs.getString(5)), intObj(rs, 6) == null ? 0 : rs.getInt(6), new Money(rs.getLong(7)),
                        rs.getInt(8)},
                idGiaoVien, DAY, idGiaoVien);
        g.setOnOpen(r -> showClassDetail(g.str(r, 0), g.str(r, 1)));

        FlatButton btnView = new FlatButton("Chi tiết lớp", "eye", TEAL, false);
        btnView.setEnabled(false);
        g.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) btnView.setEnabled(g.selectedModelRow() >= 0);
        });
        btnView.addActionListener(e -> {
            int r = g.selectedModelRow();
            if (r >= 0) showClassDetail(g.str(r, 0), g.str(r, 1));
        });
        JPanel tb = toolbar(g, null, -1, "Tìm lớp...", "lop_phu_trach");
        tb.add(btnView);

        JPanel bar = sortBar(g,
                new String[]{"Tên lớp: A → Z", "Sĩ số: đông → ít", "Sĩ số: ít → đông", "Trình độ: N1 → N5",
                        "Trình độ: N5 → N1", "Niên khóa: mới nhất trước", "Buổi sắp tới: nhiều → ít"},
                new int[]{1, 5, 5, 3, 3, 4, 7},
                new boolean[]{true, false, true, false, true, false, false},
                new String[]{"Trình độ"}, new int[]{3},
                new String[][]{{"N5", "N4", "N3", "N2", "N1"}});
        Card card = new Card("Danh sách lớp", "Bấm đúp vào một lớp để xem học sinh, lịch và đánh giá", tb);
        card.add(withBar(bar, g), BorderLayout.CENTER);
        return pad(card);
    }

    /** Dữ liệu tổng hợp của một lớp để dựng hộp thoại chi tiết. */
    private static final class ClassInfo {
        String id, name, course, level, topic, book, year;
        long fee;
        int size;
        double avg = -1;
        long ratings;
        long[] stars = new long[5];
    }

    private void showClassDetail(String id, String name) {
        async(() -> {
            ClassInfo ci = new ClassInfo();
            List<Object[]> r = Db.query("SELECT l.id_lop, l.ten_lop, k.ten_khoa_hoc, k.trinh_do, k.chuyen_de, "
                            + "k.giao_trinh, k.hoc_phi, l.nien_khoa, l.si_so FROM Lop l "
                            + "LEFT JOIN Khoa_hoc k ON l.ma_khoa_hoc = k.ma_khoa_hoc WHERE l.id_lop = ?",
                    rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getLong(7), rs.getString(8), rs.getInt(9)}, id);
            if (r.isEmpty()) return null;
            Object[] o = r.get(0);
            ci.id = (String) o[0];
            ci.name = nz((String) o[1]);
            ci.course = nz((String) o[2]);
            ci.level = nz((String) o[3]);
            ci.topic = nz((String) o[4]);
            ci.book = nz((String) o[5]);
            ci.fee = (Long) o[6];
            ci.year = nz((String) o[7]);
            ci.size = (Integer) o[8];
            ci.avg = Db.decimal("SELECT AVG(rating) FROM Danh_gia_va_phan_hoi WHERE id_lop = ?", id);
            ci.ratings = Db.scalar("SELECT COUNT(*) FROM Danh_gia_va_phan_hoi WHERE id_lop = ?", id);
            ci.stars = starCounts(Db.query("SELECT rating, COUNT(*) FROM Danh_gia_va_phan_hoi WHERE id_lop = ? "
                    + "GROUP BY rating", rs -> new Object[]{rs.getInt(1), rs.getLong(2)}, id));
            return ci;
        }, ci -> {
            if (ci == null) {
                toast("Không tìm thấy lớp " + id + ".", T_ERR);
                return;
            }
            buildClassDialog(ci);
        });
    }

    private void buildClassDialog(ClassInfo ci) {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.setOpaque(false);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel tl = tiles(3, new String[][]{
                {"Khóa học", ci.course}, {"Trình độ", ci.level}, {"Niên khóa", ci.year},
                {"Sĩ số", ci.size + " học sinh"}, {"Học phí", vnd(ci.fee)}, {"Chuyên đề", ci.topic}});
        top.add(tl, BorderLayout.CENTER);
        ProgressRing ring = new ProgressRing(AMBER);
        ring.setPreferredSize(new Dimension(130, 140));
        if (ci.avg < 0) ring.setValue(0, "—", "Chưa có đánh giá");
        else ring.setValue(ci.avg / 5.0, dec1(ci.avg), ci.ratings + " lượt đánh giá");
        top.add(ring, BorderLayout.EAST);
        body.add(top);

        body.add(section("Giáo trình"));
        JLabel book = lbl(dash(ci.book), font(Font.PLAIN, 13), INK);
        body.add(book);

        body.add(section("Danh sách học sinh trong lớp"));
        DataGrid roster = new DataGrid(new String[]{"Mã SV", "Họ Tên", "Trình Độ", "Số Điện Thoại", "Ngày Vào Lớp"},
                Db.hasStudentClass ? "Lớp chưa có học sinh nào" : "Chưa có dữ liệu xếp lớp (bảng Hoc_sinh_lop)", cols(), false);
        roster.setAlignmentX(Component.LEFT_ALIGNMENT);
        roster.setPreferredSize(new Dimension(640, 250));
        roster.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        roster.load("SELECT hs.ma_sinh_vien, hs.ho_ten, hs.trinh_do, hs.so_dien_thoai, hsl.ngay_vao "
                        + "FROM Hoc_sinh_lop hsl JOIN Hoc_sinh hs ON hsl.ma_sinh_vien = hs.ma_sinh_vien "
                        + "WHERE hsl.id_lop = ? ORDER BY hs.ho_ten",
                rs -> new Object[]{rs.getString(1), rs.getString(2), new Level(rs.getString(3)),
                        nz(rs.getString(4)), when(rs, 5, false)}, ci.id);
        body.add(roster);

        body.add(section("Buổi dạy sắp tới của lớp"));
        DataGrid next = new DataGrid(new String[]{"Ngày", "Ca", "Phòng", "Nội Dung", "Trạng Thái"},
                "Lớp chưa có buổi dạy sắp tới", cols(4), true);
        next.setAlignmentX(Component.LEFT_ALIGNMENT);
        next.setPreferredSize(new Dimension(640, 250));
        next.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        next.load("SELECT lh.ngay_day, lh.ca_hoc, lh.phong_hoc, lh.noi_dung, lh.trang_thai_buoi FROM Lich_hoc lh "
                        + "WHERE lh.id_lop = ? AND lh.id_giao_vien = ? AND DATE(lh.ngay_day) >= CURDATE() "
                        + "ORDER BY lh.ngay_day, lh.ca_hoc LIMIT 5",
                rs -> new Object[]{when(rs, 1, false), nz(rs.getString(2)), nz(rs.getString(3)),
                        nz(rs.getString(4)), rs.getString(5)}, ci.id, idGiaoVien);
        body.add(next);

        body.add(section("Phân bố đánh giá của lớp"));
        HBarChart hb = new HBarChart();
        hb.setAlignmentX(Component.LEFT_ALIGNMENT);
        hb.setPreferredSize(new Dimension(640, 170));
        hb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        hb.setData(STAR_LABELS, ci.stars, STAR_COLORS);
        body.add(hb);

        showInfo("Lớp " + (ci.name.isEmpty() ? ci.id : ci.name), "Mã lớp " + ci.id, body, 700, 560);
    }

    /* ============================ LỊCH DẠY ============================ */

    /** Chú giải màu: chấm xanh = Dạy, chấm đỏ = Nghỉ. */
    private static JComponent legend() {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(GREEN);
                g.fill(new Ellipse2D.Double(2, 15, 9, 9));
                g.setColor(MUTED);
                text(g, DAY, 16, 20);
                g.setColor(RED);
                g.fill(new Ellipse2D.Double(58, 15, 9, 9));
                g.setColor(MUTED);
                text(g, OFF, 72, 20);
                g.dispose();
            }
        };
        c.setPreferredSize(new Dimension(110, 40));
        return c;
    }

    private JComponent createSchedulePage() {
        return new SchedulePage().root;
    }

    /** Trang lịch dạy: một dữ liệu, ba cách xem; mọi thao tác đổi trạng thái dùng chung. */
    private final class SchedulePage {
        final JPanel root;
        final DataGrid grid;
        final WeekView week = new WeekView();
        final MonthView month = new MonthView();
        final CardLayout viewCards = new CardLayout(), barCards = new CardLayout();
        final JPanel viewStack = new JPanel(viewCards), barStack = new JPanel(barCards);
        final JLabel hint = lbl(" ", font(Font.PLAIN, 13), MUTED);
        final JLabel weekLabel = lbl(" ", font(Font.BOLD, 15), INK);
        final JLabel monthLabel = lbl(" ", font(Font.BOLD, 15), INK);
        final FlatButton btnOff = new FlatButton("Báo nghỉ buổi này", "x", RED, false);
        final FlatButton btnOn = new FlatButton("Khôi phục buổi dạy", "undo", GREEN, false);
        final FlatButton btnDay = new FlatButton("Nghỉ cả ngày", "calendar", AMBER, true);
        List<Session> sessions = new ArrayList<>();
        final Map<String, Session> byId = new HashMap<>();
        Session selected;

        SchedulePage() {
            grid = new DataGrid(new String[]{"Mã Lịch", "Lớp", "Ngày Dạy", "Ca Học", "Phòng", "Nội Dung", "Trạng Thái"},
                    "Chưa có lịch dạy nào", cols(6), false);
            grid.load("SELECT lh.ma_lich_hoc, COALESCE(l.ten_lop, lh.id_lop), lh.ngay_day, lh.ca_hoc, lh.phong_hoc, "
                            + "lh.noi_dung, lh.trang_thai_buoi FROM Lich_hoc lh LEFT JOIN Lop l ON lh.id_lop = l.id_lop "
                            + "WHERE lh.id_giao_vien = ? ORDER BY lh.ngay_day, lh.ca_hoc",
                    rs -> new Object[]{rs.getString(1), nz(rs.getString(2)), when(rs, 3, true), nz(rs.getString(4)),
                            nz(rs.getString(5)), nz(rs.getString(6)), rs.getString(7)}, idGiaoVien);
            grid.setOnLoaded(this::scrollToUpcoming);
            grid.table.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                int r = grid.selectedModelRow();
                if (r >= 0) select(byId.get(grid.str(r, 0)), false);
            });

            // --- các chế độ xem
            week.setOnPick(s -> select(s, true));
            month.setOnPick(s -> select(s, true));
            month.setOnDay(d -> {
                List<Session> day = new ArrayList<>();
                for (Session s : sessions) if (d.equals(s.ngay)) day.add(s);
                day.sort(TeacherDashboardUI::compareSession);
                if (!day.isEmpty()) select(day.get(0), true);
            });
            JScrollPane weekScroll = new JScrollPane(week);
            slim(weekScroll);
            weekScroll.getViewport().setBackground(Color.WHITE);
            JPanel weekFrame = new JPanel(new BorderLayout());
            weekFrame.setBorder(BorderFactory.createLineBorder(LINE));
            weekFrame.add(weekScroll);
            JPanel monthFrame = new JPanel(new BorderLayout());
            monthFrame.setBackground(Color.WHITE);
            monthFrame.setBorder(BorderFactory.createLineBorder(LINE));
            monthFrame.add(month);
            viewStack.setOpaque(false);
            viewStack.add(grid, "list");
            viewStack.add(weekFrame, "week");
            viewStack.add(monthFrame, "month");

            // --- thanh công cụ theo chế độ
            barStack.setOpaque(false);
            barStack.add(toolbar(grid, new String[]{"Tất cả", DAY, OFF}, 6, "Tìm buổi học...", "lich_day"), "list");
            barStack.add(navBar(weekLabel, () -> {
                week.setWeek(week.weekStart().minusWeeks(1));
                updateLabels();
            }, () -> {
                week.setWeek(week.weekStart().plusWeeks(1));
                updateLabels();
            }, "Tuần này", () -> {
                week.setWeek(LocalDate.now());
                updateLabels();
            }), "week");
            barStack.add(navBar(monthLabel, () -> {
                month.setMonth(month.month().minusMonths(1));
                updateLabels();
            }, () -> {
                month.setMonth(month.month().plusMonths(1));
                updateLabels();
            }, "Tháng này", () -> {
                month.setMonth(YearMonth.now());
                updateLabels();
            }), "month");

            Segmented modes = new Segmented(new String[]{"Danh sách", "Lịch tuần", "Lịch tháng"}, i -> {
                String k = i == 0 ? "list" : i == 1 ? "week" : "month";
                viewCards.show(viewStack, k);
                barCards.show(barStack, k);
            });

            // --- chân trang: thao tác
            btnOff.addActionListener(e -> change(selected, OFF));
            btnOn.addActionListener(e -> change(selected, DAY));
            btnDay.addActionListener(e -> changeDay());
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btns.setOpaque(false);
            btns.add(btnDay);
            btns.add(btnOff);
            btns.add(btnOn);
            JPanel foot = new JPanel(new BorderLayout(16, 0));
            foot.setOpaque(false);
            foot.add(hint, BorderLayout.CENTER);
            foot.add(btns, BorderLayout.EAST);

            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.setOpaque(false);
            center.add(barStack, BorderLayout.NORTH);
            center.add(viewStack, BorderLayout.CENTER);
            center.add(foot, BorderLayout.SOUTH);
            Card card = new Card("Lịch giảng dạy của tôi",
                    "Chọn một buổi rồi báo nghỉ hoặc khôi phục. Chỉ đổi được buổi từ hôm nay trở đi.", modes);
            card.add(center, BorderLayout.CENTER);
            root = (JPanel) pad(card);

            reloadSessions(null);
            refreshButtons();
        }

        /** Thanh điều hướng tuần / tháng. */
        private JPanel navBar(JLabel label, Runnable prev, Runnable next, String todayText, Runnable today) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            p.setOpaque(false);
            IconButton b1 = new IconButton("chevronL", "Trước");
            IconButton b2 = new IconButton("chevronR", "Sau");
            b1.addActionListener(e -> prev.run());
            b2.addActionListener(e -> next.run());
            FlatButton bt = new FlatButton(todayText, "calendar", TEAL, true);
            bt.addActionListener(e -> today.run());
            label.setBorder(new EmptyBorder(0, 6, 0, 6));
            label.setPreferredSize(new Dimension(300, 42));
            p.add(b1);
            p.add(label);
            p.add(b2);
            p.add(bt);
            p.add(legend());
            return p;
        }

        void updateLabels() {
            LocalDate ws = week.weekStart();
            weekLabel.setText("Tuần " + ws.format(DM_FMT) + " – " + ws.plusDays(6).format(D_FMT) + "  •  "
                    + week.countInWeek() + " buổi");
            YearMonth m = month.month();
            monthLabel.setText("Tháng " + m.getMonthValue() + "/" + m.getYear() + "  •  " + month.countInMonth() + " buổi");
        }

        /** Nạp lại toàn bộ buổi dạy cho các chế độ lịch; khôi phục lựa chọn theo mã nếu có. */
        void reloadSessions(String keepKey) {
            async(() -> Db.query(SESSION_SELECT + "WHERE lh.id_giao_vien = ? ORDER BY lh.ngay_day, lh.ca_hoc",
                    TeacherDashboardUI::sessionRow, idGiaoVien), rows -> {
                sessions = new ArrayList<>();
                byId.clear();
                for (Object[] r : rows) {
                    Session s = (Session) r[0];
                    sessions.add(s);
                    byId.put(s.ma, s);
                }
                week.setSessions(sessions);
                month.setSessions(sessions);
                updateLabels();
                Session keep = keepKey == null ? null : byId.get(keepKey);
                selected = keep;
                week.setSelected(keep);
                month.setSelected(keep);
                refreshButtons();
            });
        }

        /** Cuộn bảng tới buổi sắp tới đầu tiên. */
        void scrollToUpcoming() {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < grid.table.getRowCount(); i++) {
                Object v = grid.table.getValueAt(i, 2);
                if (v instanceof When && ((When) v).date() != null && !((When) v).date().isBefore(today)) {
                    grid.table.scrollRectToVisible(grid.table.getCellRect(Math.min(grid.table.getRowCount() - 1, i + 8), 0, true));
                    grid.table.scrollRectToVisible(grid.table.getCellRect(i, 0, true));
                    break;
                }
            }
        }

        void select(Session s, boolean updateGrid) {
            selected = s;
            week.setSelected(s);
            month.setSelected(s);
            if (s != null && updateGrid) grid.select(0, s.ma);
            refreshButtons();
        }

        boolean hasOnDay(LocalDate d, String status) {
            for (Session s : sessions) if (d.equals(s.ngay) && s.status.equals(status)) return true;
            return false;
        }

        void refreshButtons() {
            if (selected == null) {
                hint.setText("Chọn một buổi trong danh sách hoặc lịch để xử lý.");
                btnOff.setEnabled(false);
                btnOn.setEnabled(false);
                btnDay.setEnabled(false);
                return;
            }
            boolean can = !selected.past();
            hint.setText("Đang chọn:  " + selected.label() + "  •  " + selected.status
                    + (can ? "" : "  (đã diễn ra - không thể thay đổi)"));
            btnOff.setEnabled(can && !selected.off());
            btnOn.setEnabled(can && selected.off());
            btnDay.setEnabled(can && hasOnDay(selected.ngay, DAY));
        }

        void change(Session s, String newStatus) {
            if (s == null) {
                toast("Hãy chọn một buổi học trước.", T_WARN);
                return;
            }
            if (s.status.equals(newStatus)) {
                toast("Buổi " + s.ma + " đã ở trạng thái \"" + s.status + "\" rồi.", T_INFO);
                return;
            }
            if (s.past()) {
                toast("Không thể thay đổi buổi học đã diễn ra.", T_ERR);
                return;
            }
            boolean off = OFF.equals(newStatus);
            String html = "Chuyển buổi <b>" + s.ma + "</b> sang <b>" + newStatus + "</b>?<br><br>"
                    + "Lớp: <b>" + s.lop + "</b><br>Ngày: <b>" + dateLong(s.ngay) + "</b><br>Ca học: <b>" + dash(s.ca)
                    + "</b><br>Phòng: <b>" + dash(s.phong) + "</b>"
                    + (off ? "<br><br><span style='color:#b45309'>Học sinh của lớp sẽ thấy buổi này là buổi nghỉ.</span>" : "");
            if (!confirm(off ? "Báo nghỉ buổi dạy" : "Khôi phục buổi dạy", html,
                    off ? "Báo nghỉ" : "Khôi phục", off ? RED : GREEN)) return;
            final String ma = s.ma;
            async(() -> Db.update("UPDATE Lich_hoc SET trang_thai_buoi = ?, ngay_day = ngay_day WHERE ma_lich_hoc = ? "
                            + "AND id_giao_vien = ? AND trang_thai_buoi <> ? AND DATE(ngay_day) >= CURDATE()",
                    newStatus, ma, idGiaoVien, newStatus), n -> {
                if (n > 0) toast(off ? "Đã báo nghỉ buổi " + ma + "." : "Đã khôi phục buổi dạy " + ma + ".", off ? T_WARN : T_OK);
                else toast("Không đổi được: buổi " + ma + " đã qua, đã đổi trạng thái hoặc không thuộc quyền của bạn.", T_ERR);
                grid.reload();
                reloadSessions(ma);
                refreshBadges();
            });
        }

        /** Báo nghỉ tất cả các buổi 'Dạy' trong ngày của buổi đang chọn. */
        void changeDay() {
            if (selected == null || selected.ngay == null) {
                toast("Hãy chọn một buổi học trước.", T_WARN);
                return;
            }
            final LocalDate d = selected.ngay;
            if (d.isBefore(LocalDate.now())) {
                toast("Không thể thay đổi buổi học đã diễn ra.", T_ERR);
                return;
            }
            List<Session> affected = new ArrayList<>();
            for (Session s : sessions) if (d.equals(s.ngay) && !s.off()) affected.add(s);
            if (affected.isEmpty()) {
                toast("Ngày này không còn buổi dạy nào để báo nghỉ.", T_INFO);
                return;
            }
            StringBuilder sb = new StringBuilder("Báo nghỉ <b>" + affected.size() + " buổi</b> trong ngày <b>"
                    + dateLong(d) + "</b>?<br><br>");
            for (Session s : affected) sb.append("• ").append(s.ma).append(" - ").append(s.lop)
                    .append(s.ca.isEmpty() ? "" : " (" + s.ca + ")").append("<br>");
            if (!confirm("Báo nghỉ cả ngày", sb.toString(), "Báo nghỉ " + affected.size() + " buổi", AMBER)) return;
            final String keep = selected.ma;
            async(() -> Db.update("UPDATE Lich_hoc SET trang_thai_buoi = ?, ngay_day = ngay_day WHERE id_giao_vien = ? "
                            + "AND DATE(ngay_day) = ? AND trang_thai_buoi <> ? AND DATE(ngay_day) >= CURDATE()",
                    OFF, idGiaoVien, java.sql.Date.valueOf(d), OFF), n -> {
                if (n > 0) toast("Đã báo nghỉ " + n + " buổi ngày " + d.format(D_FMT) + ".", T_WARN);
                else toast("Không có buổi nào được thay đổi.", T_ERR);
                grid.reload();
                reloadSessions(keep);
                refreshBadges();
            });
        }
    }

    /* ============================ HỌC SINH CỦA TÔI ============================ */

    private JComponent createStudentPage() {
        DataGrid g = new DataGrid(new String[]{"Mã SV", "Họ Tên", "Ngày Sinh", "Tuổi", "Trình Độ", "Số Điện Thoại",
                "Số Lớp", "Bài Thi", "Điểm Cao Nhất"}, "Chưa có học sinh nào được phân công cho bạn", cols(), false);
        String classCount = Db.hasStudentClass
                ? "(SELECT COUNT(*) FROM Hoc_sinh_lop x WHERE x.ma_sinh_vien = hs.ma_sinh_vien)" : "0";
        g.load("SELECT hs.ma_sinh_vien, hs.ho_ten, hs.ngay_sinh, TIMESTAMPDIFF(YEAR, hs.ngay_sinh, CURDATE()), "
                        + "hs.trinh_do, hs.so_dien_thoai, " + classCount + ", "
                        + "(SELECT COUNT(*) FROM Ket_qua_thi k WHERE k.ma_sinh_vien = hs.ma_sinh_vien), "
                        + "(SELECT MAX(k.tong_diem) FROM Ket_qua_thi k WHERE k.ma_sinh_vien = hs.ma_sinh_vien) "
                        + "FROM Hoc_sinh hs WHERE hs.id_giao_vien = ? ORDER BY hs.ma_sinh_vien",
                rs -> {
                    int best = rs.getInt(9);
                    boolean none = rs.wasNull();
                    return new Object[]{rs.getString(1), nz(rs.getString(2)), when(rs, 3, false), intObj(rs, 4),
                            new Level(rs.getString(5)), nz(rs.getString(6)), rs.getInt(7), rs.getInt(8),
                            new Score(none ? -1 : best, Rule.TOTAL_MAX)};
                }, idGiaoVien);
        g.setOnOpen(r -> showStudentDetail(g.str(r, 0)));

        FlatButton btnView = new FlatButton("Hồ sơ học sinh", "eye", TEAL, false);
        btnView.setEnabled(false);
        g.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) btnView.setEnabled(g.selectedModelRow() >= 0);
        });
        btnView.addActionListener(e -> {
            int r = g.selectedModelRow();
            if (r >= 0) showStudentDetail(g.str(r, 0));
        });
        JPanel tb = toolbar(g, null, -1, "Tìm học sinh...", "hoc_sinh_cua_toi");
        tb.add(btnView);

        JPanel bar = sortBar(g,
                new String[]{"Tên: A → Z", "Tên: Z → A", "Ngày sinh: lớn tuổi trước", "Ngày sinh: nhỏ tuổi trước",
                        "Trình độ: N1 → N5", "Trình độ: N5 → N1", "Điểm cao nhất: cao → thấp", "Số bài thi: nhiều → ít"},
                new int[]{1, 1, 2, 2, 4, 4, 8, 7},
                new boolean[]{true, false, true, false, false, true, false, false},
                new String[]{"Trình độ"}, new int[]{4},
                new String[][]{{"N5", "N4", "N3", "N2", "N1", "Chưa xác định"}});
        Card card = new Card("Danh sách học sinh", "Bấm đúp vào một học sinh để xem hồ sơ học tập", tb);
        card.add(withBar(bar, g), BorderLayout.CENTER);
        return pad(card);
    }

    /** Dữ liệu hồ sơ một học sinh. */
    private static final class StudentDetail {
        Object[] info;
        final List<Object[]> exams = new ArrayList<>();
        final List<String> classes = new ArrayList<>();
        final List<Object[]> feedback = new ArrayList<>();
    }

    private void showStudentDetail(String ma) {
        async(() -> {
            StudentDetail d = new StudentDetail();
            List<Object[]> r = Db.query("SELECT ma_sinh_vien, ho_ten, ngay_sinh, trinh_do, so_dien_thoai "
                            + "FROM Hoc_sinh WHERE ma_sinh_vien = ? AND id_giao_vien = ?",
                    rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getTimestamp(3), rs.getString(4),
                            rs.getString(5)}, ma, idGiaoVien);
            if (r.isEmpty()) return null;
            d.info = r.get(0);
            d.exams.addAll(Db.query("SELECT ma_bai_lam, trinh_do, ma_de_thi, diem_doc_hieu, diem_tu_vung, diem_nghe, "
                            + "tong_diem, tinh_trang FROM Ket_qua_thi WHERE ma_sinh_vien = ? ORDER BY ma_bai_lam",
                    rs -> {
                        String lv = rs.getString(2);
                        int dh = ni(rs, 4), tv = ni(rs, 5), ng = ni(rs, 6), tong = ni(rs, 7);
                        return new Object[]{rs.getString(1), new Level(lv), rs.getString(3), new Score(dh, 60),
                                new Score(tv, 60), new Score(ng, 60), new Score(tong, 180),
                                verdictOf(rs.getString(8), lv, dh, tv, ng, tong)};
                    }, ma));
            if (Db.hasStudentClass) {
                try {
                    for (Object[] c : Db.query("SELECT l.ten_lop FROM Hoc_sinh_lop h JOIN Lop l ON h.id_lop = l.id_lop "
                            + "WHERE h.ma_sinh_vien = ? ORDER BY l.ten_lop", rs -> new Object[]{rs.getString(1)}, ma))
                        d.classes.add(nz((String) c[0]));
                } catch (SQLException ignored) {
                    // bảng xếp lớp chưa có: bỏ qua phần này
                }
            }
            d.feedback.addAll(Db.query("SELECT l.ten_lop, d.rating, d.mo_ta, d.ngay_gui FROM Danh_gia_va_phan_hoi d "
                            + "JOIN Lop l ON d.id_lop = l.id_lop WHERE d.ma_sinh_vien = ? AND d.id_lop IN (" + MY_CLASSES
                            + ") ORDER BY d.ngay_gui DESC",
                    rs -> new Object[]{nz(rs.getString(1)), new Stars(rs.getInt(2)), nz(rs.getString(3)),
                            when(rs, 4, false)}, ma, idGiaoVien));
            return d;
        }, d -> {
            if (d == null) {
                toast("Không tìm thấy học sinh " + ma + " trong danh sách của bạn.", T_ERR);
                return;
            }
            buildStudentDialog(d);
        });
    }

    private void buildStudentDialog(StudentDetail d) {
        String ma = nz((String) d.info[0]), name = nz((String) d.info[1]);
        LocalDate dob = ld((Timestamp) d.info[2]);
        String dobText = dob == null ? "—" : dob.format(D_FMT) + " (" + ChronoUnit.YEARS.between(dob, LocalDate.now()) + " tuổi)";
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel t = tiles(3, new String[][]{{"Mã sinh viên", ma}, {"Họ và tên", name}, {"Ngày sinh", dobText},
                {"Trình độ", new Level((String) d.info[3]).text}, {"Số điện thoại", nz((String) d.info[4])},
                {"Số lớp tham gia", String.valueOf(d.classes.size())}});
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(t);

        body.add(section("Lớp đang học"));
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pills.setOpaque(false);
        pills.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (d.classes.isEmpty()) pills.add(lbl("Chưa có dữ liệu xếp lớp.", font(Font.PLAIN, 13), MUTED));
        for (String c : d.classes) pills.add(new Pill(c, TEAL));
        body.add(pills);

        body.add(section("Xu hướng điểm thi"));
        LineChart lc = new LineChart();
        lc.setAlignmentX(Component.LEFT_ALIGNMENT);
        lc.setPreferredSize(new Dimension(640, 210));
        lc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        lc.setFixedMax(Rule.TOTAL_MAX);
        lc.setEmptyText("Học sinh chưa có bài thi nào");
        String[] lb = new String[d.exams.size()];
        double[] vv = new double[lb.length];
        int pass = 0, best = 0;
        double sum = 0;
        for (int i = 0; i < lb.length; i++) {
            Object[] e = d.exams.get(i);
            lb[i] = (String) e[0];
            vv[i] = ((Score) e[6]).v;
            sum += vv[i];
            best = Math.max(best, ((Score) e[6]).v);
            if (PASS.equals(e[7])) pass++;
        }
        lc.setData(lb, vv, true, x -> Math.round(x) + " điểm", x -> String.valueOf(Math.round(x)));
        body.add(lc);
        if (lb.length > 0) {
            JLabel sm = lbl("Tổng " + lb.length + " bài  •  Đạt " + pass + "  •  Điểm cao nhất " + best
                    + "  •  Điểm trung bình " + dec1(sum / lb.length), font(Font.BOLD, 13), TEAL_D);
            sm.setBorder(new EmptyBorder(6, 2, 0, 0));
            body.add(sm);
        }

        body.add(section("Chi tiết các bài thi"));
        DataGrid eg = new DataGrid(new String[]{"Mã Bài", "Trình Độ", "Mã Đề", "Đọc Hiểu", "Từ Vựng", "Nghe", "Tổng", "Kết Quả"},
                "Chưa có bài thi nào", cols(7), true);
        eg.setRows(d.exams);
        eg.setAlignmentX(Component.LEFT_ALIGNMENT);
        eg.setPreferredSize(new Dimension(640, 60 + Math.min(5, Math.max(1, d.exams.size())) * 44));
        eg.setMaximumSize(new Dimension(Integer.MAX_VALUE, eg.getPreferredSize().height));
        body.add(eg);

        body.add(section("Phản hồi học sinh này đã gửi"));
        DataGrid fg = new DataGrid(new String[]{"Lớp", "Đánh Giá", "Nội Dung", "Ngày Gửi"},
                "Học sinh chưa gửi phản hồi nào", cols(), true);
        fg.setRows(d.feedback);
        fg.setAlignmentX(Component.LEFT_ALIGNMENT);
        fg.setPreferredSize(new Dimension(640, 60 + Math.min(4, Math.max(1, d.feedback.size())) * 44));
        fg.setMaximumSize(new Dimension(Integer.MAX_VALUE, fg.getPreferredSize().height));
        body.add(fg);

        showInfo("Hồ sơ học sinh", name + "  •  " + ma, body, 700, 580);
    }

    /* ============================ ĐIỂM THI HỌC SINH ============================ */

    private JComponent createExamPage() {
        ScrollPanel body = new ScrollPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;

        StatCard sTotal = new StatCard("Tổng bài làm", "list", TEAL);
        StatCard sPass = new StatCard("Bài đạt", "check", GREEN);
        StatCard sFail = new StatCard("Bài trượt", "alert", RED);
        StatCard sAvg = new StatCard("Điểm trung bình", "trend", BLUE);
        JPanel stats = new JPanel(new GridLayout(1, 4, 0, 0));
        stats.setOpaque(false);
        stats.add(sTotal);
        stats.add(sPass);
        stats.add(sFail);
        stats.add(sAvg);
        body.add(stats, c);

        DonutChart donut = new DonutChart();
        Card cardDonut = new Card("Tỉ lệ đạt", "Theo quy tắc đỗ của từng trình độ", null);
        cardDonut.add(donut, BorderLayout.CENTER);
        cardDonut.setPreferredSize(new Dimension(100, 290));
        BarChart hist = new BarChart();
        Card cardHist = new Card("Phổ điểm tổng", "Số bài làm theo khoảng điểm (thang 180)", null);
        cardHist.add(hist, BorderLayout.CENTER);
        cardHist.setPreferredSize(new Dimension(100, 290));
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.45;
        body.add(cardDonut, c);
        c.gridx = 1;
        c.weightx = 0.55;
        body.add(cardHist, c);

        DataGrid g = new DataGrid(new String[]{"Mã Bài Làm", "Học Sinh", "Trình Độ", "Mã Đề", "Đọc Hiểu", "Từ Vựng",
                "Nghe", "Tổng Điểm", "Kết Quả"}, "Chưa có kết quả thi nào", cols(8), false);
        g.load(EXAM_SELECT + "WHERE hs.id_giao_vien = ? ORDER BY k.ma_bai_lam", TeacherDashboardUI::examRow, idGiaoVien);
        g.setOnOpen(r -> showExamDialog(g, r));
        g.setOnLoaded(() -> updateExamStats(g, sTotal, sPass, sFail, sAvg, donut, hist));

        FlatButton btnView = new FlatButton("Chi tiết bài làm", "eye", TEAL, false);
        btnView.setEnabled(false);
        g.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) btnView.setEnabled(g.selectedModelRow() >= 0);
        });
        btnView.addActionListener(e -> {
            int r = g.selectedModelRow();
            if (r >= 0) showExamDialog(g, r);
        });
        JPanel tb = toolbar(g, new String[]{"Tất cả", PASS, FAIL}, 8, "Tìm bài làm...", "ket_qua_thi");
        tb.add(btnView);
        JPanel bar = sortBar(g,
                new String[]{"Tổng điểm: cao → thấp", "Tổng điểm: thấp → cao", "Học sinh: A → Z", "Mã bài làm: A → Z",
                        "Nghe: cao → thấp"},
                new int[]{7, 7, 1, 0, 6},
                new boolean[]{false, true, true, true, false},
                new String[]{"Trình độ"}, new int[]{2},
                new String[][]{{"N5", "N4", "N3", "N2", "N1"}});
        Card card = new Card("Bài làm của học sinh", "Bấm đúp một bài để xem điểm so với điều kiện đỗ", tb);
        card.add(withBar(bar, g), BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(100, 560));
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 1;
        body.add(card, c);

        JScrollPane sp = new JScrollPane(body);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return pad(sp);
    }

    /** Tính lại thẻ thống kê và biểu đồ từ toàn bộ dòng của bảng (không phụ thuộc bộ lọc đang bật). */
    private void updateExamStats(DataGrid g, StatCard sTotal, StatCard sPass, StatCard sFail, StatCard sAvg,
                                 DonutChart donut, BarChart hist) {
        int total = g.rowCount(), pass = 0;
        long sum = 0;
        long[] bins = new long[6];
        for (int i = 0; i < total; i++) {
            if (PASS.equals(g.str(i, 8))) pass++;
            int tong = ((Score) g.value(i, 7)).v;
            sum += tong;
            bins[Math.min(5, Math.max(0, tong / 30))]++;
        }
        final int fp = pass, ft = total;
        final long fs = sum;
        sTotal.setValue(total, n -> n + " bài", "Của học sinh bạn phụ trách");
        sPass.setValue(pass, n -> n + " bài", total == 0 ? "Chưa có dữ liệu" : pct(fp / (double) ft) + " tổng số bài");
        sFail.setValue(total - pass, n -> n + " bài", total == 0 ? "Chưa có dữ liệu" : pct((ft - fp) / (double) ft) + " tổng số bài");
        if (total == 0) sAvg.setText("Chưa có", "Chưa có bài làm nào");
        else sAvg.setValue(Math.round(sum * 10.0 / total), n -> dec1(n / 10.0) + " / 180", "Trung bình " + dec1(fs / (double) ft) + " điểm");
        if (total == 0) donut.setData(new String[0], new double[0], new Color[0], "", "");
        else donut.setData(new String[]{"Đạt", "Trượt"}, new double[]{pass, total - pass}, new Color[]{GREEN, RED},
                pct(pass / (double) total), "tỉ lệ đạt");
        hist.setCounts(new String[]{"0–29", "30–59", "60–89", "90–119", "120–149", "150–180"}, bins, "bài");
    }

    private void showExamDialog(DataGrid g, int r) {
        String ma = g.str(r, 0), hs = g.str(r, 1), de = g.str(r, 3), verdict = g.str(r, 8);
        String lv = ((Level) g.value(r, 2)).text;
        int dh = ((Score) g.value(r, 4)).v, tv = ((Score) g.value(r, 5)).v, ng = ((Score) g.value(r, 6)).v,
                tong = ((Score) g.value(r, 7)).v;
        Rule rule = Rule.of(lv);
        boolean ok = PASS.equals(verdict);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel t = tiles(3, new String[][]{{"Học sinh", hs}, {"Mã bài làm", ma}, {"Trình độ", lv},
                {"Mã đề thi", de}, {"Tổng điểm", tong + " / " + Rule.TOTAL_MAX}, {"Kết quả", verdict}});
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(t);

        body.add(section("Điểm từng phần so với điều kiện đỗ"));
        SkillBars sb = new SkillBars();
        sb.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (rule.split || !rule.known) {
            int m = rule.known ? rule.combinedMin : -1;
            sb.setData(new String[]{"Đọc hiểu", "Từ vựng", "Nghe", "Tổng điểm"}, new int[]{dh, tv, ng, tong},
                    new int[]{60, 60, 60, 180}, new int[]{m, m, rule.known ? rule.listenMin : -1,
                            rule.known ? rule.totalMin : -1});
        } else {
            sb.setData(new String[]{"Đọc hiểu + Từ vựng", "Nghe", "Tổng điểm"}, new int[]{dh + tv, ng, tong},
                    new int[]{120, 60, 180}, new int[]{rule.combinedMin, rule.listenMin, rule.totalMin});
        }
        sb.setMaximumSize(new Dimension(Integer.MAX_VALUE, sb.getPreferredSize().height));
        body.add(sb);

        body.add(section("Nhận xét"));
        JPanel note = new JPanel();
        note.setOpaque(false);
        note.setLayout(new BoxLayout(note, BoxLayout.Y_AXIS));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.add(lbl("Điều kiện đỗ " + lv + ": " + rule.describe(), font(Font.PLAIN, 13), MUTED));
        note.add(Box.createVerticalStrut(8));
        if (ok) {
            int margin = rule.known ? tong - rule.totalMin : 0;
            note.add(lbl("Học sinh ĐẠT" + (rule.known ? ", vượt điểm sàn tổng " + margin + " điểm." : "."),
                    font(Font.BOLD, 14), GREEN));
        } else {
            note.add(lbl("Học sinh TRƯỢT vì chưa đạt:", font(Font.BOLD, 14), RED));
            List<String> miss = rule.unmet(dh, tv, ng, tong);
            if (miss.isEmpty()) note.add(lbl("•  Kết quả được ghi nhận là trượt trong CSDL.", font(Font.PLAIN, 13), INK));
            for (String m : miss) note.add(lbl("•  " + m, font(Font.PLAIN, 13), INK));
            if (rule.known && tong < rule.totalMin)
                note.add(lbl("→ Cần thêm ít nhất " + (rule.totalMin - tong) + " điểm tổng để đạt.", font(Font.BOLD, 13), TEAL_D));
        }
        body.add(note);
        showInfo("Chi tiết bài làm " + ma, hs + "  •  " + lv + "  •  " + verdict, body, 620, 520);
    }

    /* ============================ PHẢN HỒI & ĐÁNH GIÁ ============================ */

    private JComponent createFeedbackPage() {
        DataGrid g = new DataGrid(new String[]{"Mã", "Học Sinh", "Lớp", "Đánh Giá", "Nội Dung", "Ngày Gửi"},
                "Chưa có phản hồi nào từ học sinh", cols(), false);
        g.load(FEEDBACK_SELECT + "WHERE d.id_lop IN (" + MY_CLASSES + ") ORDER BY d.ngay_gui DESC",
                TeacherDashboardUI::feedbackRow, idGiaoVien);
        g.setOnOpen(r -> showFeedbackDialog(g, r));

        Segmented seg = new Segmented(new String[]{"Tất cả", "5 sao", "4 sao", "3 sao", "2 sao", "1 sao"},
                i -> g.setStatus(i == 0 ? null : stars(6 - i), 3));
        JPanel tb = toolbar(g, null, -1, "Tìm phản hồi...", "phan_hoi_hoc_sinh");
        tb.add(seg, 0);

        // --- cột trái: vòng tiến độ + phân bố
        ProgressRing ringAvg = new ProgressRing(AMBER), ringPos = new ProgressRing(GREEN);
        ringAvg.setValue(0, "—", "Đang tải...");
        ringPos.setValue(0, "—", "Đang tải...");
        JPanel rings = new JPanel(new GridLayout(1, 2, 6, 0));
        rings.setOpaque(false);
        rings.setPreferredSize(new Dimension(100, 150));
        rings.add(ringAvg);
        rings.add(ringPos);
        HBarChart hb = new HBarChart();
        hb.setOnPick(i -> seg.select(i + 1, true));
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        left.add(rings, BorderLayout.NORTH);
        left.add(hb, BorderLayout.CENTER);
        Card cardLeft = new Card("Tổng quan đánh giá", "Bấm vào một mức sao để lọc bảng", null);
        cardLeft.add(left, BorderLayout.CENTER);
        cardLeft.setPreferredSize(new Dimension(410, 100));

        async(() -> {
            List<Object[]> dist = Db.query("SELECT rating, COUNT(*) FROM Danh_gia_va_phan_hoi WHERE id_lop IN ("
                    + MY_CLASSES + ") GROUP BY rating", rs -> new Object[]{rs.getInt(1), rs.getLong(2)}, idGiaoVien);
            return dist;
        }, dist -> {
            long[] cnt = starCounts(dist);
            long total = 0, sum = 0, pos = 0;
            for (int i = 0; i < 5; i++) {
                total += cnt[i];
                sum += cnt[i] * (5 - i);
                if (i < 2) pos += cnt[i];
            }
            hb.setData(STAR_LABELS, cnt, STAR_COLORS);
            if (total == 0) {
                ringAvg.setValue(0, "—", "Chưa có đánh giá");
                ringPos.setValue(0, "—", "Chưa có đánh giá");
            } else {
                ringAvg.setValue(sum / (5.0 * total), dec1(sum / (double) total), "Điểm trung bình / 5");
                ringPos.setValue(pos / (double) total, pct(pos / (double) total), "Đánh giá tích cực (≥ 4 sao)");
            }
        });

        JPanel bar = sortBar(g,
                new String[]{"Mới nhất trước", "Cũ nhất trước", "Đánh giá: cao → thấp", "Đánh giá: thấp → cao", "Học sinh: A → Z"},
                new int[]{5, 5, 3, 3, 1},
                new boolean[]{false, true, false, true, true},
                new String[0], new int[0], new String[0][]);
        Card card = new Card("Phản hồi từ học sinh", "Bấm đúp một dòng để đọc toàn bộ nội dung góp ý", tb);
        card.add(withBar(bar, g), BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(6, 0));
        root.setOpaque(false);
        root.add(cardLeft, BorderLayout.WEST);
        root.add(card, BorderLayout.CENTER);
        return pad(root);
    }

    private void showFeedbackDialog(DataGrid g, int r) {
        String student = g.str(r, 1), cls = g.str(r, 2), content = g.str(r, 4), date = g.str(r, 5);
        Stars st = (Stars) g.value(r, 3);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel t = tiles(3, new String[][]{{"Học sinh", student}, {"Lớp", cls}, {"Ngày gửi", date}});
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(t);
        body.add(section("Mức đánh giá"));
        JComponent starRow = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g2 = (Graphics2D) g0.create();
                aa(g2);
                for (int i = 0; i < 5; i++) {
                    g2.setColor(i < st.n ? AMBER : new Color(226, 232, 240));
                    g2.fill(Icons.star(16 + i * 34, 18, 14));
                }
                g2.setFont(font(Font.BOLD, 14));
                g2.setColor(INK);
                text(g2, st.n + " / 5 sao", 188, 18);
                g2.dispose();
            }
        };
        starRow.setPreferredSize(new Dimension(300, 38));
        starRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        starRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(starRow);
        body.add(section("Nội dung góp ý"));
        StyledArea area = new StyledArea(content.isEmpty() ? "(Học sinh không để lại nội dung)" : content, 6);
        area.setEditable(false);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane sp = new JScrollPane(area);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(520, 170));
        body.add(sp);
        showInfo("Chi tiết phản hồi", student + "  •  " + cls, body, 580, 440);
    }

    /* ============================ PHIẾU LƯƠNG ============================ */

    private JComponent createPayslipPage() {
        ScrollPanel body = new ScrollPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;

        StatCard sSum = new StatCard("Tổng đã nhận", "wallet", TEAL);
        StatCard sCnt = new StatCard("Số phiếu lương", "list", BLUE);
        StatCard sLast = new StatCard("Kỳ gần nhất", "money", GREEN);
        StatCard sAvg = new StatCard("Trung bình mỗi kỳ", "trend", VIOLET);
        JPanel stats = new JPanel(new GridLayout(1, 4, 0, 0));
        stats.setOpaque(false);
        stats.add(sSum);
        stats.add(sCnt);
        stats.add(sLast);
        stats.add(sAvg);
        body.add(stats, c);

        LineChart line = new LineChart();
        line.setEmptyText("Chưa có phiếu lương nào");
        Card cardLine = new Card("Lương theo từng kỳ", "Tối đa 12 kỳ gần nhất", null);
        cardLine.add(line, BorderLayout.CENTER);
        cardLine.setPreferredSize(new Dimension(100, 320));
        c.gridy = 1;
        body.add(cardLine, c);

        DataGrid g = new DataGrid(new String[]{"Mã Phiếu", "Từ Ngày", "Đến Ngày", "Ngày Trả", "Số Ngày", "Tổng Lương"},
                "Chưa có phiếu lương nào", cols(), false);
        g.load("SELECT ma_phieu_luong, tu_ngay, den_ngay, ngay_tra, DATEDIFF(den_ngay, tu_ngay) + 1, tong_luong "
                        + "FROM Phieu_luong WHERE id_giao_vien = ? ORDER BY ngay_tra DESC",
                rs -> new Object[]{rs.getString(1), when(rs, 2, false), when(rs, 3, false), when(rs, 4, false),
                        intObj(rs, 5), new Money(rs.getLong(6))}, idGiaoVien);
        g.setOnOpen(r -> showPayslipDialog(g, r));
        FlatButton btnView = new FlatButton("Chi tiết kỳ lương", "eye", TEAL, false);
        btnView.setEnabled(false);
        g.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) btnView.setEnabled(g.selectedModelRow() >= 0);
        });
        btnView.addActionListener(e -> {
            int r = g.selectedModelRow();
            if (r >= 0) showPayslipDialog(g, r);
        });
        JPanel tb = toolbar(g, null, -1, "Tìm phiếu lương...", "phieu_luong");
        tb.add(btnView);
        JPanel bar = sortBar(g,
                new String[]{"Ngày trả: mới nhất trước", "Ngày trả: cũ nhất trước", "Lương: cao → thấp", "Lương: thấp → cao"},
                new int[]{3, 3, 5, 5},
                new boolean[]{false, true, false, true},
                new String[0], new int[0], new String[0][]);
        Card card = new Card("Lịch sử nhận lương", "Bấm đúp một phiếu để xem số buổi dạy và lương trên mỗi buổi", tb);
        card.add(withBar(bar, g), BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(100, 500));
        c.gridy = 2;
        body.add(card, c);

        async(() -> {
            double[] agg = new double[4];
            List<Object[]> a = Db.query("SELECT COALESCE(SUM(tong_luong), 0), COUNT(*), COALESCE(AVG(tong_luong), 0) "
                            + "FROM Phieu_luong WHERE id_giao_vien = ?",
                    rs -> new Object[]{rs.getLong(1), rs.getLong(2), rs.getDouble(3)}, idGiaoVien);
            if (!a.isEmpty()) {
                agg[0] = (Long) a.get(0)[0];
                agg[1] = (Long) a.get(0)[1];
                agg[2] = (Double) a.get(0)[2];
            }
            agg[3] = Db.scalar("SELECT COALESCE((SELECT tong_luong FROM Phieu_luong WHERE id_giao_vien = ? "
                    + "ORDER BY ngay_tra DESC LIMIT 1), -1)", idGiaoVien);
            return agg;
        }, v -> {
            sSum.setValue((long) v[0], TeacherDashboardUI::vnd, "Từ " + (long) v[1] + " phiếu lương");
            sCnt.setValue((long) v[1], n -> n + " phiếu", "Đã được trả cho bạn");
            if (v[3] < 0) sLast.setText("Chưa có", "Chưa có phiếu lương nào");
            else sLast.setValue((long) v[3], TeacherDashboardUI::vnd, "Phiếu mới nhất");
            sAvg.setValue((long) v[2], TeacherDashboardUI::vnd, "Trên tổng số phiếu đã nhận");
        });
        async(() -> Db.query("SELECT ngay_tra, tong_luong FROM Phieu_luong WHERE id_giao_vien = ? ORDER BY ngay_tra",
                rs -> new Object[]{rs.getTimestamp(1), rs.getLong(2)}, idGiaoVien), rows -> {
            int from = Math.max(0, rows.size() - 12), n = rows.size() - from;
            String[] lb = new String[n];
            double[] vv = new double[n];
            for (int i = 0; i < n; i++) {
                Object[] r = rows.get(from + i);
                LocalDate d = ld((Timestamp) r[0]);
                lb[i] = d == null ? "—" : String.format("%02d/%02d", d.getMonthValue(), d.getYear() % 100);
                vv[i] = (Long) r[1];
            }
            line.setData(lb, vv, false, x -> vnd(Math.round(x)), x -> compact(Math.round(x)));
        });

        JScrollPane sp = new JScrollPane(body);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return pad(sp);
    }

    private void showPayslipDialog(DataGrid g, int r) {
        String ma = g.str(r, 0);
        When tu = (When) g.value(r, 1), den = (When) g.value(r, 2), tra = (When) g.value(r, 3);
        long total = ((Money) g.value(r, 5)).v;
        async(() -> {
            long[] v = new long[]{0, 0, -1};
            if (tu.t != null && den.t != null) {
                v[0] = Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                        + "AND DATE(ngay_day) BETWEEN DATE(?) AND DATE(?)", idGiaoVien, DAY, tu.t, den.t);
                v[1] = Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                        + "AND DATE(ngay_day) BETWEEN DATE(?) AND DATE(?)", idGiaoVien, OFF, tu.t, den.t);
            }
            if (tra.t != null) {
                List<Object[]> prev = Db.query("SELECT tong_luong FROM Phieu_luong WHERE id_giao_vien = ? AND ngay_tra < ? "
                        + "ORDER BY ngay_tra DESC LIMIT 1", rs -> new Object[]{rs.getLong(1)}, idGiaoVien, tra.t);
                if (!prev.isEmpty()) v[2] = (Long) prev.get(0)[0];
            }
            return v;
        }, v -> {
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            JLabel big = lbl(vnd(total), font(Font.BOLD, 30), TEAL_D);
            body.add(big);
            String cmp;
            Color cc = MUTED;
            if (v[2] < 0) cmp = "Đây là kỳ lương đầu tiên được ghi nhận.";
            else if (v[2] == 0) cmp = "Kỳ trước không có lương để so sánh.";
            else {
                double ch = (total - v[2]) * 100.0 / v[2];
                cmp = (ch >= 0 ? "▲ Tăng " : "▼ Giảm ") + dec1(Math.abs(ch)) + "% so với kỳ trước (" + vnd(v[2]) + ")";
                cc = ch >= 0 ? GREEN : RED;
            }
            body.add(lbl(cmp, font(Font.BOLD, 13), cc));
            body.add(Box.createVerticalStrut(14));
            String perSession = v[0] > 0 ? vnd(total / v[0]) : "—";
            JPanel t = tiles(3, new String[][]{{"Mã phiếu", ma}, {"Kỳ lương", tu + " – " + den}, {"Ngày trả", tra.toString()},
                    {"Số ngày trong kỳ", g.str(r, 4)}, {"Buổi đã dạy trong kỳ", v[0] + " buổi"},
                    {"Lương trung bình / buổi", perSession}});
            t.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(t);
            if (v[1] > 0) {
                JLabel off = lbl("Trong kỳ có " + v[1] + " buổi đã báo nghỉ.", font(Font.PLAIN, 13), AMBER);
                off.setBorder(new EmptyBorder(10, 2, 0, 0));
                body.add(off);
            }
            body.add(section("Buổi dạy theo lớp trong kỳ"));
            DataGrid cg = new DataGrid(new String[]{"Lớp", "Số Buổi Dạy"}, "Không có buổi dạy nào trong kỳ này", cols(), true);
            cg.setAlignmentX(Component.LEFT_ALIGNMENT);
            cg.setPreferredSize(new Dimension(560, 230));
            cg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
            if (tu.t != null && den.t != null) {
                cg.load("SELECT COALESCE(l.ten_lop, lh.id_lop), COUNT(*) FROM Lich_hoc lh "
                                + "LEFT JOIN Lop l ON lh.id_lop = l.id_lop WHERE lh.id_giao_vien = ? "
                                + "AND lh.trang_thai_buoi = ? AND DATE(lh.ngay_day) BETWEEN DATE(?) AND DATE(?) "
                                + "GROUP BY lh.id_lop, l.ten_lop ORDER BY 2 DESC",
                        rs -> new Object[]{nz(rs.getString(1)), rs.getInt(2)}, idGiaoVien, DAY, tu.t, den.t);
            } else cg.setRows(new ArrayList<>());
            body.add(cg);
            showInfo("Phiếu lương " + ma, "Kỳ " + tu + " – " + den, body, 620, 560);
        });
    }

    /* ============================ THÔNG TIN CÁ NHÂN ============================ */

    /** Số liệu tổng hợp cho trang cá nhân. */
    private static final class ProfileStats {
        long lop, hs, taughtPast, offPast, taughtAll, payslips, paySum, exams, passed, ratings;
        double avg = -1;
    }

    private JComponent createProfilePage() {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(new LoadingView());
        async(() -> {
            ProfileStats s = new ProfileStats();
            s.lop = Db.scalar("SELECT COUNT(*) FROM Quanli WHERE id_giao_vien = ?", idGiaoVien);
            s.hs = Db.scalar("SELECT COUNT(*) FROM Hoc_sinh WHERE id_giao_vien = ?", idGiaoVien);
            s.taughtPast = Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                    + "AND ngay_day < NOW()", idGiaoVien, DAY);
            s.offPast = Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                    + "AND ngay_day < NOW()", idGiaoVien, OFF);
            s.taughtAll = Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ?",
                    idGiaoVien, DAY);
            s.payslips = Db.scalar("SELECT COUNT(*) FROM Phieu_luong WHERE id_giao_vien = ?", idGiaoVien);
            s.paySum = Db.scalar("SELECT COALESCE(SUM(tong_luong), 0) FROM Phieu_luong WHERE id_giao_vien = ?", idGiaoVien);
            s.avg = Db.decimal("SELECT AVG(rating) FROM Danh_gia_va_phan_hoi WHERE id_lop IN (" + MY_CLASSES + ")", idGiaoVien);
            s.ratings = Db.scalar("SELECT COUNT(*) FROM Danh_gia_va_phan_hoi WHERE id_lop IN (" + MY_CLASSES + ")", idGiaoVien);
            List<Object[]> ex = Db.query(EXAM_SELECT + "WHERE hs.id_giao_vien = ?", TeacherDashboardUI::examRow, idGiaoVien);
            s.exams = ex.size();
            for (Object[] r : ex) if (PASS.equals(r[8])) s.passed++;
            return s;
        }, s -> {
            holder.removeAll();
            holder.add(buildProfile(s));
            holder.revalidate();
            holder.repaint();
        });
        return pad(holder);
    }

    private JComponent buildProfile(ProfileStats s) {
        ProfileHero hero = new ProfileHero(hoTen, chungChi, idGiaoVien, soDienThoai, quocTich,
                new String[]{"Lớp", "Học sinh", "Buổi đã dạy"}, new long[]{s.lop, s.hs, s.taughtPast});

        // --- chỉ số giảng dạy
        ProgressRing r1 = new ProgressRing(AMBER), r2 = new ProgressRing(GREEN),
                r3 = new ProgressRing(TEAL), r4 = new ProgressRing(BLUE);
        if (s.avg < 0) r1.setValue(0, "—", "Đánh giá trung bình");
        else r1.setValue(s.avg / 5.0, dec1(s.avg), "Đánh giá trung bình");
        if (s.exams == 0) r2.setValue(0, "—", "Học sinh đạt thi thử");
        else r2.setValue(s.passed / (double) s.exams, pct(s.passed / (double) s.exams), "Học sinh đạt thi thử");
        long past = s.taughtPast + s.offPast;
        if (past == 0) r3.setValue(0, "—", "Buổi dạy đúng lịch");
        else r3.setValue(s.taughtPast / (double) past, pct(s.taughtPast / (double) past), "Buổi dạy đúng lịch");
        if (s.taughtAll == 0) r4.setValue(0, "—", "Tiến độ lịch dạy");
        else r4.setValue(s.taughtPast / (double) s.taughtAll, pct(s.taughtPast / (double) s.taughtAll), "Tiến độ lịch dạy");
        JPanel rings = new JPanel(new GridLayout(1, 4, 8, 0));
        rings.setOpaque(false);
        rings.add(r1);
        rings.add(r2);
        rings.add(r3);
        rings.add(r4);
        Card cardRings = new Card("Chỉ số giảng dạy", "Tổng hợp từ lịch dạy, đánh giá và kết quả thi", null);
        cardRings.add(rings, BorderLayout.CENTER);
        cardRings.setPreferredSize(new Dimension(100, 250));

        // --- thông tin chi tiết
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(infoRow("Mã giáo viên", idGiaoVien));
        rows.add(infoRow("Họ và tên", hoTen));
        rows.add(infoRow("Số điện thoại", soDienThoai));
        rows.add(infoRow("Chứng chỉ", chungChi));
        rows.add(infoRow("Quốc tịch", quocTich));
        rows.add(infoRow("Lớp phụ trách", String.valueOf(s.lop)));
        rows.add(infoRow("Học sinh phụ trách", String.valueOf(s.hs)));
        rows.add(infoRow("Số buổi đã dạy", String.valueOf(s.taughtPast)));
        rows.add(infoRow("Phiếu lương đã nhận", String.valueOf(s.payslips)));
        rows.add(infoRow("Tổng lương đã nhận", vnd(s.paySum)));
        FlatButton btnEdit = new FlatButton("Cập nhật liên hệ", "edit", TEAL, true);
        btnEdit.addActionListener(e -> editContact());
        Card cardInfo = new Card("Thông tin chi tiết", "Dữ liệu từ hồ sơ giáo viên", btnEdit);
        cardInfo.add(rows, BorderLayout.CENTER);

        // --- phân quyền
        JPanel perm = new JPanel();
        perm.setOpaque(false);
        perm.setLayout(new BoxLayout(perm, BoxLayout.Y_AXIS));
        perm.add(infoRow("Xem lớp và học sinh phụ trách", new Pill("Có", GREEN)));
        perm.add(infoRow("Xem điểm thi và phản hồi", new Pill("Có", GREEN)));
        perm.add(infoRow("Báo nghỉ / khôi phục buổi dạy của mình", new Pill("Có", GREEN)));
        perm.add(infoRow("Chỉnh sửa điểm thi, phiếu lương", new Pill("Không", RED)));
        Card cardPerm = new Card("Quyền của bạn", "Điểm thi và lương do nhân viên trung tâm quản lý", null);
        cardPerm.add(perm, BorderLayout.CENTER);

        JPanel rightCol = new JPanel(new GridBagLayout());
        rightCol.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        rightCol.add(cardRings, c);
        c.gridy = 1;
        rightCol.add(cardInfo, c);
        c.gridy = 2;
        rightCol.add(cardPerm, c);
        c.gridy = 3;
        c.weighty = 1;
        rightCol.add(Box.createVerticalGlue(), c);

        ScrollPanel wrap = new ScrollPanel(new BorderLayout());
        JPanel leftCol = new JPanel(new BorderLayout());
        leftCol.setOpaque(false);
        leftCol.add(hero, BorderLayout.NORTH);
        wrap.add(leftCol, BorderLayout.WEST);
        wrap.add(rightCol, BorderLayout.CENTER);
        JScrollPane sp = new JScrollPane(wrap);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return sp;
    }

    /** Giáo viên tự cập nhật số điện thoại liên hệ của mình. */
    private void editContact() {
        StyledField tf = new StyledField(soDienThoai).withPlaceholder("VD: 0912345678", "phone");
        JLabel note = lbl("Chứng chỉ và quốc tịch do trung tâm quản lý, vui lòng liên hệ nhân viên nếu cần thay đổi.",
                font(Font.PLAIN, 12), MUTED);
        JPanel form = vform(new String[]{"Số điện thoại liên hệ"}, new JComponent[]{tf});
        JPanel body = new JPanel(new BorderLayout(0, 4));
        body.setOpaque(false);
        body.add(form, BorderLayout.CENTER);
        body.add(note, BorderLayout.SOUTH);
        boolean ok = dialog("Cập nhật liên hệ", "Số điện thoại hiển thị trong hồ sơ giáo viên", body, "Lưu", TEAL, () -> {
            String p = tf.getText().trim();
            if (!p.matches("0\\d{9,10}")) return "Số điện thoại không hợp lệ (VD: 0912345678).";
            if (p.equals(soDienThoai)) return "Số điện thoại chưa thay đổi.";
            try {
                int n = Db.update("UPDATE Giao_vien SET so_dien_thoai = ? WHERE id_giao_vien = ?", p, idGiaoVien);
                if (n <= 0) return "Không tìm thấy hồ sơ giáo viên để cập nhật.";
                soDienThoai = p;
                return null;
            } catch (SQLException ex) {
                return "Lỗi CSDL: " + rootMsg(ex);
            }
        }, true);
        if (ok) {
            toast("Đã cập nhật số điện thoại.", T_OK);
            navigate("HoSo", false);
        }
    }

    /* ============================ MAIN ============================ */

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", font(Font.BOLD, 13));
        final String ma = args.length > 0 ? args[0] : "GV01";
        SwingUtilities.invokeLater(() -> new TeacherDashboardUI(ma).setVisible(true));
    }
}