

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * GIAO DIỆN DÀNH RIÊNG CHO SINH VIÊN / HỌC VIÊN (StudentDashboardUI.java)
 */
public class StudentDashboardUI extends JFrame {

    // ===== Thông tin sinh viên đăng nhập =====
    private static final String STUDENT_NAME = "Đào Thanh Dũng";
    private static final String STUDENT_ID = "HV2026-0882";
    private static final String STUDENT_CLASS = "Tiếng Nhật N4 - K24";

    // ===== Bảng màu Giao diện (Tone Xanh Dương Nhạt) =====
    private static final Color COLOR_PRIMARY = new Color(59, 130, 246);      // Xanh dương chủ đạo
    private static final Color COLOR_LIGHT_BLUE = new Color(224, 242, 254);   // Nền xanh nhạt
    private static final Color COLOR_BG = new Color(248, 250, 252);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_TEXT_MAIN = new Color(30, 41, 59);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_NAV_ICON = new Color(100, 116, 139);
    private static final Color COLOR_HOVER = new Color(241, 245, 249);
    private static final Color COLOR_ACTIVE_BG = new Color(238, 242, 255);
    private static final Color COLOR_SELECT = new Color(224, 242, 254);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebar;
    private JLabel lblSection;
    private final List<NavButton> navButtons = new ArrayList<>();

    public StudentDashboardUI() {
        setTitle("CỔNG THÔNG TIN SINH VIÊN - TRUNG TÂM ĐÀO TẠO");
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
        mainContentPanel.add(createTKBPanel(), "TKB");
        mainContentPanel.add(createBangDiemPanel(), "BangDiem");
        mainContentPanel.add(createHocPhiPanel(), "HocPhi");
        mainContentPanel.add(createProfilePanel(), "Profile");

        add(mainContentPanel, BorderLayout.CENTER);
    }

    private static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    // =====================================================================
    //  ICON VECTOR
    // =====================================================================
    private enum IconType { HOME, CALENDAR, AWARD, WALLET, USER, BELL, LOGOUT }

    private static void drawIcon(Graphics g, IconType type, int x, int y, int size, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        double s = size / 20.0;
        g2.scale(s, s);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
                g2.drawArc(3, 12, 14, 12, 0, 180);
                break;
            case LOGOUT:
                g2.drawArc(3, 3, 14, 14, 45, 270);
                g2.drawLine(10, 2, 10, 10);
                break;
            default:
                g2.drawOval(5, 5, 10, 10);
                break;
        }
        g2.dispose();
    }

    // =====================================================================
    //  COMPONENTS TỰ VẼ
    // =====================================================================
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
        private final String fullText;

        NavButton(String text, IconType icon) {
            super(text);
            this.fullText = text;
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
            g2.setColor(bg);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setColor(fg);
            g2.setFont(font(Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

    // =====================================================================
    //  THANH TRÊN CÙNG
    // =====================================================================
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        bar.setPreferredSize(new Dimension(0, 68));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(0, 14, 0, 0));
        left.add(new Badge("SV", COLOR_PRIMARY, Color.WHITE, 40));
        
        JLabel lblLogo = new JLabel("Cổng Sinh Viên");
        lblLogo.setFont(font(Font.BOLD, 20));
        lblLogo.setForeground(COLOR_TEXT_MAIN);
        left.add(lblLogo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 11));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 14));

        String firstChar = STUDENT_NAME.substring(STUDENT_NAME.lastIndexOf(' ') + 1).substring(0, 1);
        right.add(new Badge(firstChar, COLOR_LIGHT_BLUE, COLOR_PRIMARY, 42));

        JPanel userDetail = new JPanel(new GridLayout(2, 1));
        userDetail.setOpaque(false);
        JLabel lblUser = new JLabel(STUDENT_NAME);
        lblUser.setFont(font(Font.BOLD, 14));
        lblUser.setForeground(COLOR_TEXT_MAIN);
        JLabel lblMssv = new JLabel(STUDENT_ID + " | " + STUDENT_CLASS);
        lblMssv.setFont(font(Font.PLAIN, 12));
        lblMssv.setForeground(COLOR_TEXT_MUTED);
        userDetail.add(lblUser);
        userDetail.add(lblMssv);
        right.add(userDetail);

        // Nút Đăng xuất
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
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có muốn đăng xuất khỏi cổng sinh viên?",
                    "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose(); // Đóng giao diện Sinh viên
                // Nếu có file LoginForm, bật lại: new LoginForm().setVisible(true);
            }
        });
        right.add(btnLogout);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // =====================================================================
    //  SIDEBAR DÀNH CHO SINH VIÊN
    // =====================================================================
    private JPanel createSidebar() {
        sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));

        lblSection = new JLabel("GÓC SINH VIÊN");
        lblSection.setFont(font(Font.BOLD, 11));
        lblSection.setForeground(COLOR_TEXT_MUTED);
        lblSection.setBorder(new EmptyBorder(24, 24, 12, 10));

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(new EmptyBorder(0, 12, 10, 12));

        NavButton btnDash = createNavButton("Tổng quan", IconType.HOME, "Dashboard");
        NavButton btnTKB = createNavButton("Thời khóa biểu", IconType.CALENDAR, "TKB");
        NavButton btnDiem = createNavButton("Bảng điểm học tập", IconType.AWARD, "BangDiem");
        NavButton btnHocPhi = createNavButton("Học phí & Hóa đơn", IconType.WALLET, "HocPhi");
        NavButton btnProfile = createNavButton("Thông tin cá nhân", IconType.USER, "Profile");

        NavButton[] all = {btnDash, btnTKB, btnDiem, btnHocPhi, btnProfile};
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

    // =====================================================================
    //  1. TRANG TỔNG QUAN
    // =====================================================================
    private JPanel createDashboardOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Banner Chào Mừng
        RoundedPanel banner = new RoundedPanel(new BorderLayout(), 16, COLOR_LIGHT_BLUE);
        banner.setBorder(new EmptyBorder(20, 24, 20, 24));
        JLabel lblWelcome = new JLabel("<html><b>Chào mừng " + STUDENT_NAME + "!</b><br>"
                + "<font color='#64748B' size='3'>Chúc bạn có một ngày học tập hiệu quả tại Trung tâm.</font></html>");
        lblWelcome.setFont(font(Font.PLAIN, 18));
        lblWelcome.setForeground(COLOR_PRIMARY);
        banner.add(lblWelcome, BorderLayout.WEST);

        // Thống kê cá nhân
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(createStatCard("Số khóa đang học", "1 Khóa", "Tiếng Nhật N4", COLOR_PRIMARY));
        stats.add(createStatCard("Số buổi đã học", "18/30 Buổi", "Đạt 60% tiến trình", new Color(16, 185, 129)));
        stats.add(createStatCard("Chuyên cần", "95%", "Vắng 1 buổi (Có phép)", new Color(245, 158, 11)));
        stats.add(createStatCard("Học phí đợt này", "Đã nộp", "Hoàn thành 100%", new Color(13, 148, 136)));

        // Lịch học sắp tới & Thông báo
        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        centerGrid.setOpaque(false);

        RoundedPanel cardSchedule = createCard("Lịch học tiếp theo");
        String[] colsTKB = {"Ngày học", "Giờ học", "Phòng", "Bài học"};
        DefaultTableModel modelTKB = new DefaultTableModel(colsTKB, 0);
        modelTKB.addRow(new Object[]{"Thứ 2 (Hôm nay)", "18:00 - 20:00", "P.302", "Bài 15: Cấu trúc ~te mo ii desu"});
        modelTKB.addRow(new Object[]{"Thứ 4 (30/10)", "18:00 - 20:00", "P.302", "Luyện Kaiwa & Luyện nghe N4"});
        modelTKB.addRow(new Object[]{"Thứ 6 (01/11)", "18:00 - 20:00", "P.302", "Kiểm tra giữa khóa"});
        JTable tableTKB = createCustomTable(modelTKB);
        cardSchedule.add(createScroll(tableTKB), BorderLayout.CENTER);

        RoundedPanel cardNotice = createCard("Thông báo từ Trung tâm");
        DefaultListModel<String> noticeModel = new DefaultListModel<>();
        noticeModel.addElement("📌 [02/11] Lịch thi thử JLPT N4 cấp chứng chỉ trung tâm.");
        noticeModel.addElement("📌 [05/11] Đăng ký câu lạc bộ Tiếng Nhật Kaiwa cuối tuần.");
        noticeModel.addElement("📌 [10/11] Nghỉ lễ theo quy định của trung tâm.");
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

    // =====================================================================
    //  2. THỜI KHÓA BIỂU
    // =====================================================================
    private JPanel createTKBPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Lịch học & Điểm danh chi tiết");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Danh sách các buổi học khóa Tiếng Nhật N4");
        String[] cols = {"Buổi số", "Ngày học", "Giảng viên", "Trạng thái điểm danh", "Ghi chú"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        model.addRow(new Object[]{"Buổi 1", "02/10/2026", "Sensei Tanaka", "Có mặt", "Học Bài 11"});
        model.addRow(new Object[]{"Buổi 2", "05/10/2026", "Sensei Tanaka", "Có mặt", "Học Bài 12"});
        model.addRow(new Object[]{"Buổi 3", "09/10/2026", "Sensei Tanaka", "Vắng (Có phép)", "Xin nghỉ bệnh"});
        model.addRow(new Object[]{"Buổi 4", "12/10/2026", "Sensei Tanaka", "Có mặt", "Luyện Kaiwa"});

        JTable table = createCustomTable(model);
        card.add(createScroll(table), BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =====================================================================
    //  3. BẢNG ĐIỂM HỌC TẬP
    // =====================================================================
    private JPanel createBangDiemPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Kết quả học tập & Điểm thi");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Bảng điểm kiểm tra định kỳ");
        String[] cols = {"Tên bài thi / Kiểm tra", "Điểm Từ vựng", "Điểm Đọc hiểu", "Điểm Nghe", "Tổng điểm", "Đánh giá"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        model.addRow(new Object[]{"Kiểm tra 15p - Bài 10", "90/100", "85/100", "80/100", "85.0", "Đạt"});
        model.addRow(new Object[]{"Kiểm tra Giữa khóa N5", "95/100", "90/100", "88/100", "91.0", "Xuất sắc"});
        model.addRow(new Object[]{"Thi thử JLPT N5", "80/100", "75/100", "70/100", "75.0", "Đạt"});

        JTable table = createCustomTable(model);
        card.add(createScroll(table), BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =====================================================================
    //  4. HỌC PHÍ & HÓA ĐƠN
    // =====================================================================
    private JPanel createHocPhiPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Lịch sử nộp học phí");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Thông tin các đợt thanh toán");
        String[] cols = {"Mã hóa đơn", "Tên khóa học", "Số tiền (VNĐ)", "Ngày thanh toán", "Hình thức", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        model.addRow(new Object[]{"HD-2026-01", "Tiếng Nhật N5 Sơ cấp", "3.500.000", "10/01/2026", "Chuyển khoản", "Đã thanh toán"});
        model.addRow(new Object[]{"HD-2026-04", "Tiếng Nhật N4 Trung cấp", "4.200.000", "15/08/2026", "Tiền mặt", "Đã thanh toán"});

        JTable table = createCustomTable(model);
        card.add(createScroll(table), BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =====================================================================
    //  5. THÔNG TIN CÁ NHÂN
    // =====================================================================
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Thông tin cá nhân sinh viên");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(COLOR_TEXT_MAIN);

        RoundedPanel card = createCard("Hồ sơ học viên");
        JPanel infoGrid = new JPanel(new GridLayout(4, 2, 18, 18));
        infoGrid.setOpaque(false);
        infoGrid.setBorder(new EmptyBorder(16, 16, 16, 16));

        infoGrid.add(createProfileItem("Họ và tên:", STUDENT_NAME));
        infoGrid.add(createProfileItem("Mã sinh viên:", STUDENT_ID));
        infoGrid.add(createProfileItem("Lớp hiện tại:", STUDENT_CLASS));
        infoGrid.add(createProfileItem("Ngày sinh:", "05/12/2006"));
        infoGrid.add(createProfileItem("Số điện thoại:", "0912345678"));
        infoGrid.add(createProfileItem("Email:", "dung.dt@gmail.com"));
        infoGrid.add(createProfileItem("Địa chỉ:", "Hà Nội, Việt Nam"));
        infoGrid.add(createProfileItem("Trạng thái học tập:", "Đang theo học"));

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

    // =====================================================================
    //  HELPER METHOD
    // =====================================================================
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