import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class StudentDashboardUI extends JFrame {

    private final String maSinhVien;
    private String tenSinhVien = "";
    private String ngaySinh = "";
    private String trinhDo = "";
    private String soDienThoai = "";
    private String idGiaoVien = "";
    private String tenGiaoVien = "Chưa phân công";

    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_PRIMARY_DARK = new Color(29, 78, 216);
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color COLOR_LIGHT_BLUE = new Color(224, 242, 254);
    private static final Color COLOR_BG = new Color(244, 247, 251);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_NAV_ICON = new Color(100, 116, 139);
    private static final Color COLOR_HOVER = new Color(241, 245, 249);
    private static final Color COLOR_ACTIVE_BG = new Color(232, 240, 254);
    private static final Color COLOR_SELECT = new Color(219, 234, 254);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private static final Color COLOR_SUCCESS_DARK = new Color(5, 150, 105);
    private static final Color COLOR_WARNING = new Color(245, 158, 11);
    private static final Color COLOR_DANGER = new Color(220, 38, 38);
    private static final Color COLOR_STRIPE = new Color(248, 250, 252);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebar;
    private final List<NavButton> navButtons = new ArrayList<>();

    // Mỗi lần bấm menu sẽ dựng lại panel => dữ liệu luôn mới (vd: sau khi đăng ký khóa học)
    private final Map<String, Supplier<JPanel>> builders = new LinkedHashMap<>();
    private final Map<String, JPanel> currentPanels = new HashMap<>();

    @FunctionalInterface
    private interface RowMapper {
        Object[] map(ResultSet rs) throws SQLException;
    }

    public StudentDashboardUI() {
        this("SV01");
    }

    public StudentDashboardUI(String maSinhVien) {
        this.maSinhVien = maSinhVien;
        loadStudentProfileFromDB();

        setTitle("CỔNG THÔNG TIN SINH VIÊN S-LINK - " + this.tenSinhVien.toUpperCase());
        setSize(1320, 800);
        setMinimumSize(new Dimension(1100, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(COLOR_BG);

        builders.put("Dashboard", this::createDashboardOverviewPanel);
        builders.put("DangKy", this::createDangKyKhoaHocPanel);
        builders.put("TKB", this::createTKBPanel);
        builders.put("BangDiem", this::createBangDiemPanel);
        builders.put("HocPhi", this::createHocPhiPanel);
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

    /** Đọc nhiều dòng vào model. Luôn đóng Connection/Statement/ResultSet (try-with-resources). */
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

    private void loadStudentProfileFromDB() {
        String sql = "SELECT hs.ho_ten, hs.ngay_sinh, hs.trinh_do, hs.so_dien_thoai, hs.id_giao_vien, gv.ho_ten AS ten_gv "
                + "FROM Hoc_sinh hs LEFT JOIN Giao_vien gv ON hs.id_giao_vien = gv.id_giao_vien "
                + "WHERE hs.ma_sinh_vien = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Không thể kết nối CSDL MySQL BTL!", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, maSinhVien);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tenSinhVien = nz(rs.getString("ho_ten"));
                        ngaySinh = nz(rs.getString("ngay_sinh"));
                        trinhDo = nz(rs.getString("trinh_do"));
                        soDienThoai = nz(rs.getString("so_dien_thoai"));
                        idGiaoVien = nz(rs.getString("id_giao_vien"));
                        String gv = rs.getString("ten_gv");
                        tenGiaoVien = (gv != null) ? gv : "Chưa phân công";
                    }
                }
            }
        } catch (SQLException e) {
            showError("Lỗi SQL:", e);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String vnd(long n) {
        return String.format(Locale.US, "%,d", n).replace(',', '.') + " VNĐ";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String initial() {
        String t = tenSinhVien.trim();
        if (t.isEmpty()) return "S";
        String last = t.substring(t.lastIndexOf(' ') + 1);
        return last.isEmpty() ? "S" : last.substring(0, 1).toUpperCase();
    }

    /* ===================== UI PRIMITIVES ===================== */

    private static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    private enum IconType { HOME, BOOK, CALENDAR, AWARD, WALLET, USER }

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
            case BOOK:
                g2.drawRoundRect(3, 3, 14, 14, 2, 2);
                g2.drawLine(3, 7, 17, 7);
                g2.drawLine(8, 7, 8, 17);
                break;
            case CALENDAR:
                g2.drawRoundRect(3, 4, 14, 13, 3, 3);
                g2.drawLine(3, 8, 17, 8);
                g2.drawLine(7, 2, 7, 5); g2.drawLine(13, 2, 13, 5);
                break;
            case AWARD:
                g2.drawOval(6, 3, 8, 8);
                g2.drawLine(7, 11, 4, 17); g2.drawLine(13, 11, 16, 17);
                break;
            case WALLET:
                g2.drawRoundRect(2, 5, 16, 11, 4, 4);
                g2.drawOval(7, 7, 6, 6);
                break;
            case USER:
                g2.drawOval(7, 3, 6, 6);
                // Sửa: nửa cung đầu (vai) nằm trong vùng 20x20, không bị tràn ra ngoài icon
                g2.drawArc(3, 12, 14, 10, 0, 180);
                break;
        }
        g2.dispose();
    }

    private static class PrimaryButton extends JButton {
        PrimaryButton(String text) {
            super(text);
            setFont(font(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(210, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled()) {
                g2.setColor(new Color(148, 163, 184));
            } else if (getModel().isPressed()) {
                g2.setColor(COLOR_PRIMARY_HOVER.darker());
            } else if (getModel().isRollover()) {
                g2.setColor(COLOR_PRIMARY_HOVER);
            } else {
                g2.setColor(COLOR_PRIMARY);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private Color accent; // dải màu bên trái (dùng cho stat card)

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
            // bóng đổ nhẹ
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fillRoundRect(1, 2, w, h, radius, radius);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h - 1, radius, radius);
            if (accent != null) {
                Shape oldClip = g2.getClip();
                g2.clip(new RoundRectangle2D.Float(0, 0, w, h - 1, radius, radius));
                g2.setColor(accent);
                g2.fillRect(0, 0, 5, h);
                g2.setClip(oldClip);
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
            g2.setPaint(new GradientPaint(0, 0, COLOR_PRIMARY_DARK, getWidth(), getHeight(), new Color(56, 130, 246)));
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
            drawIcon(g, icon, 16, (getHeight() - 20) / 2, 20, active ? COLOR_PRIMARY : COLOR_NAV_ICON);
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
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, x, y);
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
        left.add(new Badge("SV", COLOR_PRIMARY, Color.WHITE, 40));

        JLabel lblLogo = new JLabel("Cổng Sinh Viên S-Link");
        lblLogo.setFont(font(Font.BOLD, 20));
        lblLogo.setForeground(COLOR_TEXT_MAIN);
        left.add(lblLogo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 11));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 14));
        right.add(new Badge(initial(), COLOR_LIGHT_BLUE, COLOR_PRIMARY, 42));

        JPanel userDetail = new JPanel(new GridLayout(2, 1));
        userDetail.setOpaque(false);
        JLabel lblUser = new JLabel(tenSinhVien);
        lblUser.setFont(font(Font.BOLD, 14));
        lblUser.setForeground(COLOR_TEXT_MAIN);
        JLabel lblMssv = new JLabel(maSinhVien + "  |  Trình độ: " + trinhDo);
        lblMssv.setFont(font(Font.PLAIN, 12));
        lblMssv.setForeground(COLOR_TEXT_MUTED);
        userDetail.add(lblUser);
        userDetail.add(lblMssv);
        right.add(userDetail);

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
        sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));

        JLabel lblSection = new JLabel("GÓC HỌC TẬP SINH VIÊN");
        lblSection.setFont(font(Font.BOLD, 11));
        lblSection.setForeground(COLOR_TEXT_MUTED);
        lblSection.setBorder(new EmptyBorder(24, 24, 12, 10));

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(new EmptyBorder(0, 12, 10, 12));

        NavButton btnDash = createNavButton("Tổng quan", IconType.HOME, "Dashboard");
        NavButton btnDangKy = createNavButton("Đăng ký khóa học", IconType.BOOK, "DangKy");
        NavButton btnTKB = createNavButton("Thời khóa biểu", IconType.CALENDAR, "TKB");
        NavButton btnDiem = createNavButton("Xem bảng điểm", IconType.AWARD, "BangDiem");
        NavButton btnHocPhi = createNavButton("Hóa đơn học phí", IconType.WALLET, "HocPhi");
        NavButton btnProfile = createNavButton("Thông tin cá nhân", IconType.USER, "Profile");

        NavButton[] all = {btnDash, btnDangKy, btnTKB, btnDiem, btnHocPhi, btnProfile};
        for (int i = 0; i < all.length; i++) {
            navPanel.add(all[i]);
            if (i < all.length - 1) navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        JLabel footer = new JLabel("S-Link Student Portal v1.0");
        footer.setFont(font(Font.PLAIN, 11));
        footer.setForeground(COLOR_TEXT_MUTED);
        footer.setBorder(new EmptyBorder(10, 24, 16, 10));

        sidebar.add(lblSection, BorderLayout.NORTH);
        sidebar.add(navPanel, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);

        setNavActive(btnDash);
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

    private JPanel createDashboardOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel banner = new GradientBanner();
        banner.setBorder(new EmptyBorder(22, 26, 22, 26));
        JLabel lblWelcome = new JLabel("<html><span style='font-size:20px'><b>Chào mừng " + esc(tenSinhVien) + "!</b></span><br>"
                + "<span style='font-size:12px;color:#DBEAFE'>Hệ thống tra cứu thông tin học tập và đăng ký khóa học S-Link.</span></html>");
        lblWelcome.setFont(font(Font.PLAIN, 14));
        lblWelcome.setForeground(Color.WHITE);
        banner.add(lblWelcome, BorderLayout.WEST);

        int tongDiemThi = 0;
        String tinhTrangThi = "Chưa thi";
        long tongTienHocPhi = 0;
        long tienChuaNop = 0;

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                String sqlDiem = "SELECT tong_diem FROM Ket_qua_thi WHERE ma_sinh_vien = ? ORDER BY ma_bai_lam DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sqlDiem)) {
                    ps.setString(1, maSinhVien);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            tongDiemThi = rs.getInt("tong_diem");
                            tinhTrangThi = (tongDiemThi >= 80) ? "ĐẠT" : "CHƯA ĐẠT";
                        }
                    }
                }
                String sqlHP = "SELECT SUM(CASE WHEN trang_thai = 'Đã nộp' THEN khoan_thu ELSE 0 END) AS da_nop, "
                        + "SUM(CASE WHEN trang_thai = 'Chưa nộp' THEN khoan_thu ELSE 0 END) AS chua_nop "
                        + "FROM hoa_don WHERE ma_ghi_danh = ? OR ma_ghi_danh LIKE CONCAT(?, ':%')";
                try (PreparedStatement ps = conn.prepareStatement(sqlHP)) {
                    ps.setString(1, maSinhVien);
                    ps.setString(2, maSinhVien);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            tongTienHocPhi = rs.getLong("da_nop");
                            tienChuaNop = rs.getLong("chua_nop");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            showError("Lỗi tải dữ liệu tổng quan:", e);
        }

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 112));
        stats.add(createStatCard("Mã Sinh Viên", maSinhVien, "Trình độ: " + trinhDo, COLOR_PRIMARY));
        stats.add(createStatCard("GV Phụ Trách", tenGiaoVien, "Mã GV: " + idGiaoVien, COLOR_SUCCESS));
        stats.add(createStatCard("Điểm thi mới nhất", tongDiemThi + " điểm", "Tình trạng: " + tinhTrangThi, COLOR_WARNING));
        stats.add(createStatCard("Học phí đã nộp", vnd(tongTienHocPhi), "Chưa nộp: " + vnd(tienChuaNop),
                tienChuaNop > 0 ? COLOR_DANGER : COLOR_SUCCESS_DARK));

        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        centerGrid.setOpaque(false);

        RoundedPanel cardSchedule = createCard("Lịch học tiếp theo");
        String[] cols = {"Ngày dạy", "Ca học", "Phòng", "Nội dung", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT lh.ngay_day, lh.ca_hoc, lh.phong_hoc, lh.noi_dung, lh.trang_thai_buoi FROM Lich_hoc lh "
                        + "JOIN Hoc_sinh hs ON lh.id_giao_vien = hs.id_giao_vien WHERE hs.ma_sinh_vien = ? LIMIT 5",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)},
                maSinhVien);
        cardSchedule.add(createScroll(createCustomTable(model, 4, -1), "Chưa có lịch học"), BorderLayout.CENTER);

        RoundedPanel cardNotice = createCard("Thông báo từ Trung tâm");
        DefaultListModel<String> noticeModel = new DefaultListModel<>();
        noticeModel.addElement("📌 [02/11] Lịch thi JLPT thử đánh giá trình độ.");
        noticeModel.addElement("📌 [05/11] Đăng ký câu lạc bộ Tiếng Nhật Kaiwa cuối tuần.");
        noticeModel.addElement("📌 [Nhắc nhở] Đảm bảo chuyên cần để đạt tiêu chuẩn dự thi.");
        JList<String> listNotice = new JList<>(noticeModel);
        listNotice.setFont(font(Font.PLAIN, 13));
        listNotice.setFixedCellHeight(44);
        listNotice.setSelectionBackground(COLOR_SELECT);
        listNotice.setSelectionForeground(COLOR_TEXT_MAIN);
        listNotice.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                JLabel lb = (JLabel) super.getListCellRendererComponent(l, v, i, sel, false);
                lb.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)),
                        new EmptyBorder(0, 10, 0, 10)));
                return lb;
            }
        });
        cardNotice.add(listNotice, BorderLayout.CENTER);

        centerGrid.add(cardSchedule);
        centerGrid.add(cardNotice);

        JPanel topStack = new JPanel(new BorderLayout(0, 16));
        topStack.setOpaque(false);
        topStack.add(banner, BorderLayout.NORTH);
        topStack.add(stats, BorderLayout.SOUTH);

        panel.add(topStack, BorderLayout.NORTH);
        panel.add(centerGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDangKyKhoaHocPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Đăng ký Khóa học mới (N5 - N1)");

        RoundedPanel card = createCard("Danh sách các Khóa học chưa đăng ký");
        String[] cols = {"Mã Khóa", "Tên Khóa Học", "Trình Độ", "Chuyên Đề", "Giáo Trình", "Học Phí"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        final Map<String, Integer> feeByCourse = new HashMap<>();

        // Sửa lỗi: trước đây so sánh ma_khoa_hoc với "SV01:KH01" nên khóa đã đăng ký vẫn hiện lại.
        String sql = "SELECT k.ma_khoa_hoc, k.ten_khoa_hoc, k.trinh_do, k.chuyen_de, k.giao_trinh, k.hoc_phi FROM Khoa_hoc k "
                + "WHERE NOT EXISTS (SELECT 1 FROM hoa_don h WHERE h.ma_ghi_danh = CONCAT(?, ':', k.ma_khoa_hoc)) "
                + "ORDER BY k.trinh_do DESC";
        loadRows(model, sql, rs -> {
            int fee = rs.getInt("hoc_phi");
            feeByCourse.put(rs.getString("ma_khoa_hoc"), fee);
            return new Object[]{rs.getString("ma_khoa_hoc"), rs.getString("ten_khoa_hoc"), rs.getString("trinh_do"),
                    rs.getString("chuyen_de"), rs.getString("giao_trinh"), vnd(fee)};
        }, maSinhVien);

        JTable table = createCustomTable(model, -1, -1);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField txtSearch = new JTextField();
        txtSearch.setFont(font(Font.PLAIN, 13));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo tên, trình độ, chuyên đề...");
        txtSearch.setToolTipText("Gõ để lọc danh sách khóa học");
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
        center.add(createScroll(table, "Bạn đã đăng ký tất cả khóa học hiện có 🎉"), BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        PrimaryButton btnDangKyClick = new PrimaryButton("Đăng ký & thanh toán");
        btnDangKyClick.setEnabled(model.getRowCount() > 0);
        btnDangKyClick.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 khóa học trong bảng để đăng ký!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int row = table.convertRowIndexToModel(viewRow); // Sửa lỗi: dùng chỉ số model khi có lọc/sắp xếp
            String maKhoaHoc = model.getValueAt(row, 0).toString();
            String tenKhoaHoc = model.getValueAt(row, 1).toString();
            int hocPhi = feeByCourse.getOrDefault(maKhoaHoc, 0);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "XÁC NHẬN ĐĂNG KÝ KHÓA HỌC:\n- Sinh viên: " + tenSinhVien + " (" + maSinhVien + ")\n- Khóa học: "
                            + tenKhoaHoc + "\n- Học phí: " + vnd(hocPhi),
                    "Xác nhận ghi danh", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION && processDangKyInDB(maKhoaHoc, hocPhi)) {
                JOptionPane.showMessageDialog(this, "🎉 Đăng ký thành công!\nHóa đơn ở trạng thái CHƯA NỘP, vui lòng thanh toán và chờ nhân viên xác nhận.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                showCard("DangKy"); // dựng lại để khóa vừa đăng ký biến mất khỏi danh sách
            }
        });

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        foot.setOpaque(false);
        foot.add(btnDangKyClick);
        card.add(foot, BorderLayout.SOUTH);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private boolean processDangKyInDB(String maKhoaHoc, int hocPhi) {
        String maGhiDanh = maSinhVien + ":" + maKhoaHoc;
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;

            // Chặn đăng ký trùng
            try (PreparedStatement chk = conn.prepareStatement("SELECT COUNT(*) FROM hoa_don WHERE ma_ghi_danh = ?")) {
                chk.setString(1, maGhiDanh);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this, "Bạn đã đăng ký khóa học này rồi!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                        return false;
                    }
                }
            }

            String maHoaDon = "HD_" + (System.currentTimeMillis() / 1000);
            String sql = "INSERT INTO hoa_don (ma_hoa_don, ma_nhan_vien, ma_ghi_danh, khoan_thu, ngay_lap, hinh_thuc, trang_thai) "
                    + "VALUES (?, 'NV01', ?, ?, NOW(), 'Chuyển khoản', 'Chưa nộp')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, maHoaDon);
                ps.setString(2, maGhiDanh);
                ps.setInt(3, hocPhi);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            showError("Lỗi SQL:", e);
            return false;
        }
    }

    private JPanel createTKBPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Lịch học & Trạng thái Buổi dạy");

        RoundedPanel card = createCard("Chi tiết các buổi học");
        String[] cols = {"Mã Lịch", "Mã Lớp", "Ngày Dạy", "Ca Học", "Phòng", "Nội Dung", "Trạng Thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT lh.ma_lich_hoc, lh.id_lop, lh.ngay_day, lh.ca_hoc, lh.phong_hoc, lh.noi_dung, lh.trang_thai_buoi "
                        + "FROM Lich_hoc lh JOIN Hoc_sinh hs ON lh.id_giao_vien = hs.id_giao_vien "
                        + "WHERE hs.ma_sinh_vien = ? ORDER BY lh.ngay_day",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7)},
                maSinhVien);

        card.add(createScroll(createCustomTable(model, 6, -1), "Chưa có lịch học nào"), BorderLayout.CENTER);
        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBangDiemPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Kết quả bài làm & Điểm thi");

        RoundedPanel card = createCard("Bảng điểm kiểm tra trình độ (≥ 80 điểm: Đạt)");
        String[] cols = {"Mã Bài Làm", "Trình Độ", "Mã Đề Thi", "Đọc Hiểu", "Từ Vựng", "Nghe", "Tổng Điểm"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT ma_bai_lam, trinh_do, ma_de_thi, diem_doc_hieu, diem_tu_vung, diem_nghe, tong_diem "
                        + "FROM Ket_qua_thi WHERE ma_sinh_vien = ?",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7)},
                maSinhVien);

        card.add(createScroll(createCustomTable(model, -1, 6), "Chưa có kết quả thi nào"), BorderLayout.CENTER);
        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHocPhiPanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Lịch sử nộp học phí");

        RoundedPanel card = createCard("Các hóa đơn đã được lập");
        String[] cols = {"Mã Hóa Đơn", "Mã Ghi Danh / Khóa", "Khoản Thu", "Ngày Lập", "Hình Thức", "Trạng Thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model,
                "SELECT ma_hoa_don, ma_ghi_danh, khoan_thu, ngay_lap, hinh_thuc, trang_thai FROM hoa_don "
                        + "WHERE ma_ghi_danh = ? OR ma_ghi_danh LIKE CONCAT(?, ':%') ORDER BY ngay_lap DESC",
                rs -> new Object[]{rs.getString(1), rs.getString(2), vnd(rs.getLong(3)), rs.getString(4), rs.getString(5),
                        rs.getString(6) == null ? "Chưa nộp" : rs.getString(6)},
                maSinhVien, maSinhVien);

        card.add(createScroll(createCustomTable(model, 5, -1), "Chưa có hóa đơn nào"), BorderLayout.CENTER);
        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = pagePanel();
        JLabel title = pageTitle("Thông tin cá nhân Sinh viên");

        RoundedPanel card = createCard("Hồ sơ học sinh");

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        head.setOpaque(false);
        head.add(new Badge(initial(), COLOR_LIGHT_BLUE, COLOR_PRIMARY, 72));
        JLabel name = new JLabel("<html><span style='font-size:18px'><b>" + esc(tenSinhVien) + "</b></span><br>"
                + "<span style='font-size:12px;color:#64748B'>" + esc(maSinhVien) + " • Trình độ " + esc(trinhDo) + "</span></html>");
        name.setFont(font(Font.PLAIN, 14));
        head.add(name);

        JPanel infoGrid = new JPanel(new GridLayout(4, 2, 18, 14));
        infoGrid.setOpaque(false);
        infoGrid.setBorder(new EmptyBorder(16, 0, 0, 0));
        infoGrid.add(createProfileItem("Mã sinh viên", maSinhVien));
        infoGrid.add(createProfileItem("Họ và tên", tenSinhVien));
        infoGrid.add(createProfileItem("Ngày sinh", ngaySinh));
        infoGrid.add(createProfileItem("Trình độ", trinhDo));
        infoGrid.add(createProfileItem("Số điện thoại", soDienThoai));
        infoGrid.add(createProfileItem("Mã GV phụ trách", idGiaoVien));
        infoGrid.add(createProfileItem("Giáo viên phụ trách", tenGiaoVien));
        infoGrid.add(createProfileItem("Trạng thái học tập", "Đang theo học"));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(head, BorderLayout.NORTH);
        body.add(infoGrid, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfileItem(String label, String val) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                new EmptyBorder(4, 0, 8, 0)));
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

    /* ===================== COMPONENT FACTORIES ===================== */

    private RoundedPanel createCard(String title) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 16, COLOR_CARD);
        card.setBorder(new EmptyBorder(18, 20, 20, 20));
        if (title != null) {
            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(font(Font.BOLD, 16));
            lblTitle.setForeground(COLOR_TEXT_MAIN);
            card.add(lblTitle, BorderLayout.NORTH);
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

    /** Bọc bảng trong scroll; nếu không có dòng nào thì hiện thông báo trống thân thiện. */
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

    /** statusCol: cột trạng thái tô màu; scoreCol: cột điểm tô xanh/đỏ (-1 = không dùng). */
    private JTable createCustomTable(DefaultTableModel model, int statusCol, int scoreCol) {
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
        table.setDefaultRenderer(Object.class, new CellRenderer(statusCol, scoreCol));

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new HeaderRenderer());
        return table;
    }

    private static class CellRenderer extends DefaultTableCellRenderer {
        private final int statusCol, scoreCol;

        CellRenderer(int statusCol, int scoreCol) {
            this.statusCol = statusCol;
            this.scoreCol = scoreCol;
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
                if (low.contains("hủy") || low.contains("nghỉ") || low.contains("huỷ") || low.contains("chưa nộp")) c = COLOR_DANGER;
                else if (low.contains("hoàn") || low.contains("đã") || low.contains("xong")) c = COLOR_SUCCESS_DARK;
                else c = COLOR_PRIMARY;
                setFont(font(Font.BOLD, 12));
                setForeground(c);
                setText("●  " + s);
            } else if (col == scoreCol) {
                try {
                    int n = Integer.parseInt(s.trim());
                    setFont(font(Font.BOLD, 13));
                    setForeground(n >= 80 ? COLOR_SUCCESS_DARK : COLOR_DANGER);
                } catch (NumberFormatException ignored) {
                    // giữ màu mặc định
                }
            }
            return this;
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
        SwingUtilities.invokeLater(() -> new StudentDashboardUI().setVisible(true));
    }
}