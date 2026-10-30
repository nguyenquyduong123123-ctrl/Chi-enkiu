import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Cổng sinh viên S-LINK.
 *
 * Quy tắc nghiệp vụ chính:
 *  - Chỉ đăng ký khóa cùng cấp hoặc tối đa MAX_LEVEL_STEP_UP cấp cao hơn trình độ hiện tại.
 *  - Mỗi sinh viên chỉ đăng ký một khóa một lần (kiểm tra trong transaction).
 *  - Hóa đơn mới luôn ở trạng thái "Chưa nộp"; chỉ hủy được hóa đơn của chính mình và chưa nộp.
 *  - Lịch học chỉ hiện cho các khóa đã được xác nhận thanh toán.
 *  - Kết quả ĐẠT/TRƯỢT lấy từ cột tinh_trang trong CSDL (đúng tiêu chí từng cấp N5-N1).
 */
public class StudentDashboardUI extends JFrame {

    // ===================== QUY TẮC NGHIỆP VỤ =====================
    private static final int MAX_LEVEL_STEP_UP = 1;
    private static final String ST_PAID = "Đã nộp";
    private static final String ST_UNPAID = "Chưa nộp";
    private static final String COND_OK = "Phù hợp";
    private static final String COND_LOW = "Đã vượt trình độ";
    private static final String COND_HIGH = "Chưa đủ trình độ";
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");

    private static final String OWN_INV =
            "(ma_ghi_danh = ? OR LEFT(ma_ghi_danh, CHAR_LENGTH(?) + 1) = CONCAT(?, ':'))";

    // ===================== BẢNG MÀU (đồng bộ AdminDashboard) =====================
    private static final Color NAVY = new Color(18, 30, 74);
    private static final Color NAVY_DARK = new Color(12, 21, 56);
    private static final Color NAV_ACTIVE = new Color(52, 88, 224);
    private static final Color NAV_TEXT = new Color(176, 188, 224);
    private static final Color COLOR_PRIMARY = new Color(59, 91, 240);
    private static final Color COLOR_PURPLE = new Color(124, 58, 237);
    private static final Color COLOR_BG = new Color(245, 247, 252);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_BORDER_DARK = new Color(203, 213, 225);
    private static final Color COLOR_TEXT = new Color(15, 23, 42);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private static final Color COLOR_SUCCESS_DARK = new Color(5, 150, 105);
    private static final Color COLOR_WARNING = new Color(245, 158, 11);
    private static final Color COLOR_WARNING_DARK = new Color(194, 112, 5);
    private static final Color COLOR_DANGER = new Color(220, 38, 38);
    private static final Color COLOR_SELECT = new Color(219, 230, 253);
    private static final Color COLOR_HOVER_ROW = new Color(237, 242, 253);
    private static final Color COLOR_STRIPE = new Color(250, 251, 254);

    private static final Color TINT_BLUE = new Color(224, 231, 255);
    private static final Color TINT_GREEN = new Color(209, 250, 229);
    private static final Color TINT_ORANGE = new Color(254, 243, 199);
    private static final Color TINT_PURPLE = new Color(237, 233, 254);

    private static final String FF = pickFont("Helvetica Neue", "Segoe UI", "Arial");

    // ===================== DỮ LIỆU SINH VIÊN =====================
    private final String maSinhVien;
    private String tenSinhVien = "";
    private String ngaySinh = "";
    private String trinhDo = "";
    private String soDienThoai = "";
    private String idGiaoVien = "";
    private String tenGiaoVien = "Chưa phân công";
    private String dbName = "";

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private final List<NavButton> navButtons = new ArrayList<>();
    private final Map<String, NavButton> navByCard = new HashMap<>();
    private final Map<String, Supplier<JPanel>> builders = new LinkedHashMap<>();
    private final Map<String, JPanel> currentPanels = new HashMap<>();

    @FunctionalInterface
    private interface RowMapper {
        Object[] map(ResultSet rs) throws SQLException;
    }

    private static final class BizException extends Exception {
        BizException(String msg) {
            super(msg);
        }
    }

    // ===================== KHỞI TẠO =====================

    public StudentDashboardUI() {
        this("SV01");
    }

    public StudentDashboardUI(String maSinhVien) {
        String id = maSinhVien == null ? "" : maSinhVien.trim();
        if (id.isEmpty()) throw new IllegalArgumentException("Mã sinh viên không được để trống.");
        this.maSinhVien = id;

        loadStudentProfile();      // ném lỗi nếu mất kết nối hoặc không có sinh viên
        ensureInvoiceStatusColumn();

        setTitle("S-LINK | Cổng sinh viên - " + tenSinhVien);
        setSize(1340, 820);
        setMinimumSize(new Dimension(1180, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(COLOR_BG);

        builders.put("Dashboard", this::createDashboardPanel);
        builders.put("DangKy", this::createDangKyPanel);
        builders.put("TKB", this::createTKBPanel);
        builders.put("BangDiem", this::createBangDiemPanel);
        builders.put("HocPhi", this::createHocPhiPanel);
        builders.put("Profile", this::createProfilePanel);

        add(createTopBar(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(mainContentPanel, BorderLayout.CENTER);
        right.add(createStatusBar(), BorderLayout.SOUTH);
        add(right, BorderLayout.CENTER);

        navigate("Dashboard");
    }

    /** Mỗi lần mở trang sẽ dựng lại panel => dữ liệu luôn mới. */
    private void showCard(String name) {
        Supplier<JPanel> b = builders.get(name);
        if (b == null) return;
        try {
            JPanel fresh = b.get();
            JPanel old = currentPanels.get(name);
            if (old != null) mainContentPanel.remove(old);
            currentPanels.put(name, fresh);
            mainContentPanel.add(fresh, name);
            mainContentPanel.revalidate();
            mainContentPanel.repaint();
            cardLayout.show(mainContentPanel, name);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể tải trang này.\n" + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void navigate(String card) {
        NavButton b = navByCard.get(card);
        if (b != null) setNavActive(b);
        showCard(card);
    }

    // ===================== DATABASE HELPERS =====================

    private void showError(String msg, Exception e) {
        JOptionPane.showMessageDialog(this, msg + "\n" + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    private void loadRows(AppModel model, String sql, RowMapper mapper, Object... params) {
        model.setRowCount(0);
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) model.addRow(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            showError("Lỗi truy vấn dữ liệu:", e);
        }
    }

    private Object[] queryOne(String sql, RowMapper mapper, Object... params) {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return null;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapper.map(rs) : null;
                }
            }
        } catch (SQLException e) {
            showError("Lỗi truy vấn dữ liệu:", e);
            return null;
        }
    }

    private void loadStudentProfile() {
        String sql = "SELECT hs.ho_ten, hs.ngay_sinh, hs.trinh_do, hs.so_dien_thoai, hs.id_giao_vien, gv.ho_ten AS ten_gv "
                + "FROM Hoc_sinh hs LEFT JOIN Giao_vien gv ON hs.id_giao_vien = gv.id_giao_vien "
                + "WHERE hs.ma_sinh_vien = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) throw new IllegalStateException("Không thể kết nối cơ sở dữ liệu MySQL.");
            dbName = nz(conn.getCatalog());
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, maSinhVien);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Không tìm thấy sinh viên có mã \"" + maSinhVien + "\".");
                    }
                    tenSinhVien = nz(rs.getString("ho_ten"));
                    ngaySinh = fmt(rs.getTimestamp("ngay_sinh"), "dd/MM/yyyy");
                    trinhDo = nz(rs.getString("trinh_do"));
                    soDienThoai = nz(rs.getString("so_dien_thoai"));
                    idGiaoVien = nz(rs.getString("id_giao_vien"));
                    String gv = rs.getString("ten_gv");
                    tenGiaoVien = gv != null ? gv : "Chưa phân công";
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi SQL: " + e.getMessage(), e);
        }
    }

    /** Bảo đảm hoa_don có cột trang_thai (schema gốc chưa có). */
    private void ensureInvoiceStatusColumn() {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return;
            boolean has;
            try (ResultSet rs = c.getMetaData().getColumns(c.getCatalog(), null, "hoa_don", "trang_thai")) {
                has = rs.next();
            }
            if (!has) {
                try (java.sql.Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE hoa_don ADD COLUMN trang_thai VARCHAR(20) NOT NULL DEFAULT 'Chưa nộp'");
                }
            }
        } catch (SQLException ignored) {
            // nếu không đủ quyền ALTER, các truy vấn sau sẽ báo lỗi rõ ràng
        }
    }

    private static final class Enrollment {
        final String maHoaDon, maKhoa, status;
        final long fee;
        final Timestamp ngayLap;

        Enrollment(String maHoaDon, String maKhoa, long fee, Timestamp ngayLap, String status) {
            this.maHoaDon = maHoaDon;
            this.maKhoa = maKhoa;
            this.fee = fee;
            this.ngayLap = ngayLap;
            this.status = status;
        }
    }

    private List<Enrollment> loadEnrollments() {
        List<Enrollment> out = new ArrayList<>();
        String sql = "SELECT ma_hoa_don, SUBSTRING(ma_ghi_danh, CHAR_LENGTH(?) + 2), khoan_thu, ngay_lap, "
                + "COALESCE(trang_thai, 'Chưa nộp') FROM hoa_don "
                + "WHERE LEFT(ma_ghi_danh, CHAR_LENGTH(?) + 1) = CONCAT(?, ':') ORDER BY ngay_lap DESC";
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return out;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, maSinhVien);
                ps.setString(2, maSinhVien);
                ps.setString(3, maSinhVien);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new Enrollment(rs.getString(1), rs.getString(2), rs.getLong(3),
                                rs.getTimestamp(4), rs.getString(5)));
                    }
                }
            }
        } catch (SQLException e) {
            showError("Lỗi tải danh sách đăng ký:", e);
        }
        return out;
    }

    private Map<String, String> loadCourseNames() {
        Map<String, String> m = new HashMap<>();
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return m;
            try (PreparedStatement ps = c.prepareStatement("SELECT ma_khoa_hoc, ten_khoa_hoc FROM Khoa_hoc");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            showError("Lỗi tải danh sách khóa học:", e);
        }
        return m;
    }

    private Object[] own() {
        return new Object[]{maSinhVien, maSinhVien, maSinhVien};
    }

    // ---------- phạm vi lịch học ----------
    private static final class Scope {
        String where = "1=0";
        final List<Object> params = new ArrayList<>();
        String emptyMsg = "Chưa có lịch học nào.";
    }

    /** Lịch theo các khóa đã đăng ký + đã thanh toán; nếu chưa đăng ký khóa nào thì dùng GV phụ trách (dữ liệu cũ). */
    private Scope scheduleScope(List<Enrollment> ens) {
        Scope s = new Scope();
        if (!ens.isEmpty()) {
            List<String> paid = new ArrayList<>();
            for (Enrollment e : ens) if (ST_PAID.equals(e.status)) paid.add(e.maKhoa);
            if (paid.isEmpty()) {
                s.emptyMsg = "Học phí chưa được xác nhận. Lịch học sẽ hiện sau khi nhân viên xác nhận thanh toán.";
            } else {
                StringBuilder in = new StringBuilder();
                for (int i = 0; i < paid.size(); i++) in.append(i == 0 ? "?" : ",?");
                s.where = "l.ma_khoa_hoc IN (" + in + ")";
                s.params.addAll(paid);
                s.emptyMsg = "Các lớp thuộc khóa học của bạn chưa được xếp lịch.";
            }
        } else if (!idGiaoVien.isEmpty()) {
            s.where = "lh.id_giao_vien = ?";
            s.params.add(idGiaoVien);
            s.emptyMsg = "Chưa có lịch học nào.";
        } else {
            s.emptyMsg = "Bạn chưa đăng ký khóa học nào.";
        }
        return s;
    }

    private static String scheduleSql(Scope s, String extraWhere, String orderBy) {
        return "SELECT lh.ngay_day, lh.ca_hoc, lh.phong_hoc, COALESCE(l.ten_lop, lh.id_lop), lh.noi_dung, "
                + "COALESCE(gv.ho_ten, ''), lh.trang_thai_buoi FROM Lich_hoc lh "
                + "LEFT JOIN Lop l ON l.id_lop = lh.id_lop "
                + "LEFT JOIN Giao_vien gv ON gv.id_giao_vien = lh.id_giao_vien "
                + "WHERE " + s.where + extraWhere + orderBy;
    }

    // ===================== TIỆN ÍCH CHUNG =====================

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String vnd(long n) {
        return String.format(Locale.US, "%,d", n).replace(',', '.') + " VNĐ";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String fmt(Timestamp t, String pattern) {
        return t == null ? "" : new SimpleDateFormat(pattern).format(t);
    }

    private static String fmtDay(Timestamp t) {
        if (t == null) return "";
        Calendar c = Calendar.getInstance();
        c.setTime(t);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        return (dow == Calendar.SUNDAY ? "Chủ nhật" : "Thứ " + dow) + ", " + fmt(t, "dd/MM/yyyy");
    }

    private String initial() {
        String t = tenSinhVien.trim();
        if (t.isEmpty()) return "S";
        String last = t.substring(t.lastIndexOf(' ') + 1);
        return last.isEmpty() ? "S" : last.substring(0, 1).toUpperCase();
    }

    private static int levelRank(String lv) {
        if (lv == null) return 0;
        String s = lv.trim().toUpperCase();
        return s.matches("N[1-5]") ? s.charAt(1) - '0' : 0;
    }

    private int studentRank() {
        int r = levelRank(trinhDo);
        return r == 0 ? 5 : r;
    }

    private String eligibility(String courseLevel) {
        int c = levelRank(courseLevel), r = studentRank();
        if (c == 0) return COND_OK;
        if (c > r) return COND_LOW;
        if (c < r - MAX_LEVEL_STEP_UP) return COND_HIGH;
        return COND_OK;
    }

    private static String pickFont(String... candidates) {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : candidates) if (available.contains(f)) return f;
        return Font.SANS_SERIF;
    }

    private static Font font(int style, int size) {
        return new Font(FF, style, size);
    }

    private static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    private static void aa(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static void drawCentered(Graphics2D g2, String s, int x, int y, int w, int h) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    private static Color statusColor(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        if (l.contains("chưa nộp") || l.contains("chưa đủ")) return COLOR_WARNING_DARK;
        if (l.contains("chưa chấm")) return COLOR_MUTED;
        if (l.contains("vượt")) return COLOR_MUTED;
        if (l.contains("trượt") || l.contains("nghỉ")) return COLOR_DANGER;
        if (l.contains("đạt") || l.contains("đã nộp") || l.equals("dạy") || l.contains("phù hợp")) return COLOR_SUCCESS_DARK;
        return COLOR_PRIMARY;
    }

    // ===================== KIỂU DỮ LIỆU CHO BẢNG (sắp xếp đúng) =====================

    private static final class Money implements Comparable<Money> {
        final long v;

        Money(long v) {
            this.v = v;
        }

        @Override
        public int compareTo(Money o) {
            return Long.compare(v, o.v);
        }

        @Override
        public String toString() {
            return vnd(v);
        }
    }

    private static final class DateVal implements Comparable<DateVal> {
        final Timestamp ts;
        final String text;

        DateVal(Timestamp ts, String text) {
            this.ts = ts;
            this.text = text;
        }

        @Override
        public int compareTo(DateVal o) {
            if (ts == null) return o.ts == null ? 0 : -1;
            if (o.ts == null) return 1;
            return ts.compareTo(o.ts);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private static final class AppModel extends DefaultTableModel {
        AppModel(String... cols) {
            super(cols, 0);
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            for (int r = 0; r < getRowCount(); r++) {
                Object v = getValueAt(r, c);
                if (v != null) return v.getClass();
            }
            return Object.class;
        }
    }

    // ===================== HIỆU ỨNG HOVER =====================

    private static final class HoverAnim {
        float value;
        private boolean over;
        private final Timer timer;

        HoverAnim(JComponent owner) {
            timer = new Timer(14, e -> {
                float target = over ? 1f : 0f;
                value += (target - value) * 0.30f;
                if (Math.abs(target - value) < 0.01f) {
                    value = target;
                    ((Timer) e.getSource()).stop();
                }
                owner.repaint();
            });
        }

        void set(boolean o) {
            if (over != o) {
                over = o;
                timer.start();
            }
        }
    }

    private static MouseAdapter hoverOf(HoverAnim hv) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hv.set(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hv.set(false);
            }
        };
    }

    // ===================== ICON =====================

    private enum IconType { HOME, BOOK, CALENDAR, AWARD, WALLET, USER }

    private static void drawIcon(Graphics g, IconType type, int x, int y, int size, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        aa(g2);
        g2.translate(x, y);
        double s = size / 20.0;
        g2.scale(s, s);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case HOME:
                Path2D roof = new Path2D.Float();
                roof.moveTo(3, 9.5);
                roof.lineTo(10, 3.5);
                roof.lineTo(17, 9.5);
                g2.draw(roof);
                Path2D body = new Path2D.Float();
                body.moveTo(5, 8.5);
                body.lineTo(5, 16.5);
                body.lineTo(15, 16.5);
                body.lineTo(15, 8.5);
                g2.draw(body);
                break;
            case BOOK:
                g2.drawRoundRect(3, 3, 14, 14, 2, 2);
                g2.drawLine(3, 7, 17, 7);
                g2.drawLine(8, 7, 8, 17);
                break;
            case CALENDAR:
                g2.drawRoundRect(3, 4, 14, 13, 3, 3);
                g2.drawLine(3, 8, 17, 8);
                g2.drawLine(7, 2, 7, 5);
                g2.drawLine(13, 2, 13, 5);
                break;
            case AWARD:
                g2.drawOval(6, 3, 8, 8);
                g2.drawLine(7, 11, 4, 17);
                g2.drawLine(13, 11, 16, 17);
                break;
            case WALLET:
                g2.drawRoundRect(2, 5, 16, 11, 4, 4);
                g2.drawOval(7, 7, 6, 6);
                break;
            case USER:
                g2.drawOval(7, 3, 6, 6);
                g2.drawArc(3, 12, 14, 10, 0, 180);
                break;
        }
        g2.dispose();
    }

    // ===================== THÀNH PHẦN GIAO DIỆN =====================

    /** Thẻ trắng bo góc, đổ bóng; khi gọi install() sẽ nhấc lên khi rê chuột (và click được nếu có onClick). */
    private static class LiftCard extends JPanel {
        static final int PAD = 9;
        private final int radius;
        private final HoverAnim hv = new HoverAnim(this);
        private Runnable onClick;
        private boolean pressed;

        private final MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hv.set(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                SwingUtilities.invokeLater(() -> hv.set(getMousePosition(true) != null));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = onClick != null;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                MouseEvent me = SwingUtilities.convertMouseEvent(e.getComponent(), e, LiftCard.this);
                boolean fire = pressed && onClick != null && contains(me.getPoint());
                pressed = false;
                repaint();
                if (fire) onClick.run();
            }
        };

        LiftCard(LayoutManager lm, int radius) {
            super(lm);
            this.radius = radius;
            setOpaque(false);
        }

        LiftCard padding(int t, int l, int b, int r) {
            setBorder(new EmptyBorder(PAD + t, PAD + l, PAD + b, PAD + r));
            return this;
        }

        LiftCard onClick(Runnable r) {
            this.onClick = r;
            return this;
        }

        /** Gọi sau khi đã thêm xong các component con. */
        LiftCard install() {
            attach(this);
            return this;
        }

        private void attach(Component c) {
            c.addMouseListener(listener);
            if (onClick != null) c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (c instanceof Container) {
                for (Component ch : ((Container) c).getComponents()) attach(ch);
            }
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            float h = hv.value;
            int w = getWidth() - PAD * 2, hh = getHeight() - PAD * 2;
            for (int i = 6; i >= 1; i--) {
                int a = (int) ((3 + 9 * h) * (7 - i) / 6f);
                g2.setColor(new Color(15, 23, 62, Math.max(0, Math.min(255, a))));
                g2.fill(new RoundRectangle2D.Float(PAD - i, PAD - i + 4, w + 2 * i, hh + 2 * i, radius + i, radius + i));
            }
            float lift = h * 3f - (pressed ? 1.5f : 0f);
            g2.translate(0, -lift);
            super.paint(g2);
            g2.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth() - PAD * 2, hh = getHeight() - PAD * 2;
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(PAD, PAD, w, hh, radius, radius));
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(lerp(COLOR_BORDER, COLOR_PRIMARY, hv.value * 0.85f));
            g2.draw(new RoundRectangle2D.Float(PAD, PAD, w, hh, radius, radius));
            g2.dispose();
        }
    }

    private static final class SquareBadge extends JComponent {
        private final String text;
        private final Color bg, fg;
        private final int radius;
        private final boolean round;

        SquareBadge(String text, Color bg, Color fg, int size, int radius, boolean round) {
            this.text = text;
            this.bg = bg;
            this.fg = fg;
            this.radius = radius;
            this.round = round;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(bg);
            if (round) g2.fillOval(0, 0, getWidth(), getHeight());
            else g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.setColor(fg);
            g2.setFont(font(Font.BOLD, Math.max(12, getWidth() * 2 / 5)));
            drawCentered(g2, text, 0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class GradientBanner extends JPanel {
        GradientBanner(LayoutManager lm) {
            super(lm);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            Shape clip = new RoundRectangle2D.Float(0, 0, w, h, 20, 20);
            g2.setPaint(new GradientPaint(0, 0, COLOR_PRIMARY, w, h, COLOR_PURPLE));
            g2.fill(clip);
            g2.clip(clip);
            g2.setColor(new Color(255, 255, 255, 24));
            g2.fillOval(w - 170, -80, 260, 260);
            g2.setColor(new Color(255, 255, 255, 16));
            g2.fillOval(w - 330, h - 60, 170, 170);
            g2.dispose();
        }
    }

    private static final class SidebarPanel extends JPanel {
        SidebarPanel() {
            super(new BorderLayout());
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, NAVY, 0, getHeight(), NAVY_DARK));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class NavButton extends JButton {
        private boolean active;
        private final IconType icon;
        private final HoverAnim hv = new HoverAnim(this);

        NavButton(String text, IconType icon) {
            super(text);
            this.icon = icon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            setPreferredSize(new Dimension(220, 48));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addMouseListener(hoverOf(hv));
        }

        void setActive(boolean a) {
            active = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            if (active) {
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fill(new RoundRectangle2D.Float(0, 3, w, h - 2, 14, 14));
                g2.setColor(NAV_ACTIVE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h - 2, 14, 14));
            } else if (hv.value > 0) {
                g2.setColor(new Color(255, 255, 255, (int) (22 * hv.value)));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h - 2, 14, 14));
            }
            int shift = active ? 0 : (int) (4 * hv.value);
            Color c = active ? Color.WHITE : lerp(NAV_TEXT, Color.WHITE, hv.value);
            drawIcon(g2, icon, 18 + shift, (h - 2 - 20) / 2, 20, c);
            g2.setFont(font(active ? Font.BOLD : Font.PLAIN, 14));
            g2.setColor(c);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 52 + shift, (h - 2 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class PrimaryButton extends JButton {
        private final HoverAnim hv = new HoverAnim(this);

        PrimaryButton(String text) {
            super(text);
            setFont(font(Font.BOLD, 14));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(230, 52));
            addMouseListener(hoverOf(hv));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight() - 8;
            boolean on = isEnabled();
            float lift = on ? hv.value * 2f : 0f;
            if (on) {
                g2.setColor(new Color(59, 91, 240, (int) (45 + 55 * hv.value)));
                g2.fill(new RoundRectangle2D.Float(4, 6, w - 8, h, 14, 14));
            }
            g2.translate(0, -lift);
            if (on) g2.setPaint(new GradientPaint(0, 0, COLOR_PRIMARY, w, 0, COLOR_PURPLE));
            else g2.setColor(new Color(203, 213, 225));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            if (on && hv.value > 0) {
                g2.setColor(new Color(255, 255, 255, (int) (28 * hv.value)));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            }
            g2.setColor(on ? Color.WHITE : new Color(241, 245, 249));
            g2.setFont(getFont());
            drawCentered(g2, getText(), 0, 0, w, h);
            g2.dispose();
        }
    }

    private static final class GhostButton extends JButton {
        private final Color base, accent;
        private final HoverAnim hv = new HoverAnim(this);

        GhostButton(String text, Color base, Color accent) {
            super(text);
            this.base = base;
            this.accent = accent;
            setFont(font(Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(Math.max(120, getFontMetrics(getFont()).stringWidth(text) + 40), 40));
            addMouseListener(hoverOf(hv));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth() - 1, h = getHeight() - 1;
            boolean on = isEnabled();
            float t = on ? hv.value : 0f;
            g2.setColor(lerp(Color.WHITE, accent, 0.10f * t));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(on ? lerp(COLOR_BORDER_DARK, accent, t) : COLOR_BORDER);
            g2.draw(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
            g2.setFont(getFont());
            g2.setColor(on ? lerp(base, accent, t) : new Color(148, 163, 184));
            drawCentered(g2, getText(), 0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class Chip extends JToggleButton {
        private final HoverAnim hv = new HoverAnim(this);

        Chip(String text) {
            super(text);
            setFont(font(Font.BOLD, 12));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(getFontMetrics(getFont()).stringWidth(text) + 32, 34));
            addMouseListener(hoverOf(hv));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth() - 1, h = getHeight() - 1;
            RoundRectangle2D pill = new RoundRectangle2D.Float(0, 0, w, h, h, h);
            if (isSelected()) {
                g2.setColor(COLOR_PRIMARY);
                g2.fill(pill);
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(lerp(Color.WHITE, COLOR_HOVER_ROW, hv.value));
                g2.fill(pill);
                g2.setColor(lerp(COLOR_BORDER_DARK, COLOR_PRIMARY, hv.value));
                g2.draw(pill);
                g2.setColor(lerp(new Color(51, 65, 85), COLOR_PRIMARY, hv.value));
            }
            g2.setFont(getFont());
            drawCentered(g2, getText(), 0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class HintField extends JTextField {
        private final String hint;

        HintField(String hint) {
            this.hint = hint;
            setFont(font(Font.PLAIN, 13));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_DARK, 1, true),
                    new EmptyBorder(9, 12, 9, 12)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    private static final class EmptyState extends JComponent {
        private final String text;

        EmptyState(String text) {
            this.text = text;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int cx = getWidth() / 2, cy = getHeight() / 2 - 20;
            g2.setColor(new Color(238, 243, 252));
            g2.fillOval(cx - 36, cy - 36, 72, 72);
            drawIcon(g2, IconType.CALENDAR, cx - 16, cy - 16, 32, COLOR_PRIMARY);
            g2.setFont(font(Font.PLAIN, 14));
            g2.setColor(COLOR_MUTED);
            drawCentered(g2, text, 0, cy + 46, getWidth(), 24);
            g2.dispose();
        }
    }

    private static final class PillCell extends JPanel {
        String text = "";
        Color color = COLOR_PRIMARY;
        Color rowBg = Color.WHITE;

        PillCell() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(rowBg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(font(Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int pw = fm.stringWidth(text) + 28, ph = 26;
            int x = (getWidth() - pw) / 2, y = (getHeight() - ph) / 2;
            g2.setColor(alpha(color, 30));
            g2.fill(new RoundRectangle2D.Float(x, y, pw, ph, ph, ph));
            g2.setColor(color);
            drawCentered(g2, text, x, y, pw, ph);
            g2.dispose();
        }
    }

    private static final class NoticeRow extends JPanel {
        private final HoverAnim hv = new HoverAnim(this);

        NoticeRow(String text, Color dot) {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(11, 12, 11, 12));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            JComponent d = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    aa(g2);
                    g2.setColor(alpha(dot, 45));
                    g2.fillOval(0, 0, 18, 18);
                    g2.setColor(dot);
                    g2.fillOval(5, 5, 8, 8);
                    g2.dispose();
                }
            };
            d.setPreferredSize(new Dimension(18, 18));
            JPanel dotWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 1));
            dotWrap.setOpaque(false);
            dotWrap.add(d);

            JLabel lb = new JLabel("<html><div style='width:250px'>" + esc(text) + "</div></html>");
            lb.setFont(font(Font.PLAIN, 13));
            lb.setForeground(COLOR_TEXT);

            add(dotWrap, BorderLayout.WEST);
            add(lb, BorderLayout.CENTER);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hv.set(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    SwingUtilities.invokeLater(() -> hv.set(getMousePosition(true) != null));
                }
            };
            addMouseListener(ma);
            lb.addMouseListener(ma);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(alpha(new Color(238, 243, 252), (int) (255 * hv.value)));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
        }
    }

    // ===================== TOP BAR / SIDEBAR / STATUS =====================

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        bar.setPreferredSize(new Dimension(0, 76));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 15));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(0, 14, 0, 0));
        left.add(new SquareBadge("S", COLOR_PRIMARY, Color.WHITE, 46, 12, false));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 0));
        titles.setOpaque(false);
        JLabel l1 = new JLabel("Sinh viên");
        l1.setFont(font(Font.BOLD, 22));
        l1.setForeground(COLOR_TEXT);
        JLabel l2 = new JLabel("STUDENT PORTAL");
        l2.setFont(font(Font.PLAIN, 11));
        l2.setForeground(COLOR_MUTED);
        titles.add(l1);
        titles.add(l2);
        left.add(titles);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 18));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 14));
        JLabel dot = new JLabel("●");
        dot.setFont(font(Font.PLAIN, 10));
        dot.setForeground(COLOR_SUCCESS);
        JLabel hello = new JLabel("Xin chào, " + tenSinhVien);
        hello.setFont(font(Font.PLAIN, 14));
        hello.setForeground(COLOR_TEXT);
        GhostButton logout = new GhostButton("Đăng xuất", new Color(30, 41, 59), COLOR_DANGER);
        logout.addActionListener(e -> {
            int r = JOptionPane.showOptionDialog(this, "Bạn có muốn đăng xuất khỏi hệ thống?", "Xác nhận",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    new Object[]{"Đăng xuất", "Ở lại"}, "Ở lại");
            if (r == 0) {
                dispose();
                new LoginForm().setVisible(true);
            }
        });
        right.add(dot);
        right.add(hello);
        right.add(logout);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createSidebar() {
        JPanel sb = new SidebarPanel();
        sb.setPreferredSize(new Dimension(252, 0));

        JLabel section = new JLabel("MENU SINH VIÊN");
        section.setFont(font(Font.BOLD, 11));
        section.setForeground(new Color(148, 163, 205));
        section.setBorder(new EmptyBorder(26, 28, 14, 10));

        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(0, 14, 10, 14));

        NavButton[] all = {
                createNavButton("Tổng quan", IconType.HOME, "Dashboard"),
                createNavButton("Đăng ký khóa học", IconType.BOOK, "DangKy"),
                createNavButton("Thời khóa biểu", IconType.CALENDAR, "TKB"),
                createNavButton("Bảng điểm", IconType.AWARD, "BangDiem"),
                createNavButton("Học phí", IconType.WALLET, "HocPhi"),
                createNavButton("Thông tin cá nhân", IconType.USER, "Profile")
        };
        for (int i = 0; i < all.length; i++) {
            nav.add(all[i]);
            if (i < all.length - 1) nav.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        JPanel mini = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        mini.setOpaque(false);
        mini.setBorder(new EmptyBorder(12, 12, 12, 12));
        mini.add(new SquareBadge(initial(), new Color(255, 255, 255, 30), Color.WHITE, 40, 0, true), BorderLayout.WEST);
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 1));
        info.setOpaque(false);
        JLabel n1 = new JLabel(tenSinhVien);
        n1.setFont(font(Font.BOLD, 13));
        n1.setForeground(Color.WHITE);
        JLabel n2 = new JLabel(maSinhVien + (trinhDo.isEmpty() ? "" : "  ·  " + trinhDo));
        n2.setFont(font(Font.PLAIN, 11));
        n2.setForeground(NAV_TEXT);
        info.add(n1);
        info.add(n2);
        mini.add(info, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(new EmptyBorder(8, 14, 16, 14));
        foot.add(mini, BorderLayout.CENTER);

        sb.add(section, BorderLayout.NORTH);
        sb.add(nav, BorderLayout.CENTER);
        sb.add(foot, BorderLayout.SOUTH);
        return sb;
    }

    private NavButton createNavButton(String text, IconType icon, String cardName) {
        NavButton b = new NavButton(text, icon);
        b.addActionListener(e -> navigate(cardName));
        navButtons.add(b);
        navByCard.put(cardName, b);
        return b;
    }

    private void setNavActive(NavButton active) {
        for (NavButton b : navButtons) b.setActive(b == active);
    }

    private JPanel createStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 7));
        p.setBackground(COLOR_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        JLabel dot = new JLabel("●");
        dot.setFont(font(Font.PLAIN, 9));
        dot.setForeground(COLOR_SUCCESS);
        JLabel t = new JLabel("Đã kết nối database: " + (dbName.isEmpty() ? "btl" : dbName)
                + "   |   Phiên làm việc: " + maSinhVien);
        t.setFont(font(Font.PLAIN, 12));
        t.setForeground(COLOR_MUTED);
        p.add(dot);
        p.add(t);
        return p;
    }

    // ===================== KHUNG TRANG DÙNG CHUNG =====================

    private JPanel pagePanel(String title, String sub) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(COLOR_BG);
        p.setBorder(new EmptyBorder(20, 20, 6, 20));
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(0, 9, 6, 0));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 24));
        t.setForeground(COLOR_TEXT);
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 13));
        s.setForeground(COLOR_MUTED);
        head.add(t);
        head.add(s);
        p.add(head, BorderLayout.NORTH);
        return p;
    }

    private LiftCard createCard(String title) {
        LiftCard card = new LiftCard(new BorderLayout(0, 14), 18).padding(18, 20, 20, 20);
        if (title != null) {
            JLabel l = new JLabel(title);
            l.setFont(font(Font.BOLD, 17));
            l.setForeground(COLOR_TEXT);
            card.add(l, BorderLayout.NORTH);
        }
        return card;
    }

    private LiftCard statCard(String label, String value, String sub, Color subColor,
                              String badge, Color tint, Color fg, Runnable onClick) {
        LiftCard c = new LiftCard(new BorderLayout(0, 8), 18).padding(16, 20, 14, 18);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel lb = new JLabel(label);
        lb.setFont(font(Font.PLAIN, 13));
        lb.setForeground(COLOR_MUTED);
        top.add(lb, BorderLayout.WEST);
        top.add(new SquareBadge(badge, tint, fg, 38, 10, false), BorderLayout.EAST);

        JLabel v = new JLabel(value);
        v.setFont(font(Font.BOLD, 23));
        v.setForeground(COLOR_TEXT);
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 12));
        s.setForeground(subColor);
        JPanel mid = new JPanel(new GridLayout(2, 1, 0, 3));
        mid.setOpaque(false);
        mid.add(v);
        mid.add(s);

        c.add(top, BorderLayout.NORTH);
        c.add(mid, BorderLayout.CENTER);
        if (onClick != null) c.onClick(onClick);
        return c.install();
    }

    private JPanel chipBar(String[] labels, int selected, IntConsumer cb) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            Chip ch = new Chip(labels[i]);
            ch.setSelected(i == selected);
            ch.addActionListener(e -> cb.accept(idx));
            g.add(ch);
            p.add(ch);
        }
        return p;
    }

    // ===================== BẢNG =====================

    private JTable createTable(AppModel model, int statusCol, int... leftCols) {
        JTable table = new JTable(model);
        table.setRowHeight(48);
        table.setFont(font(Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(COLOR_SELECT);
        table.setSelectionForeground(COLOR_TEXT);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, new CellRenderer(statusCol, leftCols));

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 44));
        header.setDefaultRenderer(new HeaderRenderer());

        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Integer old = (Integer) table.getClientProperty("hoverRow");
                if (old == null || old != row) {
                    table.putClientProperty("hoverRow", row);
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                table.putClientProperty("hoverRow", -1);
                table.repaint();
            }
        });
        return table;
    }

    private JComponent createScroll(JTable table, String emptyText) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        if (table.getModel().getRowCount() > 0) return sp;

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        wrap.add(table.getTableHeader(), BorderLayout.NORTH);
        wrap.add(new EmptyState(emptyText), BorderLayout.CENTER);
        return wrap;
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private final int statusCol;
        private final Set<Integer> left = new HashSet<>();
        private final PillCell pill = new PillCell();

        CellRenderer(int statusCol, int... leftCols) {
            this.statusCol = statusCol;
            for (int c : leftCols) left.add(c);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Integer hr = (Integer) t.getClientProperty("hoverRow");
            boolean hover = hr != null && hr == row;
            Color bg = sel ? COLOR_SELECT : hover ? COLOR_HOVER_ROW : (row % 2 == 0 ? Color.WHITE : COLOR_STRIPE);
            String s = v == null ? "" : v.toString();
            if (col == statusCol && !s.isEmpty()) {
                pill.text = s;
                pill.color = statusColor(s);
                pill.rowBg = bg;
                return pill;
            }
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setBackground(bg);
            setForeground(COLOR_TEXT);
            setFont(font(Font.PLAIN, 13));
            setHorizontalAlignment(left.contains(col) ? SwingConstants.LEFT : SwingConstants.CENTER);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            return this;
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setFont(font(Font.BOLD, 12));
            setBackground(new Color(238, 243, 252));
            setForeground(new Color(51, 65, 85));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(147, 170, 235)),
                    new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    // ===================== TỔNG QUAN =====================

    private static final class Notice {
        final String text;
        final Color color;

        Notice(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }

    private JPanel createDashboardPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 8));
        page.setBackground(COLOR_BG);
        page.setBorder(new EmptyBorder(20, 20, 6, 20));

        List<Enrollment> ens = loadEnrollments();
        Scope scope = scheduleScope(ens);

        // ---- học phí ----
        long paid = 0, unpaid = 0;
        int unpaidCount = 0;
        Object[] inv = queryOne("SELECT COALESCE(SUM(CASE WHEN COALESCE(trang_thai,'Chưa nộp') = 'Đã nộp' THEN khoan_thu ELSE 0 END), 0), "
                        + "COALESCE(SUM(CASE WHEN COALESCE(trang_thai,'Chưa nộp') <> 'Đã nộp' THEN khoan_thu ELSE 0 END), 0), "
                        + "COALESCE(SUM(CASE WHEN COALESCE(trang_thai,'Chưa nộp') <> 'Đã nộp' THEN 1 ELSE 0 END), 0) "
                        + "FROM hoa_don WHERE " + OWN_INV,
                rs -> new Object[]{rs.getLong(1), rs.getLong(2), rs.getInt(3)}, own());
        if (inv != null) {
            paid = (Long) inv[0];
            unpaid = (Long) inv[1];
            unpaidCount = (Integer) inv[2];
        }

        // ---- kết quả thi gần nhất (dùng tinh_trang của CSDL) ----
        Object[] exam = queryOne("SELECT trinh_do, tong_diem, tinh_trang FROM Ket_qua_thi "
                        + "WHERE ma_sinh_vien = ? ORDER BY ma_bai_lam DESC LIMIT 1",
                rs -> new Object[]{rs.getString(1), rs.getInt(2), rs.getString(3)}, maSinhVien);

        // ---- lịch học ----
        AppModel sm = new AppModel("Ngày học", "Ca học", "Phòng", "Nội dung", "Trạng thái");
        RowMapper dm = rs -> new Object[]{new DateVal(rs.getTimestamp(1), fmtDay(rs.getTimestamp(1))),
                rs.getString(2), rs.getString(3), rs.getString(5), rs.getString(7)};
        loadRows(sm, scheduleSql(scope, " AND lh.ngay_day >= CURDATE()", " ORDER BY lh.ngay_day LIMIT 5"),
                dm, scope.params.toArray());
        boolean upcoming = sm.getRowCount() > 0;
        if (!upcoming) {
            loadRows(sm, scheduleSql(scope, " AND lh.ngay_day < CURDATE()", " ORDER BY lh.ngay_day DESC LIMIT 5"),
                    dm, scope.params.toArray());
        }

        // ---- banner ----
        GradientBanner banner = new GradientBanner(new BorderLayout());
        banner.setBorder(new EmptyBorder(22, 28, 22, 28));
        JPanel bt = new JPanel(new GridLayout(2, 1, 0, 6));
        bt.setOpaque(false);
        JLabel h1 = new JLabel("Chào mừng trở lại, " + tenSinhVien + "!");
        h1.setFont(font(Font.BOLD, 26));
        h1.setForeground(Color.WHITE);
        JLabel h2 = new JLabel("Theo dõi lịch học, kết quả thi và học phí của bạn tại S-LINK.");
        h2.setFont(font(Font.PLAIN, 14));
        h2.setForeground(new Color(235, 240, 255));
        bt.add(h1);
        bt.add(h2);
        JLabel today = new JLabel(fmtDay(new Timestamp(System.currentTimeMillis())));
        today.setFont(font(Font.BOLD, 13));
        today.setForeground(new Color(235, 240, 255));
        banner.add(bt, BorderLayout.WEST);
        banner.add(today, BorderLayout.EAST);
        JPanel bannerWrap = new JPanel(new BorderLayout());
        bannerWrap.setOpaque(false);
        bannerWrap.setBorder(new EmptyBorder(0, LiftCard.PAD, 0, LiftCard.PAD));
        bannerWrap.add(banner, BorderLayout.CENTER);

        // ---- thẻ thống kê ----
        String examValue = "Chưa thi", examSub = "Chưa có kết quả";
        Color examSubColor = COLOR_MUTED;
        if (exam != null) {
            String st = exam[2] == null ? "Chưa chấm" : exam[2].toString();
            examValue = exam[1] + " điểm";
            examSub = st + "  ·  " + nz((String) exam[0]);
            examSubColor = statusColor(st);
        }
        JPanel stats = new JPanel(new GridLayout(1, 4, 0, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 142));
        stats.add(statCard("Mã sinh viên", maSinhVien, "Trình độ: " + (trinhDo.isEmpty() ? "—" : trinhDo),
                COLOR_MUTED, "SV", TINT_BLUE, COLOR_PRIMARY, () -> navigate("Profile")));
        stats.add(statCard("Giáo viên phụ trách", tenGiaoVien, idGiaoVien.isEmpty() ? "Chưa có mã GV" : "Mã GV: " + idGiaoVien,
                COLOR_MUTED, "GV", TINT_GREEN, COLOR_SUCCESS_DARK, () -> navigate("Profile")));
        stats.add(statCard("Kết quả thi gần nhất", examValue, examSub, examSubColor,
                "KQ", TINT_ORANGE, new Color(217, 119, 6), () -> navigate("BangDiem")));
        stats.add(statCard("Học phí đã nộp", vnd(paid), "Chưa nộp: " + vnd(unpaid),
                unpaid > 0 ? COLOR_DANGER : COLOR_SUCCESS_DARK, "HP", TINT_PURPLE, COLOR_PURPLE,
                () -> navigate("HocPhi")));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(bannerWrap, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);

        // ---- khối giữa ----
        LiftCard schedule = createCard(upcoming ? "Lịch học sắp tới" : "Các buổi học gần nhất");
        schedule.add(createScroll(createTable(sm, 4, 3), scope.emptyMsg), BorderLayout.CENTER);

        List<Notice> notices = new ArrayList<>();
        if (ens.isEmpty()) {
            notices.add(new Notice("Bạn chưa đăng ký khóa học nào. Vào mục Đăng ký khóa học để bắt đầu.", COLOR_WARNING));
        }
        if (unpaidCount > 0) {
            notices.add(new Notice("Bạn có " + unpaidCount + " hóa đơn chưa nộp (" + vnd(unpaid)
                    + "). Lịch học chỉ mở sau khi học phí được xác nhận.", COLOR_DANGER));
        }
        if (upcoming) {
            int cancelled = 0;
            String next = null;
            for (int i = 0; i < sm.getRowCount(); i++) {
                String st = String.valueOf(sm.getValueAt(i, 4));
                if ("Nghỉ".equals(st)) cancelled++;
                else if (next == null) {
                    next = sm.getValueAt(i, 0) + " · " + sm.getValueAt(i, 1) + " · phòng " + sm.getValueAt(i, 2);
                }
            }
            if (next != null) notices.add(new Notice("Buổi học tiếp theo: " + next, COLOR_PRIMARY));
            if (cancelled > 0) notices.add(new Notice("Có " + cancelled + " buổi nghỉ trong các buổi sắp tới, hãy kiểm tra thời khóa biểu.", COLOR_WARNING));
        }
        if (exam == null) {
            notices.add(new Notice("Bạn chưa có kết quả thi nào.", COLOR_MUTED));
        } else {
            String st = exam[2] == null ? "chưa được chấm" : exam[2].toString();
            notices.add(new Notice("Kết quả thi gần nhất: " + exam[1] + " điểm (" + nz((String) exam[0]) + ") - " + st + ".",
                    "ĐẠT".equals(st) ? COLOR_SUCCESS : "TRƯỢT".equals(st) ? COLOR_DANGER : COLOR_MUTED));
        }
        if (notices.isEmpty()) notices.add(new Notice("Không có thông báo mới.", COLOR_MUTED));

        LiftCard noticeCard = createCard("Thông báo & nhắc nhở");
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        for (Notice n : notices) {
            list.add(new NoticeRow(n.text, n.color));
            list.add(Box.createVerticalStrut(2));
        }
        JPanel listWrap = new JPanel(new BorderLayout());
        listWrap.setOpaque(false);
        listWrap.add(list, BorderLayout.NORTH);
        noticeCard.add(listWrap, BorderLayout.CENTER);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridy = 0;
        gc.gridx = 0;
        gc.weightx = 0.60;
        center.add(schedule, gc);
        gc.gridx = 1;
        gc.weightx = 0.40;
        center.add(noticeCard, gc);

        page.add(top, BorderLayout.NORTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    // ===================== ĐĂNG KÝ KHÓA HỌC =====================

    private JPanel createDangKyPanel() {
        JPanel page = pagePanel("Đăng ký khóa học",
                "Bạn đang ở trình độ " + (trinhDo.isEmpty() ? "chưa xác định" : trinhDo)
                        + ". Chỉ đăng ký được khóa cùng cấp hoặc tối đa " + MAX_LEVEL_STEP_UP + " cấp cao hơn.");
        LiftCard card = createCard(null);

        AppModel model = new AppModel("Mã khóa", "Tên khóa học", "Trình độ", "Chuyên đề", "Giáo trình", "Học phí", "Điều kiện");
        loadRows(model,
                "SELECT k.ma_khoa_hoc, k.ten_khoa_hoc, k.trinh_do, k.chuyen_de, k.giao_trinh, k.hoc_phi FROM Khoa_hoc k "
                        + "WHERE NOT EXISTS (SELECT 1 FROM hoa_don h WHERE h.ma_ghi_danh = CONCAT(?, ':', k.ma_khoa_hoc)) "
                        + "ORDER BY k.trinh_do DESC, k.ma_khoa_hoc",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), new Money(rs.getLong(6)), eligibility(rs.getString(3))},
                maSinhVien);

        JTable table = createTable(model, 6, 1, 3, 4);
        TableRowSorter<AppModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // ---- thanh công cụ ----
        HintField txt = new HintField("Tìm theo tên, chuyên đề, giáo trình...");
        final String[] levels = {"Tất cả", "N5", "N4", "N3", "N2", "N1"};
        final String[] level = {"Tất cả"};
        Chip eligibleChip = new Chip("Chỉ khóa phù hợp");
        eligibleChip.setSelected(true);

        JLabel lblSel = new JLabel("Chọn một khóa học trong bảng để đăng ký");
        lblSel.setFont(font(Font.BOLD, 14));
        lblSel.setForeground(COLOR_TEXT);
        JLabel lblWarn = new JLabel(" ");
        lblWarn.setFont(font(Font.PLAIN, 12));
        lblWarn.setForeground(COLOR_WARNING_DARK);
        PrimaryButton btn = new PrimaryButton("Đăng ký khóa học");
        btn.setEnabled(false);

        Runnable refreshSelection = () -> {
            int vr = table.getSelectedRow();
            if (vr < 0) {
                lblSel.setText("Chọn một khóa học trong bảng để đăng ký");
                lblWarn.setText(" ");
                btn.setEnabled(false);
                return;
            }
            int mr = table.convertRowIndexToModel(vr);
            String cond = String.valueOf(model.getValueAt(mr, 6));
            lblSel.setText("Đã chọn: " + model.getValueAt(mr, 1) + "  ·  " + model.getValueAt(mr, 2)
                    + "  ·  Học phí " + model.getValueAt(mr, 5));
            boolean ok = COND_OK.equals(cond);
            btn.setEnabled(ok);
            if (ok) lblWarn.setText("Hóa đơn sẽ ở trạng thái CHƯA NỘP cho đến khi nhân viên xác nhận thanh toán.");
            else if (COND_LOW.equals(cond)) lblWarn.setText("Không thể đăng ký: khóa này thấp hơn trình độ hiện tại của bạn.");
            else lblWarn.setText("Không thể đăng ký: bạn cần hoàn thành các cấp thấp hơn trước.");
        };

        Runnable applyFilter = () -> {
            final String q = txt.getText().trim().toLowerCase();
            final boolean onlyOk = eligibleChip.isSelected();
            final String lv = level[0];
            sorter.setRowFilter(new RowFilter<AppModel, Integer>() {
                @Override
                public boolean include(Entry<? extends AppModel, ? extends Integer> en) {
                    if (!"Tất cả".equals(lv) && !lv.equalsIgnoreCase(en.getStringValue(2))) return false;
                    if (onlyOk && !COND_OK.equals(en.getStringValue(6))) return false;
                    if (q.isEmpty()) return true;
                    for (int i = 0; i < en.getValueCount(); i++) {
                        if (en.getStringValue(i).toLowerCase().contains(q)) return true;
                    }
                    return false;
                }
            });
            table.clearSelection();
            refreshSelection.run();
        };

        txt.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyFilter.run();
            }

            public void removeUpdate(DocumentEvent e) {
                applyFilter.run();
            }

            public void changedUpdate(DocumentEvent e) {
                applyFilter.run();
            }
        });
        eligibleChip.addActionListener(e -> applyFilter.run());
        JPanel levelChips = chipBar(levels, 0, i -> {
            level[0] = levels[i];
            applyFilter.run();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(levelChips);
        right.add(eligibleChip);
        JPanel tools = new JPanel(new BorderLayout(14, 0));
        tools.setOpaque(false);
        tools.add(txt, BorderLayout.CENTER);
        tools.add(right, BorderLayout.EAST);

        applyFilter.run();   // áp dụng "Chỉ khóa phù hợp" ngay từ đầu

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshSelection.run();
        });

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(tools, BorderLayout.NORTH);
        center.add(createScroll(table, "Bạn đã đăng ký tất cả khóa học hiện có"), BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout(16, 0));
        foot.setOpaque(false);
        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 2));
        texts.setOpaque(false);
        texts.add(lblSel);
        texts.add(lblWarn);
        foot.add(texts, BorderLayout.CENTER);
        foot.add(btn, BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);

        btn.addActionListener(e -> {
            int vr = table.getSelectedRow();
            if (vr < 0) {
                warn("Vui lòng chọn 1 khóa học trong bảng để đăng ký!");
                return;
            }
            int mr = table.convertRowIndexToModel(vr);
            String maKhoa = String.valueOf(model.getValueAt(mr, 0));
            String tenKhoa = String.valueOf(model.getValueAt(mr, 1));
            String hocPhi = String.valueOf(model.getValueAt(mr, 5));

            JPanel msg = new JPanel(new GridLayout(0, 1, 0, 6));
            msg.add(new JLabel("Sinh viên: " + tenSinhVien + " (" + maSinhVien + ")"));
            msg.add(new JLabel("Khóa học: " + tenKhoa));
            msg.add(new JLabel("Học phí: " + hocPhi));
            JComboBox<String> cbo = new JComboBox<>(new String[]{"Chuyển khoản", "Tiền mặt"});
            JPanel pay = new JPanel(new BorderLayout(8, 0));
            pay.add(new JLabel("Hình thức thanh toán:"), BorderLayout.WEST);
            pay.add(cbo, BorderLayout.CENTER);
            msg.add(pay);

            int r = JOptionPane.showOptionDialog(this, msg, "Xác nhận đăng ký", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Xác nhận đăng ký", "Hủy"}, "Xác nhận đăng ký");
            if (r == 0 && registerCourse(maKhoa, String.valueOf(cbo.getSelectedItem()))) {
                JOptionPane.showMessageDialog(this,
                        "Đăng ký thành công!\nHóa đơn đang ở trạng thái CHƯA NỘP. Vui lòng thanh toán và chờ nhân viên xác nhận.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                showCard("DangKy");
            }
        });

        page.add(card, BorderLayout.CENTER);
        return page;
    }

    /** Toàn bộ kiểm tra + ghi dữ liệu nằm trong 1 transaction; dữ liệu lấy lại từ CSDL, không tin giao diện. */
    private boolean registerCourse(String maKhoa, String hinhThuc) {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) throw new BizException("Không thể kết nối cơ sở dữ liệu.");
            c.setAutoCommit(false);
            try {
                String tenKhoa, level;
                long fee;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT ten_khoa_hoc, trinh_do, hoc_phi FROM Khoa_hoc WHERE ma_khoa_hoc = ?")) {
                    ps.setString(1, maKhoa);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new BizException("Khóa học không còn tồn tại.");
                        tenKhoa = rs.getString(1);
                        level = rs.getString(2);
                        fee = rs.getLong(3);
                    }
                }
                if (fee <= 0) throw new BizException("Học phí của khóa \"" + tenKhoa + "\" không hợp lệ.");
                String cond = eligibility(level);
                if (COND_LOW.equals(cond)) throw new BizException("Khóa học này thấp hơn trình độ hiện tại của bạn.");
                if (COND_HIGH.equals(cond)) throw new BizException("Bạn chưa đủ trình độ để đăng ký khóa này.");

                String maGhiDanh = maSinhVien + ":" + maKhoa;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM hoa_don WHERE ma_ghi_danh = ? FOR UPDATE")) {
                    ps.setString(1, maGhiDanh);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) throw new BizException("Bạn đã đăng ký khóa học này rồi!");
                    }
                }

                String maNv;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT ma_nhan_vien FROM Nhan_vien ORDER BY (ma_nhan_vien = 'NV01') DESC, ma_nhan_vien LIMIT 1");
                     ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new BizException("Hệ thống chưa có nhân viên để tiếp nhận hóa đơn.");
                    maNv = rs.getString(1);
                }

                String maHoaDon = "HD" + System.currentTimeMillis() + Math.abs(System.nanoTime() % 1000);
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO hoa_don (ma_hoa_don, ma_nhan_vien, ma_ghi_danh, khoan_thu, ngay_lap, hinh_thuc, trang_thai) "
                                + "VALUES (?, ?, ?, ?, NOW(), ?, 'Chưa nộp')")) {
                    ps.setString(1, maHoaDon);
                    ps.setString(2, maNv);
                    ps.setString(3, maGhiDanh);
                    ps.setLong(4, fee);
                    ps.setString(5, hinhThuc);
                    if (ps.executeUpdate() != 1) throw new BizException("Không thể tạo hóa đơn.");
                }
                c.commit();
                return true;
            } catch (BizException | SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (BizException ex) {
            warn(ex.getMessage());
            return false;
        } catch (SQLException ex) {
            showError("Lỗi SQL khi đăng ký:", ex);
            return false;
        }
    }

    // ===================== THỜI KHÓA BIỂU =====================

    private JPanel createTKBPanel() {
        List<Enrollment> ens = loadEnrollments();
        Scope scope = scheduleScope(ens);
        JPanel page = pagePanel("Thời khóa biểu",
                ens.isEmpty() ? "Lịch dạy của giáo viên phụ trách bạn"
                        : "Lịch học các khóa bạn đã đăng ký và được xác nhận thanh toán");
        LiftCard card = createCard(null);

        final String[] extra = {
                "",
                " AND lh.ngay_day >= CURDATE() AND lh.trang_thai_buoi = 'Dạy'",
                " AND lh.ngay_day < CURDATE()",
                " AND lh.trang_thai_buoi = 'Nghỉ'"};
        final String[] order = {
                " ORDER BY lh.ngay_day", " ORDER BY lh.ngay_day", " ORDER BY lh.ngay_day DESC", " ORDER BY lh.ngay_day"};

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);

        IntConsumer reload = idx -> {
            AppModel model = new AppModel("Ngày học", "Ca học", "Phòng", "Lớp", "Nội dung", "Giáo viên", "Trạng thái");
            loadRows(model, scheduleSql(scope, extra[idx], order[idx]),
                    rs -> new Object[]{new DateVal(rs.getTimestamp(1), fmtDay(rs.getTimestamp(1))),
                            rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getString(7)},
                    scope.params.toArray());
            holder.removeAll();
            holder.add(createScroll(createTable(model, 6, 3, 4, 5), scope.emptyMsg), BorderLayout.CENTER);
            holder.revalidate();
            holder.repaint();
        };

        JPanel chips = chipBar(new String[]{"Tất cả", "Sắp diễn ra", "Đã diễn ra", "Buổi nghỉ"}, 0, reload);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(chips, BorderLayout.NORTH);
        center.add(holder, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        reload.accept(0);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    // ===================== BẢNG ĐIỂM =====================

    private JPanel createBangDiemPanel() {
        JPanel page = pagePanel("Bảng điểm", "Kết quả các bài kiểm tra trình độ của bạn");

        AppModel model = new AppModel("Mã bài làm", "Trình độ", "Mã đề thi", "Đọc hiểu", "Từ vựng", "Nghe", "Tổng điểm", "Kết quả");
        loadRows(model,
                "SELECT ma_bai_lam, trinh_do, ma_de_thi, diem_doc_hieu, diem_tu_vung, diem_nghe, tong_diem, tinh_trang "
                        + "FROM Ket_qua_thi WHERE ma_sinh_vien = ? ORDER BY ma_bai_lam DESC",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7),
                        rs.getString(8) == null ? "Chưa chấm" : rs.getString(8)},
                maSinhVien);

        int total = model.getRowCount(), best = 0, passed = 0;
        for (int i = 0; i < total; i++) {
            best = Math.max(best, ((Number) model.getValueAt(i, 6)).intValue());
            if ("ĐẠT".equals(model.getValueAt(i, 7))) passed++;
        }

        JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 130));
        stats.add(statCard("Số bài đã thi", String.valueOf(total), "Tổng số bài làm", COLOR_MUTED,
                "BT", TINT_BLUE, COLOR_PRIMARY, null));
        stats.add(statCard("Điểm cao nhất", total == 0 ? "—" : best + " điểm", "Trong tất cả bài thi", COLOR_MUTED,
                "TOP", TINT_ORANGE, new Color(217, 119, 6), null));
        stats.add(statCard("Số bài đạt", passed + " / " + total,
                total == 0 ? "Chưa có dữ liệu" : "Tỉ lệ đạt: " + (passed * 100 / total) + "%",
                passed > 0 ? COLOR_SUCCESS_DARK : COLOR_MUTED, "ĐẠT", TINT_GREEN, COLOR_SUCCESS_DARK, null));

        LiftCard card = createCard("Chi tiết bài làm");
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(createScroll(createTable(model, 7), "Chưa có kết quả thi nào"), BorderLayout.CENTER);
        JLabel note = new JLabel("Tiêu chí đạt (kèm điểm tối thiểu từng phần): N5 ≥ 80 · N4 ≥ 90 · N3 ≥ 95 · N2 ≥ 90 · N1 ≥ 100");
        note.setFont(font(Font.PLAIN, 12));
        note.setForeground(COLOR_MUTED);
        body.add(note, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.setOpaque(false);
        content.add(stats, BorderLayout.NORTH);
        content.add(card, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    // ===================== HỌC PHÍ =====================

    private JPanel createHocPhiPanel() {
        JPanel page = pagePanel("Học phí", "Các hóa đơn học phí của bạn. Có thể hủy hóa đơn chưa nộp.");

        Map<String, String> courseNames = loadCourseNames();
        AppModel model = new AppModel("Mã hóa đơn", "Khóa học", "Khoản thu", "Ngày lập", "Hình thức", "Trạng thái");
        loadRows(model,
                "SELECT ma_hoa_don, ma_ghi_danh, khoan_thu, ngay_lap, hinh_thuc, COALESCE(trang_thai, 'Chưa nộp') "
                        + "FROM hoa_don WHERE " + OWN_INV + " ORDER BY ngay_lap DESC",
                rs -> {
                    String gd = rs.getString(2);
                    String ma = gd != null && gd.contains(":") ? gd.substring(gd.indexOf(':') + 1) : gd;
                    String ten = courseNames.get(ma);
                    return new Object[]{rs.getString(1), ten != null ? ten + " (" + ma + ")" : nz(gd),
                            new Money(rs.getLong(3)),
                            new DateVal(rs.getTimestamp(4), fmt(rs.getTimestamp(4), "dd/MM/yyyy HH:mm")),
                            rs.getString(5), rs.getString(6)};
                },
                own());

        long paid = 0, unpaid = 0;
        int cnt = model.getRowCount();
        for (int i = 0; i < cnt; i++) {
            long v = ((Money) model.getValueAt(i, 2)).v;
            if (ST_PAID.equals(model.getValueAt(i, 5))) paid += v;
            else unpaid += v;
        }

        JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 130));
        stats.add(statCard("Đã nộp", vnd(paid), "Đã được xác nhận", COLOR_SUCCESS_DARK,
                "OK", TINT_GREEN, COLOR_SUCCESS_DARK, null));
        stats.add(statCard("Chưa nộp", vnd(unpaid), unpaid > 0 ? "Cần thanh toán" : "Không có khoản nợ",
                unpaid > 0 ? COLOR_DANGER : COLOR_SUCCESS_DARK, "HP", TINT_ORANGE, new Color(217, 119, 6), null));
        stats.add(statCard("Số hóa đơn", String.valueOf(cnt), "Tổng số hóa đơn đã lập", COLOR_MUTED,
                "HD", TINT_PURPLE, COLOR_PURPLE, null));

        LiftCard card = createCard("Lịch sử hóa đơn");
        JTable table = createTable(model, 5, 1);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(createScroll(table, "Chưa có hóa đơn nào"), BorderLayout.CENTER);

        JLabel hint = new JLabel("Chọn hóa đơn \"Chưa nộp\" để hủy đăng ký khóa học tương ứng.");
        hint.setFont(font(Font.PLAIN, 12));
        hint.setForeground(COLOR_MUTED);
        GhostButton cancel = new GhostButton("Hủy hóa đơn đã chọn", new Color(30, 41, 59), COLOR_DANGER);
        cancel.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int vr = table.getSelectedRow();
            cancel.setEnabled(vr >= 0
                    && ST_UNPAID.equals(String.valueOf(model.getValueAt(table.convertRowIndexToModel(vr), 5))));
        });
        cancel.addActionListener(e -> {
            int vr = table.getSelectedRow();
            if (vr < 0) return;
            int mr = table.convertRowIndexToModel(vr);
            String maHd = String.valueOf(model.getValueAt(mr, 0));
            int r = JOptionPane.showOptionDialog(this,
                    "Hủy hóa đơn " + maHd + " (" + model.getValueAt(mr, 1) + ")?\nThao tác này sẽ hủy đăng ký khóa học tương ứng.",
                    "Xác nhận hủy", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new Object[]{"Hủy hóa đơn", "Giữ lại"}, "Giữ lại");
            if (r == 0 && cancelInvoice(maHd)) showCard("HocPhi");
        });

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.add(hint, BorderLayout.WEST);
        foot.add(cancel, BorderLayout.EAST);
        body.add(foot, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.setOpaque(false);
        content.add(stats, BorderLayout.NORTH);
        content.add(card, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    /** Chỉ hủy được hóa đơn của chính mình và còn "Chưa nộp". */
    private boolean cancelInvoice(String maHoaDon) {
        String sql = "DELETE FROM hoa_don WHERE ma_hoa_don = ? "
                + "AND LEFT(ma_ghi_danh, CHAR_LENGTH(?) + 1) = CONCAT(?, ':') "
                + "AND COALESCE(trang_thai, 'Chưa nộp') = 'Chưa nộp'";
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) {
                warn("Không thể kết nối cơ sở dữ liệu.");
                return false;
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, maHoaDon);
                ps.setString(2, maSinhVien);
                ps.setString(3, maSinhVien);
                if (ps.executeUpdate() == 1) return true;
                warn("Không thể hủy: hóa đơn đã được nộp hoặc không thuộc về bạn.");
                return false;
            }
        } catch (SQLException e) {
            showError("Lỗi khi hủy hóa đơn:", e);
            return false;
        }
    }

    // ===================== THÔNG TIN CÁ NHÂN =====================

    private String learningStatus() {
        List<Enrollment> ens = loadEnrollments();
        if (ens.isEmpty()) return "Chưa đăng ký khóa học";
        int paid = 0;
        for (Enrollment e : ens) if (ST_PAID.equals(e.status)) paid++;
        return paid > 0 ? "Đang theo học (" + paid + " khóa)" : "Chờ xác nhận học phí";
    }

    private JPanel createProfilePanel() {
        JPanel page = pagePanel("Thông tin cá nhân", "Hồ sơ học viên tại trung tâm");

        // ---- thẻ đầu trang ----
        LiftCard head = new LiftCard(new BorderLayout(18, 0), 18).padding(18, 22, 18, 22);
        head.add(new SquareBadge(initial(), TINT_BLUE, COLOR_PRIMARY, 72, 0, true), BorderLayout.WEST);
        JPanel names = new JPanel(new GridLayout(2, 1, 0, 4));
        names.setOpaque(false);
        JLabel n1 = new JLabel(tenSinhVien);
        n1.setFont(font(Font.BOLD, 22));
        n1.setForeground(COLOR_TEXT);
        JLabel n2 = new JLabel(maSinhVien + "  ·  Trình độ " + (trinhDo.isEmpty() ? "chưa xác định" : trinhDo));
        n2.setFont(font(Font.PLAIN, 13));
        n2.setForeground(COLOR_MUTED);
        names.add(n1);
        names.add(n2);
        head.add(names, BorderLayout.CENTER);

        GhostButton editPhone = new GhostButton("Cập nhật số điện thoại", new Color(30, 41, 59), COLOR_PRIMARY);
        editPhone.addActionListener(e -> updatePhone());
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 16));
        btnWrap.setOpaque(false);
        btnWrap.add(editPhone);
        head.add(btnWrap, BorderLayout.EAST);
        head.install();

        // ---- lưới thông tin ----
        JPanel grid = new JPanel(new GridLayout(4, 2, 0, 0));
        grid.setOpaque(false);
        String[][] items = {
                {"Mã sinh viên", maSinhVien}, {"Họ và tên", tenSinhVien},
                {"Ngày sinh", ngaySinh}, {"Trình độ", trinhDo},
                {"Số điện thoại", soDienThoai}, {"Mã GV phụ trách", idGiaoVien},
                {"Giáo viên phụ trách", tenGiaoVien}, {"Trạng thái học tập", learningStatus()}
        };
        for (String[] it : items) grid.add(profileItem(it[0], it[1]));

        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.setOpaque(false);
        JPanel headWrap = new JPanel(new BorderLayout());
        headWrap.setOpaque(false);
        headWrap.add(head, BorderLayout.CENTER);
        headWrap.setPreferredSize(new Dimension(0, 128));
        content.add(headWrap, BorderLayout.NORTH);
        content.add(grid, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private LiftCard profileItem(String label, String val) {
        LiftCard c = new LiftCard(new GridLayout(2, 1, 0, 4), 16).padding(12, 20, 12, 20);
        JLabel l = new JLabel(label);
        l.setFont(font(Font.PLAIN, 12));
        l.setForeground(COLOR_MUTED);
        JLabel v = new JLabel(val == null || val.isEmpty() ? "—" : val);
        v.setFont(font(Font.BOLD, 16));
        v.setForeground(COLOR_TEXT);
        c.add(l);
        c.add(v);
        return c.install();
    }

    private void updatePhone() {
        String input = JOptionPane.showInputDialog(this,
                "Nhập số điện thoại mới (10 chữ số, bắt đầu bằng 0):", soDienThoai);
        if (input == null) return;
        input = input.trim();
        if (!PHONE.matcher(input).matches()) {
            warn("Số điện thoại không hợp lệ. Vui lòng nhập đúng 10 chữ số, bắt đầu bằng 0.");
            return;
        }
        if (input.equals(soDienThoai)) return;
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) {
                warn("Không thể kết nối cơ sở dữ liệu.");
                return;
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE Hoc_sinh SET so_dien_thoai = ? WHERE ma_sinh_vien = ?")) {
                ps.setString(1, input);
                ps.setString(2, maSinhVien);
                if (ps.executeUpdate() == 1) {
                    soDienThoai = input;
                    JOptionPane.showMessageDialog(this, "Đã cập nhật số điện thoại.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    showCard("Profile");
                }
            }
        } catch (SQLException e) {
            showError("Không thể cập nhật số điện thoại:", e);
        }
    }

    // ===================== MAIN =====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new StudentDashboardUI().setVisible(true);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Không thể mở cổng sinh viên",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}