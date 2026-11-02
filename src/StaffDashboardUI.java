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
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CỔNG NHÂN VIÊN S-LINK  (bản làm lại - Java Swing / JFrame, chạy độc lập, không cần PortalFrame)
 *
 * Cách mở:   new StaffDashboardUI("NV01").setVisible(true);
 * Yêu cầu:   MySQL Connector/J trong classpath, CSDL "btl" (file SQL của bạn).
 * Kết nối:   sửa DB_URL / DB_USER / DB_PASS bên dưới, hoặc chạy với -Ddb.url=... -Ddb.user=... -Ddb.pass=...
 *
 * Tự động bổ sung 3 cột còn thiếu cho bảng hoa_don (chạy 1 lần, an toàn nếu đã có):
 *   trang_thai ('Đã nộp' / 'Chưa nộp'), ngay_nop, nv_xac_nhan.
 */
public class StaffDashboardUI extends JFrame {

    /* ============================ CẤU HÌNH ============================ */
    static final String DB_URL = System.getProperty("db.url",
            "jdbc:mysql://localhost:3306/btl?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh");
    static final String DB_USER = System.getProperty("db.user", "root");
    static final String DB_PASS = System.getProperty("db.pass", "123456");

    /* ============================ GIAO DIỆN ============================ */
    private static final String FAMILY = pickFamily();

    static final Color BG = new Color(246, 244, 255);
    static final Color INK = new Color(30, 27, 59);
    static final Color MUTED = new Color(107, 114, 128);
    static final Color LINE = new Color(233, 230, 245);
    static final Color ACCENT = new Color(124, 58, 237);
    static final Color ACCENT_D = new Color(91, 33, 182);
    static final Color INDIGO = new Color(79, 70, 229);
    static final Color GREEN = new Color(22, 163, 74);
    static final Color AMBER = new Color(217, 119, 6);
    static final Color RED = new Color(220, 38, 38);
    static final Color BLUE = new Color(37, 99, 235);

    static final int T_OK = 0, T_WARN = 1, T_ERR = 2, T_INFO = 3;

    private static final String[] DAYS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter IN_FMT =
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

    static void shadow(Graphics2D g, double x, double y, double w, double h, double arc, double strength) {
        for (int i = 8; i >= 1; i--) {
            int a = (int) Math.max(0, Math.min(255, strength * (9 - i)));
            g.setColor(new Color(76, 29, 149, a));
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

    static String nz(String s) {
        return s == null ? "" : s.trim();
    }
    
static String payMethod(String s) {
        String t = nz(s);
        String k = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        switch (k) {
            case "tien mat": return "Tiền mặt";
            case "chuyen khoan": return "Chuyển khoản";
            case "the": return "Thẻ";
            case "": case "da nop": case "chua nop": return "—";
            default: return t;
        }
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

    static String rootMsg(Throwable e) {
        while (e.getCause() != null && e.getCause() != e) e = e.getCause();
        return e.getMessage() == null ? e.toString() : e.getMessage();
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

    static Object when(ResultSet rs, int i, boolean time) throws SQLException {
        return new When(rs.getTimestamp(i), time);
    }

    static Integer intObj(ResultSet rs, int i) throws SQLException {
        int v = rs.getInt(i);
        return rs.wasNull() ? null : v;
    }

    /* ============================ DỮ LIỆU ============================ */

    interface RowMapper {
        Object[] map(ResultSet rs) throws SQLException;
    }

    static final class Db {
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

        static int update(String sql, Object... p) throws SQLException {
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, p);
                return ps.executeUpdate();
            }
        }

        /** Bổ sung cột còn thiếu cho hoa_don; bỏ qua nếu cột đã tồn tại (mã lỗi 1060). */
        static void ensureSchema() throws SQLException {
            String[] ddl = {
                    "ALTER TABLE hoa_don ADD COLUMN trang_thai VARCHAR(20) NOT NULL DEFAULT 'Chưa nộp'",
                    "ALTER TABLE hoa_don ADD COLUMN ngay_nop TIMESTAMP NULL DEFAULT NULL",
                    "ALTER TABLE hoa_don ADD COLUMN nv_xac_nhan VARCHAR(50) NULL DEFAULT NULL", "CREATE TABLE IF NOT EXISTS Hoc_sinh_lop (id_lop VARCHAR(50) NOT NULL, ma_sinh_vien VARCHAR(50) NOT NULL, "
                            + "ngay_vao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id_lop, ma_sinh_vien), "
                            + "FOREIGN KEY (id_lop) REFERENCES Lop(id_lop), FOREIGN KEY (ma_sinh_vien) REFERENCES Hoc_sinh(ma_sinh_vien))"
                    };
            try (Connection c = open(); Statement st = c.createStatement()) {
                for (String s : ddl) {
                    try {
                        st.execute(s);
                    } catch (SQLException e) {
                        if (e.getErrorCode() != 1060) throw e;
                    }
                }
            }
        }
    }

    /** Tiền - giữ số để sắp xếp đúng, hiển thị dạng 1.200.000 ₫ */
    static final class Money implements Comparable<Money> {
        final long v;
        Money(long v) { this.v = v; }
        public int compareTo(Money o) { return Long.compare(v, o.v); }
        public String toString() { return vnd(v); }
    }

    /** Ngày giờ - giữ Timestamp để sắp xếp đúng thay vì sắp xếp chuỗi dd/MM/yyyy */
    static final class When implements Comparable<When> {
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
    }

    /** Chạy truy vấn nền, trả kết quả về luồng giao diện */
    static <T> void async(Callable<T> job, Consumer<T> ok) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return job.call(); }
            @Override protected void done() {
                try {
                    ok.accept(get());
                } catch (Exception e) {
                    toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR);
                }
            }
        }.execute();
    }

    /* ============================ HIỆU ỨNG ============================ */

    /** Giá trị số chuyển động mượt (ease-out) - dùng cho hover, fade, đếm số... */
    static final class Tween {
        private double v;
        private Timer timer;
        private final Runnable change;

        Tween(double init, Runnable change) { this.v = init; this.change = change; }

        double get() { return v; }

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

    static void runWhileShowing(JComponent c, Timer t) {
        c.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (c.isShowing()) t.start(); else t.stop();
            }
        });
    }

    /* ============================ ICON VECTOR ============================ */

    static final class Icons {
        private static final Pattern TOK = Pattern.compile("([MLHVCZ])([^MLHVCZ]*)");

        static Path2D path(String d) {
            Path2D.Double p = new Path2D.Double();
            Matcher m = TOK.matcher(d);
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
                case "user":
                    g.draw(new Ellipse2D.Double(8, 3.5, 8, 8)); g.draw(path("M4 21C4 15 20 15 20 21")); break;
                case "search":
                    g.draw(new Ellipse2D.Double(4, 4, 12, 12)); g.draw(path("M14 14L20 20")); break;
                case "chevron":
                    g.draw(path("M6 9L12 15L18 9")); break;
                case "check":
                    g.draw(path("M5 12.5L10 17.5L19 7")); break;
                case "plus":
                    g.draw(path("M12 5V19M5 12H19")); break;
                case "undo":
                    g.draw(path("M9 14L4 9L9 4M4 9H14C18 9 20 11 20 14C20 17 18 19 14 19H9")); break;
                case "clock":
                    g.draw(new Ellipse2D.Double(3, 3, 18, 18)); g.draw(path("M12 7V12L15.5 14")); break;
                case "inbox":
                    g.draw(path("M3 13L6 5H18L21 13V19H3ZM3 13H9C9 15 10.5 16 12 16C13.5 16 15 15 15 13H21")); break;
                case "alert":
                    g.draw(path("M12 3L22 20H2ZM12 10V14M12 17V17.2")); break;
                default:
                    g.draw(new Ellipse2D.Double(4, 4, 16, 16));
            }
            g.dispose();
        }
    }

    /* ============================ WIDGET ============================ */

    /** Nút bo tròn có hiệu ứng hover / nhấn */
    static final class FlatButton extends JButton {
        private final Tween hover = new Tween(0, this::repaint);
        private final String icon;
        private final boolean ghost;
        private Color base;

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
                fill = new Color(238, 238, 244); fg = new Color(160, 160, 175);
            } else if (ghost) {
                fill = lerp(Color.WHITE, alpha(base, 24), hv); fg = base; border = base;
            } else {
                fill = lerp(base, shade(base, 0.82), hv); fg = Color.WHITE;
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
            setCaretColor(ACCENT);
            setSelectionColor(alpha(ACCENT, 70));
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
            g.setColor(new Color(250, 249, 255));
            g.fill(rr(0, 0, w, h, 26));
            if (f > 0.01) {
                g.setColor(alpha(ACCENT, (int) (45 * f)));
                g.setStroke(new BasicStroke(4f));
                g.draw(rr(1.5, 1.5, w - 3, h - 3, 26));
            }
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(lerp(new Color(221, 214, 254), ACCENT, f));
            g.draw(rr(.7, .7, w - 1.4, h - 1.4, 26));
            if (icon != null) Icons.paint(g, icon, 14, (h - 16) / 2.0, 16, lerp(new Color(156, 163, 175), ACCENT, f));
            if (getText().isEmpty() && !placeholder.isEmpty()) {
                g.setFont(getFont());
                g.setColor(new Color(156, 163, 175));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(placeholder, getInsets().left, (h + fm.getAscent() - fm.getDescent()) / 2);
            }
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
                    list.setSelectionBackground(new Color(233, 227, 254));
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
            g.setColor(new Color(250, 249, 255));
            g.fill(rr(0, 0, getWidth(), getHeight(), 26));
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(new Color(221, 214, 254));
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
        private int sel = 0;

        Segmented(String[] labels, IntConsumer cb) {
            this.labels = labels;
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
                            sel = i;
                            pos.to(i, 260);
                            cb.accept(i);
                            repaint();
                        }
                    }
                }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(240, 236, 255));
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
                g.setColor(lerp(MUTED, ACCENT_D, near));
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
    };

    /** Thẻ thống kê: đếm số chạy + nâng lên khi hover */
    static final class StatCard extends JComponent {
        private final String title, icon;
        private final Color color;
        private String sub = " ";
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

        void setValue(long v, LongFunction<String> f, String sub) {
            this.target = v;
            this.fmt = f;
            this.sub = sub;
            this.loaded = true;
            count.set(0);
            count.to(1, 1000);
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
            // dải màu nhấn bên trái
            Shape old = g.getClip();
            g.clip(rr(x, y, w, h, 28));
            g.setColor(alpha(color, 22));
            g.fill(new Ellipse2D.Double(x + w - 70, y - 40, 120, 120));
            g.setClip(old);
            // bong bóng icon
            double bs = 46, bx = x + w - bs - 18, by = y + 18;
            g.setPaint(new GradientPaint((float) bx, (float) by, lerp(color, Color.WHITE, 0.25),
                    (float) (bx + bs), (float) (by + bs), shade(color, 0.85)));
            g.fill(rr(bx, by, bs, bs, 28));
            Icons.paint(g, icon, bx + 12, by + 12, 22, Color.WHITE);
            // chữ
            g.setFont(font(Font.PLAIN, 13));
            g.setColor(MUTED);
            g.drawString(title, (int) x + 20, (int) y + 34);
            String val = loaded ? fmt.apply((long) (target * count.get())) : "···";
            float fs = 28f;
            Font vf = font(Font.BOLD, fs);
            double maxW = w - 40;
            while (g.getFontMetrics(vf).stringWidth(val) > maxW && fs > 14) vf = font(Font.BOLD, fs -= 1f);
            g.setFont(vf);
            g.setColor(INK);
            g.drawString(val, (int) x + 20, (int) y + 76);
            g.setFont(font(Font.PLAIN, 12));
            g.setColor(shade(color, 0.9));
            g.drawString(sub, (int) x + 20, (int) y + 100);
            g.dispose();
        }
    }

    /** Biểu đồ cột doanh thu - cột mọc lên khi tải, rê chuột để xem số tiền */
    static final class BarChart extends JComponent {
        private String[] labels = {};
        private long[] vals = {};
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
                        double slot = (getWidth() - 52) / (double) n;
                        int i = (int) ((e.getX() - 52) / slot);
                        if (e.getX() >= 52 && i >= 0 && i < n) hov = i;
                    }
                    if (old != hov) repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) { hov = -1; repaint(); }
            });
        }

        void setData(String[] l, long[] v) {
            labels = l;
            vals = v;
            grow.set(0);
            grow.to(1, 1000);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            int left = 52, top = 22, bottom = 30;
            int plotH = h - top - bottom, baseY = top + plotH;
            long max = 1;
            for (long v : vals) max = Math.max(max, v);
            double mag = Math.pow(10, Math.floor(Math.log10(max)));
            long nice = (long) (Math.ceil(max / mag) * mag);
            g.setFont(font(Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i <= 4; i++) {
                int y = baseY - (int) (plotH * i / 4.0);
                g.setColor(i == 0 ? new Color(221, 214, 254) : new Color(243, 240, 255));
                g.drawLine(left, y, w, y);
                g.setColor(MUTED);
                String s = compact(nice * i / 4);
                g.drawString(s, left - 8 - fm.stringWidth(s), y + 4);
            }
            int n = vals.length;
            if (n == 0) { g.dispose(); return; }
            double slot = (w - left) / (double) n;
            double bw = Math.min(46, slot * 0.52);
            double e = grow.get();
            for (int i = 0; i < n; i++) {
                double bx = left + slot * i + (slot - bw) / 2;
                double bh = plotH * (vals[i] / (double) nice) * e;
                boolean on = i == hov;
                Shape old = g.getClip();
                g.clipRect(0, 0, w, baseY);
                if (bh > 0.5) {
                    g.setPaint(new GradientPaint(0, (float) (baseY - bh), on ? new Color(167, 139, 250) : new Color(139, 92, 246),
                            0, baseY, on ? ACCENT : INDIGO));
                    g.fill(rr(bx, baseY - bh, bw, bh + 12, 16));
                }
                g.setClip(old);
                g.setColor(on ? ACCENT_D : MUTED);
                g.setFont(font(on ? Font.BOLD : Font.PLAIN, 12));
                int lw = g.getFontMetrics().stringWidth(labels[i]);
                g.drawString(labels[i], (int) (bx + bw / 2 - lw / 2.0), baseY + 20);
                if (on) {
                    String tip = vnd(vals[i]);
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

    /** Banner chào mừng có các hình tròn chuyển động nhẹ */
    static final class Banner extends JComponent {
        private final String title, sub, chip;
        private double phase;

        Banner(String title, String sub, String chip) {
            this.title = title;
            this.sub = sub;
            this.chip = chip;
            setPreferredSize(new Dimension(100, 140));
            runWhileShowing(this, new Timer(40, e -> { phase += 0.025; repaint(); }));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int P = Card.P;
            double x = P, y = P, w = getWidth() - 2 * P, h = getHeight() - 2 * P;
            shadow(g, x, y, w, h, 32, 1.0);
            Shape old = g.getClip();
            g.clip(rr(x, y, w, h, 32));
            g.setPaint(new GradientPaint((float) x, (float) y, new Color(124, 58, 237), (float) (x + w), (float) (y + h), new Color(79, 70, 229)));
            g.fill(rr(x, y, w, h, 32));
            for (int i = 0; i < 4; i++) {
                double cx = x + w * (0.52 + 0.13 * i) + Math.sin(phase + i * 1.3) * 22;
                double cy = y + h * 0.5 + Math.cos(phase * 1.3 + i) * 16;
                double r = 55 + i * 26;
                g.setColor(new Color(255, 255, 255, 14 + i * 3));
                g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
            g.setClip(old);
            // chip vai trò
            g.setFont(font(Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            String c = chip.isEmpty() ? "Nhân viên" : chip;
            int cw = fm.stringWidth(c) + 24;
            g.setColor(new Color(255, 255, 255, 40));
            g.fill(rr(x + 30, y + 22, cw, 26, 26));
            g.setColor(Color.WHITE);
            g.drawString(c, (int) x + 42, (int) y + 40);
            g.setFont(font(Font.BOLD, 26));
            g.drawString(title, (int) x + 30, (int) y + 80);
            g.setFont(font(Font.PLAIN, 14));
            g.setColor(new Color(233, 227, 254));
            g.drawString(sub, (int) x + 30, (int) y + 106);
            // thẻ ngày
            LocalDate d = LocalDate.now();
            int bw = 92, bh = 84;
            double bx = x + w - bw - 28, by = y + (h - bh) / 2;
            g.setColor(new Color(255, 255, 255, 36));
            g.fill(rr(bx, by, bw, bh, 26));
            g.setColor(Color.WHITE);
            g.setFont(font(Font.BOLD, 30));
            String dd = String.valueOf(d.getDayOfMonth());
            g.drawString(dd, (int) (bx + (bw - g.getFontMetrics().stringWidth(dd)) / 2), (int) by + 44);
            g.setFont(font(Font.PLAIN, 12));
            String mm = "Tháng " + d.getMonthValue() + "/" + d.getYear();
            g.drawString(mm, (int) (bx + (bw - g.getFontMetrics().stringWidth(mm)) / 2), (int) by + 66);
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

        static String initialsOf(String name) {
            String[] w = nz(name).split("\\s+");
            if (w.length == 0 || w[0].isEmpty()) return "?";
            String a = w[0].substring(0, 1);
            String b = w.length > 1 ? w[w.length - 1].substring(0, 1) : "";
            return (a + b).toUpperCase(new Locale("vi"));
        }

        static void paint(Graphics2D g, String ini, double x, double y, double s) {
            g.setPaint(new GradientPaint((float) x, (float) y, new Color(167, 139, 250), (float) (x + s), (float) (y + s), new Color(236, 72, 153)));
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
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    static final class Spinner extends JComponent {
        private int angle;

        Spinner() {
            setPreferredSize(new Dimension(44, 44));
            runWhileShowing(this, new Timer(16, e -> { angle = (angle + 9) % 360; repaint(); }));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int s = Math.min(getWidth(), getHeight()) - 8;
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(233, 227, 254));
            g.drawOval((getWidth() - s) / 2, (getHeight() - s) / 2, s, s);
            g.setColor(ACCENT);
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

        EmptyState(String text) { this.text = text; }

        void setText(String t) { text = t; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            int cx = getWidth() / 2, cy = getHeight() / 2 - 20;
            g.setColor(new Color(245, 243, 255));
            g.fill(new Ellipse2D.Double(cx - 50, cy - 50, 100, 100));
            Icons.paint(g, "inbox", cx - 24, cy - 24, 48, new Color(167, 139, 250));
            g.setFont(font(Font.PLAIN, 14));
            g.setColor(MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, cx - fm.stringWidth(text) / 2, cy + 82);
            g.dispose();
        }
    }

    static final class ScrollPanel extends JPanel implements Scrollable {
        ScrollPanel(LayoutManager l) { super(l); setOpaque(false); }
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
            g.setColor(isThumbRollover() || isDragging ? new Color(167, 139, 250) : new Color(214, 204, 250));
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

    /* ============================ BẢNG DỮ LIỆU ============================ */

    static final class GridModel extends DefaultTableModel {
        GridModel(String[] c) { super(c, 0); }
        @Override public boolean isCellEditable(int r, int c) { return false; }
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
            case "Đã nộp": case "Dạy": return GREEN;
            case "Chưa nộp": return AMBER;
            case "Nghỉ": return RED;
            default: return ACCENT;
        }
    }

    static final class StyledTable extends JTable {
        int hoverRow = -1;
        private final Set<Integer> pills;

        StyledTable(GridModel m, Set<Integer> pills, boolean compact) {
            super(m);
            this.pills = pills;
            setRowHeight(compact ? 42 : 48);
            setShowGrid(false);
            setIntercellSpacing(new Dimension(0, 0));
            setFillsViewportHeight(true);
            setFont(font(Font.PLAIN, 13));
            setSelectionBackground(new Color(233, 227, 254));
            setSelectionForeground(INK);
            setFocusable(true);
            setDefaultRenderer(Object.class, new CellR());
            JTableHeaderStyle.apply(this, compact);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int r = rowAtPoint(e.getPoint());
                    if (r != hoverRow) { hoverRow = r; repaint(); }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) { hoverRow = -1; repaint(); }
            });
        }

        private final class CellR implements TableCellRenderer {
            private final JLabel label = new JLabel();
            private final JPanel pillCell = new JPanel() {
                String text = "";
                Color bg = Color.WHITE, pc = ACCENT;
                @Override public void setName(String n) { text = n == null ? "" : n; }
                @Override public void setBackground(Color c) { bg = c; }
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    aa(g);
                    g.setColor(bg);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(new Color(246, 244, 252));
                    g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                    if (!text.isEmpty()) {
                        g.setFont(font(Font.BOLD, 12));
                        FontMetrics fm = g.getFontMetrics();
                        int w = fm.stringWidth(text) + 22, h = 26, y = (getHeight() - h) / 2;
                        g.setColor(alpha(pc, 30));
                        g.fill(rr(16, y, w, h, 26));
                        g.setColor(shade(pc, 0.85));
                        g.drawString(text, 27, y + (h + fm.getAscent() - fm.getDescent()) / 2);
                    }
                    g.dispose();
                }
                @Override public void setForeground(Color c) { pc = c; }
            };

            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Color bg = sel ? new Color(233, 227, 254) : row == hoverRow ? new Color(250, 248, 255) : Color.WHITE;
                String s = v == null ? "" : v.toString();
                if (pills.contains(col) && !s.isEmpty() && !s.equals("—")) {
                    pillCell.setBackground(bg);
                    pillCell.setName(s);
                    pillCell.setForeground(pillColor(s));
                    return pillCell;
                }
                label.setText(s);
                label.setOpaque(true);
                label.setBackground(bg);
                label.setForeground(col == 0 ? ACCENT_D : INK);
                label.setFont(font(col == 0 || v instanceof Money ? Font.BOLD : Font.PLAIN, 13));
                label.setHorizontalAlignment(v instanceof Money || v instanceof Integer ? SwingConstants.RIGHT : SwingConstants.LEFT);
                label.setBorder(new MatteBorder(0, 0, 1, 0, new Color(246, 244, 252)));
                label.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(0, 0, 1, 0, new Color(246, 244, 252)), new EmptyBorder(0, 16, 0, 16)));
                return label;
            }
        }
    }

    static final class JTableHeaderStyle {
        static void apply(JTable t, boolean compact) {
            t.getTableHeader().setReorderingAllowed(false);
            t.getTableHeader().setPreferredSize(new Dimension(0, compact ? 38 : 44));
            t.getTableHeader().setDefaultRenderer((tbl, v, sel, foc, row, col) -> {
                String s = String.valueOf(v).toUpperCase(new Locale("vi"));
                List<? extends RowSorter.SortKey> keys = tbl.getRowSorter() == null ? null : tbl.getRowSorter().getSortKeys();
                if (keys != null && !keys.isEmpty() && keys.get(0).getColumn() == col) {
                    s += keys.get(0).getSortOrder() == SortOrder.ASCENDING ? "  ▲" : "  ▼";
                }
                JLabel l = new JLabel(s);
                l.setOpaque(true);
                l.setBackground(new Color(249, 248, 255));
                l.setForeground(new Color(124, 106, 170));
                l.setFont(font(Font.BOLD, 11));
                Class<?> c = tbl.getModel().getColumnClass(col);
                l.setHorizontalAlignment(c == Money.class || c == Integer.class ? SwingConstants.RIGHT : SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 0, 1, 0, LINE), new EmptyBorder(0, 16, 0, 16)));
                return l;
            });
        }
    }

    /** Bảng + tìm kiếm + lọc trạng thái + trạng thái tải/rỗng, nạp dữ liệu bất đồng bộ */
    static final class DataGrid extends JPanel {
        final GridModel model;
        final StyledTable table;
        final TableRowSorter<GridModel> sorter;
        private final CardLayout cards = new CardLayout();
        private final JPanel stack = new JPanel(cards);
        private final EmptyState empty;
        private final JLabel count = new JLabel(" ");
        private final String emptyMsg;
        private String text = "", status;
        private int statusCol = -1;
        private final Map<Integer, String> colFilters = new HashMap<>();
        private boolean loading;
        private String lastSql;
        private RowMapper lastMap;
        private Object[] lastParams = new Object[0];
        private Runnable onLoaded;

        DataGrid(String[] cols, String emptyMsg, Set<Integer> pills, boolean compact) {
            super(new BorderLayout(0, 8));
            setOpaque(false);
            this.emptyMsg = emptyMsg;
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
        }

        void setOnLoaded(Runnable r) { onLoaded = r; }

        void load(String sql, RowMapper map, Object... params) {
            lastSql = sql;
            lastMap = map;
            lastParams = params;
            reload();
        }

        void reload() {
            loading = true;
            cards.show(stack, "loading");
            final String sql = lastSql;
            final RowMapper map = lastMap;
            final Object[] p = lastParams;
            new SwingWorker<List<Object[]>, Void>() {
                @Override protected List<Object[]> doInBackground() throws Exception { return Db.query(sql, map, p); }
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
            if (total == 0) { empty.setText(emptyMsg); cards.show(stack, "empty"); }
            else if (shown == 0) { empty.setText("Không tìm thấy kết quả phù hợp."); cards.show(stack, "empty"); }
            else cards.show(stack, "table");
            count.setText("Hiển thị " + shown + " / " + total + " bản ghi");
        }

        void setText(String t) { text = t.trim(); applyFilter(); }

        void setStatus(String s, int col) { status = s; statusCol = col; applyFilter(); }

        private void applyFilter() {
            List<RowFilter<GridModel, Integer>> fs = new ArrayList<>();
            if (!text.isEmpty()) fs.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
            if (status != null && statusCol >= 0) fs.add(RowFilter.regexFilter("^" + Pattern.quote(status) + "$", statusCol));
            for (Map.Entry<Integer, String> en : colFilters.entrySet())
                fs.add(RowFilter.regexFilter("^" + Pattern.quote(en.getValue()) + "$", en.getKey()));
            sorter.setRowFilter(fs.isEmpty() ? null : RowFilter.andFilter(fs));
        }

        void setColFilter(int col, String value) {
            if (value == null) colFilters.remove(col); else colFilters.put(col, value);
            applyFilter();
        }

        void sortBy(int col, boolean asc) {
            if (col < 0) { sorter.setSortKeys(null); return; }
            sorter.setSortKeys(java.util.Collections.singletonList(
                    new RowSorter.SortKey(col, asc ? SortOrder.ASCENDING : SortOrder.DESCENDING)));
        }

        int selectedModelRow() {
            int r = table.getSelectedRow();
            return r < 0 ? -1 : table.convertRowIndexToModel(r);
        }

        Object value(int modelRow, int col) { return model.getValueAt(modelRow, col); }
    }

    /* ============================ SIDEBAR / HEADER / TOAST ============================ */

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

        void setSelected(boolean s) { sel.to(s ? 1 : 0, 280); }

        void setBadge(int n) { badge = n; repaint(); }

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
                g.setPaint(new GradientPaint(0, 0, alpha(new Color(139, 92, 246), (int) (255 * s)),
                        w, 0, alpha(new Color(99, 102, 241), (int) (255 * s))));
                g.fill(rr(0, 0, w, h, 28));
            }
            double k = Math.max(s, hv * 0.75);
            Icons.paint(g, icon, 16 + hv * 2, (h - 20) / 2.0, 20, lerp(new Color(196, 181, 253), Color.WHITE, k));
            g.setFont(font(s > 0.5 ? Font.BOLD : Font.PLAIN, 14));
            g.setColor(lerp(new Color(209, 213, 254), Color.WHITE, k));
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

        ToastLayer() { setOpaque(false); }

        @Override public boolean contains(int x, int y) { return false; }

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
            String ic = kind == T_OK ? "check" : kind == T_INFO ? "clock" : "alert";
            g.setFont(font(Font.PLAIN, 14));
            FontMetrics fm = g.getFontMetrics();
            int maxText = Math.min(560, getWidth() - 160);
            String t = msg;
            while (fm.stringWidth(t) > maxText && t.length() > 4) t = t.substring(0, t.length() - 2);
            if (!t.equals(msg)) t += "…";
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
            if (animate) { t.set(0); t.to(1, 420); } else t.set(1);
        }

        @Override public void paint(Graphics g0) {
            double a = t.get();
            if (a >= 0.999) { super.paint(g0); return; }
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, a))));
            g.translate(0, (1 - a) * 22);
            super.paint(g);
            g.dispose();
        }
    }

    /** Hồ sơ nhân viên (thẻ bên trái) */
    static final class ProfileHero extends JComponent {
        private final String name, role, id;
        private final String[] labels;
        private final long[] nums;

        ProfileHero(String name, String role, String id, String[] labels, long[] nums) {
            this.name = name; this.role = role; this.id = id; this.labels = labels; this.nums = nums;
            setPreferredSize(new Dimension(340, 440));
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
            g.setPaint(new GradientPaint((float) x, (float) y, new Color(124, 58, 237), (float) (x + w), (float) (y + 130), new Color(79, 70, 229)));
            g.fillRect((int) x, (int) y, (int) w, 130);
            g.setColor(new Color(255, 255, 255, 22));
            g.fill(new Ellipse2D.Double(x + w - 110, y - 60, 200, 200));
            g.fill(new Ellipse2D.Double(x - 50, y + 50, 140, 140));
            g.setClip(old);
            double cx = x + w / 2;
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(cx - 54, y + 130 - 54, 108, 108));
            Avatar.paint(g, Avatar.initialsOf(name), cx - 48, y + 130 - 48, 96);
            g.setFont(font(Font.BOLD, 21));
            g.setColor(INK);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(name, (int) (cx - fm.stringWidth(name) / 2.0), (int) y + 222);
            g.setFont(font(Font.PLAIN, 13));
            g.setColor(MUTED);
            fm = g.getFontMetrics();
            g.drawString(id, (int) (cx - fm.stringWidth(id) / 2.0), (int) y + 246);
            String r = role.isEmpty() ? "Chưa xác định vai trò" : role;
            g.setFont(font(Font.BOLD, 12));
            fm = g.getFontMetrics();
            int pw = fm.stringWidth(r) + 28;
            g.setColor(alpha(ACCENT, 28));
            g.fill(rr(cx - pw / 2.0, y + 262, pw, 28, 28));
            g.setColor(ACCENT_D);
            g.drawString(r, (int) (cx - fm.stringWidth(r) / 2.0), (int) y + 281);
            g.setColor(LINE);
            g.drawLine((int) x + 24, (int) y + 316, (int) (x + w - 24), (int) y + 316);
            double col = (w - 48) / 3.0;
            for (int i = 0; i < 3; i++) {
                double mx = x + 24 + col * i + col / 2;
                String n = String.valueOf(nums[i]);
                g.setFont(font(Font.BOLD, 24));
                fm = g.getFontMetrics();
                g.setColor(ACCENT_D);
                g.drawString(n, (int) (mx - fm.stringWidth(n) / 2.0), (int) y + 360);
                g.setFont(font(Font.PLAIN, 12));
                fm = g.getFontMetrics();
                g.setColor(MUTED);
                g.drawString(labels[i], (int) (mx - fm.stringWidth(labels[i]) / 2.0), (int) y + 382);
            }
            g.dispose();
        }
    }

    /* ============================ TRẠNG THÁI KHUNG ============================ */

    private static StaffDashboardUI current;

    static void toast(String msg, int kind) {
        if (current != null) SwingUtilities.invokeLater(() -> current.toastLayer.post(msg, kind));
    }

    private static final class PageDef {
        final String key, title, subtitle;
        final Supplier<JComponent> factory;
        NavItem nav;
        PageDef(String key, String title, String subtitle, Supplier<JComponent> f) {
            this.key = key; this.title = title; this.subtitle = subtitle; this.factory = f;
        }
    }

    private final String maNhanVien;
    private String hoTen = "";
    private String vaiTro = "";
    private long luong = 0;
    private boolean valid = false;
    private String startupMsg;

    private final Map<String, PageDef> pages = new LinkedHashMap<>();
    private final FadePanel host = new FadePanel();
    private final ToastLayer toastLayer = new ToastLayer();
    private final JLabel pageTitle = lbl(" ", font(Font.BOLD, 22), INK);
    private final JLabel pageSub = lbl(" ", font(Font.PLAIN, 13), MUTED);
    private final JLabel clock = lbl(" ", font(Font.PLAIN, 13), MUTED);
    private Timer clockTimer;
    private String currentKey = "";

    public StaffDashboardUI() {
        this("NV01");
    }

    public StaffDashboardUI(String maNhanVien) {
        super("CỔNG NHÂN VIÊN S-LINK");
        this.maNhanVien = maNhanVien;
        current = this;
        try {
            Db.ensureSchema();
        } catch (SQLException e) {
            startupMsg = "Không kết nối / cập nhật được CSDL: " + rootMsg(e)
                    + "\n\nKiểm tra DB_URL, DB_USER, DB_PASS và MySQL Connector/J.";
        }
        if (startupMsg == null) loadProfile();
        else hoTen = maNhanVien;
        setTitle("CỔNG NHÂN VIÊN S-LINK - " + hoTen.toUpperCase(new Locale("vi")));

        pages.put("Dashboard", new PageDef("Dashboard", "Tổng quan", "Bức tranh nhanh về hoạt động trung tâm", this::createOverviewPage));
        pages.put("HoaDon", new PageDef("HoaDon", "Hóa đơn học phí", "Xác nhận học phí học sinh đã nộp", this::createInvoicePage));
        pages.put("HocSinh", new PageDef("HocSinh", "Học sinh", "Danh sách học sinh toàn trung tâm", this::createStudentPage));
        pages.put("Lop", new PageDef("Lop", "Lớp học", "Các lớp và nhân viên phụ trách", this::createClassPage));
        pages.put("KhoaHoc", new PageDef("KhoaHoc", "Khóa học", "Chương trình và học phí", this::createCoursePage));
        pages.put("Lich", new PageDef("Lich", "Lịch học", "Lịch giảng dạy toàn trung tâm", this::createSchedulePage));
        pages.put("Luong", new PageDef("Luong", "Phiếu lương GV", "Lập và theo dõi phiếu lương giáo viên", this::createPayslipPage));
        pages.put("HoSo", new PageDef("HoSo", "Thông tin cá nhân", "Hồ sơ và phân quyền của bạn", this::createProfilePage));
        String[] icons = {"home", "wallet", "group", "book", "list", "calendar", "award", "user"};
        int i = 0;
        for (PageDef p : pages.values()) {
            final String key = p.key;
            p.nav = new NavItem(p.title, icons[i++], () -> navigate(key, true));
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
            @Override public void windowClosed(WindowEvent e) { if (clockTimer != null) clockTimer.stop(); }
        });

        navigate("Dashboard", true);
        refreshBadge();
        SwingUtilities.invokeLater(() -> {
            if (startupMsg != null) {
                JOptionPane.showMessageDialog(this, startupMsg, "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void loadProfile() {
        try {
            List<Object[]> r = Db.query("SELECT ho_ten, vai_tro, luong FROM Nhan_vien WHERE ma_nhan_vien = ?",
                    rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getLong(3)}, maNhanVien);
            if (r.isEmpty()) {
                hoTen = maNhanVien;
                startupMsg = "Không tìm thấy nhân viên có mã " + maNhanVien
                        + " trong CSDL.\nCổng sẽ chạy ở chế độ chỉ xem, mọi thao tác ghi dữ liệu bị khóa.";
            } else {
                hoTen = nz((String) r.get(0)[0]);
                vaiTro = nz((String) r.get(0)[1]);
                luong = (Long) r.get(0)[2];
                valid = true;
            }
        } catch (SQLException e) {
            hoTen = maNhanVien;
            startupMsg = "Không đọc được hồ sơ nhân viên: " + rootMsg(e);
        }
    }

    private static Image appIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        aa(g);
        g.setPaint(new GradientPaint(0, 0, new Color(139, 92, 246), 64, 64, new Color(79, 70, 229)));
        g.fill(rr(2, 2, 60, 60, 28));
        g.setColor(Color.WHITE);
        g.setFont(font(Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("S", (64 - fm.stringWidth("S")) / 2, (64 + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }

    /* ============================ PHÂN QUYỀN ============================ */

    private boolean roleHas(String... keys) {
        String v = vaiTro.toLowerCase(new Locale("vi"));
        for (String k : keys) if (v.contains(k)) return true;
        return false;
    }

    /** Whitelist: chỉ Kế toán / Quản lý / Giám đốc / Admin. Vai trò trống hoặc lạ → KHÔNG có quyền. */
    private boolean canPayroll() {
        return valid && roleHas("kế toán", "quản lý", "giám đốc", "quản trị", "admin");
    }

    private boolean canConfirm() { return valid; }

    private boolean canRevert() { return canPayroll(); }

    /* ============================ KHUNG GIAO DIỆN ============================ */

    private JPanel buildSidebar() {
        JPanel sb = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setPaint(new GradientPaint(0, 0, new Color(52, 18, 112), 0, getHeight(), new Color(24, 19, 62)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(255, 255, 255, 10));
                g.fill(new Ellipse2D.Double(-90, getHeight() - 210, 280, 280));
                g.setColor(new Color(167, 139, 250, 26));
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
                g.setPaint(new GradientPaint(0, 0, new Color(167, 139, 250), 44, 44, new Color(99, 102, 241)));
                g.fill(rr(4, 2, 44, 44, 28));
                g.setColor(Color.WHITE);
                g.setFont(font(Font.BOLD, 26));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("S", 4 + (44 - fm.stringWidth("S")) / 2, 2 + (44 + fm.getAscent() - fm.getDescent()) / 2);
                g.setFont(font(Font.BOLD, 20));
                g.drawString("S-Link", 62, 26);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(new Color(196, 181, 253));
                g.drawString("Staff Portal", 62, 43);
                g.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(232, 50));
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(logo);
        sb.add(Box.createVerticalStrut(28));
        JLabel menu = lbl("MENU", font(Font.BOLD, 11), new Color(167, 139, 250));
        menu.setBorder(new EmptyBorder(0, 14, 8, 0));
        sb.add(menu);
        for (PageDef p : pages.values()) {
            sb.add(p.nav);
            sb.add(Box.createVerticalStrut(4));
        }
        sb.add(Box.createVerticalGlue());

        final String role = vaiTro.isEmpty() ? "Chưa xác định" : vaiTro;
        JComponent chip = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setColor(new Color(255, 255, 255, 22));
                g.fill(rr(0, 0, getWidth(), getHeight(), 28));
                Avatar.paint(g, Avatar.initialsOf(hoTen), 12, 11, 42);
                g.setFont(font(Font.BOLD, 13));
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                String n = hoTen;
                while (fm.stringWidth(n) > getWidth() - 76 && n.length() > 3) n = n.substring(0, n.length() - 2);
                g.drawString(n.equals(hoTen) ? n : n + "…", 64, 29);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(new Color(196, 181, 253));
                g.drawString(maNhanVien + " • " + role, 64, 46);
                g.dispose();
            }
        };
        chip.setPreferredSize(new Dimension(232, 64));
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(chip);
        sb.add(Box.createVerticalStrut(10)); // Khoảng cách nhỏ

        FlatButton btnLogout = new FlatButton("Đăng xuất", "undo", new Color(239, 68, 68), true);
        btnLogout.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnLogout.addActionListener(e -> {
            int r = JOptionPane.showOptionDialog(StaffDashboardUI.this, 
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

        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 19));
        r.setOpaque(false);
        r.add(clock);
        r.add(new Avatar(hoTen, 40));
        h.add(r, BorderLayout.EAST);
        clock.setText(nowText());
        clockTimer = new Timer(1000, e -> clock.setText(nowText()));
        clockTimer.start();
        return h;
    }

    private static String nowText() {
        LocalDateTime n = LocalDateTime.now();
        return DAYS[n.getDayOfWeek().getValue() - 1] + ", " + n.format(DateTimeFormatter.ofPattern("dd/MM/yyyy  •  HH:mm:ss"));
    }

    private static String greeting() {
        int h = LocalDateTime.now().getHour();
        return h < 11 ? "Chào buổi sáng" : h < 14 ? "Chào buổi trưa" : h < 18 ? "Chào buổi chiều" : "Chào buổi tối";
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

    private void refreshBadge() {
        async(() -> Db.scalar("SELECT COUNT(*) FROM hoa_don WHERE trang_thai = ?", "Chưa nộp"), n -> {
            PageDef p = pages.get("HoaDon");
            if (p != null) p.nav.setBadge(n.intValue());
        });
    }

    private static JComponent pad(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(14, 20, 16, 20));
        p.add(c);
        return p;
    }

    /* ============================ HỘP THOẠI ============================ */

    /** Hộp thoại hiện đại. onOk trả về thông báo lỗi (giữ hộp thoại, rung) hoặc null (đóng, thành công). */
    boolean dialog(String title, String sub, JComponent body, String okText, Color okColor,
                   Supplier<String> onOk, boolean withCancel) {
        JDialog d = new JDialog(this, true);
        d.setUndecorated(true);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createLineBorder(new Color(221, 214, 254)));

        JPanel head = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setPaint(new GradientPaint(0, 0, new Color(124, 58, 237), getWidth(), getHeight(), new Color(79, 70, 229)));
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
            head.add(lbl(sub, font(Font.PLAIN, 13), new Color(221, 214, 254)));
        }
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
                    if (n[0] > 22) { ((Timer) ev.getSource()).stop(); d.setLocation(p0); }
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
        root.setPreferredSize(new Dimension(Math.max(540, ps.width), ps.height));
        d.pack();
        d.setLocationRelativeTo(this);
        Tween[] tw = new Tween[1];
        try {
            d.setOpacity(0.05f);
            tw[0] = new Tween(0.05, () -> {
                try { d.setOpacity((float) Math.max(0.05, Math.min(1, tw[0].get()))); } catch (Exception ignored) { }
            });
            tw[0].to(1, 220);
        } catch (Exception ignored) { }
        d.setVisible(true);
        return result[0];
    }

    private boolean confirm(String title, String html, String okText, Color okColor) {
        JLabel l = new JLabel("<html><div style='width:420px;line-height:1.5'>" + html + "</div></html>");
        l.setFont(font(Font.PLAIN, 14));
        l.setForeground(INK);
        return dialog(title, "Vui lòng kiểm tra kỹ trước khi xác nhận", l, okText, okColor, () -> null, true);
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
        body.add(new Banner(greeting() + ", " + hoTen + "!",
                "Theo dõi học phí, học sinh, lớp học và lịch giảng dạy của trung tâm.", vaiTro), c);

        StatCard cStu = new StatCard("Tổng học sinh", "group", ACCENT);
        StatCard cPaid = new StatCard("Học phí đã thu", "wallet", GREEN);
        StatCard cPend = new StatCard("Chờ xác nhận", "clock", AMBER);
        StatCard cMine = new StatCard("Lớp tôi phụ trách", "book", BLUE);
        JPanel stats = new JPanel(new GridLayout(1, 4, 0, 0));
        stats.setOpaque(false);
        stats.add(cStu);
        stats.add(cPaid);
        stats.add(cPend);
        stats.add(cMine);
        c.gridy = 1;
        body.add(stats, c);

        BarChart chart = new BarChart();
        FlatButton btnReport = new FlatButton("Xuất báo cáo", "list", GREEN, true);
        btnReport.setEnabled(canPayroll());
        if (!canPayroll()) btnReport.setToolTipText("Chỉ Kế toán / Quản lý được xuất báo cáo");
        btnReport.addActionListener(e -> exportCenter("hoadon"));   // hoặc exportReport() nếu muốn báo cáo tài chính cũ
        Card cardChart = new Card("Doanh thu 6 tháng gần nhất", "Tính theo hóa đơn đã nộp", btnReport);
        cardChart.add(chart, BorderLayout.CENTER);
        cardChart.setPreferredSize(new Dimension(100, 390));

        DataGrid pending = new DataGrid(new String[]{"Mã HĐ", "Học sinh", "Khoản thu", "Ngày lập"},
                "Không có hóa đơn nào đang chờ 🎉", cols(), true);
        pending.load(INVOICE_SELECT + " WHERE h.trang_thai = 'Chưa nộp' ORDER BY h.ngay_lap DESC LIMIT 5",
                rs -> new Object[]{rs.getString(1), rs.getString(2), new Money(rs.getLong(4)), when(rs, 5, false)});
        FlatButton go = new FlatButton("Xử lý ngay", "check", ACCENT, true);
        go.addActionListener(e -> navigate("HoaDon", true));
        Card cardPending = new Card("Hóa đơn chờ xác nhận", "5 hóa đơn mới nhất chưa nộp", go);
        cardPending.add(pending, BorderLayout.CENTER);
        cardPending.setPreferredSize(new Dimension(100, 390));

        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0.44;
        body.add(cardChart, c);
        c.gridx = 1;
        c.weightx = 0.56;
        body.add(cardPending, c);

        DataGrid sched = new DataGrid(new String[]{"Ngày", "Lớp", "Giáo viên", "Phòng", "Trạng thái"},
                "Chưa có lịch học sắp tới", cols(4), true);
        sched.load("SELECT lh.ngay_day, COALESCE(l.ten_lop, lh.id_lop), COALESCE(gv.ho_ten, lh.id_giao_vien), "
                        + "lh.phong_hoc, lh.trang_thai_buoi FROM Lich_hoc lh "
                        + "LEFT JOIN Lop l ON lh.id_lop = l.id_lop "
                        + "LEFT JOIN Giao_vien gv ON lh.id_giao_vien = gv.id_giao_vien "
                        + "WHERE lh.ngay_day >= CURDATE() ORDER BY lh.ngay_day LIMIT 5",
                rs -> new Object[]{when(rs, 1, true), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
        Card cardSched = new Card("Lịch học sắp tới", "5 buổi gần nhất kể từ hôm nay", null);
        cardSched.add(sched, BorderLayout.CENTER);
        cardSched.setPreferredSize(new Dimension(100, 370));
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1;
        body.add(cardSched, c);

        // dữ liệu thống kê (nền)
        async(() -> new long[]{
                Db.scalar("SELECT COUNT(*) FROM Hoc_sinh"),
                Db.scalar("SELECT COALESCE(SUM(khoan_thu), 0) FROM hoa_don WHERE trang_thai = ?", "Đã nộp"),
                Db.scalar("SELECT COUNT(*) FROM hoa_don WHERE trang_thai = ?", "Đã nộp"),
                Db.scalar("SELECT COALESCE(SUM(khoan_thu), 0) FROM hoa_don WHERE trang_thai = ?", "Chưa nộp"),
                Db.scalar("SELECT COUNT(*) FROM hoa_don WHERE trang_thai = ?", "Chưa nộp"),
                Db.scalar("SELECT COUNT(*) FROM Lop WHERE ma_nhan_vien = ?", maNhanVien),
                Db.scalar("SELECT COUNT(*) FROM Lop")
        }, v -> {
            cStu.setValue(v[0], n -> n + " học sinh", "Đang quản lý trong hệ thống");
            cPaid.setValue(v[1], StaffDashboardUI::vnd, v[2] + " hóa đơn đã nộp");
            cPend.setValue(v[3], StaffDashboardUI::vnd, v[4] + " hóa đơn chưa nộp");
            cMine.setValue(v[5], n -> n + " lớp", "Tổng toàn trung tâm: " + v[6]);
        });

        // biểu đồ doanh thu
        YearMonth first = YearMonth.now().minusMonths(5);
        async(() -> Db.query("SELECT DATE_FORMAT(COALESCE(ngay_nop, ngay_lap), '%Y-%m'), SUM(khoan_thu) FROM hoa_don "
                        + "WHERE trang_thai = ? AND COALESCE(ngay_nop, ngay_lap) >= ? GROUP BY 1",
                rs -> new Object[]{rs.getString(1), rs.getLong(2)}, "Đã nộp", Timestamp.valueOf(first.atDay(1).atStartOfDay())),
                rows -> {
                    Map<String, Long> m = new HashMap<>();
                    for (Object[] r : rows) m.put((String) r[0], (Long) r[1]);
                    String[] lb = new String[6];
                    long[] vals = new long[6];
                    for (int i = 0; i < 6; i++) {
                        YearMonth ym = first.plusMonths(i);
                        lb[i] = "T" + ym.getMonthValue();
                        vals[i] = m.getOrDefault(ym.toString(), 0L);
                    }
                    chart.setData(lb, vals);
                });

        JScrollPane sp = new JScrollPane(body);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return pad(sp);
    }

    /* ============================ THANH CÔNG CỤ DÙNG CHUNG ============================ */

    private JPanel toolbar(DataGrid g, String[] statuses, int statusCol, String placeholder) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        p.setOpaque(false);
        StyledField s = new StyledField("").withPlaceholder(placeholder, "search");
        s.setPreferredSize(new Dimension(270, 42));
        s.getDocument().addDocumentListener(doc(() -> g.setText(s.getText())));
        if (statuses != null) {
            p.add(new Segmented(statuses, i -> g.setStatus(i == 0 ? null : statuses[i], statusCol)));
        }
        p.add(s);
        return p;
    }
    /** Thanh sắp xếp + lọc theo cột, đặt phía trên bảng */
    private JPanel sortBar(DataGrid g, String[] sortLabels, int[] sortCols, boolean[] sortAsc,
                           String[] fLabels, int[] fCols, String[][] fValues) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);

        String[] sl = new String[sortLabels.length + 1];
        sl[0] = "Mặc định";
        System.arraycopy(sortLabels, 0, sl, 1, sortLabels.length);
        RoundCombo cbSort = new RoundCombo(sl);
        cbSort.setPreferredSize(new Dimension(220, 42));
        cbSort.addActionListener(e -> {
            int i = cbSort.getSelectedIndex();
            if (i <= 0) g.sortBy(-1, true); else g.sortBy(sortCols[i - 1], sortAsc[i - 1]);
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
            cb.setPreferredSize(new Dimension(200, 42));
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

    /** Ghép thanh sắp xếp/lọc lên trên bảng */
    private static JComponent withBar(JComponent bar, JComponent grid) {
        JPanel mid = new JPanel(new BorderLayout(0, 12));
        mid.setOpaque(false);
        mid.add(bar, BorderLayout.NORTH);
        mid.add(grid, BorderLayout.CENTER);
        return mid;
    }
    private JComponent listPage(String title, String subtitle, String[] cols, String sql, RowMapper map,
                                String empty, Set<Integer> pills, String[] statuses, int statusCol) {
        DataGrid g = new DataGrid(cols, empty, pills, false);
        g.load(sql, map);
        Card card = new Card(title, subtitle, toolbar(g, statuses, statusCol, "Tìm kiếm trong danh sách..."));
        card.add(g, BorderLayout.CENTER);
        return pad(card);
    }

    /* ============================ HÓA ĐƠN HỌC PHÍ ============================ */

    // ma_ghi_danh có dạng "SV01:KH01" (mã học sinh : mã khóa học)
    private static final String INVOICE_SELECT =
            "SELECT h.ma_hoa_don, COALESCE(hs.ho_ten, h.ma_ghi_danh), COALESCE(k.ten_khoa_hoc, '—'), "
                    + "h.khoan_thu, h.ngay_lap, h.hinh_thuc, h.ngay_nop, COALESCE(h.trang_thai, 'Chưa nộp') FROM hoa_don h "
                    + "LEFT JOIN Hoc_sinh hs ON hs.ma_sinh_vien = SUBSTRING_INDEX(h.ma_ghi_danh, ':', 1) "
                    + "LEFT JOIN Khoa_hoc k ON k.ma_khoa_hoc = SUBSTRING_INDEX(h.ma_ghi_danh, ':', -1)";

    private JComponent createInvoicePage() {
        DataGrid g = new DataGrid(new String[]{"Mã Hóa Đơn", "Học Sinh", "Khóa Học", "Khoản Thu", "Ngày Lập",
        		"Hình Thức", "Ngày Nộp", "Trạng Thái"}, "Chưa có hóa đơn nào", cols(7), false);
        g.load(INVOICE_SELECT + " ORDER BY h.ngay_lap DESC",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), new Money(rs.getLong(4)),
                		when(rs, 5, true), payMethod(rs.getString(6)), when(rs, 7, true), rs.getString(8)});
        FlatButton btnPaid = new FlatButton("Xác nhận đã nộp", "check", GREEN, false);
        FlatButton btnUndo = new FlatButton("Hoàn tác (chưa nộp)", "undo", AMBER, true);
        if (!canRevert()) btnUndo.setToolTipText("Chỉ Kế toán / Quản lý được hoàn tác thanh toán");
        JLabel hint = lbl("Chọn một hóa đơn trong danh sách để xử lý.", font(Font.PLAIN, 13), MUTED);

        Runnable sync = () -> {
            int r = g.selectedModelRow();
            if (r < 0) {
                hint.setText("Chọn một hóa đơn trong danh sách để xử lý.");
                btnPaid.setEnabled(false);
                btnUndo.setEnabled(false);
                return;
            }
            String st = String.valueOf(g.value(r, 7));
            hint.setText("Đang chọn:  " + g.value(r, 0) + "   •   " + g.value(r, 1) + "   •   " + g.value(r, 3));
            btnPaid.setEnabled(canConfirm() && "Chưa nộp".equals(st));
            btnUndo.setEnabled(canRevert() && "Đã nộp".equals(st));
        };
        g.table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) sync.run(); });
        g.setOnLoaded(sync);
        sync.run();
        btnPaid.addActionListener(e -> changeInvoice(g, true));
        btnUndo.addActionListener(e -> changeInvoice(g, false));

        JPanel foot = new JPanel(new BorderLayout(16, 0));
        foot.setOpaque(false);
        foot.add(hint, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.add(btnUndo);
        btns.add(btnPaid);
        foot.add(btns, BorderLayout.EAST);

        Card card = new Card("Tất cả hóa đơn học phí", "Lọc theo trạng thái hoặc tìm theo mã, tên học sinh, khóa học",
                toolbar(g, new String[]{"Tất cả", "Chưa nộp", "Đã nộp"}, 7, "Tìm hóa đơn..."));
        card.add(g, BorderLayout.CENTER);
        card.add(foot, BorderLayout.SOUTH);
        return pad(card);
    }

    // Gán ngay_lap = ngay_lap để MySQL (cấu hình cũ) không tự đổi cột TIMESTAMP đầu tiên thành thời điểm hiện tại.
    private static final String SQL_PAID = "UPDATE hoa_don SET trang_thai = 'Đã nộp', ngay_nop = NOW(), nv_xac_nhan = ?, "
            + "ngay_lap = ngay_lap WHERE ma_hoa_don = ? AND COALESCE(trang_thai, 'Chưa nộp') = 'Chưa nộp'";
    private static final String SQL_UNPAID = "UPDATE hoa_don SET trang_thai = 'Chưa nộp', ngay_nop = NULL, nv_xac_nhan = NULL, "
            + "ngay_lap = ngay_lap WHERE ma_hoa_don = ? AND trang_thai = 'Đã nộp'";

    private void changeInvoice(DataGrid g, boolean markPaid) {
        int r = g.selectedModelRow();
        if (r < 0) { toast("Hãy chọn một hóa đơn trước.", T_WARN); return; }
        if (markPaid && !canConfirm()) { toast("Bạn không có quyền xác nhận hóa đơn.", T_ERR); return; }
        if (!markPaid && !canRevert()) { toast("Chỉ Kế toán / Quản lý được hoàn tác thanh toán.", T_ERR); return; }
        String ma = String.valueOf(g.value(r, 0));
        String hs = String.valueOf(g.value(r, 1));
        String tien = String.valueOf(g.value(r, 3));
        String cur = String.valueOf(g.value(r, 7));
        String target = markPaid ? "Đã nộp" : "Chưa nộp";
        if (cur.equals(target)) { toast("Hóa đơn " + ma + " đã ở trạng thái \"" + cur + "\" rồi.", T_INFO); return; }
        String html = "Chuyển hóa đơn <b>" + ma + "</b> sang <b>" + target + "</b>?<br><br>"
                + "Học sinh: <b>" + hs + "</b><br>Khoản thu: <b>" + tien + "</b>"
                + (markPaid ? "" : "<br><br><span style='color:#b45309'>Thao tác này xóa thông tin xác nhận thanh toán trước đó.</span>");
        if (!confirm(markPaid ? "Xác nhận thu học phí" : "Hoàn tác thanh toán", html,
                markPaid ? "Xác nhận đã nộp" : "Hoàn tác", markPaid ? GREEN : AMBER)) return;
        async(() -> markPaid ? Db.update(SQL_PAID, maNhanVien, ma) : Db.update(SQL_UNPAID, ma), n -> {
            if (n > 0) toast(markPaid ? "Đã xác nhận hóa đơn " + ma + " đã nộp." : "Đã hoàn tác hóa đơn " + ma + ".", T_OK);
            else toast("Hóa đơn " + ma + " vừa được người khác xử lý. Danh sách đã được làm mới.", T_WARN);
            g.reload();
            refreshBadge();
        });
    }

    /* ============================ DANH SÁCH ============================ */

    private JComponent createStudentPage() {
        DataGrid g = new DataGrid(new String[]{"Mã SV", "Họ Tên", "Ngày Sinh", "Trình Độ", "Số Điện Thoại", "GV Phụ Trách"},
                "Chưa có học sinh nào", cols(3), false);
        g.load("SELECT hs.ma_sinh_vien, hs.ho_ten, hs.ngay_sinh, hs.trinh_do, hs.so_dien_thoai, "
                        + "COALESCE(gv.ho_ten, 'Chưa phân công') FROM Hoc_sinh hs "
                        + "LEFT JOIN Giao_vien gv ON hs.id_giao_vien = gv.id_giao_vien ORDER BY hs.ma_sinh_vien",
                rs -> new Object[]{rs.getString(1), rs.getString(2), when(rs, 3, false),
                        rs.getString(4), rs.getString(5), rs.getString(6)});

        FlatButton btnAdd = new FlatButton("Thêm học sinh", "plus", ACCENT, false);
        btnAdd.setEnabled(valid);
        btnAdd.addActionListener(e -> addStudent(g));
        FlatButton btnDel = new FlatButton("Xóa học sinh", null, RED, true);
        btnDel.setEnabled(false);
        g.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) btnDel.setEnabled(valid && g.selectedModelRow() >= 0);
        });
        btnDel.addActionListener(e -> deleteStudent(g));

        JPanel tb = toolbar(g, null, -1, "Tìm học sinh...");
        tb.add(btnAdd);   
        tb.add(btnDel);// toolbar() trả về JPanel nên add thêm nút được
        Card card = new Card("Danh sách học sinh", "Tất cả học sinh trong trung tâm", tb);
        JPanel bar = sortBar(g,
                new String[]{"Tên: A → Z", "Tên: Z → A", "Ngày sinh: lớn tuổi trước", "Ngày sinh: nhỏ tuổi trước",
                        "Trình độ: N1 → N5", "Trình độ: N5 → N1", "Mã SV: A → Z"},
                new int[]{1, 1, 2, 2, 3, 3, 0},
                new boolean[]{true, false, true, false, false, true, true},
                new String[]{"Trình độ"}, new int[]{3},
                new String[][]{{"N5", "N4", "N3", "N2", "N1", "Chưa xác định"}});
        card.add(withBar(bar, g), BorderLayout.CENTER);        return pad(card);
    }
    private void addStudent(DataGrid g) {
        if (!valid) { toast("Bạn không có quyền thêm học sinh.", T_ERR); return; }
        List<Object[]> gv;
        String nextMa;
        try {
            gv = Db.query("SELECT id_giao_vien, ho_ten FROM Giao_vien ORDER BY id_giao_vien",
                    rs -> new Object[]{rs.getString(1), nz(rs.getString(2))});
            long n = Db.scalar("SELECT COALESCE(MAX(CAST(SUBSTRING(ma_sinh_vien, 3) AS UNSIGNED)), 0) "
                    + "FROM Hoc_sinh WHERE ma_sinh_vien REGEXP '^SV[0-9]+$'");
            nextMa = String.format("SV%02d", n + 1);
        } catch (SQLException e) { toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR); return; }
        if (gv.isEmpty()) { toast("Hãy thêm giáo viên trước (học sinh bắt buộc có GV phụ trách).", T_WARN); return; }

        String[] items = new String[gv.size()];
        for (int i = 0; i < items.length; i++) items[i] = gv.get(i)[0] + " - " + gv.get(i)[1];

        StyledField tfMa = new StyledField(nextMa);
        StyledField tfName = new StyledField("");
        StyledField tfDob = new StyledField("").withPlaceholder("VD: 15/03/2008", null);
        RoundCombo cbLevel = new RoundCombo(new String[]{"Chưa xác định", "N5", "N4", "N3", "N2", "N1"});
        StyledField tfPhone = new StyledField("");
        RoundCombo cbGv = new RoundCombo(items);
        JPanel form = vform(
                new String[]{"Mã sinh viên", "Họ tên", "Ngày sinh (dd/MM/yyyy)", "Trình độ", "Số điện thoại", "Giáo viên phụ trách"},
                new JComponent[]{tfMa, tfName, tfDob, cbLevel, tfPhone, cbGv});

        boolean ok = dialog("Thêm học sinh", "Nhập thông tin học sinh mới", form, "Thêm", ACCENT, () -> {
            String ma = tfMa.getText().trim(), name = tfName.getText().trim(), phone = tfPhone.getText().trim();
            if (ma.isEmpty() || name.isEmpty() || phone.isEmpty()) return "Hãy nhập đủ mã, họ tên và số điện thoại.";
            if (!phone.matches("0\\d{9,10}")) return "Số điện thoại không hợp lệ (VD: 0912345678).";
            LocalDate dob;
            try { dob = LocalDate.parse(tfDob.getText().trim(), IN_FMT); }
            catch (DateTimeParseException ex) { return "Ngày sinh phải đúng dạng dd/MM/yyyy."; }
            if (dob.isAfter(LocalDate.now()) || dob.getYear() < 1970) return "Ngày sinh không hợp lệ.";
            try {
                if (Db.scalar("SELECT COUNT(*) FROM Hoc_sinh WHERE ma_sinh_vien = ?", ma) > 0)
                    return "Mã " + ma + " đã tồn tại.";
                Db.update("INSERT INTO Hoc_sinh (ma_sinh_vien, id_giao_vien, ho_ten, ngay_sinh, trinh_do, so_dien_thoai) "
                                + "VALUES (?, ?, ?, ?, ?, ?)",
                        ma, gv.get(cbGv.getSelectedIndex())[0], name,
                        Timestamp.valueOf(dob.atStartOfDay()), cbLevel.getSelectedItem(), phone);
                return null;
            } catch (SQLException ex) { return "Lỗi CSDL: " + rootMsg(ex); }
        }, true);
        if (ok) { toast("Đã thêm học sinh " + tfMa.getText().trim() + ".", T_OK); g.reload(); }
    }
    private void deleteStudent(DataGrid g) {
        int r = g.selectedModelRow();
        if (r < 0) { toast("Hãy chọn một học sinh trước.", T_WARN); return; }
        if (!valid) { toast("Bạn không có quyền xóa học sinh.", T_ERR); return; }
        String ma = String.valueOf(g.value(r, 0)), ten = String.valueOf(g.value(r, 1));
        try {
            long hd = Db.scalar("SELECT COUNT(*) FROM hoa_don WHERE SUBSTRING_INDEX(ma_ghi_danh, ':', 1) = ?", ma);
            long kt = Db.scalar("SELECT COUNT(*) FROM Ket_qua_thi WHERE ma_sinh_vien = ?", ma);
            long dg = Db.scalar("SELECT COUNT(*) FROM Danh_gia_va_phan_hoi WHERE ma_sinh_vien = ?", ma);
            if (hd + kt + dg > 0) {
                toast("Không xóa được: còn " + hd + " hóa đơn, " + kt + " kết quả thi, " + dg + " đánh giá.", T_ERR);
                return;
            }
            if (!confirm("Xóa học sinh",
                    "Xóa học sinh <b>" + ten + "</b> (" + ma + ")?<br><br>"
                            + "<span style='color:#b91c1c'>Học sinh sẽ bị gỡ khỏi mọi lớp. Không thể hoàn tác.</span>",
                    "Xóa", RED)) return;
            List<Object[]> lops = Db.query("SELECT id_lop FROM Hoc_sinh_lop WHERE ma_sinh_vien = ?",
                    rs -> new Object[]{rs.getString(1)}, ma);
            Db.update("DELETE FROM Hoc_sinh_lop WHERE ma_sinh_vien = ?", ma);
            Db.update("DELETE FROM Hoc_sinh WHERE ma_sinh_vien = ?", ma);
            for (Object[] l : lops)
                Db.update("UPDATE Lop SET si_so = (SELECT COUNT(*) FROM Hoc_sinh_lop WHERE id_lop = ?) WHERE id_lop = ?",
                        l[0], l[0]);
            toast("Đã xóa học sinh " + ma + ".", T_OK);
            g.reload();
        } catch (SQLException e) {
            toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR);
        }
    }
    private JComponent createClassPage() {
        DataGrid g = new DataGrid(new String[]{"Mã Lớp", "Tên Lớp", "Khóa Học", "Trình Độ", "Niên Khóa", "Sĩ Số", "NV Phụ Trách"},
                "Chưa có lớp học nào", cols(3), false);
        g.load("SELECT l.id_lop, l.ten_lop, k.ten_khoa_hoc, k.trinh_do, l.nien_khoa, l.si_so, "
                        + "COALESCE(nv.ho_ten, l.ma_nhan_vien) FROM Lop l "
                        + "LEFT JOIN Khoa_hoc k ON l.ma_khoa_hoc = k.ma_khoa_hoc "
                        + "LEFT JOIN Nhan_vien nv ON l.ma_nhan_vien = nv.ma_nhan_vien ORDER BY l.id_lop",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), intObj(rs, 6), rs.getString(7)});

        FlatButton btnAdd = new FlatButton("Thêm lớp", "plus", ACCENT, false);
        FlatButton btnAddStu = new FlatButton("Thêm học sinh vào lớp", "group", GREEN, true);
        btnAdd.setEnabled(valid);
        btnAddStu.setEnabled(false);
        g.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) btnAddStu.setEnabled(valid && g.selectedModelRow() >= 0);
        });
        btnAdd.addActionListener(e -> addClass(g));
        btnAddStu.addActionListener(e -> addStudentToClass(g));
        FlatButton btnDel = new FlatButton("Xóa lớp", null, RED, true);
        btnDel.setEnabled(false);
        g.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) btnDel.setEnabled(valid && g.selectedModelRow() >= 0);
        });
        btnDel.addActionListener(e -> deleteClass(g));
        JPanel tb = toolbar(g, null, -1, "Tìm lớp...");
        tb.add(btnAddStu);
        tb.add(btnAdd);tb.add(btnDel);
        Card card = new Card("Danh sách lớp học", "Chọn một lớp rồi bấm \"Thêm học sinh vào lớp\"", tb);
        JPanel bar = sortBar(g,
                new String[]{"Tên lớp: A → Z", "Sĩ số: đông → ít", "Sĩ số: ít → đông",
                        "Trình độ: N1 → N5", "Trình độ: N5 → N1", "Niên khóa: mới nhất trước"},
                new int[]{1, 5, 5, 3, 3, 4},
                new boolean[]{true, false, true, false, true, false},
                new String[]{"Trình độ", "Niên khóa"}, new int[]{3, 4},
                new String[][]{{"N5", "N4", "N3", "N2", "N1"}, {"2025-2026", "2026-2027"}});
        card.add(withBar(bar, g), BorderLayout.CENTER);        return pad(card);
    }
    /** Mã lớp kế tiếp: L01, L02, ... */
    private static String nextClassId() throws SQLException {
        long n = Db.scalar("SELECT COALESCE(MAX(CAST(SUBSTRING(id_lop, 2) AS UNSIGNED)), 0) "
                + "FROM Lop WHERE id_lop REGEXP '^L[0-9]+$'");
        return String.format("L%02d", n + 1);
    }

    /** Niên khóa theo ngày hiện tại (năm học bắt đầu từ tháng 9) */
    private static String currentSchoolYear() {
        LocalDate d = LocalDate.now();
        int y = d.getMonthValue() >= 9 ? d.getYear() : d.getYear() - 1;
        return y + "-" + (y + 1);
    }

    private void addClass(DataGrid g) {
        if (!valid) { toast("Bạn không có quyền thêm lớp.", T_ERR); return; }
        List<Object[]> kh, nv;
        try {
            kh = Db.query("SELECT ma_khoa_hoc, ten_khoa_hoc, trinh_do, chuyen_de FROM Khoa_hoc "
                            + "ORDER BY trinh_do DESC, ma_khoa_hoc",
                    rs -> new Object[]{rs.getString(1), nz(rs.getString(2)), nz(rs.getString(3)), nz(rs.getString(4))});
            nv = Db.query("SELECT ma_nhan_vien, ho_ten FROM Nhan_vien ORDER BY ma_nhan_vien",
                    rs -> new Object[]{rs.getString(1), nz(rs.getString(2))});
        } catch (SQLException e) { toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR); return; }
        if (kh.isEmpty()) { toast("Chưa có khóa học nào để mở lớp.", T_WARN); return; }

        String[] khItems = new String[kh.size()], nvItems = new String[nv.size()];
        for (int i = 0; i < khItems.length; i++) khItems[i] = kh.get(i)[0] + " - " + kh.get(i)[1];
        for (int i = 0; i < nvItems.length; i++) nvItems[i] = nv.get(i)[0] + " - " + nv.get(i)[1];

        final String year = currentSchoolYear();
        StyledField tfId = new StyledField("");
        StyledField tfName = new StyledField("");
        StyledField tfYear = new StyledField(year);
        for (StyledField f : new StyledField[]{tfId, tfName, tfYear}) {
            f.setEditable(false);
            f.setForeground(ACCENT_D);
            f.setFocusable(false);
        }
        RoundCombo cbKh = new RoundCombo(khItems);
        RoundCombo cbNv = new RoundCombo(nvItems);
        for (int i = 0; i < nv.size(); i++) if (nv.get(i)[0].equals(maNhanVien)) cbNv.setSelectedIndex(i);

        // Tự sinh mã lớp + tên lớp theo khóa học đang chọn
        Runnable refresh = () -> {
            try {
                tfId.setText(nextClassId());
                Object[] k = kh.get(cbKh.getSelectedIndex());
                long n = Db.scalar("SELECT COUNT(*) FROM Lop WHERE ma_khoa_hoc = ? AND nien_khoa = ?", k[0], year);
                String topic = ((String) k[3]).isEmpty() ? (String) k[1] : (String) k[3];
                tfName.setText(k[2] + " - " + topic + " - Lớp " + String.format("%02d", n + 1));
            } catch (SQLException ex) {
                tfName.setText("");
            }
        };
        cbKh.addActionListener(e -> refresh.run());
        refresh.run();

        JPanel form = vform(new String[]{"Khóa học", "Mã lớp (tự động)", "Niên khóa (tự động)", "Tên lớp (tự động)", "Nhân viên phụ trách"},
                new JComponent[]{cbKh, tfId, tfYear, tfName, cbNv});

        String[] created = {null};
        boolean ok = dialog("Thêm lớp học", "Mã lớp, niên khóa và tên lớp được tạo tự động theo khóa học",
                form, "Tạo lớp", ACCENT, () -> {
            try {
                refresh.run();   // tính lại lúc bấm, tránh trùng khi có người khác vừa tạo lớp
                String id = tfId.getText().trim(), name = tfName.getText().trim();
                if (id.isEmpty() || name.isEmpty()) return "Không tạo được mã lớp, hãy thử lại.";
                Db.update("INSERT INTO Lop (id_lop, ma_khoa_hoc, ma_nhan_vien, nien_khoa, ten_lop, si_so) VALUES (?, ?, ?, ?, ?, 0)",
                        id, kh.get(cbKh.getSelectedIndex())[0], nv.get(cbNv.getSelectedIndex())[0], year, name);
                created[0] = id;
                return null;
            } catch (SQLException ex) { return "Lỗi CSDL: " + rootMsg(ex); }
        }, true);
        if (ok && created[0] != null) { toast("Đã tạo lớp " + created[0] + ".", T_OK); g.reload(); }
    }
        
    private void addStudentToClass(DataGrid g) {
        int r = g.selectedModelRow();
        if (r < 0) { toast("Hãy chọn một lớp trước.", T_WARN); return; }
        String idLop = String.valueOf(g.value(r, 0)), tenLop = String.valueOf(g.value(r, 1));
        List<Object[]> hs;
        try {
            hs = Db.query("SELECT ma_sinh_vien, ho_ten FROM Hoc_sinh WHERE ma_sinh_vien NOT IN "
                            + "(SELECT ma_sinh_vien FROM Hoc_sinh_lop WHERE id_lop = ?) ORDER BY ma_sinh_vien",
                    rs -> new Object[]{rs.getString(1), nz(rs.getString(2))}, idLop);
        } catch (SQLException e) { toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR); return; }
        if (hs.isEmpty()) { toast("Không còn học sinh nào để thêm vào lớp này.", T_INFO); return; }

        String[] items = new String[hs.size()];
        for (int i = 0; i < items.length; i++) items[i] = hs.get(i)[0] + " - " + hs.get(i)[1];
        RoundCombo cb = new RoundCombo(items);
        JPanel form = vform(new String[]{"Chọn học sinh"}, new JComponent[]{cb});

        boolean ok = dialog("Thêm học sinh vào lớp", tenLop + " (" + idLop + ")", form, "Thêm vào lớp", GREEN, () -> {
            String ma = String.valueOf(hs.get(cb.getSelectedIndex())[0]);
            try {
                Db.update("INSERT INTO Hoc_sinh_lop (id_lop, ma_sinh_vien) VALUES (?, ?)", idLop, ma);
                Db.update("UPDATE Lop SET si_so = (SELECT COUNT(*) FROM Hoc_sinh_lop WHERE id_lop = ?) WHERE id_lop = ?",
                        idLop, idLop);
                return null;
            } catch (SQLException ex) {
                return ex.getErrorCode() == 1062 ? "Học sinh này đã ở trong lớp rồi." : "Lỗi CSDL: " + rootMsg(ex);
            }
        }, true);
        if (ok) { toast("Đã thêm học sinh vào lớp " + idLop + ".", T_OK); g.reload(); }
    }
    private void deleteClass(DataGrid g) {
        int r = g.selectedModelRow();
        if (r < 0) { toast("Hãy chọn một lớp trước.", T_WARN); return; }
        if (!valid) { toast("Bạn không có quyền xóa lớp.", T_ERR); return; }
        String id = String.valueOf(g.value(r, 0)), ten = String.valueOf(g.value(r, 1));
        try {
            long lh = Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_lop = ?", id);
            long dg = Db.scalar("SELECT COUNT(*) FROM Danh_gia_va_phan_hoi WHERE id_lop = ?", id);
            long ql = Db.scalar("SELECT COUNT(*) FROM Quanli WHERE id_lop = ?", id);
            if (lh + dg + ql > 0) {
                toast("Không xóa được: còn " + lh + " buổi học, " + dg + " đánh giá, " + ql + " phân công GV.", T_ERR);
                return;
            }
            long hs = Db.scalar("SELECT COUNT(*) FROM Hoc_sinh_lop WHERE id_lop = ?", id);
            if (!confirm("Xóa lớp học",
                    "Xóa lớp <b>" + ten + "</b> (" + id + ")?<br><br>"
                            + (hs > 0 ? "Có <b>" + hs + "</b> học sinh sẽ bị gỡ khỏi lớp (học sinh vẫn còn trong hệ thống).<br><br>" : "")
                            + "<span style='color:#b91c1c'>Không thể hoàn tác.</span>",
                    "Xóa lớp", RED)) return;
            Db.update("DELETE FROM Hoc_sinh_lop WHERE id_lop = ?", id);
            Db.update("DELETE FROM Lop WHERE id_lop = ?", id);
            toast("Đã xóa lớp " + id + ".", T_OK);
            g.reload();
        } catch (SQLException e) {
            toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR);
        }
    }
    private JComponent createCoursePage() {
        DataGrid g = new DataGrid(new String[]{"Mã Khóa", "Tên Khóa Học", "Trình Độ", "Chuyên Đề", "Giáo Trình", "Học Phí", "Số Lớp"},
                "Chưa có khóa học nào", cols(2), false);
        g.load("SELECT k.ma_khoa_hoc, k.ten_khoa_hoc, k.trinh_do, k.chuyen_de, k.giao_trinh, k.hoc_phi, "
                        + "(SELECT COUNT(*) FROM Lop l WHERE l.ma_khoa_hoc = k.ma_khoa_hoc) "
                        + "FROM Khoa_hoc k ORDER BY k.trinh_do DESC, k.ma_khoa_hoc",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), new Money(rs.getLong(6)), intObj(rs, 7)});
        JPanel bar = sortBar(g,
                new String[]{"Học phí: cao → thấp", "Học phí: thấp → cao", "Số lớp: nhiều → ít",
                        "Tên: A → Z", "Trình độ: N1 → N5", "Trình độ: N5 → N1"},
                new int[]{5, 5, 6, 1, 2, 2},
                new boolean[]{false, true, false, true, true, false},
                new String[]{"Trình độ"}, new int[]{2},
                new String[][]{{"N5", "N4", "N3", "N2", "N1"}});
        Card card = new Card("Danh sách khóa học", "Chương trình đào tạo và học phí",
                toolbar(g, null, -1, "Tìm khóa học..."));
        card.add(withBar(bar, g), BorderLayout.CENTER);
        return pad(card);
    }

    private JComponent createSchedulePage() {
        // Buổi sắp tới lên trước (gần nhất trước), buổi đã qua xếp sau (mới nhất trước)
        return listPage("Lịch học toàn trung tâm", "Buổi sắp tới được xếp lên đầu",
                new String[]{"Mã Lịch", "Lớp", "Giáo Viên", "Ngày Dạy", "Ca Học", "Phòng", "Nội Dung", "Trạng Thái"},
                "SELECT lh.ma_lich_hoc, COALESCE(l.ten_lop, lh.id_lop), COALESCE(gv.ho_ten, lh.id_giao_vien), "
                        + "lh.ngay_day, lh.ca_hoc, lh.phong_hoc, lh.noi_dung, lh.trang_thai_buoi FROM Lich_hoc lh "
                        + "LEFT JOIN Lop l ON lh.id_lop = l.id_lop "
                        + "LEFT JOIN Giao_vien gv ON lh.id_giao_vien = gv.id_giao_vien "
                        + "ORDER BY (lh.ngay_day < CURDATE()), IF(lh.ngay_day >= CURDATE(), lh.ngay_day, NULL), lh.ngay_day DESC",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), when(rs, 4, true),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)},
                "Chưa có lịch học nào", cols(7), new String[]{"Tất cả", "Dạy", "Nghỉ"}, 7);
    }

    /* ============================ PHIẾU LƯƠNG ============================ */

    private JComponent createPayslipPage() {
        StatCard sMonth = new StatCard("Đã chi tháng này", "wallet", ACCENT);
        StatCard sCount = new StatCard("Phiếu lập tháng này", "list", BLUE);
        StatCard sAll = new StatCard("Tổng số phiếu", "award", GREEN);
        JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
        stats.setOpaque(false);
        stats.add(sMonth);
        stats.add(sCount);
        stats.add(sAll);
        LocalDate now = LocalDate.now();
        Timestamp m1 = Timestamp.valueOf(now.withDayOfMonth(1).atStartOfDay());
        Timestamp m2 = Timestamp.valueOf(now.withDayOfMonth(1).plusMonths(1).atStartOfDay());
        async(() -> new long[]{
                Db.scalar("SELECT COALESCE(SUM(tong_luong), 0) FROM Phieu_luong WHERE ngay_tra >= ? AND ngay_tra < ?", m1, m2),
                Db.scalar("SELECT COUNT(*) FROM Phieu_luong WHERE ngay_tra >= ? AND ngay_tra < ?", m1, m2),
                Db.scalar("SELECT COUNT(*) FROM Phieu_luong")
        }, v -> {
            sMonth.setValue(v[0], StaffDashboardUI::vnd, "Theo ngày trả lương");
            sCount.setValue(v[1], n -> n + " phiếu", "Tháng " + now.getMonthValue() + "/" + now.getYear());
            sAll.setValue(v[2], n -> n + " phiếu", "Toàn bộ lịch sử");
        });

        DataGrid g = new DataGrid(new String[]{"Mã Phiếu", "Giáo Viên", "Từ Ngày", "Đến Ngày", "Ngày Trả", "Tổng Lương", "Người Lập"},
                "Chưa có phiếu lương nào", cols(), false);
        g.load("SELECT p.ma_phieu_luong, COALESCE(gv.ho_ten, p.id_giao_vien), p.tu_ngay, p.den_ngay, p.ngay_tra, "
                        + "p.tong_luong, COALESCE(nv.ho_ten, p.ma_nhan_vien) FROM Phieu_luong p "
                        + "LEFT JOIN Giao_vien gv ON p.id_giao_vien = gv.id_giao_vien "
                        + "LEFT JOIN Nhan_vien nv ON p.ma_nhan_vien = nv.ma_nhan_vien ORDER BY p.ngay_tra DESC, p.ma_phieu_luong DESC",
                rs -> new Object[]{rs.getString(1), rs.getString(2), when(rs, 3, false), when(rs, 4, false),
                        when(rs, 5, false), new Money(rs.getLong(6)), rs.getString(7)});

        FlatButton btnNew = new FlatButton("Lập phiếu lương", "plus", ACCENT, false);
        btnNew.setEnabled(canPayroll());
        if (!canPayroll()) btnNew.setToolTipText("Chỉ Kế toán / Quản lý được lập phiếu lương");
        btnNew.addActionListener(e -> createPayslip());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        StyledField s = new StyledField("").withPlaceholder("Tìm phiếu lương...", "search");
        s.setPreferredSize(new Dimension(250, 42));
        s.getDocument().addDocumentListener(doc(() -> g.setText(s.getText())));
        right.add(s);
        FlatButton btnReport = new FlatButton("Xuất báo cáo", "list", GREEN, true);
        btnReport.setEnabled(canPayroll());
        btnReport.addActionListener(e -> exportCenter("phieuluong"));
        right.add(btnReport);
        right.add(btnNew);
        right.add(btnNew);

        Card card = new Card("Lịch sử phiếu lương", canPayroll() ? "Mỗi giáo viên chỉ có một phiếu cho mỗi kỳ, không được chồng kỳ"
                : "Bạn chỉ có quyền xem - Kế toán / Quản lý mới được lập phiếu", right);
        card.add(g, BorderLayout.CENTER);

        JPanel col = new JPanel(new BorderLayout(0, 0));
        col.setOpaque(false);
        col.add(stats, BorderLayout.NORTH);
        col.add(card, BorderLayout.CENTER);
        return pad(col);
    }

    private void createPayslip() {
        if (!canPayroll()) { toast("Bạn không có quyền lập phiếu lương.", T_ERR); return; }
        List<Object[]> gv;
        try {
            gv = Db.query("SELECT id_giao_vien, ho_ten FROM Giao_vien ORDER BY id_giao_vien",
                    rs -> new Object[]{rs.getString(1), nz(rs.getString(2))});
        } catch (SQLException e) {
            toast("Lỗi cơ sở dữ liệu: " + rootMsg(e), T_ERR);
            return;
        }
        if (gv.isEmpty()) { toast("Chưa có giáo viên nào trong hệ thống.", T_WARN); return; }

        String[] items = new String[gv.size()];
        for (int i = 0; i < items.length; i++) items[i] = gv.get(i)[0] + " - " + gv.get(i)[1];
        RoundCombo cbGv = new RoundCombo(items);
        LocalDate now = LocalDate.now();
        StyledField tfFrom = new StyledField(now.withDayOfMonth(1).format(D_FMT));
        StyledField tfTo = new StyledField(now.withDayOfMonth(now.lengthOfMonth()).format(D_FMT));
        StyledField tfPay = new StyledField(now.format(D_FMT));
        StyledField tfTotal = new StyledField("").withPlaceholder("VD: 8500000", null);
        JLabel info = lbl(" ", font(Font.PLAIN, 12), MUTED);

        Runnable refreshInfo = () -> {
            try {
                LocalDate f = LocalDate.parse(tfFrom.getText().trim(), IN_FMT);
                LocalDate t = LocalDate.parse(tfTo.getText().trim(), IN_FMT);
                if (t.isBefore(f)) { info.setText(" "); return; }
                String idGv = String.valueOf(gv.get(cbGv.getSelectedIndex())[0]);
                long n = Db.scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = 'Dạy' "
                                + "AND ngay_day >= ? AND ngay_day < ?", idGv,
                        Timestamp.valueOf(f.atStartOfDay()), Timestamp.valueOf(t.plusDays(1).atStartOfDay()));
                info.setForeground(n == 0 ? AMBER : GREEN);
                info.setText(n == 0 ? "⚠ Giáo viên này không có buổi dạy nào trong kỳ (buổi nghỉ không tính)."
                        : "Số buổi dạy trong kỳ: " + n + " buổi (không tính buổi nghỉ) - dùng để đối chiếu tổng lương.");
            } catch (Exception ex) {
                info.setText(" ");
            }
        };
        cbGv.addActionListener(e -> refreshInfo.run());
        tfFrom.getDocument().addDocumentListener(doc(refreshInfo));
        tfTo.getDocument().addDocumentListener(doc(refreshInfo));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        form.add(lbl("Giáo viên", font(Font.BOLD, 12), MUTED), c);
        c.gridy = 1; c.insets = new Insets(0, 0, 14, 0);
        form.add(cbGv, c);
        c.gridwidth = 1; c.gridy = 2; c.insets = new Insets(0, 0, 4, 8);
        form.add(lbl("Từ ngày (dd/MM/yyyy)", font(Font.BOLD, 12), MUTED), c);
        c.gridx = 1; c.insets = new Insets(0, 8, 4, 0);
        form.add(lbl("Đến ngày (dd/MM/yyyy)", font(Font.BOLD, 12), MUTED), c);
        c.gridx = 0; c.gridy = 3; c.insets = new Insets(0, 0, 14, 8);
        form.add(tfFrom, c);
        c.gridx = 1; c.insets = new Insets(0, 8, 14, 0);
        form.add(tfTo, c);
        c.gridx = 0; c.gridy = 4; c.insets = new Insets(0, 0, 4, 8);
        form.add(lbl("Ngày trả (dd/MM/yyyy)", font(Font.BOLD, 12), MUTED), c);
        c.gridx = 1; c.insets = new Insets(0, 8, 4, 0);
        form.add(lbl("Tổng lương (VNĐ)", font(Font.BOLD, 12), MUTED), c);
        c.gridx = 0; c.gridy = 5; c.insets = new Insets(0, 0, 10, 8);
        form.add(tfPay, c);
        c.gridx = 1; c.insets = new Insets(0, 8, 10, 0);
        form.add(tfTotal, c);
        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; c.insets = new Insets(0, 0, 0, 0);
        form.add(info, c);
        refreshInfo.run();

        String[] created = {null};
        boolean ok = dialog("Lập phiếu lương", "Người lập: " + hoTen + " (" + maNhanVien + ")", form, "Lập phiếu", ACCENT, () -> {
            if (!canPayroll()) return "Bạn không có quyền lập phiếu lương.";
            LocalDate from, to, pay;
            try {
                from = LocalDate.parse(tfFrom.getText().trim(), IN_FMT);
                to = LocalDate.parse(tfTo.getText().trim(), IN_FMT);
                pay = LocalDate.parse(tfPay.getText().trim(), IN_FMT);
            } catch (DateTimeParseException ex) {
                return "Ngày phải đúng dạng dd/MM/yyyy và hợp lệ, ví dụ 05/11/2026.";
            }
            if (to.isBefore(from)) return "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.";
            if (pay.isBefore(to)) return "Ngày trả lương không được trước ngày kết thúc kỳ lương.";
            String digits = tfTotal.getText().replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return "Hãy nhập tổng lương.";
            long total;
            try { total = Long.parseLong(digits); } catch (NumberFormatException ex) { return "Tổng lương quá lớn."; }
            if (total <= 0 || total > Integer.MAX_VALUE) return "Tổng lương phải từ 1 đến 2.147.483.647 ₫.";
            String idGv = String.valueOf(gv.get(cbGv.getSelectedIndex())[0]);
            try {
                long dup = Db.scalar("SELECT COUNT(*) FROM Phieu_luong WHERE id_giao_vien = ? AND tu_ngay < ? AND den_ngay >= ?",
                        idGv, Timestamp.valueOf(to.plusDays(1).atStartOfDay()), Timestamp.valueOf(from.atStartOfDay()));
                if (dup > 0) return "Giáo viên này đã có phiếu lương trùng khoảng thời gian trên.";
                String ma = "PL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                Db.update("INSERT INTO Phieu_luong (ma_phieu_luong, id_giao_vien, ma_nhan_vien, tu_ngay, den_ngay, ngay_tra, tong_luong) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)", ma, idGv, maNhanVien,
                        Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.atStartOfDay()),
                        Timestamp.valueOf(pay.atStartOfDay()), (int) total);
                created[0] = ma;
                return null;
            } catch (SQLException ex) {
                return "Lỗi CSDL: " + rootMsg(ex);
            }
        }, true);
        if (ok && created[0] != null) {
            toast("Đã lập phiếu lương " + created[0] + ".", T_OK);
            if ("Luong".equals(currentKey)) navigate("Luong", false);
        }
    }

    /* ============================ HỒ SƠ ============================ */
    private static JPanel vform(String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.gridx = 0;
        int y = 0;
        for (int i = 0; i < labels.length; i++) {
            c.gridy = y++; c.insets = new Insets(0, 0, 4, 0);
            p.add(lbl(labels[i], font(Font.BOLD, 12), MUTED), c);
            c.gridy = y++; c.insets = new Insets(0, 0, 12, 0);
            p.add(fields[i], c);
        }
        return p;
    }

    private static long q(String sql, Object... p) {
        try { return Db.scalar(sql, p); } catch (SQLException e) { return 0; }
    }

    private JComponent createProfilePage() {
        long lop = q("SELECT COUNT(*) FROM Lop WHERE ma_nhan_vien = ?", maNhanVien);
        long lap = q("SELECT COUNT(*) FROM hoa_don WHERE ma_nhan_vien = ?", maNhanVien);
        long xn = q("SELECT COUNT(*) FROM hoa_don WHERE nv_xac_nhan = ?", maNhanVien);
        long phieu = q("SELECT COUNT(*) FROM Phieu_luong WHERE ma_nhan_vien = ?", maNhanVien);

        ProfileHero hero = new ProfileHero(hoTen, vaiTro, maNhanVien,
                new String[]{"Lớp", "HĐ xác nhận", "Phiếu lương"}, new long[]{lop, xn, phieu});

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(infoRow("Mã nhân viên", lbl(maNhanVien, font(Font.BOLD, 14), INK)));
        rows.add(infoRow("Họ và tên", lbl(hoTen, font(Font.BOLD, 14), INK)));
        rows.add(infoRow("Vai trò", lbl(vaiTro.isEmpty() ? "—" : vaiTro, font(Font.BOLD, 14), INK)));
        rows.add(infoRow("Mức lương", lbl(vnd(luong), font(Font.BOLD, 14), INK)));
        rows.add(infoRow("Lớp phụ trách", lbl(String.valueOf(lop), font(Font.BOLD, 14), INK)));
        rows.add(infoRow("Hóa đơn đã lập", lbl(String.valueOf(lap), font(Font.BOLD, 14), INK)));
        rows.add(infoRow("Hóa đơn đã xác nhận", lbl(String.valueOf(xn), font(Font.BOLD, 14), INK)));
        rows.add(infoRow("Phiếu lương đã lập", lbl(String.valueOf(phieu), font(Font.BOLD, 14), INK)));

        JPanel perm = new JPanel();
        perm.setOpaque(false);
        perm.setLayout(new BoxLayout(perm, BoxLayout.Y_AXIS));
        perm.add(infoRow("Xác nhận học phí đã nộp", new Pill(canConfirm() ? "Có" : "Không", canConfirm() ? GREEN : RED)));
        perm.add(infoRow("Lập phiếu lương giáo viên", new Pill(canPayroll() ? "Có" : "Không", canPayroll() ? GREEN : RED)));
        perm.add(infoRow("Hoàn tác thanh toán", new Pill(canRevert() ? "Có" : "Không", canRevert() ? GREEN : RED)));

        Card info = new Card("Thông tin chi tiết", "Dữ liệu từ hồ sơ nhân viên", null);
        info.add(rows, BorderLayout.CENTER);
        Card pm = new Card("Phân quyền của bạn", "Thu ngân / Tư vấn: xác nhận học phí. Kế toán / Quản lý: toàn quyền", null);
        pm.add(perm, BorderLayout.CENTER);

        JPanel rightCol = new JPanel(new GridBagLayout());
        rightCol.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        rightCol.add(info, c);
        c.gridy = 1;
        rightCol.add(pm, c);
        c.gridy = 2;
        c.weighty = 1;
        rightCol.add(Box.createVerticalGlue(), c);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        JPanel leftCol = new JPanel(new BorderLayout());
        leftCol.setOpaque(false);
        leftCol.add(hero, BorderLayout.NORTH);
        wrap.add(leftCol, BorderLayout.WEST);
        wrap.add(rightCol, BorderLayout.CENTER);
        return pad(wrap);
    }

    private static JComponent infoRow(String k, JComponent v) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(243, 240, 255)), new EmptyBorder(11, 2, 11, 2)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl(k, font(Font.PLAIN, 14), MUTED), BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        r.setOpaque(false);
        r.add(v);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    /* ============================ MAIN ============================ */
    /* ============================ BÁO CÁO TÀI CHÍNH (EXCEL) ============================ */

    /** Ghi file .xlsx thuần Java (không cần thư viện ngoài) */
    static final class Xlsx {
        static final class Sheet {
            final String name;
            final List<Object[]> rows = new ArrayList<>();
            final List<Boolean> bold = new ArrayList<>();
            Sheet(String name) { this.name = name; }
            Sheet row(boolean b, Object... cells) { rows.add(cells); bold.add(b); return this; }
        }

        private static final String H = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
        private final List<Sheet> sheets = new ArrayList<>();

        Sheet sheet(String name) { Sheet s = new Sheet(name); sheets.add(s); return s; }

        private static String esc(String s) {
            StringBuilder b = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (c == '&') b.append("&amp;");
                else if (c == '<') b.append("&lt;");
                else if (c == '>') b.append("&gt;");
                else if (c == '"') b.append("&quot;");
                else if (c >= 0x20 || c == '\n' || c == '\t') b.append(c);
            }
            return b.toString();
        }

        private static String colName(int i) {
            String s = "";
            for (i++; i > 0; i = (i - 1) / 26) s = (char) ('A' + (i - 1) % 26) + s;
            return s;
        }

        private static void put(ZipOutputStream z, String name, String content) throws IOException {
            z.putNextEntry(new ZipEntry(name));
            z.write(content.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }

        private String sheetXml(Sheet sh) {
            int ncol = 0;
            for (Object[] r : sh.rows) ncol = Math.max(ncol, r.length);
            double[] w = new double[ncol];
            for (Object[] r : sh.rows) {
                if (r.length <= 1) continue;
                for (int c = 0; c < r.length; c++) {
                    Object v = r[c];
                    if (v == null) continue;
                    int len = v instanceof Number
                            ? String.format(Locale.US, "%,d", ((Number) v).longValue()).length()
                            : v.toString().length();
                    w[c] = Math.max(w[c], len);
                }
            }
            StringBuilder b = new StringBuilder(H)
                    .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
            if (ncol > 0) {
                b.append("<cols>");
                for (int c = 0; c < ncol; c++)
                    b.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                            .append("\" width=\"").append(Math.min(50, Math.max(12, w[c] + 3)))
                            .append("\" customWidth=\"1\"/>");
                b.append("</cols>");
            }
            b.append("<sheetData>");
            for (int r = 0; r < sh.rows.size(); r++) {
                Object[] cells = sh.rows.get(r);
                boolean bd = sh.bold.get(r);
                b.append("<row r=\"").append(r + 1).append("\">");
                for (int c = 0; c < cells.length; c++) {
                    Object v = cells[c];
                    if (v == null) continue;
                    String ref = colName(c) + (r + 1);
                    if (v instanceof Number) {
                        b.append("<c r=\"").append(ref).append("\" s=\"").append(bd ? 3 : 2).append("\"><v>")
                                .append(((Number) v).longValue()).append("</v></c>");
                    } else {
                        b.append("<c r=\"").append(ref).append("\" t=\"inlineStr\" s=\"").append(bd ? 1 : 0)
                                .append("\"><is><t xml:space=\"preserve\">").append(esc(v.toString()))
                                .append("</t></is></c>");
                    }
                }
                b.append("</row>");
            }
            return b.append("</sheetData></worksheet>").toString();
        }

        void save(File f) throws IOException {
            String rel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
            try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                StringBuilder ct = new StringBuilder(H)
                        .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                        .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                        .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                        .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                        .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
                StringBuilder wb = new StringBuilder(H)
                        .append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"")
                        .append(rel).append("\"><sheets>");
                StringBuilder wr = new StringBuilder(H)
                        .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
                for (int i = 0; i < sheets.size(); i++) {
                    int n = i + 1;
                    ct.append("<Override PartName=\"/xl/worksheets/sheet").append(n)
                            .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                    wb.append("<sheet name=\"").append(esc(sheets.get(i).name)).append("\" sheetId=\"").append(n)
                            .append("\" r:id=\"rId").append(n).append("\"/>");
                    wr.append("<Relationship Id=\"rId").append(n).append("\" Type=\"").append(rel)
                            .append("/worksheet\" Target=\"worksheets/sheet").append(n).append(".xml\"/>");
                    put(z, "xl/worksheets/sheet" + n + ".xml", sheetXml(sheets.get(i)));
                }
                wr.append("<Relationship Id=\"rId").append(sheets.size() + 1).append("\" Type=\"").append(rel)
                        .append("/styles\" Target=\"styles.xml\"/></Relationships>");
                put(z, "[Content_Types].xml", ct.append("</Types>").toString());
                put(z, "_rels/.rels", H + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"" + rel + "/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
                put(z, "xl/workbook.xml", wb.append("</sheets></workbook>").toString());
                put(z, "xl/_rels/workbook.xml.rels", wr.toString());
                put(z, "xl/styles.xml", H + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                        + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
                        + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
                        + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
                        + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                        + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                        + "<cellXfs count=\"4\">"
                        + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                        + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
                        + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                        + "<xf numFmtId=\"3\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyNumberFormat=\"1\"/>"
                        + "</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>");
            }
        }
    }
    private void addTemplateSheets(Xlsx x, LocalDate from, LocalDate to, Timestamp t1, Timestamp t2) throws SQLException {
        String ky = "Kỳ báo cáo: " + from.format(D_FMT) + " - " + to.format(D_FMT);

        /* ---------- I. ĐÀO TẠO ---------- */
        Map<String, Long> done = new HashMap<>();
        for (Object[] r : Db.query(
                "SELECT k.trinh_do, COUNT(DISTINCT hsl.ma_sinh_vien) FROM Hoc_sinh_lop hsl "
                        + "JOIN Lop l ON l.id_lop = hsl.id_lop JOIN Khoa_hoc k ON k.ma_khoa_hoc = l.ma_khoa_hoc "
                        + "JOIN Ket_qua_thi kq ON kq.ma_sinh_vien = hsl.ma_sinh_vien AND kq.trinh_do = k.trinh_do "
                        + "AND kq.tinh_trang = 'ĐẠT' GROUP BY k.trinh_do",
                rs -> new Object[]{rs.getString(1), rs.getLong(2)})) done.put((String) r[0], (Long) r[1]);

        Xlsx.Sheet s1 = x.sheet("I. Đào tạo");
        s1.row(true, "I. SỐ LỚP VÀ SỐ HỌC VIÊN THEO CẤP ĐỘ ĐÀO TẠO");
        s1.row(false, ky);
        s1.row(false);
        s1.row(true, "STT", "Cấp độ đào tạo", "Số lớp", "Tổng số học viên",
                "Đã hoàn thành chương trình", "Chưa hoàn thành chương trình");
        long tLop = 0, tHs = 0, tDone = 0;
        int stt = 1;
        for (Object[] r : Db.query(
                "SELECT k.trinh_do, COUNT(DISTINCT l.id_lop), COUNT(h.ma_sinh_vien) FROM Khoa_hoc k "
                        + "LEFT JOIN Lop l ON l.ma_khoa_hoc = k.ma_khoa_hoc "
                        + "LEFT JOIN Hoc_sinh_lop h ON h.id_lop = l.id_lop "
                        + "GROUP BY k.trinh_do ORDER BY k.trinh_do DESC",
                rs -> new Object[]{rs.getString(1), rs.getLong(2), rs.getLong(3)})) {
            long lop = (Long) r[1], hs = (Long) r[2];
            long dn = Math.min(hs, done.getOrDefault((String) r[0], 0L));
            s1.row(false, stt++, "Tiếng Nhật " + r[0], lop, hs, dn, hs - dn);
            tLop += lop; tHs += hs; tDone += dn;
        }
        s1.row(true, null, "Tổng cộng", tLop, tHs, tDone, tHs - tDone);
        

        /* ---------- III.1 NHÂN SỰ ---------- */
        Xlsx.Sheet s2 = x.sheet("II. Nhân sự");
        s2.row(true, "1. ĐỘI NGŨ NHÂN SỰ CỦA TRUNG TÂM");
        s2.row(false);
        s2.row(true, "STT", "Chức danh", "Số lượng", "Thực hiện hợp đồng lao động");
        List<Object[]> gvAll = Db.query("SELECT ho_ten, quoc_tich, chung_chi, so_dien_thoai FROM Giao_vien ORDER BY id_giao_vien",
                rs -> new Object[]{nz(rs.getString(1)), nz(rs.getString(2)), nz(rs.getString(3)), nz(rs.getString(4))});
        List<Object[]> gvVn = new ArrayList<>(), gvNn = new ArrayList<>();
        for (Object[] g : gvAll) {
            String q = ((String) g[1]).toLowerCase(new Locale("vi"));
            (q.contains("việt") || q.contains("viet") ? gvVn : gvNn).add(g);
        }
        stt = 1;
        for (Object[] r : Db.query("SELECT vai_tro, COUNT(*) FROM Nhan_vien GROUP BY vai_tro ORDER BY COUNT(*) DESC",
                rs -> new Object[]{rs.getString(1), rs.getLong(2)}))
            s2.row(false, stt++, r[0], r[1], r[1]);
        s2.row(false, stt++, "Giáo viên nước ngoài", gvNn.size(), gvNn.size());
        s2.row(false, stt, "Giáo viên Việt Nam", gvVn.size(), gvVn.size());
        s2.row(false);
        s2.row(true, "Thông tin người lao động nước ngoài");
        s2.row(true, "STT", "Họ và tên", "Quốc tịch", "Bằng cấp / chứng chỉ", "Số điện thoại");
        stt = 1;
        for (Object[] g : gvNn) s2.row(false, stt++, g[0], g[1], g[2], g[3]);
        s2.row(false);
        s2.row(true, "Thông tin người lao động Việt Nam");
        s2.row(true, "STT", "Họ và tên", "Nhiệm vụ được phân công", "Trình độ / chứng chỉ", "Số điện thoại");
        stt = 1;
        for (Object[] g : gvVn) s2.row(false, stt++, g[0], "Giáo viên", g[2], g[3]);
        for (Object[] r : Db.query("SELECT ho_ten, vai_tro FROM Nhan_vien ORDER BY ma_nhan_vien",
                rs -> new Object[]{nz(rs.getString(1)), nz(rs.getString(2))}))
            s2.row(false, stt++, r[0], r[1]);

        /* ---------- III.2 BÁO CÁO TÀI CHÍNH ---------- */
        Xlsx.Sheet s3 = x.sheet("III. Tài chính");
        s3.row(true, "2. BÁO CÁO TÀI CHÍNH");
        s3.row(false, ky);
        s3.row(false);
        s3.row(true, "a) Tổng thu");
        s3.row(true, "- Học phí niêm yết theo khóa học");
        s3.row(true, "Mã khóa", "Tên khóa học", "Cấp độ", "Chuyên đề", "Học phí (₫)");
        for (Object[] r : Db.query("SELECT ma_khoa_hoc, ten_khoa_hoc, trinh_do, chuyen_de, hoc_phi FROM Khoa_hoc "
                        + "ORDER BY trinh_do DESC, ma_khoa_hoc",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getLong(5)}))
            s3.row(false, r);
        s3.row(false);
        s3.row(true, "- Thực thu học phí theo cấp độ (hóa đơn đã nộp)");
        s3.row(true, "Cấp độ", "Số hóa đơn", "Số tiền (₫)");
        long thu = 0;
        for (Object[] r : Db.query("SELECT COALESCE(k.trinh_do, 'Khác'), COUNT(*), SUM(h.khoan_thu) FROM hoa_don h "
                        + "LEFT JOIN Khoa_hoc k ON k.ma_khoa_hoc = SUBSTRING_INDEX(h.ma_ghi_danh, ':', -1) "
                        + "WHERE h.trang_thai = 'Đã nộp' AND COALESCE(h.ngay_nop, h.ngay_lap) >= ? "
                        + "AND COALESCE(h.ngay_nop, h.ngay_lap) < ? GROUP BY 1 ORDER BY 1 DESC",
                rs -> new Object[]{rs.getString(1), rs.getLong(2), rs.getLong(3)}, t1, t2)) {
            s3.row(false, r);
            thu += (Long) r[2];
        }
        s3.row(true, "Tổng thu", null, thu);
        s3.row(false, "- Các khoản thu khác: Không có");
        s3.row(false);

        long luongGv = Db.scalar("SELECT COALESCE(SUM(tong_luong), 0) FROM Phieu_luong WHERE ngay_tra >= ? AND ngay_tra < ?", t1, t2);
        long luongNv = Db.scalar("SELECT COALESCE(SUM(luong), 0) FROM Nhan_vien");
        long thang = java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1;
        s3.row(true, "b) Tổng chi");
        s3.row(false, "- Lương giáo viên (theo phiếu lương đã lập)", luongGv);
        s3.row(false, "- Lương nhân viên (ước tính: tổng mức lương tháng × " + thang + " tháng)", luongNv * thang);
        s3.row(true, "Tổng chi", luongGv + luongNv * thang);
        s3.row(false);
        s3.row(true, "Chênh lệch (thu - chi)", thu - luongGv - luongNv * thang);
    }
    private void exportReport() {
        if (!canPayroll()) { toast("Chỉ Kế toán / Quản lý được xuất báo cáo tài chính.", T_ERR); return; }
        LocalDate now = LocalDate.now();
        StyledField tfFrom = new StyledField(now.withDayOfMonth(1).minusMonths(5).format(D_FMT));
        StyledField tfTo = new StyledField(now.format(D_FMT));
        JPanel form = vform(new String[]{"Từ ngày (dd/MM/yyyy)", "Đến ngày (dd/MM/yyyy)"},
                new JComponent[]{tfFrom, tfTo});
        LocalDate[] range = new LocalDate[2];
        boolean ok = dialog("Xuất báo cáo tài chính", "Thu học phí, công nợ và chi lương giáo viên", form,
                "Chọn nơi lưu", ACCENT, () -> {
                    try {
                        range[0] = LocalDate.parse(tfFrom.getText().trim(), IN_FMT);
                        range[1] = LocalDate.parse(tfTo.getText().trim(), IN_FMT);
                    } catch (DateTimeParseException ex) {
                        return "Ngày phải đúng dạng dd/MM/yyyy và hợp lệ.";
                    }
                    if (range[1].isBefore(range[0])) return "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.";
                    return null;
                }, true);
        if (!ok) return;
        final LocalDate from = range[0], to = range[1];

        DateTimeFormatter compact = DateTimeFormatter.ofPattern("yyyyMMdd");
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("BaoCaoTaiChinh_" + from.format(compact) + "_" + to.format(compact) + ".xlsx"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File chosen = fc.getSelectedFile();
        if (!chosen.getName().toLowerCase().endsWith(".xlsx"))
            chosen = new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        final File out = chosen;
        if (out.exists() && JOptionPane.showConfirmDialog(this, "File đã tồn tại. Ghi đè?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        final Timestamp t1 = Timestamp.valueOf(from.atStartOfDay());
        final Timestamp t2 = Timestamp.valueOf(to.plusDays(1).atStartOfDay());
        toast("Đang tạo báo cáo...", T_INFO);

        async(() -> {
            RowMapper inv = rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                    when(rs, 5, true).toString(), payMethod(rs.getString(6)), when(rs, 7, true).toString()};
            List<Object[]> paid = Db.query(INVOICE_SELECT
                    + " WHERE h.trang_thai = 'Đã nộp' AND COALESCE(h.ngay_nop, h.ngay_lap) >= ?"
                    + " AND COALESCE(h.ngay_nop, h.ngay_lap) < ? ORDER BY COALESCE(h.ngay_nop, h.ngay_lap)", inv, t1, t2);
            List<Object[]> unpaid = Db.query(INVOICE_SELECT
                    + " WHERE COALESCE(h.trang_thai, 'Chưa nộp') = 'Chưa nộp' AND h.ngay_lap >= ?"
                    + " AND h.ngay_lap < ? ORDER BY h.ngay_lap", inv, t1, t2);
            List<Object[]> pay = Db.query("SELECT p.ma_phieu_luong, COALESCE(gv.ho_ten, p.id_giao_vien), p.tu_ngay, p.den_ngay, "
                            + "p.ngay_tra, p.tong_luong, COALESCE(nv.ho_ten, p.ma_nhan_vien) FROM Phieu_luong p "
                            + "LEFT JOIN Giao_vien gv ON p.id_giao_vien = gv.id_giao_vien "
                            + "LEFT JOIN Nhan_vien nv ON p.ma_nhan_vien = nv.ma_nhan_vien "
                            + "WHERE p.ngay_tra >= ? AND p.ngay_tra < ? ORDER BY p.ngay_tra, p.ma_phieu_luong",
                    rs -> new Object[]{rs.getString(1), rs.getString(2), when(rs, 3, false).toString(),
                            when(rs, 4, false).toString(), when(rs, 5, false).toString(), rs.getLong(6), rs.getString(7)},
                    t1, t2);

            Map<String, Long> mThu = new HashMap<>(), mChi = new HashMap<>();
            for (Object[] r : Db.query("SELECT DATE_FORMAT(COALESCE(ngay_nop, ngay_lap), '%Y-%m'), SUM(khoan_thu) "
                            + "FROM hoa_don WHERE trang_thai = 'Đã nộp' AND COALESCE(ngay_nop, ngay_lap) >= ? "
                            + "AND COALESCE(ngay_nop, ngay_lap) < ? GROUP BY 1",
                    rs -> new Object[]{rs.getString(1), rs.getLong(2)}, t1, t2)) mThu.put((String) r[0], (Long) r[1]);
            for (Object[] r : Db.query("SELECT DATE_FORMAT(ngay_tra, '%Y-%m'), SUM(tong_luong) FROM Phieu_luong "
                            + "WHERE ngay_tra >= ? AND ngay_tra < ? GROUP BY 1",
                    rs -> new Object[]{rs.getString(1), rs.getLong(2)}, t1, t2)) mChi.put((String) r[0], (Long) r[1]);

            long thu = 0, congNo = 0, chi = 0;
            for (Object[] r : paid) thu += (Long) r[3];
            for (Object[] r : unpaid) congNo += (Long) r[3];
            for (Object[] r : pay) chi += (Long) r[5];

            Xlsx x = new Xlsx();
            addTemplateSheets(x, from, to, t1, t2);
            Xlsx.Sheet s1 = x.sheet("Tổng hợp");
            s1.row(true, "BÁO CÁO TÀI CHÍNH TRUNG TÂM S-LINK");
            s1.row(false, "Kỳ báo cáo: " + from.format(D_FMT) + " - " + to.format(D_FMT));
            s1.row(false, "Người lập: " + hoTen + " (" + maNhanVien + ") - xuất lúc "
                    + LocalDateTime.now().format(DT_FMT));
            s1.row(false);
            s1.row(true, "CHỈ TIÊU", "SỐ TIỀN (₫)");
            s1.row(false, "Tổng thu học phí (đã nộp)", thu);
            s1.row(false, "Chi lương giáo viên", chi);
            s1.row(true, "Chênh lệch (thu - chi)", thu - chi);
            s1.row(false, "Công nợ học phí (chưa nộp, lập trong kỳ)", congNo);
            s1.row(false);
            s1.row(true, "THEO THÁNG");
            s1.row(true, "Tháng", "Thu học phí (₫)", "Chi lương GV (₫)", "Chênh lệch (₫)");
            for (YearMonth ym = YearMonth.from(from); !ym.isAfter(YearMonth.from(to)); ym = ym.plusMonths(1)) {
                long a = mThu.getOrDefault(ym.toString(), 0L), b = mChi.getOrDefault(ym.toString(), 0L);
                s1.row(false, String.format("%02d/%d", ym.getMonthValue(), ym.getYear()), a, b, a - b);
            }
            s1.row(true, "TỔNG CỘNG", thu, chi, thu - chi);

            Xlsx.Sheet s2 = x.sheet("Thu học phí");
            s2.row(true, "Mã HĐ", "Học sinh", "Khóa học", "Số tiền (₫)", "Ngày lập", "Hình thức", "Ngày nộp");
            for (Object[] r : paid) s2.row(false, r);
            s2.row(true, "TỔNG CỘNG", null, null, thu);

            Xlsx.Sheet s3 = x.sheet("Công nợ");
            s3.row(true, "Mã HĐ", "Học sinh", "Khóa học", "Số tiền (₫)", "Ngày lập", "Hình thức");
            for (Object[] r : unpaid) s3.row(false, r[0], r[1], r[2], r[3], r[4], r[5]);
            s3.row(true, "TỔNG CÔNG NỢ", null, null, congNo);

            Xlsx.Sheet s4 = x.sheet("Chi lương GV");
            s4.row(true, "Mã phiếu", "Giáo viên", "Từ ngày", "Đến ngày", "Ngày trả", "Tổng lương (₫)", "Người lập");
            for (Object[] r : pay) s4.row(false, r);
            s4.row(true, "TỔNG CỘNG", null, null, null, null, chi);

            x.save(out);
            return out.getName();
        }, name -> toast("Đã xuất báo cáo: " + name, T_OK));
    }
    /* ============================ TRUNG TÂM XUẤT DỮ LIỆU (EXCEL) ============================ */
    /* DÁN CẢ KHỐI NÀY vào trong class StaffDashboardUI, ngay phía trên dòng:  public static void main(String[] args) */

    static final int F_CHOICE = 0, F_TEXT = 1, F_NUM = 2, F_DATE = 3;

    /** Một điều kiện lọc */
    static final class Flt {
        final int kind;
        final String label, expr;
        final String[] names, sqls;
        Flt(int kind, String label, String expr, String[] names, String[] sqls) {
            this.kind = kind; this.label = label; this.expr = expr; this.names = names; this.sqls = sqls;
        }
    }

    /** Lựa chọn: các cặp (nhãn, điều kiện SQL). "Tất cả" được tự thêm vào đầu. */
    static Flt fCh(String label, String... pairs) {
        int n = pairs.length / 2;
        String[] names = new String[n + 1], sqls = new String[n + 1];
        names[0] = "Tất cả";
        for (int i = 0; i < n; i++) { names[i + 1] = pairs[2 * i]; sqls[i + 1] = pairs[2 * i + 1]; }
        return new Flt(F_CHOICE, label, null, names, sqls);
    }
    static Flt fTx(String label, String expr) { return new Flt(F_TEXT, label, expr, null, null); }
    static Flt fNm(String label, String expr) { return new Flt(F_NUM, label, expr, null, null); }
    static Flt fDt(String label, String expr) { return new Flt(F_DATE, label, expr, null, null); }
    static Flt fLvl(String label, String col) {
        return fCh(label, "N5", col + " = 'N5'", "N4", col + " = 'N4'", "N3", col + " = 'N3'",
                "N2", col + " = 'N2'", "N1", col + " = 'N1'");
    }

    /** Một loại dữ liệu có thể xuất */
    static final class DS {
        final String key, file, title, sql;
        final String[] heads, sortNames, sortSql;
        final Flt[] filters;
        final int[] sums;
        final boolean sensitive;
        DS(String key, String file, String title, String sql, String[] heads, String[] sortPairs,
           Flt[] filters, int[] sums, boolean sensitive) {
            this.key = key; this.file = file; this.title = title; this.sql = sql; this.heads = heads;
            this.filters = filters; this.sums = sums; this.sensitive = sensitive;
            int n = sortPairs.length / 2;
            sortNames = new String[n];
            sortSql = new String[n];
            for (int i = 0; i < n; i++) { sortNames[i] = sortPairs[2 * i]; sortSql[i] = sortPairs[2 * i + 1]; }
        }
    }

    interface Cond { String build(List<Object> par, List<String> dsc); }

    static final class FormUi {
        final JPanel panel = new JPanel(new GridBagLayout());
        RoundCombo sort;
        StyledField top;
        JCheckBox total;
        final List<Cond> conds = new ArrayList<>();
    }

    static final class ExportJob {
        final DS ds; final String sql; final Object[] params; final List<String> desc;
        final String sortName; final int top; final boolean total;
        ExportJob(DS ds, String sql, Object[] params, List<String> desc, String sortName, int top, boolean total) {
            this.ds = ds; this.sql = sql; this.params = params; this.desc = desc;
            this.sortName = sortName; this.top = top; this.total = total;
        }
    }

    /** Toàn bộ định nghĩa: câu SELECT gốc + cách sắp xếp + bộ lọc của từng loại dữ liệu */
    static List<DS> exportSpecs() {
        List<DS> L = new ArrayList<>();

        /* ---------- 1. HÓA ĐƠN ---------- */
        L.add(new DS("hoadon", "HoaDon", "Hóa đơn học phí",
                "SELECT h.ma_hoa_don AS ma_hd, COALESCE(hs.ho_ten, h.ma_ghi_danh) AS hoc_sinh, "
                        + "COALESCE(k.ten_khoa_hoc, '—') AS khoa_hoc, COALESCE(k.trinh_do, '—') AS cap_do, "
                        + "h.khoan_thu AS khoan_thu, h.ngay_lap AS ngay_lap, COALESCE(h.hinh_thuc, '—') AS hinh_thuc, "
                        + "COALESCE(h.trang_thai, 'Chưa nộp') AS trang_thai, h.ngay_nop AS ngay_nop, "
                        + "COALESCE(nv.ho_ten, h.ma_nhan_vien) AS nguoi_lap, COALESCE(xn.ho_ten, h.nv_xac_nhan, '') AS nguoi_xn "
                        + "FROM hoa_don h "
                        + "LEFT JOIN Hoc_sinh hs ON hs.ma_sinh_vien = SUBSTRING_INDEX(h.ma_ghi_danh, ':', 1) "
                        + "LEFT JOIN Khoa_hoc k ON k.ma_khoa_hoc = SUBSTRING_INDEX(h.ma_ghi_danh, ':', -1) "
                        + "LEFT JOIN Nhan_vien nv ON nv.ma_nhan_vien = h.ma_nhan_vien "
                        + "LEFT JOIN Nhan_vien xn ON xn.ma_nhan_vien = h.nv_xac_nhan",
                new String[]{"Mã hóa đơn", "Học sinh", "Khóa học", "Cấp độ", "Khoản thu (₫)", "Ngày lập", "Hình thức",
                        "Trạng thái", "Ngày nộp", "Người lập", "Người xác nhận"},
                new String[]{
                        "Ngày lập: mới nhất trước", "ngay_lap DESC, ma_hd",
                        "Ngày lập: cũ nhất trước", "ngay_lap ASC, ma_hd",
                        "Khoản thu: cao → thấp", "khoan_thu DESC, ma_hd",
                        "Khoản thu: thấp → cao", "khoan_thu ASC, ma_hd",
                        "Tên học sinh: A → Z", "hoc_sinh ASC, ngay_lap DESC",
                        "Tên học sinh: Z → A", "hoc_sinh DESC, ngay_lap DESC",
                        "Tên khóa học: A → Z", "khoa_hoc ASC, ma_hd",
                        "Cấp độ: N1 → N5", "FIELD(cap_do, 'N1', 'N2', 'N3', 'N4', 'N5'), ma_hd",
                        "Cấp độ: N5 → N1", "FIELD(cap_do, 'N5', 'N4', 'N3', 'N2', 'N1'), ma_hd",
                        "Ngày nộp: mới nhất trước", "ngay_nop DESC, ma_hd",
                        "Chưa nộp xếp trước", "FIELD(trang_thai, 'Chưa nộp', 'Đã nộp'), ngay_lap DESC",
                        "Đã nộp xếp trước", "FIELD(trang_thai, 'Đã nộp', 'Chưa nộp'), ngay_lap DESC",
                        "Mã hóa đơn: A → Z", "ma_hd ASC"},
                new Flt[]{
                        fCh("Trạng thái", "Đã nộp", "trang_thai = 'Đã nộp'", "Chưa nộp", "trang_thai = 'Chưa nộp'"),
                        fCh("Hình thức thanh toán", "Tiền mặt", "hinh_thuc = 'Tiền mặt'", "Chuyển khoản", "hinh_thuc = 'Chuyển khoản'",
                                "Thẻ", "hinh_thuc = 'Thẻ'", "Chưa có hình thức", "hinh_thuc = '—'"),
                        fLvl("Cấp độ khóa học", "cap_do"),
                        fCh("Công nợ quá hạn",
                                "Quá 30 ngày", "(trang_thai = 'Chưa nộp' AND ngay_lap < NOW() - INTERVAL 30 DAY)",
                                "Quá 60 ngày", "(trang_thai = 'Chưa nộp' AND ngay_lap < NOW() - INTERVAL 60 DAY)",
                                "Quá 90 ngày", "(trang_thai = 'Chưa nộp' AND ngay_lap < NOW() - INTERVAL 90 DAY)"),
                        fCh("Mức tiền", "Dưới 3 triệu", "khoan_thu < 3000000", "Từ 3 đến 5 triệu", "khoan_thu BETWEEN 3000000 AND 5000000",
                                "Trên 5 triệu", "khoan_thu > 5000000"),
                        fDt("Ngày lập", "ngay_lap"), fDt("Ngày nộp", "ngay_nop"),
                        fNm("Khoản thu (₫)", "khoan_thu"),
                        fTx("Học sinh", "hoc_sinh"), fTx("Khóa học", "khoa_hoc"),
                        fTx("Người lập", "nguoi_lap"), fTx("Người xác nhận", "nguoi_xn")},
                new int[]{4}, false));

        /* ---------- 2. HỌC SINH ---------- */
        L.add(new DS("hocsinh", "HocSinh", "Học sinh",
                "SELECT hs.ma_sinh_vien AS ma, hs.ho_ten AS ho_ten, hs.ngay_sinh AS ngay_sinh, "
                        + "TIMESTAMPDIFF(YEAR, hs.ngay_sinh, CURDATE()) AS tuoi, hs.trinh_do AS trinh_do, "
                        + "hs.so_dien_thoai AS sdt, COALESCE(gv.ho_ten, 'Chưa phân công') AS gv, "
                        + "(SELECT COUNT(*) FROM Hoc_sinh_lop x WHERE x.ma_sinh_vien = hs.ma_sinh_vien) AS so_lop, "
                        + "(SELECT COALESCE(SUM(d.khoan_thu), 0) FROM hoa_don d WHERE SUBSTRING_INDEX(d.ma_ghi_danh, ':', 1) = hs.ma_sinh_vien "
                        + "AND d.trang_thai = 'Đã nộp') AS da_dong, "
                        + "(SELECT COALESCE(SUM(d.khoan_thu), 0) FROM hoa_don d WHERE SUBSTRING_INDEX(d.ma_ghi_danh, ':', 1) = hs.ma_sinh_vien "
                        + "AND d.trang_thai = 'Chưa nộp') AS con_no "
                        + "FROM Hoc_sinh hs LEFT JOIN Giao_vien gv ON gv.id_giao_vien = hs.id_giao_vien",
                new String[]{"Mã SV", "Họ tên", "Ngày sinh", "Tuổi", "Trình độ", "Số điện thoại", "GV phụ trách",
                        "Số lớp", "Đã đóng (₫)", "Còn nợ (₫)"},
                new String[]{
                        "Tên: A → Z", "ho_ten ASC",
                        "Tên: Z → A", "ho_ten DESC",
                        "Tuổi: lớn → nhỏ", "tuoi DESC, ho_ten",
                        "Tuổi: nhỏ → lớn", "tuoi ASC, ho_ten",
                        "Trình độ: N1 → N5", "FIELD(trinh_do, 'N1', 'N2', 'N3', 'N4', 'N5', 'Chưa xác định'), ho_ten",
                        "Trình độ: N5 → N1", "FIELD(trinh_do, 'N5', 'N4', 'N3', 'N2', 'N1', 'Chưa xác định'), ho_ten",
                        "Số lớp: nhiều → ít", "so_lop DESC, ho_ten",
                        "Đã đóng học phí: nhiều → ít", "da_dong DESC, ho_ten",
                        "Còn nợ: nhiều → ít", "con_no DESC, ho_ten",
                        "Ngày sinh: cũ nhất trước", "ngay_sinh ASC",
                        "Mã sinh viên", "LENGTH(ma), ma"},
                new Flt[]{
                        fCh("Trình độ", "N5", "trinh_do = 'N5'", "N4", "trinh_do = 'N4'", "N3", "trinh_do = 'N3'",
                                "N2", "trinh_do = 'N2'", "N1", "trinh_do = 'N1'", "Chưa xác định", "trinh_do = 'Chưa xác định'"),
                        fCh("Tình trạng học phí", "Còn nợ", "con_no > 0", "Không nợ", "con_no = 0", "Đã đóng trên 10 triệu", "da_dong > 10000000"),
                        fCh("Lớp học", "Đã có lớp", "so_lop > 0", "Chưa có lớp", "so_lop = 0", "Học từ 3 lớp trở lên", "so_lop >= 3"),
                        fCh("Nhóm tuổi", "Dưới 18", "tuoi < 18", "Từ 18 đến 25", "tuoi BETWEEN 18 AND 25", "Trên 25", "tuoi > 25"),
                        fNm("Tuổi", "tuoi"), fDt("Ngày sinh", "ngay_sinh"),
                        fNm("Số lớp", "so_lop"), fNm("Đã đóng (₫)", "da_dong"), fNm("Còn nợ (₫)", "con_no"),
                        fTx("Họ tên", "ho_ten"), fTx("Số điện thoại", "sdt"), fTx("Giáo viên phụ trách", "gv")},
                new int[]{8, 9}, false));

        /* ---------- 3. GIÁO VIÊN (có lương nên cần quyền Kế toán / Quản lý) ---------- */
        L.add(new DS("giaovien", "GiaoVien", "Giáo viên",
                "SELECT gv.id_giao_vien AS ma, gv.ho_ten AS ho_ten, gv.quoc_tich AS quoc_tich, gv.chung_chi AS chung_chi, "
                        + "gv.so_dien_thoai AS sdt, "
                        + "(SELECT COUNT(*) FROM Quanli q WHERE q.id_giao_vien = gv.id_giao_vien) AS so_lop, "
                        + "(SELECT COUNT(*) FROM Hoc_sinh h WHERE h.id_giao_vien = gv.id_giao_vien) AS so_hs, "
                        + "(SELECT COUNT(*) FROM Lich_hoc lh WHERE lh.id_giao_vien = gv.id_giao_vien AND lh.trang_thai_buoi = 'Dạy') AS buoi_day, "
                        + "(SELECT COALESCE(SUM(p.tong_luong), 0) FROM Phieu_luong p WHERE p.id_giao_vien = gv.id_giao_vien) AS tong_luong "
                        + "FROM Giao_vien gv",
                new String[]{"Mã GV", "Họ tên", "Quốc tịch", "Chứng chỉ", "Số điện thoại", "Số lớp phụ trách",
                        "Số học sinh", "Số buổi đã dạy", "Tổng lương đã nhận (₫)"},
                new String[]{
                        "Tên: A → Z", "ho_ten ASC",
                        "Tên: Z → A", "ho_ten DESC",
                        "Quốc tịch: A → Z", "quoc_tich ASC, ho_ten",
                        "Số buổi dạy: nhiều → ít", "buoi_day DESC, ho_ten",
                        "Số buổi dạy: ít → nhiều", "buoi_day ASC, ho_ten",
                        "Tổng lương: cao → thấp", "tong_luong DESC, ho_ten",
                        "Tổng lương: thấp → cao", "tong_luong ASC, ho_ten",
                        "Số lớp: nhiều → ít", "so_lop DESC, ho_ten",
                        "Số học sinh: nhiều → ít", "so_hs DESC, ho_ten",
                        "Mã giáo viên", "LENGTH(ma), ma"},
                new Flt[]{
                        fCh("Nhóm giáo viên", "Việt Nam", "quoc_tich LIKE '%Việt%'", "Nước ngoài", "quoc_tich NOT LIKE '%Việt%'"),
                        fCh("Chứng chỉ JLPT", "Có N1", "chung_chi LIKE '%N1%'", "Có N2", "chung_chi LIKE '%N2%'",
                                "Có N3", "chung_chi LIKE '%N3%'", "Không có JLPT", "chung_chi NOT LIKE '%JLPT%'"),
                        fCh("Khối lượng dạy", "Chưa dạy buổi nào", "buoi_day = 0", "Đã có buổi dạy", "buoi_day > 0",
                                "Dạy trên 20 buổi", "buoi_day > 20"),
                        fTx("Họ tên", "ho_ten"), fTx("Quốc tịch", "quoc_tich"), fTx("Chứng chỉ", "chung_chi"),
                        fTx("Số điện thoại", "sdt"),
                        fNm("Số lớp phụ trách", "so_lop"), fNm("Số học sinh", "so_hs"),
                        fNm("Số buổi đã dạy", "buoi_day"), fNm("Tổng lương đã nhận (₫)", "tong_luong")},
                new int[]{7, 8}, true));

        /* ---------- 4. NHÂN VIÊN (có lương nên cần quyền) ---------- */
        L.add(new DS("nhanvien", "NhanVien", "Nhân viên",
                "SELECT nv.ma_nhan_vien AS ma, nv.ho_ten AS ho_ten, nv.vai_tro AS vai_tro, nv.luong AS luong, "
                        + "(SELECT COUNT(*) FROM Lop l WHERE l.ma_nhan_vien = nv.ma_nhan_vien) AS so_lop, "
                        + "(SELECT COUNT(*) FROM hoa_don h WHERE h.nv_xac_nhan = nv.ma_nhan_vien) AS hd_xn, "
                        + "(SELECT COUNT(*) FROM Phieu_luong p WHERE p.ma_nhan_vien = nv.ma_nhan_vien) AS phieu_lap "
                        + "FROM Nhan_vien nv",
                new String[]{"Mã NV", "Họ tên", "Chức danh", "Mức lương (₫)", "Số lớp phụ trách",
                        "Hóa đơn đã xác nhận", "Phiếu lương đã lập"},
                new String[]{
                        "Lương: cao → thấp", "luong DESC, ho_ten",
                        "Lương: thấp → cao", "luong ASC, ho_ten",
                        "Tên: A → Z", "ho_ten ASC",
                        "Tên: Z → A", "ho_ten DESC",
                        "Chức danh: A → Z", "vai_tro ASC, ho_ten",
                        "Số lớp phụ trách: nhiều → ít", "so_lop DESC, ho_ten",
                        "Hóa đơn xác nhận: nhiều → ít", "hd_xn DESC, ho_ten",
                        "Mã nhân viên", "LENGTH(ma), ma"},
                new Flt[]{
                        fCh("Chức danh", "Quản lý", "vai_tro LIKE '%Quản lý%'", "Kế toán", "vai_tro LIKE '%Kế toán%'",
                                "Tư vấn", "vai_tro LIKE '%Tư vấn%'", "Thu ngân", "vai_tro LIKE '%Thu ngân%'",
                                "Lễ tân", "vai_tro LIKE '%Lễ tân%'"),
                        fCh("Quyền lập phiếu lương",
                                "Có quyền", "(vai_tro LIKE '%Kế toán%' OR vai_tro LIKE '%Quản lý%' OR vai_tro LIKE '%Giám đốc%')",
                                "Không có quyền", "NOT (vai_tro LIKE '%Kế toán%' OR vai_tro LIKE '%Quản lý%' OR vai_tro LIKE '%Giám đốc%')"),
                        fCh("Mức lương", "Dưới 8 triệu", "luong < 8000000", "Từ 8 đến 12 triệu", "luong BETWEEN 8000000 AND 12000000",
                                "Trên 12 triệu", "luong > 12000000"),
                        fNm("Mức lương (₫)", "luong"),
                        fTx("Họ tên", "ho_ten"), fTx("Chức danh (gõ tay)", "vai_tro"),
                        fNm("Số lớp phụ trách", "so_lop"), fNm("Hóa đơn đã xác nhận", "hd_xn"),
                        fNm("Phiếu lương đã lập", "phieu_lap")},
                new int[]{3}, true));

        /* ---------- 5. LỚP HỌC ---------- */
        L.add(new DS("lop", "LopHoc", "Lớp học",
                "SELECT l.id_lop AS ma, l.ten_lop AS ten_lop, COALESCE(k.ten_khoa_hoc, '—') AS khoa_hoc, "
                        + "COALESCE(k.trinh_do, '—') AS trinh_do, l.nien_khoa AS nien_khoa, COALESCE(l.si_so, 0) AS si_so, "
                        + "COALESCE(nv.ho_ten, l.ma_nhan_vien) AS nv, "
                        + "(SELECT GROUP_CONCAT(g.ho_ten SEPARATOR ', ') FROM Quanli q JOIN Giao_vien g ON g.id_giao_vien = q.id_giao_vien "
                        + "WHERE q.id_lop = l.id_lop) AS giao_vien, COALESCE(k.hoc_phi, 0) AS hoc_phi, "
                        + "(SELECT COUNT(*) FROM Lich_hoc lh WHERE lh.id_lop = l.id_lop AND lh.trang_thai_buoi = 'Dạy') AS so_buoi "
                        + "FROM Lop l LEFT JOIN Khoa_hoc k ON k.ma_khoa_hoc = l.ma_khoa_hoc "
                        + "LEFT JOIN Nhan_vien nv ON nv.ma_nhan_vien = l.ma_nhan_vien",
                new String[]{"Mã lớp", "Tên lớp", "Khóa học", "Trình độ", "Niên khóa", "Sĩ số", "NV phụ trách",
                        "Giáo viên", "Học phí (₫)", "Số buổi đã dạy"},
                new String[]{
                        "Mã lớp", "LENGTH(ma), ma",
                        "Tên lớp: A → Z", "ten_lop ASC",
                        "Sĩ số: đông nhất → ít nhất", "si_so DESC, ma",
                        "Sĩ số: ít nhất → đông nhất", "si_so ASC, ma",
                        "Học phí: cao → thấp", "hoc_phi DESC, ma",
                        "Học phí: thấp → cao", "hoc_phi ASC, ma",
                        "Trình độ: N1 → N5", "FIELD(trinh_do, 'N1', 'N2', 'N3', 'N4', 'N5'), ma",
                        "Trình độ: N5 → N1", "FIELD(trinh_do, 'N5', 'N4', 'N3', 'N2', 'N1'), ma",
                        "Niên khóa: mới nhất trước", "nien_khoa DESC, ma",
                        "Số buổi đã dạy: nhiều → ít", "so_buoi DESC, ma"},
                new Flt[]{
                        fLvl("Trình độ", "trinh_do"),
                        fCh("Niên khóa", "2025-2026", "nien_khoa = '2025-2026'", "2026-2027", "nien_khoa = '2026-2027'"),
                        fCh("Tình trạng sĩ số", "Lớp trống", "si_so = 0", "Dưới 10 học viên", "si_so < 10",
                                "Từ 10 học viên trở lên", "si_so >= 10"),
                        fNm("Sĩ số", "si_so"), fNm("Học phí (₫)", "hoc_phi"), fNm("Số buổi đã dạy", "so_buoi"),
                        fTx("Tên lớp", "ten_lop"), fTx("Khóa học", "khoa_hoc"),
                        fTx("Nhân viên phụ trách", "nv"), fTx("Giáo viên", "giao_vien")},
                new int[]{5, 9}, false));

        /* ---------- 6. KHÓA HỌC ---------- */
        L.add(new DS("khoahoc", "KhoaHoc", "Khóa học",
                "SELECT k.ma_khoa_hoc AS ma, k.ten_khoa_hoc AS ten, k.trinh_do AS trinh_do, k.chuyen_de AS chuyen_de, "
                        + "k.giao_trinh AS giao_trinh, k.hoc_phi AS hoc_phi, "
                        + "(SELECT COUNT(*) FROM Lop l WHERE l.ma_khoa_hoc = k.ma_khoa_hoc) AS so_lop, "
                        + "(SELECT COALESCE(SUM(l.si_so), 0) FROM Lop l WHERE l.ma_khoa_hoc = k.ma_khoa_hoc) AS so_hv, "
                        + "(SELECT COALESCE(SUM(d.khoan_thu), 0) FROM hoa_don d WHERE d.trang_thai = 'Đã nộp' "
                        + "AND SUBSTRING_INDEX(d.ma_ghi_danh, ':', -1) = k.ma_khoa_hoc) AS doanh_thu "
                        + "FROM Khoa_hoc k",
                new String[]{"Mã khóa", "Tên khóa học", "Trình độ", "Chuyên đề", "Giáo trình", "Học phí (₫)",
                        "Số lớp", "Số học viên", "Doanh thu đã thu (₫)"},
                new String[]{
                        "Học phí: đắt nhất → rẻ nhất", "hoc_phi DESC, ma",
                        "Học phí: rẻ nhất → đắt nhất", "hoc_phi ASC, ma",
                        "Doanh thu: cao → thấp", "doanh_thu DESC, ma",
                        "Doanh thu: thấp → cao", "doanh_thu ASC, ma",
                        "Số học viên: đông nhất → ít nhất", "so_hv DESC, ma",
                        "Số lớp: nhiều → ít", "so_lop DESC, ma",
                        "Tên khóa học: A → Z", "ten ASC",
                        "Tên khóa học: Z → A", "ten DESC",
                        "Trình độ: N1 → N5", "FIELD(trinh_do, 'N1', 'N2', 'N3', 'N4', 'N5'), ma",
                        "Trình độ: N5 → N1", "FIELD(trinh_do, 'N5', 'N4', 'N3', 'N2', 'N1'), ma",
                        "Mã khóa", "LENGTH(ma), ma"},
                new Flt[]{
                        fLvl("Trình độ", "trinh_do"),
                        fCh("Mức học phí", "Dưới 3 triệu", "hoc_phi < 3000000", "Từ 3 đến 5 triệu", "hoc_phi BETWEEN 3000000 AND 5000000",
                                "Trên 5 triệu", "hoc_phi > 5000000"),
                        fCh("Tình trạng mở lớp", "Đã mở lớp", "so_lop > 0", "Chưa mở lớp", "so_lop = 0"),
                        fCh("Doanh thu", "Chưa có doanh thu", "doanh_thu = 0", "Đã có doanh thu", "doanh_thu > 0"),
                        fNm("Học phí (₫)", "hoc_phi"), fNm("Số lớp", "so_lop"),
                        fNm("Số học viên", "so_hv"), fNm("Doanh thu (₫)", "doanh_thu"),
                        fTx("Tên khóa học", "ten"), fTx("Chuyên đề", "chuyen_de"), fTx("Giáo trình", "giao_trinh")},
                new int[]{6, 7, 8}, false));

        /* ---------- 7. LỊCH HỌC ---------- */
        L.add(new DS("lich", "LichHoc", "Lịch học",
                "SELECT lh.ma_lich_hoc AS ma, COALESCE(l.ten_lop, lh.id_lop) AS lop, "
                        + "COALESCE(gv.ho_ten, lh.id_giao_vien) AS giao_vien, lh.ngay_day AS ngay_day, lh.ca_hoc AS ca_hoc, "
                        + "lh.phong_hoc AS phong, lh.noi_dung AS noi_dung, lh.trang_thai_buoi AS trang_thai, "
                        + "COALESCE(k.trinh_do, '—') AS cap_do FROM Lich_hoc lh "
                        + "LEFT JOIN Lop l ON l.id_lop = lh.id_lop "
                        + "LEFT JOIN Khoa_hoc k ON k.ma_khoa_hoc = l.ma_khoa_hoc "
                        + "LEFT JOIN Giao_vien gv ON gv.id_giao_vien = lh.id_giao_vien",
                new String[]{"Mã lịch", "Lớp", "Giáo viên", "Ngày dạy", "Ca học", "Phòng", "Nội dung", "Trạng thái", "Cấp độ"},
                new String[]{
                        "Buổi sắp tới trước, rồi đã qua", "(ngay_day < CURDATE()), IF(ngay_day >= CURDATE(), ngay_day, NULL), ngay_day DESC",
                        "Ngày dạy: mới → cũ", "ngay_day DESC, ma",
                        "Ngày dạy: cũ → mới", "ngay_day ASC, ma",
                        "Giáo viên: A → Z", "giao_vien ASC, ngay_day",
                        "Lớp: A → Z", "lop ASC, ngay_day",
                        "Phòng học: A → Z", "phong ASC, ngay_day",
                        "Cấp độ: N1 → N5", "FIELD(cap_do, 'N1', 'N2', 'N3', 'N4', 'N5'), ngay_day",
                        "Cấp độ: N5 → N1", "FIELD(cap_do, 'N5', 'N4', 'N3', 'N2', 'N1'), ngay_day"},
                new Flt[]{
                        fCh("Trạng thái buổi", "Dạy", "trang_thai = 'Dạy'", "Nghỉ", "trang_thai = 'Nghỉ'"),
                        fCh("Thời gian", "Hôm nay", "DATE(ngay_day) = CURDATE()",
                                "Sắp tới (từ hôm nay)", "ngay_day >= CURDATE()", "Đã qua", "ngay_day < CURDATE()",
                                "Tuần này", "YEARWEEK(ngay_day, 1) = YEARWEEK(CURDATE(), 1)",
                                "Tháng này", "(YEAR(ngay_day) = YEAR(CURDATE()) AND MONTH(ngay_day) = MONTH(CURDATE()))"),
                        fLvl("Cấp độ", "cap_do"),
                        fDt("Ngày dạy", "ngay_day"),
                        fTx("Lớp", "lop"), fTx("Giáo viên", "giao_vien"), fTx("Ca học", "ca_hoc"),
                        fTx("Phòng học", "phong"), fTx("Nội dung", "noi_dung")},
                new int[]{}, false));

        /* ---------- 8. PHIẾU LƯƠNG (cần quyền) ---------- */
        L.add(new DS("phieuluong", "PhieuLuong", "Phiếu lương giáo viên",
                "SELECT p.ma_phieu_luong AS ma, COALESCE(gv.ho_ten, p.id_giao_vien) AS giao_vien, p.tu_ngay AS tu_ngay, "
                        + "p.den_ngay AS den_ngay, p.ngay_tra AS ngay_tra, p.tong_luong AS tong_luong, "
                        + "COALESCE(nv.ho_ten, p.ma_nhan_vien) AS nguoi_lap, "
                        + "(SELECT COUNT(*) FROM Lich_hoc lh WHERE lh.id_giao_vien = p.id_giao_vien AND lh.trang_thai_buoi = 'Dạy' "
                        + "AND lh.ngay_day >= p.tu_ngay AND lh.ngay_day < p.den_ngay + INTERVAL 1 DAY) AS so_buoi "
                        + "FROM Phieu_luong p LEFT JOIN Giao_vien gv ON gv.id_giao_vien = p.id_giao_vien "
                        + "LEFT JOIN Nhan_vien nv ON nv.ma_nhan_vien = p.ma_nhan_vien",
                new String[]{"Mã phiếu", "Giáo viên", "Từ ngày", "Đến ngày", "Ngày trả", "Tổng lương (₫)",
                        "Người lập", "Số buổi dạy trong kỳ"},
                new String[]{
                        "Ngày trả: mới nhất trước", "ngay_tra DESC, ma",
                        "Ngày trả: cũ nhất trước", "ngay_tra ASC, ma",
                        "Tổng lương: cao → thấp", "tong_luong DESC, ma",
                        "Tổng lương: thấp → cao", "tong_luong ASC, ma",
                        "Giáo viên: A → Z", "giao_vien ASC, ngay_tra DESC",
                        "Giáo viên: Z → A", "giao_vien DESC, ngay_tra DESC",
                        "Số buổi dạy: nhiều → ít", "so_buoi DESC, ma",
                        "Người lập: A → Z", "nguoi_lap ASC, ngay_tra DESC"},
                new Flt[]{
                        fCh("Thời gian trả lương",
                                "Tháng này", "(YEAR(ngay_tra) = YEAR(CURDATE()) AND MONTH(ngay_tra) = MONTH(CURDATE()))",
                                "3 tháng gần nhất", "ngay_tra >= CURDATE() - INTERVAL 3 MONTH",
                                "6 tháng gần nhất", "ngay_tra >= CURDATE() - INTERVAL 6 MONTH",
                                "Năm nay", "YEAR(ngay_tra) = YEAR(CURDATE())"),
                        fCh("Mức lương", "Dưới 8 triệu", "tong_luong < 8000000", "Từ 8 đến 10 triệu", "tong_luong BETWEEN 8000000 AND 10000000",
                                "Trên 10 triệu", "tong_luong > 10000000"),
                        fDt("Ngày trả", "ngay_tra"), fDt("Từ ngày (đầu kỳ)", "tu_ngay"),
                        fNm("Tổng lương (₫)", "tong_luong"), fNm("Số buổi dạy trong kỳ", "so_buoi"),
                        fTx("Giáo viên", "giao_vien"), fTx("Người lập", "nguoi_lap")},
                new int[]{5, 7}, true));

        /* ---------- 9. KẾT QUẢ THI ---------- */
        L.add(new DS("ketquathi", "KetQuaThi", "Kết quả thi",
                "SELECT kq.ma_bai_lam AS ma, COALESCE(hs.ho_ten, kq.ma_sinh_vien) AS hoc_sinh, kq.trinh_do AS trinh_do, "
                        + "kq.diem_doc_hieu AS doc_hieu, kq.diem_tu_vung AS tu_vung, kq.diem_nghe AS nghe, "
                        + "kq.tong_diem AS tong_diem, COALESCE(kq.tinh_trang, '—') AS ket_qua "
                        + "FROM Ket_qua_thi kq LEFT JOIN Hoc_sinh hs ON hs.ma_sinh_vien = kq.ma_sinh_vien",
                new String[]{"Mã bài làm", "Học sinh", "Trình độ", "Đọc hiểu", "Từ vựng", "Nghe", "Tổng điểm", "Kết quả"},
                new String[]{
                        "Tổng điểm: cao → thấp", "tong_diem DESC, ma",
                        "Tổng điểm: thấp → cao", "tong_diem ASC, ma",
                        "Điểm nghe: cao → thấp", "nghe DESC, ma",
                        "Điểm đọc hiểu: cao → thấp", "doc_hieu DESC, ma",
                        "Tên học sinh: A → Z", "hoc_sinh ASC, trinh_do",
                        "Trình độ: N1 → N5", "FIELD(trinh_do, 'N1', 'N2', 'N3', 'N4', 'N5'), tong_diem DESC",
                        "Trình độ: N5 → N1", "FIELD(trinh_do, 'N5', 'N4', 'N3', 'N2', 'N1'), tong_diem DESC",
                        "ĐẠT xếp trước", "FIELD(ket_qua, 'ĐẠT', 'TRƯỢT'), tong_diem DESC"},
                new Flt[]{
                        fCh("Kết quả", "ĐẠT", "ket_qua = 'ĐẠT'", "TRƯỢT", "ket_qua = 'TRƯỢT'"),
                        fLvl("Trình độ", "trinh_do"),
                        fCh("Mức điểm", "Từ 100 điểm trở lên", "tong_diem >= 100", "Từ 80 đến 99", "tong_diem BETWEEN 80 AND 99",
                                "Dưới 80", "tong_diem < 80"),
                        fNm("Tổng điểm", "tong_diem"), fNm("Điểm đọc hiểu", "doc_hieu"),
                        fNm("Điểm từ vựng", "tu_vung"), fNm("Điểm nghe", "nghe"),
                        fTx("Học sinh", "hoc_sinh")},
                new int[]{}, false));

        return L;
    }

    /* ---------- tiện ích nhỏ cho hộp thoại ---------- */

    private static Long parseNum(String s, String label) {
        String d = s.replaceAll("[^0-9]", "");
        if (d.isEmpty()) return null;
        try { return Long.parseLong(d); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(label + ": số quá lớn."); }
    }

    private static LocalDate parseDay(String s, String label) {
        String t = s.trim();
        if (t.isEmpty()) return null;
        try { return LocalDate.parse(t, IN_FMT); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(label + ": ngày phải đúng dạng dd/MM/yyyy."); }
    }

    private static void addCell(JPanel p, GridBagConstraints c, int col, int row, String label, JComponent field) {
        int l = col == 0 ? 0 : 10, r = col == 0 ? 10 : 0;
        c.gridx = col;
        c.gridwidth = 1;
        c.gridy = row * 2;
        c.insets = new Insets(0, l, 4, r);
        p.add(lbl(label, font(Font.BOLD, 12), MUTED), c);
        c.gridy = row * 2 + 1;
        c.insets = new Insets(0, l, 12, r);
        p.add(field, c);
    }

    /** Dựng form sắp xếp + bộ lọc cho một loại dữ liệu */
    private static FormUi buildForm(DS d) {
        FormUi f = new FormUi();
        f.panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTH;

        f.sort = new RoundCombo(d.sortNames);
        f.top = new StyledField("").withPlaceholder("Để trống = lấy tất cả", null);
        int row = 0;
        addCell(f.panel, c, 0, row, "Sắp xếp theo", f.sort);
        addCell(f.panel, c, 1, row, "Chỉ lấy N dòng đầu (Top N)", f.top);
        row++;

        c.gridx = 0;
        c.gridy = row * 2;
        c.gridwidth = 2;
        c.insets = new Insets(4, 0, 10, 0);
        f.panel.add(lbl("BỘ LỌC  (để trống hoặc chọn \"Tất cả\" = không lọc)", font(Font.BOLD, 11), ACCENT_D), c);
        row++;

        int col = 0;
        for (Flt fl : d.filters) {
            JComponent comp;
            String shown = fl.kind == F_DATE ? fl.label + " (dd/MM/yyyy)" : fl.label;
            switch (fl.kind) {
                case F_CHOICE: {
                    final RoundCombo cb = new RoundCombo(fl.names);
                    f.conds.add((par, dsc) -> {
                        int i = cb.getSelectedIndex();
                        if (i <= 0) return null;
                        dsc.add(fl.label + ": " + fl.names[i]);
                        return fl.sqls[i];
                    });
                    comp = cb;
                    break;
                }
                case F_TEXT: {
                    final StyledField tf = new StyledField("").withPlaceholder("Chứa chữ...", null);
                    f.conds.add((par, dsc) -> {
                        String t = tf.getText().trim();
                        if (t.isEmpty()) return null;
                        par.add("%" + t + "%");
                        dsc.add(fl.label + " chứa \"" + t + "\"");
                        return "(" + fl.expr + " LIKE ?)";
                    });
                    comp = tf;
                    break;
                }
                case F_NUM: {
                    final StyledField a = new StyledField("").withPlaceholder("Từ", null);
                    final StyledField b = new StyledField("").withPlaceholder("Đến", null);
                    JPanel pr = new JPanel(new GridLayout(1, 2, 8, 0));
                    pr.setOpaque(false);
                    pr.add(a);
                    pr.add(b);
                    f.conds.add((par, dsc) -> {
                        Long lo = parseNum(a.getText(), fl.label), hi = parseNum(b.getText(), fl.label);
                        if (lo == null && hi == null) return null;
                        if (lo != null && hi != null && lo > hi)
                            throw new IllegalArgumentException(fl.label + ": giá trị \"Từ\" đang lớn hơn \"Đến\".");
                        List<String> parts = new ArrayList<>();
                        String txt = "";
                        if (lo != null) { par.add(lo); parts.add(fl.expr + " >= ?"); txt += "từ " + vnd(lo).replace(" ₫", "") + " "; }
                        if (hi != null) { par.add(hi); parts.add(fl.expr + " <= ?"); txt += "đến " + vnd(hi).replace(" ₫", ""); }
                        dsc.add(fl.label + ": " + txt.trim());
                        return "(" + String.join(" AND ", parts) + ")";
                    });
                    comp = pr;
                    break;
                }
                default: {
                    final StyledField a = new StyledField("").withPlaceholder("Từ ngày", null);
                    final StyledField b = new StyledField("").withPlaceholder("Đến ngày", null);
                    JPanel pr = new JPanel(new GridLayout(1, 2, 8, 0));
                    pr.setOpaque(false);
                    pr.add(a);
                    pr.add(b);
                    f.conds.add((par, dsc) -> {
                        LocalDate lo = parseDay(a.getText(), fl.label), hi = parseDay(b.getText(), fl.label);
                        if (lo == null && hi == null) return null;
                        if (lo != null && hi != null && hi.isBefore(lo))
                            throw new IllegalArgumentException(fl.label + ": ngày \"Đến\" đang trước ngày \"Từ\".");
                        List<String> parts = new ArrayList<>();
                        String txt = "";
                        if (lo != null) { par.add(Timestamp.valueOf(lo.atStartOfDay())); parts.add(fl.expr + " >= ?"); txt += "từ " + lo.format(D_FMT) + " "; }
                        if (hi != null) { par.add(Timestamp.valueOf(hi.plusDays(1).atStartOfDay())); parts.add(fl.expr + " < ?"); txt += "đến " + hi.format(D_FMT); }
                        dsc.add(fl.label + ": " + txt.trim());
                        return "(" + String.join(" AND ", parts) + ")";
                    });
                    comp = pr;
                }
            }
            addCell(f.panel, c, col, row, shown, comp);
            if (++col == 2) { col = 0; row++; }
        }
        if (col == 1) row++;

        if (d.sums.length > 0) {
            JCheckBox chk = new JCheckBox("Thêm dòng TỔNG CỘNG ở cuối bảng", true);
            chk.setOpaque(false);
            chk.setFont(font(Font.PLAIN, 13));
            chk.setForeground(INK);
            chk.setFocusPainted(false);
            f.total = chk;
            c.gridx = 0;
            c.gridy = row * 2;
            c.gridwidth = 2;
            c.insets = new Insets(0, 0, 8, 0);
            f.panel.add(chk, c);
        }
        return f;
    }

    private static String exportKey(String pageKey) {
        switch (pageKey) {
            case "HocSinh": return "hocsinh";
            case "Lop": return "lop";
            case "KhoaHoc": return "khoahoc";
            case "Lich": return "lich";
            case "Luong": return "phieuluong";
            default: return "hoadon";
        }
    }

    private static String fmtTime(LocalDateTime t) {
        return t.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? t.format(D_FMT) : t.format(DT_FMT);
    }

    /** Đổi giá trị trong CSDL sang giá trị ghi vào Excel (số -> số, ngày -> chữ dd/MM/yyyy) */
    private static Object xcell(Object v) {
        if (v == null) return "";
        if (v instanceof Timestamp) return fmtTime(((Timestamp) v).toLocalDateTime());
        if (v instanceof LocalDateTime) return fmtTime((LocalDateTime) v);
        if (v instanceof java.sql.Date) return ((java.sql.Date) v).toLocalDate().format(D_FMT);
        if (v instanceof LocalDate) return ((LocalDate) v).format(D_FMT);
        if (v instanceof Number) return Long.valueOf(((Number) v).longValue());
        return v.toString();
    }

    /** Hộp thoại chọn dữ liệu / sắp xếp / bộ lọc rồi xuất Excel */
    private void exportCenter(String preselect) {
        if (!valid) { toast("Bạn không có quyền xuất dữ liệu.", T_ERR); return; }
        List<DS> list = new ArrayList<>();
        for (DS d : exportSpecs()) if (!d.sensitive || canPayroll()) list.add(d);
        String[] names = new String[list.size()];
        int start = 0;
        for (int i = 0; i < names.length; i++) {
            names[i] = list.get(i).title;
            if (list.get(i).key.equals(preselect)) start = i;
        }
        RoundCombo cbData = new RoundCombo(names);
        CardLayout cl = new CardLayout();
        ScrollPanel cards = new ScrollPanel(cl);
        List<FormUi> forms = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            FormUi fu = buildForm(list.get(i));
            forms.add(fu);
            cards.add(fu.panel, String.valueOf(i));
        }
        cbData.addActionListener(e -> cl.show(cards, String.valueOf(cbData.getSelectedIndex())));
        cbData.setSelectedIndex(start);
        cl.show(cards, String.valueOf(start));

        JScrollPane sp = new JScrollPane(cards);
        slim(sp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setPreferredSize(new Dimension(700, 430));
        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setOpaque(false);
        body.add(vform(new String[]{"Bạn muốn xuất dữ liệu gì?"}, new JComponent[]{cbData}), BorderLayout.NORTH);
        body.add(sp, BorderLayout.CENTER);

        final ExportJob[] box = new ExportJob[1];
        boolean ok = dialog("Xuất dữ liệu ra Excel", "Chọn dữ liệu, cách sắp xếp và các điều kiện lọc",
                body, "Chọn nơi lưu", GREEN, () -> {
                    DS ds = list.get(cbData.getSelectedIndex());
                    FormUi fu = forms.get(cbData.getSelectedIndex());
                    List<Object> params = new ArrayList<>();
                    List<String> desc = new ArrayList<>(), where = new ArrayList<>();
                    try {
                        for (Cond cd : fu.conds) {
                            String s = cd.build(params, desc);
                            if (s != null) where.add(s);
                        }
                    } catch (IllegalArgumentException ex) {
                        return ex.getMessage();
                    }
                    int limit = 0;
                    if (!fu.top.getText().trim().isEmpty()) {
                        String digits = fu.top.getText().replaceAll("[^0-9]", "");
                        if (digits.isEmpty()) return "Top N phải là một số nguyên dương.";
                        try { limit = Integer.parseInt(digits); }
                        catch (NumberFormatException ex) { return "Top N quá lớn."; }
                        if (limit < 1 || limit > 100000) return "Top N phải từ 1 đến 100.000.";
                    }
                    int si = fu.sort.getSelectedIndex();
                    String sql = "SELECT * FROM (" + ds.sql + ") t"
                            + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                            + " ORDER BY " + ds.sortSql[si]
                            + (limit > 0 ? " LIMIT " + limit : "");
                    box[0] = new ExportJob(ds, sql, params.toArray(), desc, ds.sortNames[si], limit,
                            fu.total != null && fu.total.isSelected());
                    return null;
                }, true);
        if (!ok || box[0] == null) return;
        runExport(box[0]);
    }

    private void runExport(ExportJob j) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(j.ds.file + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File chosen = fc.getSelectedFile();
        if (!chosen.getName().toLowerCase().endsWith(".xlsx"))
            chosen = new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        final File out = chosen;
        if (out.exists() && JOptionPane.showConfirmDialog(this, "File đã tồn tại. Ghi đè?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        toast("Đang xuất dữ liệu...", T_INFO);

        Callable<String> job = () -> {
            List<Object[]> rows = Db.query(j.sql, rs -> {
                int n = rs.getMetaData().getColumnCount();
                Object[] o = new Object[n];
                for (int i = 0; i < n; i++) o[i] = xcell(rs.getObject(i + 1));
                return o;
            }, j.params);
            if (rows.isEmpty()) return null;

            Xlsx x = new Xlsx();
            Xlsx.Sheet s = x.sheet(j.ds.title);
            s.row(true, j.ds.title.toUpperCase(new Locale("vi")));
            s.row(false, "Xuất lúc: " + LocalDateTime.now().format(DT_FMT) + "  •  Người xuất: " + hoTen + " (" + maNhanVien + ")");
            s.row(false, "Sắp xếp: " + j.sortName);
            s.row(false, "Bộ lọc: " + (j.desc.isEmpty() ? "Không lọc (lấy tất cả)" : String.join("; ", j.desc))
                    + (j.top > 0 ? "  •  Giới hạn: " + j.top + " dòng đầu" : ""));
            s.row(false, "Số dòng: " + rows.size());
            s.row(false);
            s.row(true, (Object[]) j.ds.heads);
            for (Object[] r : rows) s.row(false, r);
            if (j.total && j.ds.sums.length > 0) {
                Object[] t = new Object[j.ds.heads.length];
                t[0] = "TỔNG CỘNG";
                for (int c : j.ds.sums) {
                    long sum = 0;
                    for (Object[] r : rows) if (r[c] instanceof Long) sum += (Long) r[c];
                    t[c] = sum;
                }
                s.row(true, t);
            }
            x.save(out);
            return "Đã xuất " + rows.size() + " dòng: " + out.getName();
        };
        async(job, msg -> {
            if (msg == null) toast("Không có dòng nào khớp bộ lọc, chưa tạo file.", T_WARN);
            else toast(msg, T_OK);
        });
    }


    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        final String ma = args.length > 0 ? args[0] : "NV01";
        SwingUtilities.invokeLater(() -> new StaffDashboardUI(ma).setVisible(true));
    }
}
