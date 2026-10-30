import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Cổng Giáo viên S-Link.
 * Dựa trên sơ đồ ER: Giáo viên -(Phụ trách)-> Lịch học, -(Quản lí)-> Lớp, -(Kèm cặp)-> Học viên,
 * -(Nhận)-> Phiếu lương; Học viên -(Gửi)-> Đánh giá & Phản hồi (theo Lớp).
 */
public class TeacherDashboardUI extends JFrame {

    private final String idGiaoVien;
    private String tenGiaoVien = "";
    private String soDienThoai = "";
    private String chungChi = "";
    private String quocTich = "";

    // Tập các lớp của giáo viên: lớp trong bảng Quanli HOẶC lớp có lịch do giáo viên dạy (cần 2 tham số)
    private static final String LOP_SET =
            "(SELECT id_lop FROM Quanli WHERE id_giao_vien = ? UNION SELECT id_lop FROM Lich_hoc WHERE id_giao_vien = ?)";

    private static final Color COLOR_PRIMARY = new Color(79, 70, 229);
    private static final Color COLOR_PRIMARY_DARK = new Color(67, 56, 202);
    private static final Color COLOR_LIGHT = new Color(224, 231, 255);
    private static final Color COLOR_BG = new Color(244, 247, 251);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_HOVER = new Color(241, 245, 249);
    private static final Color COLOR_ACTIVE_BG = new Color(238, 242, 255);
    private static final Color COLOR_SELECT = new Color(224, 231, 255);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private static final Color COLOR_SUCCESS_DARK = new Color(5, 150, 105);
    private static final Color COLOR_WARNING = new Color(245, 158, 11);
    private static final Color COLOR_DANGER = new Color(220, 38, 38);
    private static final Color COLOR_STRIPE = new Color(248, 250, 252);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] LEVELS = {"N5", "N4", "N3", "N2", "N1"};

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private final List<NavButton> navButtons = new ArrayList<>();
    private final Map<String, Supplier<JPanel>> builders = new LinkedHashMap<>();
    private final Map<String, JPanel> currentPanels = new HashMap<>();

    @FunctionalInterface
    private interface RowMapper {
        Object[] map(ResultSet rs) throws SQLException;
    }

    public TeacherDashboardUI() {
        this("GV01");
    }

    public TeacherDashboardUI(String idGiaoVien) {
        this.idGiaoVien = idGiaoVien;
        loadTeacherProfile();

        setTitle("CỔNG GIÁO VIÊN S-LINK - " + tenGiaoVien.toUpperCase());
        setSize(1320, 800);
        setMinimumSize(new Dimension(1100, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(COLOR_BG);

        builders.put("Dashboard", this::createDashboardPanel);
        builders.put("LichDay", this::createLichDayPanel);
        builders.put("Lop", this::createLopPanel);
        builders.put("HocVien", this::createHocVienPanel);
        builders.put("DanhGia", this::createDanhGiaPanel);
        builders.put("Luong", this::createLuongPanel);
        builders.put("Profile", this::createProfilePanel);

        add(createTopBar(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);

        showCard("Dashboard");
    }

    private void showCard(String name) {
        JPanel old = currentPanels.get(name);
        if (old != null) mainContentPanel.remove(old);
        JPanel fresh = builders.get(name).get();
        currentPanels.put(name, fresh);
        mainContentPanel.add(fresh, name);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
        cardLayout.show(mainContentPanel, name);
    }

    /* ===================== DATABASE HELPERS ===================== */

    private void showError(String msg, Exception e) {
        JOptionPane.showMessageDialog(this, msg + "\n" + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void loadRows(DefaultTableModel model, String sql, RowMapper mapper, String... params) {
        model.setRowCount(0);
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) model.addRow(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            showError("Lỗi truy vấn dữ liệu:", e);
        }
    }

    /** Lấy 1 số; trả về null nếu không có dữ liệu (vd: AVG của bảng rỗng). */
    private Double scalar(String sql, String... params) {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return null;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double d = rs.getDouble(1);
                        return rs.wasNull() ? null : d;
                    }
                }
            }
        } catch (SQLException e) {
            showError("Lỗi truy vấn dữ liệu:", e);
        }
        return null;
    }

    private String[] gv2() {
        return new String[]{idGiaoVien, idGiaoVien};
    }

    private void loadTeacherProfile() {
        String sql = "SELECT ho_ten, so_dien_thoai, chung_chi, quoc_tich FROM Giao_vien WHERE id_giao_vien = ?";
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) {
                JOptionPane.showMessageDialog(this, "Không thể kết nối CSDL MySQL BTL!", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, idGiaoVien);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tenGiaoVien = nz(rs.getString("ho_ten"));
                        soDienThoai = nz(rs.getString("so_dien_thoai"));
                        chungChi = nz(rs.getString("chung_chi"));
                        quocTich = nz(rs.getString("quoc_tich"));
                    }
                }
            }
        } catch (SQLException e) {
            showError("Lỗi SQL:", e);
        }
    }

    private boolean updateTrangThaiBuoi(String maLich, String value) {
        String sql = "UPDATE Lich_hoc SET trang_thai_buoi = ? WHERE ma_lich_hoc = ? AND id_giao_vien = ?";
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return false;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, value);
                ps.setString(2, maLich);
                ps.setString(3, idGiaoVien); // chỉ được sửa buổi của chính mình
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            showError("Lỗi cập nhật buổi học:", e);
            return false;
        }
    }

    /* ===================== FORMAT HELPERS ===================== */

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String fmt(Timestamp ts) {
        return ts == null ? "" : ts.toLocalDateTime().format(DATE_FMT);
    }

    private static String vnd(long n) {
        return String.format(Locale.US, "%,d", n).replace(',', '.') + " VNĐ";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String buoi(String s) {
        if (s == null) return "";
        switch (s.trim().toLowerCase()) {
            case "day": return "Có dạy";
            case "nghi": return "Nghỉ";
            default: return s;
        }
    }

    private static String ketQua(String s) {
        if (s == null) return "Chưa thi";
        switch (s.trim().toLowerCase()) {
            case "dat": return "Đạt";
            case "truot": return "Trượt";
            default: return s;
        }
    }

    private String initial() {
        String t = tenGiaoVien.trim();
        if (t.isEmpty()) return "G";
        String last = t.substring(t.lastIndexOf(' ') + 1);
        return last.isEmpty() ? "G" : last.substring(0, 1).toUpperCase();
    }

    private static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    /* ===================== UI PRIMITIVES ===================== */

    private enum IconType { HOME, CALENDAR, BOOK, USERS, STAR, WALLET, USER }

    private static void drawIcon(Graphics g, IconType type, int x, int y, int size, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        double s = size / 20.0;
        g2.scale(s, s);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case HOME:
                Path2D roof = new Path2D.Float();
                roof.moveTo(3, 9.5); roof.lineTo(10, 3.5); roof.lineTo(17, 9.5);
                g2.draw(roof);
                Path2D body = new Path2D.Float();
                body.moveTo(5, 8.5); body.lineTo(5, 16.5); body.lineTo(15, 16.5); body.lineTo(15, 8.5);
                g2.draw(body);
                break;
            case CALENDAR:
                g2.drawRoundRect(3, 4, 14, 13, 3, 3);
                g2.drawLine(3, 8, 17, 8);
                g2.drawLine(7, 2, 7, 5); g2.drawLine(13, 2, 13, 5);
                break;
            case BOOK:
                g2.drawRoundRect(3, 3, 14, 14, 2, 2);
                g2.drawLine(3, 7, 17, 7);
                g2.drawLine(8, 7, 8, 17);
                break;
            case USERS:
                g2.drawOval(2, 5, 5, 5);
                g2.drawOval(13, 5, 5, 5);
                g2.drawOval(7, 3, 6, 6);
                g2.drawArc(4, 12, 12, 9, 0, 180);
                break;
            case STAR:
                Path2D st = new Path2D.Float();
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? 8 : 3.6;
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    double px = 10 + r * Math.cos(a), py = 10.5 + r * Math.sin(a);
                    if (i == 0) st.moveTo(px, py); else st.lineTo(px, py);
                }
                st.closePath();
                g2.draw(st);
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

    private static class PrimaryButton extends JButton {
        private final Color base;

        PrimaryButton(String text, Color base) {
            super(text);
            this.base = base;
            setFont(font(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled()) g2.setColor(new Color(148, 163, 184));
            else if (getModel().isPressed()) g2.setColor(base.darker().darker());
            else if (getModel().isRollover()) g2.setColor(base.darker());
            else g2.setColor(base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private Color accent;

        RoundedPanel(LayoutManager lm, int radius, Color fill) {
            super(lm);
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        RoundedPanel accent(Color c) {
            this.accent = c;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fillRoundRect(1, 2, w, h, radius, radius);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h - 1, radius, radius);
            if (accent != null) {
                Shape old = g2.getClip();
                g2.clip(new RoundRectangle2D.Float(0, 0, w, h - 1, radius, radius));
                g2.setColor(accent);
                g2.fillRect(0, 0, 5, h);
                g2.setClip(old);
            }
            g2.setColor(COLOR_BORDER);
            g2.drawRoundRect(0, 0, w, h - 1, radius, radius);
            g2.dispose();
        }
    }

    private static class GradientBanner extends JPanel {
        GradientBanner() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, COLOR_PRIMARY_DARK, getWidth(), getHeight(), new Color(99, 102, 241)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(new Color(255, 255, 255, 28));
            g2.fillOval(getWidth() - 150, -60, 220, 220);
            g2.fillOval(getWidth() - 260, getHeight() - 70, 140, 140);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class NavButton extends JButton {
        private boolean active;
        private final IconType icon;

        NavButton(String text, IconType icon) {
            super(text);
            this.icon = icon;
            setFont(font(Font.PLAIN, 14));
            setForeground(COLOR_TEXT_MAIN);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 46, 0, 0));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setPreferredSize(new Dimension(220, 46));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setActive(boolean a) {
            active = a;
            setForeground(a ? COLOR_PRIMARY : COLOR_TEXT_MAIN);
            setFont(font(a ? Font.BOLD : Font.PLAIN, 14));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (active) {
                g2.setColor(COLOR_ACTIVE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(COLOR_PRIMARY);
                g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
            } else if (getModel().isRollover()) {
                g2.setColor(COLOR_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.dispose();
            drawIcon(g, icon, 16, (getHeight() - 20) / 2, 20, active ? COLOR_PRIMARY : COLOR_TEXT_MUTED);
            super.paintComponent(g);
        }
    }

    private static class Badge extends JComponent {
        private final String text;
        private final Color bg, fg;

        Badge(String text, Color bg, Color fg, int size) {
            this.text = text;
            this.bg = bg;
            this.fg = fg;
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setColor(fg);
            g2.setFont(font(Font.BOLD, Math.max(13, getWidth() / 3)));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /* ===================== TOP BAR + SIDEBAR ===================== */

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        bar.setPreferredSize(new Dimension(0, 68));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(0, 14, 0, 0));
        left.add(new Badge("GV", COLOR_PRIMARY, Color.WHITE, 40));
        JLabel lblLogo = new JLabel("Cổng Giáo Viên S-Link");
        lblLogo.setFont(font(Font.BOLD, 20));
        lblLogo.setForeground(COLOR_TEXT_MAIN);
        left.add(lblLogo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 11));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 14));
        right.add(new Badge(initial(), COLOR_LIGHT, COLOR_PRIMARY, 42));

        JPanel detail = new JPanel(new GridLayout(2, 1));
        detail.setOpaque(false);
        JLabel lblName = new JLabel(tenGiaoVien);
        lblName.setFont(font(Font.BOLD, 14));
        lblName.setForeground(COLOR_TEXT_MAIN);
        JLabel lblId = new JLabel(idGiaoVien + "  |  Chứng chỉ: " + chungChi);
        lblId.setFont(font(Font.PLAIN, 12));
        lblId.setForeground(COLOR_TEXT_MUTED);
        detail.add(lblName);
        detail.add(lblId);
        right.add(detail);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(font(Font.BOLD, 13));
        btnLogout.setForeground(new Color(220, 38, 38));
        btnLogout.setBackground(new Color(254, 242, 242));
        btnLogout.setOpaque(true);
        btnLogout.setContentAreaFilled(true);
        btnLogout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                new EmptyBorder(7, 14, 7, 14)));
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có muốn đăng xuất khỏi hệ thống?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose();
                new LoginForm().setVisible(true);
            }
        });
        right.add(btnLogout);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));

        JLabel lblSection = new JLabel("GÓC GIẢNG DẠY");
        lblSection.setFont(font(Font.BOLD, 11));
        lblSection.setForeground(COLOR_TEXT_MUTED);
        lblSection.setBorder(new EmptyBorder(24, 24, 12, 10));

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(new EmptyBorder(0, 12, 10, 12));

        NavButton first = createNavButton("Tổng quan", IconType.HOME, "Dashboard");
        NavButton[] all = {
                first,
                createNavButton("Lịch dạy", IconType.CALENDAR, "LichDay"),
                createNavButton("Lớp phụ trách", IconType.BOOK, "Lop"),
                createNavButton("Học viên của tôi", IconType.USERS, "HocVien"),
                createNavButton("Đánh giá & phản hồi", IconType.STAR, "DanhGia"),
                createNavButton("Phiếu lương", IconType.WALLET, "Luong"),
                createNavButton("Thông tin cá nhân", IconType.USER, "Profile")
        };
        for (int i = 0; i < all.length; i++) {
            navPanel.add(all[i]);
            if (i < all.length - 1) navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        JLabel footer = new JLabel("S-Link Teacher Portal v1.0");
        footer.setFont(font(Font.PLAIN, 11));
        footer.setForeground(COLOR_TEXT_MUTED);
        footer.setBorder(new EmptyBorder(10, 24, 16, 10));

        sidebar.add(lblSection, BorderLayout.NORTH);
        sidebar.add(navPanel, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);

        setNavActive(first);
        return sidebar;
    }

    private NavButton createNavButton(String text, IconType icon, String cardName) {
        NavButton button = new NavButton(text, icon);
        button.addActionListener(e -> {
            setNavActive(button);
            showCard(cardName);
        });
        navButtons.add(button);
        return button;
    }

    private void setNavActive(NavButton active) {
        for (NavButton b : navButtons) b.setActive(b == active);
    }

    /* ===================== PANELS ===================== */

    private JPanel pagePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));
        return panel;
    }

    private JLabel pageTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(font(Font.BOLD, 22));
        title.setForeground(COLOR_TEXT_MAIN);
        return title;
    }

    /** Tổng quan */
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel banner = new GradientBanner();
        banner.setBorder(new EmptyBorder(22, 26, 22, 26));
        JLabel lblWelcome = new JLabel("<html><span style='font-size:20px'><b>Xin chào, " + esc(tenGiaoVien) + "!</b></span><br>"
                + "<span style='font-size:12px;color:#E0E7FF'>Quản lý lịch dạy, lớp học, học viên và phản hồi của bạn tại S-Link.</span></html>");
        lblWelcome.setFont(font(Font.PLAIN, 14));
        lblWelcome.setForeground(Color.WHITE);
        banner.add(lblWelcome, BorderLayout.WEST);

        long soLop = nvl(scalar("SELECT COUNT(*) FROM Lop WHERE id_lop IN " + LOP_SET, gv2()));
        long soHocVien = nvl(scalar("SELECT COUNT(*) FROM Hoc_sinh WHERE id_giao_vien = ?", idGiaoVien));
        long soBuoiSapToi = nvl(scalar("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? "
                + "AND ngay_day >= CURDATE() AND trang_thai_buoi = 'day'", idGiaoVien));
        Double avg = scalar("SELECT AVG(rating) FROM Danh_gia_va_phan_hoi WHERE id_lop IN " + LOP_SET, gv2());
        long soDanhGia = nvl(scalar("SELECT COUNT(*) FROM Danh_gia_va_phan_hoi WHERE id_lop IN " + LOP_SET, gv2()));
        Double luong = scalar("SELECT tong_luong FROM Phieu_luong WHERE id_giao_vien = ? ORDER BY ngay_tra DESC LIMIT 1", idGiaoVien);

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 112));
        stats.add(createStatCard("Lớp phụ trách", soLop + " lớp", "Học viên kèm cặp: " + soHocVien, COLOR_PRIMARY));
        stats.add(createStatCard("Buổi dạy sắp tới", soBuoiSapToi + " buổi", "Tính từ hôm nay", COLOR_SUCCESS));
        stats.add(createStatCard("Đánh giá trung bình",
                avg == null ? "Chưa có" : String.format(Locale.US, "%.1f / 5", avg),
                soDanhGia + " lượt đánh giá", COLOR_WARNING));
        stats.add(createStatCard("Lương kỳ gần nhất", luong == null ? "Chưa có" : vnd(luong.longValue()),
                "Xem chi tiết ở mục Phiếu lương", new Color(13, 148, 136)));

        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        centerGrid.setOpaque(false);

        RoundedPanel cardSchedule = createCard("Lịch dạy gần đây");
        DefaultTableModel mLich = new DefaultTableModel(new String[]{"Ngày dạy", "Ca học", "Lớp", "Phòng", "Trạng thái"}, 0);
        loadRows(mLich,
                "SELECT lh.ngay_day, lh.ca_hoc, l.ten_lop, lh.phong_hoc, lh.trang_thai_buoi FROM Lich_hoc lh "
                        + "JOIN Lop l ON lh.id_lop = l.id_lop WHERE lh.id_giao_vien = ? ORDER BY lh.ngay_day DESC LIMIT 5",
                rs -> new Object[]{fmt(rs.getTimestamp(1)), rs.getString(2), rs.getString(3), rs.getString(4), buoi(rs.getString(5))},
                idGiaoVien);
        cardSchedule.add(createScroll(createTable(mLich, 4, -1), "Chưa có lịch dạy"), BorderLayout.CENTER);

        RoundedPanel cardReview = createCard("Phản hồi mới nhất từ học viên");
        DefaultTableModel mDg = new DefaultTableModel(new String[]{"Học viên", "Đánh giá", "Nội dung"}, 0);
        loadRows(mDg,
                "SELECT h.ho_ten, d.rating, d.mo_ta FROM Danh_gia_va_phan_hoi d "
                        + "JOIN Hoc_sinh h ON d.ma_sinh_vien = h.ma_sinh_vien "
                        + "WHERE d.id_lop IN " + LOP_SET + " ORDER BY d.ngay_gui DESC LIMIT 5",
                rs -> new Object[]{rs.getString(1), rs.getInt(2), rs.getString(3)},
                gv2());
        cardReview.add(createScroll(createTable(mDg, -1, 1), "Chưa có phản hồi nào"), BorderLayout.CENTER);

        centerGrid.add(cardSchedule);
        centerGrid.add(cardReview);

        JPanel topStack = new JPanel(new BorderLayout(0, 16));
        topStack.setOpaque(false);
        topStack.add(banner, BorderLayout.NORTH);
        topStack.add(stats, BorderLayout.SOUTH);

        panel.add(topStack, BorderLayout.NORTH);
        panel.add(centerGrid, BorderLayout.CENTER);
        return panel;
    }

    private static long nvl(Double d) {
        return d == null ? 0 : d.longValue();
    }

    /** Lịch dạy: giáo viên đánh dấu buổi Có dạy / Nghỉ */
    private JPanel createLichDayPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Lịch dạy của tôi");

        RoundedPanel card = createCard("Chi tiết các buổi dạy — chọn một buổi rồi đánh dấu trạng thái");
        String[] cols = {"Mã Lịch", "Lớp", "Ngày Dạy", "Ca Học", "Phòng", "Nội Dung", "Trạng Thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT lh.ma_lich_hoc, l.ten_lop, lh.ngay_day, lh.ca_hoc, lh.phong_hoc, lh.noi_dung, lh.trang_thai_buoi "
                        + "FROM Lich_hoc lh JOIN Lop l ON lh.id_lop = l.id_lop "
                        + "WHERE lh.id_giao_vien = ? ORDER BY lh.ngay_day DESC",
                rs -> new Object[]{rs.getString(1), rs.getString(2), fmt(rs.getTimestamp(3)), rs.getString(4),
                        rs.getString(5), rs.getString(6), buoi(rs.getString(7))},
                idGiaoVien);

        JTable table = createTable(model, 6, -1);
        card.add(createScroll(table, "Chưa có lịch dạy nào"), BorderLayout.CENTER);

        PrimaryButton btnDay = new PrimaryButton("Đánh dấu: Có dạy", COLOR_SUCCESS_DARK);
        PrimaryButton btnNghi = new PrimaryButton("Đánh dấu: Nghỉ", COLOR_DANGER);
        boolean has = model.getRowCount() > 0;
        btnDay.setEnabled(has);
        btnNghi.setEnabled(has);

        btnDay.addActionListener(e -> changeStatus(table, model, "day", "Có dạy"));
        btnNghi.addActionListener(e -> changeStatus(table, model, "nghi", "Nghỉ"));

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        foot.setOpaque(false);
        foot.add(btnNghi);
        foot.add(btnDay);
        card.add(foot, BorderLayout.SOUTH);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void changeStatus(JTable table, DefaultTableModel model, String dbValue, String label) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 buổi học trong bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        String maLich = model.getValueAt(row, 0).toString();
        String ngay = model.getValueAt(row, 2).toString();
        String lop = model.getValueAt(row, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Đánh dấu buổi " + maLich + " (" + lop + ", " + ngay + ") là: " + label + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION && updateTrangThaiBuoi(maLich, dbValue)) {
            showCard("LichDay");
        }
    }

    /** Lớp phụ trách (Giáo viên - Quản lí - Lớp) */
    private JPanel createLopPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Lớp học phụ trách");

        RoundedPanel card = createCard("Danh sách lớp tôi đang giảng dạy");
        String[] cols = {"Mã Lớp", "Tên Lớp", "Khóa Học", "Trình Độ", "Niên Khóa", "Sĩ Số"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT l.id_lop, l.ten_lop, k.ten_khoa_hoc, k.trinh_do, l.nien_khoa, l.si_so "
                        + "FROM Lop l JOIN Khoa_hoc k ON l.ma_khoa_hoc = k.ma_khoa_hoc "
                        + "WHERE l.id_lop IN " + LOP_SET + " ORDER BY l.ten_lop",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6) == null ? "—" : rs.getString(6)},
                gv2());

        card.add(createScroll(createTable(model, -1, -1), "Bạn chưa được phân công lớp nào"), BorderLayout.CENTER);
        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    /** Học viên được kèm cặp (Giáo viên - Kèm cặp - Học viên) + điểm thi mới nhất */
    private JPanel createHocVienPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Học viên của tôi");

        RoundedPanel card = createCard("Học viên do tôi phụ trách");
        String[] cols = {"Mã SV", "Họ Tên", "Ngày Sinh", "Trình Độ", "Số Điện Thoại", "Điểm Thi Mới Nhất", "Kết Quả"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT h.ma_sinh_vien, h.ho_ten, h.ngay_sinh, h.trinh_do, h.so_dien_thoai, "
                        + "(SELECT k.tong_diem FROM Ket_qua_thi k WHERE k.ma_sinh_vien = h.ma_sinh_vien ORDER BY LENGTH(k.ma_bai_lam) DESC, k.ma_bai_lam DESC LIMIT 1), "
                        + "(SELECT k.tinh_trang FROM Ket_qua_thi k WHERE k.ma_sinh_vien = h.ma_sinh_vien ORDER BY LENGTH(k.ma_bai_lam) DESC, k.ma_bai_lam DESC LIMIT 1) "
                        + "FROM Hoc_sinh h WHERE h.id_giao_vien = ? ORDER BY h.ho_ten",
                rs -> new Object[]{rs.getString(1), rs.getString(2), fmt(rs.getTimestamp(3)), rs.getString(4),
                        rs.getString(5), rs.getString(6) == null ? "—" : rs.getString(6), ketQua(rs.getString(7))},
                idGiaoVien);

        JTable table = createTable(model, 6, -1);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField txtSearch = new JTextField();
        txtSearch.setFont(font(Font.PLAIN, 13));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER), new EmptyBorder(8, 10, 8, 10)));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo mã, tên, trình độ...");
        txtSearch.setToolTipText("Gõ để lọc danh sách học viên");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() {
                String t = txtSearch.getText().trim();
                sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t)));
            }
            public void insertUpdate(DocumentEvent e) { apply(); }
            public void removeUpdate(DocumentEvent e) { apply(); }
            public void changedUpdate(DocumentEvent e) { apply(); }
        });

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(txtSearch, BorderLayout.NORTH);
        center.add(createScroll(table, "Bạn chưa phụ trách học viên nào"), BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        PrimaryButton btnDiem = new PrimaryButton("Nhập điểm thi", COLOR_PRIMARY);
        btnDiem.setEnabled(model.getRowCount() > 0);
        btnDiem.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 học viên trong bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(viewRow);
            showAddScoreDialog(model.getValueAt(row, 0).toString(), model.getValueAt(row, 1).toString(),
                    model.getValueAt(row, 3).toString());
        });
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        foot.setOpaque(false);
        foot.add(btnDiem);
        card.add(foot, BorderLayout.SOUTH);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    /** Đánh giá & phản hồi của học viên theo các lớp tôi dạy */
    private JPanel createDanhGiaPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Đánh giá & phản hồi từ học viên");

        RoundedPanel card = createCard("Phản hồi theo các lớp tôi phụ trách");
        String[] cols = {"Ngày Gửi", "Học Viên", "Lớp", "Đánh Giá", "Nội Dung"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT d.ngay_gui, h.ho_ten, l.ten_lop, d.rating, d.mo_ta FROM Danh_gia_va_phan_hoi d "
                        + "JOIN Hoc_sinh h ON d.ma_sinh_vien = h.ma_sinh_vien "
                        + "JOIN Lop l ON d.id_lop = l.id_lop "
                        + "WHERE d.id_lop IN " + LOP_SET + " ORDER BY d.ngay_gui DESC",
                rs -> new Object[]{fmt(rs.getTimestamp(1)), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5)},
                gv2());

        JTable table = createTable(model, -1, 3);
        table.getColumnModel().getColumn(4).setPreferredWidth(320);
        card.add(createScroll(table, "Chưa có phản hồi nào"), BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    /** Phiếu lương (Giáo viên - Nhận - Phiếu lương) */
    private JPanel createLuongPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Phiếu lương");

        RoundedPanel card = createCard("Lịch sử nhận lương");
        String[] cols = {"Mã Phiếu", "Từ Ngày", "Đến Ngày", "Ngày Trả", "Tổng Lương"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT ma_phieu_luong, tu_ngay, den_ngay, ngay_tra, tong_luong FROM Phieu_luong "
                        + "WHERE id_giao_vien = ? ORDER BY ngay_tra DESC",
                rs -> new Object[]{rs.getString(1), fmt(rs.getTimestamp(2)), fmt(rs.getTimestamp(3)),
                        fmt(rs.getTimestamp(4)), vnd(rs.getLong(5))},
                idGiaoVien);

        Double tong = scalar("SELECT SUM(tong_luong) FROM Phieu_luong WHERE id_giao_vien = ?", idGiaoVien);
        JLabel lblTong = new JLabel("Tổng đã nhận: " + (tong == null ? vnd(0) : vnd(tong.longValue())));
        lblTong.setFont(font(Font.BOLD, 14));
        lblTong.setForeground(COLOR_SUCCESS_DARK);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(lblTong, BorderLayout.NORTH);
        center.add(createScroll(createTable(model, -1, -1), "Chưa có phiếu lương nào"), BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Thông tin cá nhân Giáo viên");

        RoundedPanel card = createCard("Hồ sơ giáo viên");

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        head.setOpaque(false);
        head.add(new Badge(initial(), COLOR_LIGHT, COLOR_PRIMARY, 72));
        JLabel name = new JLabel("<html><span style='font-size:18px'><b>" + esc(tenGiaoVien) + "</b></span><br>"
                + "<span style='font-size:12px;color:#64748B'>" + esc(idGiaoVien) + " • " + esc(quocTich) + "</span></html>");
        name.setFont(font(Font.PLAIN, 14));
        head.add(name);

        JPanel grid = new JPanel(new GridLayout(3, 2, 18, 14));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(16, 0, 0, 0));
        grid.add(profileItem("ID giáo viên", idGiaoVien));
        grid.add(profileItem("Họ và tên", tenGiaoVien));
        grid.add(profileItem("Số điện thoại", soDienThoai));
        grid.add(profileItem("Chứng chỉ", chungChi));
        grid.add(profileItem("Quốc tịch", quocTich));
        grid.add(profileItem("Trạng thái", "Đang giảng dạy"));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(head, BorderLayout.NORTH);
        body.add(grid, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel profileItem(String label, String val) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER), new EmptyBorder(4, 0, 8, 0)));
        JLabel lbl = new JLabel(label);
        lbl.setFont(font(Font.PLAIN, 12));
        lbl.setForeground(COLOR_TEXT_MUTED);
        JLabel v = new JLabel(val == null || val.isEmpty() ? "—" : val);
        v.setFont(font(Font.BOLD, 15));
        v.setForeground(COLOR_TEXT_MAIN);
        p.add(lbl);
        p.add(v);
        return p;
    }

    /* ===================== NHẬP ĐIỂM THI ===================== */

    private static String tinhTinhTrang(String lv, int dh, int tv, int ng) {
        int tong = dh + tv + ng;
        boolean nhom45 = (dh + tv) >= 38 && ng >= 19;
        boolean moiPhan = dh >= 19 && tv >= 19 && ng >= 19;
        boolean dat;
        switch (lv == null ? "" : lv) {
            case "N5": dat = nhom45 && tong >= 80; break;
            case "N4": dat = nhom45 && tong >= 90; break;
            case "N3": dat = moiPhan && tong >= 95; break;
            case "N2": dat = moiPhan && tong >= 90; break;
            case "N1": dat = moiPhan && tong >= 100; break;
            default: dat = false;
        }
        return dat ? "dat" : "truot";
    }

    private List<String[]> loadDeThi() {
        List<String[]> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return list;
            try (PreparedStatement ps = c.prepareStatement("SELECT ma_de_thi, cap_do FROM De_thi ORDER BY ma_de_thi");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new String[]{rs.getString(1), nz(rs.getString(2))});
            }
        } catch (SQLException e) {
            showError("Lỗi tải danh sách đề thi:", e);
        }
        return list;
    }

    /** Ghi điểm vào bảng Ket_qua_thi. Chỉ ghi nếu học viên thuộc giáo viên này. */
    private boolean insertKetQua(String maBai, String maSv, String level, String maDe,
                                 int dh, int tv, int ng, int tong, String tinhTrang) {
        String sql = "INSERT INTO Ket_qua_thi (ma_bai_lam, ma_sinh_vien, trinh_do, ma_de_thi, diem_doc_hieu, "
                + "diem_tu_vung, diem_nghe, tong_diem, tinh_trang) "
                + "SELECT ?, h.ma_sinh_vien, ?, ?, ?, ?, ?, ?, ? FROM Hoc_sinh h "
                + "WHERE h.ma_sinh_vien = ? AND h.id_giao_vien = ?";
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return false;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, maBai);
                ps.setString(2, level);
                ps.setString(3, maDe);
                ps.setInt(4, dh);
                ps.setInt(5, tv);
                ps.setInt(6, ng);
                ps.setInt(7, tong);
                ps.setString(8, tinhTrang);
                ps.setString(9, maSv);
                ps.setString(10, idGiaoVien);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    JOptionPane.showMessageDialog(this, "Học viên này không thuộc quyền phụ trách của bạn.", "Không thể lưu", JOptionPane.WARNING_MESSAGE);
                }
                return rows > 0;
            }
        } catch (SQLException e) {
            showError("Lỗi lưu điểm thi:", e);
            return false;
        }
    }

    private static JSpinner scoreSpinner() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
        sp.setFont(font(Font.PLAIN, 13));
        return sp;
    }

    private static JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 13));
        l.setForeground(COLOR_TEXT_MUTED);
        return l;
    }

    private void showAddScoreDialog(String maSv, String tenSv, String trinhDoHienTai) {
        List<String[]> allDe = loadDeThi();
        if (allDe.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có đề thi nào trong bảng De_thi.\nHãy thêm đề thi trước khi nhập điểm.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog(this, "Nhập điểm thi", true);
        dlg.setLayout(new BorderLayout());

        JLabel head = new JLabel("<html><span style='font-size:15px'><b>" + esc(tenSv) + "</b></span><br>"
                + "<span style='font-size:12px;color:#64748B'>" + esc(maSv) + "</span></html>");
        head.setFont(font(Font.PLAIN, 13));
        head.setBorder(new EmptyBorder(18, 22, 4, 22));

        JComboBox<String> cbLevel = new JComboBox<>(LEVELS);
        String lv = nz(trinhDoHienTai).trim().toUpperCase();
        for (String l : LEVELS) if (l.equals(lv)) cbLevel.setSelectedItem(l);
        JComboBox<String> cbDe = new JComboBox<>();
        List<String[]> shown = new ArrayList<>();
        JSpinner sDh = scoreSpinner(), sTv = scoreSpinner(), sNg = scoreSpinner();
        JLabel lblPreview = new JLabel();
        lblPreview.setFont(font(Font.BOLD, 14));

        Runnable fillDe = () -> {
            shown.clear();
            cbDe.removeAllItems();
            String level = (String) cbLevel.getSelectedItem();
            for (String[] d : allDe) if (d[1].equalsIgnoreCase(level)) shown.add(d);
            if (shown.isEmpty()) shown.addAll(allDe); // không có đề đúng cấp độ thì hiện tất cả
            for (String[] d : shown) cbDe.addItem(d[0] + " (" + d[1] + ")");
        };
        Runnable preview = () -> {
            int dh = (Integer) sDh.getValue(), tv = (Integer) sTv.getValue(), ng = (Integer) sNg.getValue();
            boolean dat = "dat".equals(tinhTinhTrang((String) cbLevel.getSelectedItem(), dh, tv, ng));
            lblPreview.setText("Tổng điểm: " + (dh + tv + ng) + " / 180   →   " + (dat ? "Đạt" : "Trượt"));
            lblPreview.setForeground(dat ? COLOR_SUCCESS_DARK : COLOR_DANGER);
        };
        cbLevel.addActionListener(e -> { fillDe.run(); preview.run(); });
        sDh.addChangeListener(e -> preview.run());
        sTv.addChangeListener(e -> preview.run());
        sNg.addChangeListener(e -> preview.run());
        fillDe.run();
        preview.run();

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setBorder(new EmptyBorder(14, 22, 6, 22));
        form.add(formLabel("Trình độ"));
        form.add(cbLevel);
        form.add(formLabel("Mã đề thi"));
        form.add(cbDe);
        form.add(formLabel("Điểm đọc hiểu (0-60)"));
        form.add(sDh);
        form.add(formLabel("Điểm từ vựng (0-60)"));
        form.add(sTv);
        form.add(formLabel("Điểm nghe (0-60)"));
        form.add(sNg);

        JPanel center = new JPanel(new BorderLayout());
        center.add(form, BorderLayout.CENTER);
        JPanel previewWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 22, 8));
        previewWrap.add(lblPreview);
        center.add(previewWrap, BorderLayout.SOUTH);

        PrimaryButton btnSave = new PrimaryButton("Lưu điểm", COLOR_PRIMARY);
        PrimaryButton btnCancel = new PrimaryButton("Hủy", new Color(100, 116, 139));
        btnSave.setPreferredSize(new Dimension(120, 38));
        btnCancel.setPreferredSize(new Dimension(100, 38));
        btnCancel.addActionListener(e -> dlg.dispose());
        btnSave.addActionListener(e -> {
            if (shown.isEmpty() || cbDe.getSelectedIndex() < 0) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn mã đề thi!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String level = (String) cbLevel.getSelectedItem();
            String maDe = shown.get(cbDe.getSelectedIndex())[0];
            int dh = (Integer) sDh.getValue(), tv = (Integer) sTv.getValue(), ng = (Integer) sNg.getValue();
            String maBai = "BL" + (System.currentTimeMillis() / 1000);
            if (insertKetQua(maBai, maSv, level, maDe, dh, tv, ng, dh + tv + ng, tinhTinhTrang(level, dh, tv, ng))) {
                dlg.dispose();
                JOptionPane.showMessageDialog(this, "Đã lưu điểm thi (" + maBai + ") vào CSDL.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                showCard("HocVien");
            }
        });
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        foot.setBorder(new EmptyBorder(0, 12, 10, 12));
        foot.add(btnCancel);
        foot.add(btnSave);

        dlg.add(head, BorderLayout.NORTH);
        dlg.add(center, BorderLayout.CENTER);
        dlg.add(foot, BorderLayout.SOUTH);
        dlg.setSize(440, 430);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    /* ===================== COMPONENT FACTORIES ===================== */

    private RoundedPanel createCard(String title) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 16, COLOR_CARD);
        card.setBorder(new EmptyBorder(18, 20, 20, 20));
        if (title != null) {
            JLabel lbl = new JLabel(title);
            lbl.setFont(font(Font.BOLD, 16));
            lbl.setForeground(COLOR_TEXT_MAIN);
            card.add(lbl, BorderLayout.NORTH);
        }
        return card;
    }

    private JPanel createStatCard(String title, String val, String sub, Color accent) {
        RoundedPanel card = new RoundedPanel(new GridLayout(3, 1, 0, 2), 14, Color.WHITE).accent(accent);
        card.setBorder(new EmptyBorder(14, 20, 14, 14));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.PLAIN, 12));
        t.setForeground(COLOR_TEXT_MUTED);
        JLabel v = new JLabel(val);
        v.setFont(font(Font.BOLD, 19));
        v.setForeground(accent);
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 12));
        s.setForeground(COLOR_TEXT_MUTED);
        card.add(t);
        card.add(v);
        card.add(s);
        return card;
    }

    private JComponent createScroll(JTable table, String emptyText) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        if (table.getModel().getRowCount() > 0) return sp;

        JLabel empty = new JLabel(emptyText, SwingConstants.CENTER);
        empty.setFont(font(Font.PLAIN, 14));
        empty.setForeground(COLOR_TEXT_MUTED);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        wrap.add(table.getTableHeader(), BorderLayout.NORTH);
        wrap.add(empty, BorderLayout.CENTER);
        return wrap;
    }

    /** statusCol: cột trạng thái tô màu; ratingCol: cột số sao (-1 = không dùng). */
    private JTable createTable(DefaultTableModel model, int statusCol, int ratingCol) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setRowHeight(44);
        table.setFont(font(Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(COLOR_SELECT);
        table.setSelectionForeground(COLOR_TEXT_MAIN);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, new CellRenderer(statusCol, ratingCol));
        if (ratingCol >= 0) table.getColumnModel().getColumn(ratingCol).setCellRenderer(new StarRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new HeaderRenderer());
        return table;
    }

    private static class CellRenderer extends DefaultTableCellRenderer {
        private final int statusCol, ratingCol;

        CellRenderer(int statusCol, int ratingCol) {
            this.statusCol = statusCol;
            this.ratingCol = ratingCol;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(font(Font.PLAIN, 13));
            setForeground(COLOR_TEXT_MAIN);
            setHorizontalAlignment(SwingConstants.LEFT);
            if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : COLOR_STRIPE);

            String s = v == null ? "" : v.toString();
            if (col == statusCol && !s.isEmpty()) {
                String low = s.toLowerCase();
                Color c;
                if (low.contains("chưa thi")) c = COLOR_TEXT_MUTED;
                else if (low.contains("nghỉ") || low.contains("chưa") || low.contains("trượt")) c = COLOR_DANGER;
                else if (low.contains("dạy") || low.contains("đạt") || low.contains("đã")) c = COLOR_SUCCESS_DARK;
                else c = COLOR_PRIMARY;
                setFont(font(Font.BOLD, 12));
                setForeground(c);
                setText("●  " + s);
            }
            return this;
        }
    }

    /** Vẽ ngôi sao bằng đồ họa (không phụ thuộc font, tránh ký tự ★ hiện thành ô vuông). */
    private static Path2D starShape(double cx, double cy, double rOuter) {
        Path2D st = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? rOuter : rOuter * 0.45;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double px = cx + r * Math.cos(a), py = cy + r * Math.sin(a);
            if (i == 0) st.moveTo(px, py); else st.lineTo(px, py);
        }
        st.closePath();
        return st;
    }

    private static class StarRenderer extends JComponent implements TableCellRenderer {
        private int rating;
        private Color bg = Color.WHITE;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            try {
                rating = Integer.parseInt(String.valueOf(v).trim());
            } catch (NumberFormatException e) {
                rating = 0;
            }
            bg = sel ? COLOR_SELECT : (row % 2 == 0 ? Color.WHITE : COLOR_STRIPE);
            setToolTipText(rating + " / 5 sao");
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            int size = 18, gap = 4, x0 = 12;
            double cy = getHeight() / 2.0;
            for (int i = 0; i < 5; i++) {
                Path2D star = starShape(x0 + size / 2.0 + i * (size + gap), cy, size / 2.0);
                g2.setColor(i < rating ? COLOR_WARNING : new Color(203, 213, 225));
                g2.fill(star);
            }
            g2.dispose();
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setFont(font(Font.BOLD, 12));
            setBackground(new Color(241, 245, 249));
            setForeground(COLOR_TEXT_MUTED);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                    new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new TeacherDashboardUI("GV01").setVisible(true));
    }
}