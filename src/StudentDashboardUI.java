

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.Path2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboardUI extends JFrame {

    private final String maSinhVien;
    private String tenSinhVien = "";
    private String ngaySinh = "";
    private String trinhDo = "";
    private String soDienThoai = "";
    private String idGiaoVien = "";
    private String tenGiaoVien = "";

    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color COLOR_LIGHT_BLUE = new Color(224, 242, 254);
    private static final Color COLOR_BG = new Color(248, 250, 252);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(203, 213, 225);
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(71, 85, 105);
    private static final Color COLOR_NAV_ICON = new Color(71, 85, 105);
    private static final Color COLOR_HOVER = new Color(241, 245, 249);
    private static final Color COLOR_ACTIVE_BG = new Color(238, 242, 255);
    private static final Color COLOR_SELECT = new Color(224, 242, 254);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebar;
    private final List<NavButton> navButtons = new ArrayList<>();

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

        add(createTopBar(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(COLOR_BG);

        mainContentPanel.add(createDashboardOverviewPanel(), "Dashboard");
        mainContentPanel.add(createDangKyKhoaHocPanel(), "DangKy");
        mainContentPanel.add(createTKBPanel(), "TKB");
        mainContentPanel.add(createBangDiemPanel(), "BangDiem");
        mainContentPanel.add(createHocPhiPanel(), "HocPhi");
        mainContentPanel.add(createProfilePanel(), "Profile");

        add(mainContentPanel, BorderLayout.CENTER);
    }

    private void loadStudentProfileFromDB() {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối CSDL MySQL BTL!", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Khớp với bảng Hoc_sinh và Giao_vien mới
            String sql = "SELECT hs.ho_ten, hs.ngay_sinh, hs.trinh_do, hs.so_dien_thoai, hs.id_giao_vien, gv.ho_ten AS ten_gv " +
                         "FROM Hoc_sinh hs " +
                         "LEFT JOIN Giao_vien gv ON hs.id_giao_vien = gv.id_giao_vien " +
                         "WHERE hs.ma_sinh_vien = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, maSinhVien);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                this.tenSinhVien = rs.getString("ho_ten");
                this.ngaySinh = rs.getString("ngay_sinh");
                this.trinhDo = rs.getString("trinh_do");
                this.soDienThoai = rs.getString("so_dien_thoai");
                this.idGiaoVien = rs.getString("id_giao_vien");
                this.tenGiaoVien = (rs.getString("ten_gv") != null) ? rs.getString("ten_gv") : "Chưa phân công";
            }
            rs.close(); pstmt.close(); conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi SQL: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

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
                g2.drawArc(3, 12, 14, 12, 0, 180);
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
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (getModel().isPressed()) {
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
        RoundedPanel(LayoutManager lm, int radius, Color fill) {
            super(lm); this.radius = radius; this.fill = fill; setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(COLOR_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            } else if (getModel().isRollover()) {
                g2.setColor(COLOR_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
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
            this.text = text; this.bg = bg; this.fg = fg;
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
            g2.setFont(font(Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

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

        String firstChar = (tenSinhVien.length() > 0 && tenSinhVien.contains(" ")) 
                ? tenSinhVien.substring(tenSinhVien.lastIndexOf(' ') + 1).substring(0, 1) 
                : "S";
        right.add(new Badge(firstChar, COLOR_LIGHT_BLUE, COLOR_PRIMARY, 42));

        JPanel userDetail = new JPanel(new GridLayout(2, 1));
        userDetail.setOpaque(false);
        JLabel lblUser = new JLabel(tenSinhVien);
        lblUser.setFont(font(Font.BOLD, 14));
        lblUser.setForeground(COLOR_TEXT_MAIN);
        JLabel lblMssv = new JLabel(maSinhVien + " | Trình độ: " + trinhDo);
        lblMssv.setFont(font(Font.PLAIN, 12));
        lblMssv.setForeground(COLOR_TEXT_MUTED);
        userDetail.add(lblUser);
        userDetail.add(lblMssv);
        right.add(userDetail);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(font(Font.BOLD, 13));
        btnLogout.setForeground(new Color(239, 68, 68));
        btnLogout.setBackground(new Color(254, 242, 242));
        btnLogout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                new EmptyBorder(6, 12, 6, 12)));
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

        sidebar.add(lblSection, BorderLayout.NORTH);
        sidebar.add(navPanel, BorderLayout.CENTER);

        setNavActive(btnDash);
        return sidebar;
    }

    private NavButton createNavButton(String text, IconType icon, String cardName) {
        NavButton button = new NavButton(text, icon);
        button.addActionListener(e -> {
            setNavActive(button);
            cardLayout.show(mainContentPanel, cardName);
        });
        navButtons.add(button);
        return button;
    }

    private void setNavActive(NavButton active) {
        for (NavButton b : navButtons) b.setActive(b == active);
    }

    private JPanel createDashboardOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        RoundedPanel banner = new RoundedPanel(new BorderLayout(), 16, COLOR_LIGHT_BLUE);
        banner.setBorder(new EmptyBorder(20, 24, 20, 24));
        JLabel lblWelcome = new JLabel("<html><b>Chào mừng " + tenSinhVien + "!</b><br>"
                + "<font color='#475569' size='3'>Hệ thống tra cứu thông tin học tập và đăng ký khóa học S-Link.</font></html>");
        lblWelcome.setFont(font(Font.PLAIN, 18));
        lblWelcome.setForeground(COLOR_PRIMARY);
        banner.add(lblWelcome, BorderLayout.WEST);

        int tongDiemThi = 0;
        String tinhTrangThi = "Chưa thi";
        int tongTienHocPhi = 0;

        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            try {
                // Khớp bảng Ket_qua_thi mới
                String sqlDiem = "SELECT tong_diem FROM Ket_qua_thi WHERE ma_sinh_vien = ? ORDER BY ma_bai_lam DESC LIMIT 1";
                PreparedStatement pstDiem = conn.prepareStatement(sqlDiem);
                pstDiem.setString(1, maSinhVien);
                ResultSet rsDiem = pstDiem.executeQuery();
                if (rsDiem.next()) {
                    tongDiemThi = rsDiem.getInt("tong_diem");
                    tinhTrangThi = (tongDiemThi >= 80) ? "ĐẠT" : "CHƯA ĐẠT";
                }
                rsDiem.close(); pstDiem.close();

                // Khớp bảng hoa_don mới
                String sqlHP = "SELECT SUM(khoan_thu) AS tong FROM hoa_don WHERE ma_ghi_danh = ? OR ma_ghi_danh LIKE CONCAT(?, ':%')";
                PreparedStatement pstHP = conn.prepareStatement(sqlHP);
                pstHP.setString(1, maSinhVien);
                pstHP.setString(2, maSinhVien);
                ResultSet rsHP = pstHP.executeQuery();
                if (rsHP.next()) {
                    tongTienHocPhi = rsHP.getInt("tong");
                }
                rsHP.close(); pstHP.close();
                conn.close();
            } catch (Exception ignored) {}
        }

        String tongTienFmt = String.format("%,d VNĐ", tongTienHocPhi).replace(',', '.');

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(createStatCard("Mã Sinh Viên", maSinhVien, "Trình độ: " + trinhDo, COLOR_PRIMARY));
        stats.add(createStatCard("GV Phụ Trách", tenGiaoVien, "Mã GV: " + idGiaoVien, COLOR_SUCCESS));
        stats.add(createStatCard("Điểm thi mới nhất", tongDiemThi + " Điểm", "Tình trạng: " + tinhTrangThi, new Color(245, 158, 11)));
        stats.add(createStatCard("Tổng học phí đã nộp", tongTienFmt, "Trạng thái: Đã ghi nhận", new Color(13, 148, 136)));

        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        centerGrid.setOpaque(false);

        RoundedPanel cardSchedule = createCard("Lịch học tiếp theo (Lich_hoc)");
        String[] cols = {"Ngày dạy", "Ca học", "Phòng", "Nội dung", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        Connection connLich = DBConnection.getConnection();
        if (connLich != null) {
            try {
                // Khớp bảng Lich_hoc mới kết nối qua id_giao_vien
                String sqlLich = "SELECT lh.* FROM Lich_hoc lh " +
                                 "JOIN Hoc_sinh hs ON lh.id_giao_vien = hs.id_giao_vien " +
                                 "WHERE hs.ma_sinh_vien = ? LIMIT 5";
                PreparedStatement pstLich = connLich.prepareStatement(sqlLich);
                pstLich.setString(1, maSinhVien);
                ResultSet rsLich = pstLich.executeQuery();
                while (rsLich.next()) {
                    model.addRow(new Object[]{
                        rsLich.getString("ngay_day"),
                        rsLich.getString("ca_hoc"),
                        rsLich.getString("phong_hoc"),
                        rsLich.getString("noi_dung"),
                        rsLich.getString("trang_thai_buoi")
                    });
                }
                rsLich.close(); pstLich.close(); connLich.close();
            } catch (Exception ignored) {}
        }

        cardSchedule.add(createScroll(createCustomTable(model)), BorderLayout.CENTER);

        RoundedPanel cardNotice = createCard("Thông báo từ Trung tâm");
        DefaultListModel<String> noticeModel = new DefaultListModel<>();
        noticeModel.addElement("📌 [02/11] Lịch thi JLPT thử đánh giá trình độ.");
        noticeModel.addElement("📌 [05/11] Đăng ký câu lạc bộ Tiếng Nhật Kaiwa cuối tuần.");
        noticeModel.addElement("📌 [Nhắc nhở] Đảm bảo chuyên cần để đạt tiêu chuẩn dự thi.");
        JList<String> listNotice = new JList<>(noticeModel);
        listNotice.setFont(font(Font.PLAIN, 13));
        listNotice.setFixedCellHeight(38);
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
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Đăng ký Khóa học mới (N5 - N1)");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Danh sách các Khóa học chưa đăng ký");
        String[] cols = {"Mã Khóa", "Tên Khóa Học", "Trình Độ", "Chuyên Đề", "Giáo Trình", "Học Phí (VNĐ)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        JTable table = createCustomTable(model);
        card.add(createScroll(table), BorderLayout.CENTER);

        PrimaryButton btnDangKyClick = new PrimaryButton("Đăng ký khóa đã chọn");
        btnDangKyClick.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 khóa học trong bảng để đăng ký!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String maKhocHoc = model.getValueAt(selectedRow, 0).toString();
            String tenKhocHoc = model.getValueAt(selectedRow, 1).toString();
            String hocPhiStr = model.getValueAt(selectedRow, 5).toString().replace(".", "").replace(" VNĐ", "").trim();
            int hocPhi = Integer.parseInt(hocPhiStr);

            int confirm = JOptionPane.showConfirmDialog(this, 
                "XÁC NHẬN ĐĂNG KÝ KHÓA HỌC:\n- Sinh viên: " + tenSinhVien + " (" + maSinhVien + ")\n- Khóa học: " + tenKhocHoc + "\n- Học phí: " + model.getValueAt(selectedRow, 5), 
                "Xác nhận ghi danh", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean ok = processDangKyInDB(maKhocHoc, hocPhi);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "🎉 Đăng ký thành công! Hóa đơn đã được lưu vào CSDL MySQL.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    loadCoursesFromDB(model);
                }
            }
        });

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.setOpaque(false);
        foot.add(btnDangKyClick);
        card.add(foot, BorderLayout.SOUTH);

        loadCoursesFromDB(model);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void loadCoursesFromDB(DefaultTableModel model) {
        model.setRowCount(0);
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        try {
            // Khớp bảng Khoa_hoc mới
            String sql = "SELECT * FROM Khoa_hoc " +
                         "WHERE ma_khoa_hoc NOT IN (" +
                         "   SELECT ma_ghi_danh FROM hoa_don " +
                         "   WHERE ma_ghi_danh = ? OR ma_ghi_danh LIKE CONCAT(?, ':%') OR ma_ghi_danh = Khoa_hoc.ma_khoa_hoc" +
                         ") ORDER BY trinh_do DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, maSinhVien);
            pstmt.setString(2, maSinhVien);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int hocPhi = rs.getInt("hoc_phi");
                String hocPhiFormatted = String.format("%,d VNĐ", hocPhi).replace(',', '.');
                model.addRow(new Object[]{
                    rs.getString("ma_khoa_hoc"),
                    rs.getString("ten_khoa_hoc"),
                    rs.getString("trinh_do"),
                    rs.getString("chuyen_de"),
                    rs.getString("giao_trinh"),
                    hocPhiFormatted
                });
            }
            rs.close(); pstmt.close(); conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi nạp khóa học: " + e.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean processDangKyInDB(String maKhoaHoc, int hocPhi) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;
        try {
            String maHoaDon = "HD_" + (System.currentTimeMillis() / 1000);
            // Khớp bảng hoa_don mới
            String sql = "INSERT INTO hoa_don (ma_hoa_don, ma_nhan_vien, ma_ghi_danh, khoan_thu, ngay_lap, hinh_thuc) " +
                         "VALUES (?, 'NV01', ?, ?, NOW(), 'Chuyển khoản')";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, maHoaDon);
            pstmt.setString(2, maSinhVien + ":" + maKhoaHoc);
            pstmt.setInt(3, hocPhi);
            int rows = pstmt.executeUpdate();
            pstmt.close(); conn.close();
            return rows > 0;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi SQL: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private JPanel createTKBPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Lịch học & Trạng thái Buổi dạy (Bảng Lich_hoc)");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Chi tiết các buổi học");
        String[] cols = {"Mã Lịch", "Mã Lớp", "Ngày Dạy", "Ca Học", "Phòng", "Nội Dung", "Trạng Thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            try {
                // Khớp bảng Lich_hoc mới
                String sql = "SELECT lh.* FROM Lich_hoc lh " +
                             "JOIN Hoc_sinh hs ON lh.id_giao_vien = hs.id_giao_vien " +
                             "WHERE hs.ma_sinh_vien = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, maSinhVien);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("ma_lich_hoc"), rs.getString("id_lop"), 
                        rs.getString("ngay_day"), rs.getString("ca_hoc"), 
                        rs.getString("phong_hoc"), rs.getString("noi_dung"), 
                        rs.getString("trang_thai_buoi")
                    });
                }
                rs.close(); pstmt.close(); conn.close();
            } catch (Exception ignored) {}
        }

        JTable table = createCustomTable(model);
        card.add(createScroll(table), BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBangDiemPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Kết quả bài làm & Điểm thi (Bảng Ket_qua_thi)");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Bảng điểm kiểm tra trình độ");
        String[] cols = {"Mã Bài Làm", "Trình Độ", "Mã Đề Thi", "Đọc Hiểu", "Từ Vựng", "Nghe", "Tổng Điểm"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            try {
                // Khớp bảng Ket_qua_thi mới
                String sql = "SELECT * FROM Ket_qua_thi WHERE ma_sinh_vien = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, maSinhVien);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("ma_bai_lam"), rs.getString("trinh_do"),
                        rs.getString("ma_de_thi"), rs.getInt("diem_doc_hieu"),
                        rs.getInt("diem_tu_vung"), rs.getInt("diem_nghe"),
                        rs.getInt("tong_diem")
                    });
                }
                rs.close(); pstmt.close(); conn.close();
            } catch (Exception ignored) {}
        }

        JTable table = createCustomTable(model);
        card.add(createScroll(table), BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHocPhiPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Lịch sử nộp học phí (Bảng hoa_don)");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Các hóa đơn đã được lập");
        String[] cols = {"Mã Hóa Đơn", "Mã Ghi Danh / Khóa", "Khoản Thu (VNĐ)", "Ngày Lập", "Hình Thức"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            try {
                // Khớp bảng hoa_don mới
                String sql = "SELECT * FROM hoa_don WHERE ma_ghi_danh = ? OR ma_ghi_danh LIKE CONCAT(?, ':%')";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, maSinhVien);
                pstmt.setString(2, maSinhVien);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int thu = rs.getInt("khoan_thu");
                    String thuFmt = String.format("%,d VNĐ", thu).replace(',', '.');
                    model.addRow(new Object[]{
                        rs.getString("ma_hoa_don"), rs.getString("ma_ghi_danh"),
                        thuFmt, rs.getString("ngay_lap"),
                        rs.getString("hinh_thuc")
                    });
                }
                rs.close(); pstmt.close(); conn.close();
            } catch (Exception ignored) {}
        }

        JTable table = createCustomTable(model);
        card.add(createScroll(table), BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Thông tin cá nhân Sinh viên (Bảng Hoc_sinh)");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Hồ sơ học sinh từ CSDL MySQL BTL");
        JPanel infoGrid = new JPanel(new GridLayout(4, 2, 18, 18));
        infoGrid.setOpaque(false);
        infoGrid.setBorder(new EmptyBorder(16, 16, 16, 16));

        infoGrid.add(createProfileItem("Mã Sinh Viên (ma_sinh_vien):", maSinhVien));
        infoGrid.add(createProfileItem("Họ Tên (ho_ten):", tenSinhVien));
        infoGrid.add(createProfileItem("Ngày Sinh (ngay_sinh):", ngaySinh));
        infoGrid.add(createProfileItem("Trình Độ (trinh_do):", trinhDo));
        infoGrid.add(createProfileItem("Số Điện Thoại (so_dien_thoai):", soDienThoai));
        infoGrid.add(createProfileItem("Mã GV Phụ Trách (id_giao_vien):", idGiaoVien));
        infoGrid.add(createProfileItem("Tên Giáo Viên Phụ Trách:", tenGiaoVien));
        infoGrid.add(createProfileItem("Trạng Thái Học Tập:", "Đang theo học"));

        card.add(infoGrid, BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfileItem(String label, String val) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(font(Font.BOLD, 13));
        lbl.setForeground(COLOR_TEXT_MUTED);
        JLabel v = new JLabel(val);
        v.setFont(font(Font.PLAIN, 14));
        v.setForeground(COLOR_TEXT_MAIN);
        p.add(lbl, BorderLayout.WEST);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

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
        RoundedPanel card = new RoundedPanel(new GridLayout(3, 1, 0, 4), 14, Color.WHITE);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel t = new JLabel(title);
        t.setFont(font(Font.PLAIN, 13));
        t.setForeground(COLOR_TEXT_MUTED);

        JLabel v = new JLabel(val);
        v.setFont(font(Font.BOLD, 20));
        v.setForeground(accent);

        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 12));
        s.setForeground(COLOR_TEXT_MUTED);

        card.add(t); card.add(v); card.add(s);
        return card;
    }

    private JScrollPane createScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    private JTable createCustomTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table.setRowHeight(42);
        table.setFont(font(Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(COLOR_SELECT);
        table.setSelectionForeground(COLOR_TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));
        header.setDefaultRenderer(new HeaderRenderer());
        return table;
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() { setOpaque(true); setHorizontalAlignment(SwingConstants.LEFT); }
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
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new StudentDashboardUI().setVisible(true));
    }
}