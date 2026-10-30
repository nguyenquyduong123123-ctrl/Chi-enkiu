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
                    "ALTER TABLE hoa_don ADD COLUMN nv_xac_nhan VARCHAR(50) NULL DEFAULT NULL"};
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
            sorter.setRowFilter(fs.isEmpty() ? null : RowFilter.andFilter(fs));
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
        Card cardChart = new Card("Doanh thu 6 tháng gần nhất", "Tính theo hóa đơn đã nộp (rê chuột vào cột để xem chi tiết)", null);
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
                "Hình Thức", "Ngày Nộp", "Trạng Thái"}, "Chưa có hóa đơn nào", cols(5, 7), false);
        g.load(INVOICE_SELECT + " ORDER BY h.ngay_lap DESC",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), new Money(rs.getLong(4)),
                        when(rs, 5, true), nz(rs.getString(6)), when(rs, 7, true), rs.getString(8)});

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
        return listPage("Danh sách học sinh", "Tất cả học sinh trong trung tâm",
                new String[]{"Mã SV", "Họ Tên", "Ngày Sinh", "Trình Độ", "Số Điện Thoại", "GV Phụ Trách"},
                "SELECT hs.ma_sinh_vien, hs.ho_ten, hs.ngay_sinh, hs.trinh_do, hs.so_dien_thoai, "
                        + "COALESCE(gv.ho_ten, 'Chưa phân công') FROM Hoc_sinh hs "
                        + "LEFT JOIN Giao_vien gv ON hs.id_giao_vien = gv.id_giao_vien ORDER BY hs.ma_sinh_vien",
                rs -> new Object[]{rs.getString(1), rs.getString(2), when(rs, 3, false),
                        rs.getString(4), rs.getString(5), rs.getString(6)},
                "Chưa có học sinh nào", cols(3), null, -1);
    }

    private JComponent createClassPage() {
        return listPage("Danh sách lớp học", "Các lớp và nhân viên phụ trách",
                new String[]{"Mã Lớp", "Tên Lớp", "Khóa Học", "Trình Độ", "Niên Khóa", "Sĩ Số", "NV Phụ Trách"},
                "SELECT l.id_lop, l.ten_lop, k.ten_khoa_hoc, k.trinh_do, l.nien_khoa, l.si_so, "
                        + "COALESCE(nv.ho_ten, l.ma_nhan_vien) FROM Lop l "
                        + "LEFT JOIN Khoa_hoc k ON l.ma_khoa_hoc = k.ma_khoa_hoc "
                        + "LEFT JOIN Nhan_vien nv ON l.ma_nhan_vien = nv.ma_nhan_vien ORDER BY l.id_lop",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), intObj(rs, 6), rs.getString(7)},
                "Chưa có lớp học nào", cols(3), null, -1);
    }

    private JComponent createCoursePage() {
        return listPage("Danh sách khóa học", "Chương trình đào tạo và học phí",
                new String[]{"Mã Khóa", "Tên Khóa Học", "Trình Độ", "Chuyên Đề", "Giáo Trình", "Học Phí", "Số Lớp"},
                "SELECT k.ma_khoa_hoc, k.ten_khoa_hoc, k.trinh_do, k.chuyen_de, k.giao_trinh, k.hoc_phi, "
                        + "(SELECT COUNT(*) FROM Lop l WHERE l.ma_khoa_hoc = k.ma_khoa_hoc) "
                        + "FROM Khoa_hoc k ORDER BY k.trinh_do DESC, k.ma_khoa_hoc",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), new Money(rs.getLong(6)), intObj(rs, 7)},
                "Chưa có khóa học nào", cols(2), null, -1);
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
