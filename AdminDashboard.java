
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class AdminDashboard extends JFrame {

    private final Color BLUE = new Color(49, 94, 251);
    private final Color DARK = new Color(22, 32, 55);
    private final Color ACTIVE = new Color(38, 66, 160);
    private final Color BG = new Color(245, 247, 252);
    private final Color TEXT = new Color(34, 43, 63);
    private final Color MUTED = new Color(112, 122, 143);
    private final Color BORDER = new Color(226, 231, 240);
    private final Color MENU_TEXT = new Color(207, 216, 232);

    // ===== 7 mục menu: {tên hiển thị, tên bảng trong MySQL, mô tả} =====
    // Nếu bảng trong database của bạn đặt tên khác thì sửa cột thứ 2 ở đây.
    private final String[][] SECTIONS = {
        {"Học sinh",   "hoc_sinh",    "Danh sách học sinh đang theo học."},
        {"Giáo viên",  "giao_vien",   "Danh sách giáo viên giảng dạy."},
        {"Nhân viên",  "nhan_vien",   "Danh sách nhân viên văn phòng."},
        {"Lương giáo viên",  "phieu_luong", "Phiếu lương của giáo viên, kèm họ tên.", "giao_vien"},
        {"Lớp học",    "lop",     "Danh sách lớp học và sĩ số."},
        {"Lịch học",   "lich_hoc",    "Lịch học theo lớp, ngày và phòng."},
        {"Khóa học",   "khoa_hoc",    "Danh sách khóa học đang mở."},
        {"Đề thi",     "de_thi",      "Danh sách đề thi."},
        {"Kết quả thi", "ket_qua_thi", "Kết quả thi của từng học sinh."}
    };

    // Biểu tượng cho các mục menu (cùng thứ tự với SECTIONS)
    private final String[] ICONS = {"cap", "board", "briefcase", "money", "group", "calendar", "book", "doc", "exam"};

    // Tự đổi tên cột SQL (ma_khoa_hoc) thành tiêu đề tiếng Việt (Mã khóa học)
    private final Map<String, String> WORDS = new HashMap<String, String>();

    private Connection conn;
    private FadePanel content;
    private JPanel tablePanel;
    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private JLabel statusLabel;
    private JPanel inputPanel;

    private final List<JButton> menuButtons = new ArrayList<JButton>();
    private JButton activeButton;

    private String currentTable = "";
    private String currentTitle = "";

    // Chế độ ghép bảng (lương nhân viên / lương giáo viên kèm họ tên)
    private String joinTable = null, joinFk = null, joinPk = null, joinName = null;
    private int namePos = -1; // vị trí cột "Họ và tên" trong bảng hiển thị
    private final List<String> columns = new ArrayList<String>();
    private final List<String> primaryKeys = new ArrayList<String>();

    public AdminDashboard() {
        setTitle("Admin | Hệ thống quản trị");
        setSize(1380, 820);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initWords();

        try {
            conn = DBConnection.getConnection();

            if (conn == null || conn.isClosed()) {
                throw new SQLException("Không có kết nối MySQL.");
            }

            getContentPane().setBackground(BG);
            setLayout(new BorderLayout());
            add(createTopBar(), BorderLayout.NORTH);
            add(createSidebar(), BorderLayout.WEST);

            content = new FadePanel(new BorderLayout(0, 20));
            content.setBackground(BG);
            content.setBorder(new EmptyBorder(25, 28, 22, 28));
            add(content, BorderLayout.CENTER);

            setActive(menuButtons.get(0));
            showDashboard();
            setVisible(true);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Không kết nối được MySQL!\n"
                    + "Kiểm tra database btl và file DBConnection.java.\n\n"
                    + e.getMessage(),
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void initWords() {
        String[] w = {
            "ma","mã","id","mã","ho","họ","ten","tên","ngay","ngày","gioi","giới","tinh","tính",
            "lop","lớp","khoa","khóa","hoc","học","giao","giáo","vien","viên","nhan","nhân",
            "sdt","SĐT","so","số","dien","điện","thoai","thoại","chuyen","chuyên","mon","môn",
            "de","đề","trinh","trình","do","độ","chuc","chức","vu","vụ","tro","trò","luong","lương",
            "nien","niên","si","sĩ","phong","phòng","gio","giờ","thoi","thời","dang","đăng","ki","kí",
            "phi","phí","diem","điểm","xep","xếp","loai","loại","bat","bắt","dau","đầu","ket","kết",
            "thuc","thúc","qua","quả","lich","lịch","phieu","phiếu","hoa","hóa","don","đơn",
            "dia","địa","chi","chỉ","gv","GV","hs","HS","nv","NV","kq","KQ","thang","tháng",
            "nam","năm","tien","tiền","trang","trạng","thai","thái","cap","cấp",
            "quoc","quốc","tich","tịch","chung","chứng","tong","tổng","mat","mật","khau","khẩu",
            "tai","tài","khoan","khoản","mo","mô","ta","tả","tieu","tiêu","nguoi","người",
            "chu","chú","ghi","ghi","va","và","danh","đánh","gia","giá","phan","phản","hoi","hồi"
        };
        for (int i = 0; i + 1 < w.length; i += 2) WORDS.put(w[i], w[i + 1]);
    }

    private String label(String col) {
        StringBuilder sb = new StringBuilder();
        for (String part : col.toLowerCase().split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(WORDS.containsKey(part) ? WORDS.get(part) : part);
        }
        String r = sb.toString();
        return r.isEmpty() ? col : Character.toUpperCase(r.charAt(0)) + r.substring(1);
    }

    // ================= TIỆN ÍCH GIAO DIỆN =================
    // Trộn 2 màu theo tỉ lệ t (0..1) — dùng cho các hiệu ứng chuyển màu mượt
    private static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static float[] rgb(Color c) {
        return new float[]{c.getRed(), c.getGreen(), c.getBlue()};
    }

    private static int clamp255(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    // Hiệu ứng đếm số tăng dần (0 -> giá trị) cho thẻ thống kê
    private void animateCount(JLabel lbl, int target) {
        if (target <= 0) return;
        lbl.setText("0");
        final long start = System.nanoTime();
        final int duration = 850;
        final Timer t = new Timer(16, null);
        t.addActionListener(e -> {
            float p = Math.min(1f, (System.nanoTime() - start) / 1000000f / duration);
            float ease = 1f - (float) Math.pow(1f - p, 3);
            lbl.setText(String.valueOf(Math.round(target * ease)));
            if (p >= 1f) {
                lbl.setText(String.valueOf(target));
                t.stop();
            }
        });
        t.start();
    }

    // Panel nội dung chính: mờ dần khi chuyển trang
    private class FadePanel extends JPanel {
        private float alpha = 1f;
        private Timer timer;

        FadePanel(LayoutManager lm) {
            super(lm);
        }

        void fadeIn() {
            if (timer != null) timer.stop();
            alpha = 0f;
            timer = new Timer(16, e -> {
                alpha += 0.085f;
                if (alpha >= 1f) {
                    alpha = 1f;
                    timer.stop();
                }
                repaint();
            });
            timer.start();
        }

        @Override
        public void paint(Graphics g) {
            if (alpha >= 1f) {
                super.paint(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            super.paint(g2);
            g2.dispose();
        }
    }

    // ================= THANH TRÊN =================
    private JPanel createTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setPreferredSize(new Dimension(0, 76));
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 14));
        brand.setOpaque(false);

        JLabel logo = new JLabel("A", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(49, 94, 251),
                        getWidth(), getHeight(), new Color(112, 78, 230)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setOpaque(false);
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 25));
        logo.setPreferredSize(new Dimension(46, 46));

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel name = new JLabel("Admin");
        name.setFont(new Font("Segoe UI", Font.BOLD, 21));
        name.setForeground(TEXT);

        JLabel subtitle = new JLabel("ADMINISTRATION SYSTEM");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subtitle.setForeground(MUTED);

        brandText.add(name);
        brandText.add(Box.createVerticalStrut(2));
        brandText.add(subtitle);
        brand.add(logo);
        brand.add(brandText);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        right.setOpaque(false);

        JLabel admin = new JLabel("●  Xin chào, Admin");
        admin.setForeground(TEXT);
        admin.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton logout = new RoundButton("Đăng xuất", 10, Color.WHITE, TEXT, BORDER);
        ((RoundButton) logout).setFixedHeight(36);

        logout.addActionListener(e -> {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có muốn đăng xuất khỏi hệ thống không?",
                    "Xác nhận đăng xuất",
                    JOptionPane.YES_NO_OPTION
            );

            if (answer == JOptionPane.YES_OPTION) {

                // Đóng kết nối database
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                // Mở lại màn hình LoginForm
                LoginForm login = new LoginForm();
                login.setVisible(true);

                // Đóng AdminDashboard
                dispose();
            }
        });

        right.add(admin);
        right.add(logout);
        top.add(brand, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    // ================= MENU TRÁI =================
    private JPanel createSidebar() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(238, 0));
        side.setBackground(DARK);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(25, 13, 20, 13));

        JLabel heading = new JLabel("MENU QUẢN TRỊ");
        heading.setForeground(new Color(151, 164, 190));
        heading.setFont(new Font("Segoe UI", Font.BOLD, 11));
        heading.setBorder(new EmptyBorder(5, 13, 15, 0));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        side.add(heading);

        addMenuButton(side, "home", "Tổng quan", () -> showDashboard());

        for (int i = 0; i < SECTIONS.length; i++) {
            final String[] sec = SECTIONS[i];
            addMenuButton(side, ICONS[i], sec[0],
                    () -> showSection(sec[0], sec[1], sec[2], sec.length > 3 ? sec[3] : null));
        }

        side.add(Box.createVerticalGlue());
        return side;
    }

    private void addMenuButton(JPanel side, String icon, String text, Runnable action) {

        RoundButton button = new RoundButton(text, 12, DARK, MENU_TEXT, null);
        button.setMenuMode(new Color(42, 55, 84), ACTIVE);
        button.setIcon(new MenuIcon(icon));
        button.setIconTextGap(14);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBorder(new EmptyBorder(12, 15, 12, 10));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setAlignmentX(LEFT_ALIGNMENT);

        button.addActionListener(e -> {
            setActive(button);
            action.run();
        });

        menuButtons.add(button);
        side.add(button);
        side.add(Box.createVerticalStrut(5));
    }

    private void setActive(JButton btn) {
        activeButton = btn;
        for (JButton b : menuButtons) {
            ((RoundButton) b).setActiveState(b == btn);
        }
    }

    private JButton toolButton(String text, Color bg, Color fg, Color border) {
        RoundButton b = new RoundButton(text, 10, bg, fg, border);
        b.setFixedHeight(38);
        return b;
    }

    // ================= TỔNG QUAN =================
    // Thẻ thống kê: đổ bóng + viền chuyển màu mượt khi rê chuột
    private class HoverCard extends JPanel {
        private final Color accent;
        private float hp = 0f;
        private boolean over = false;
        private final Timer tm;

        HoverCard(Color accent) {
            super(new BorderLayout(0, 10));
            this.accent = accent;
            setOpaque(false);
            setBorder(new EmptyBorder(23, 23, 21, 23));
            tm = new Timer(15, e -> {
                float target = over ? 1f : 0f;
                hp += (target - hp) * 0.25f;
                if (Math.abs(target - hp) < 0.02f) {
                    hp = target;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    over = true;
                    tm.start();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!contains(e.getPoint())) {
                        over = false;
                        tm.start();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // bóng mềm đậm dần khi rê chuột
            for (int i = 1; i <= 4; i++) {
                int a = Math.round(16f * hp / i);
                if (a <= 0) continue;
                g2.setColor(new Color(22, 32, 55, a));
                g2.fillRoundRect(3 - i, 3 - i + 2, w - 6 + 2 * i, h - 6 + 2 * i, 22 + i * 2, 22 + i * 2);
            }

            g2.setColor(mix(Color.WHITE, new Color(248, 250, 255), hp));
            g2.fillRoundRect(3, 3, w - 7, h - 7, 22, 22);
            g2.setColor(mix(BORDER, accent, hp));
            g2.drawRoundRect(3, 3, w - 7, h - 7, 22, 22);
            g2.dispose();
        }
    }

    private JPanel createCard(String title, String value, Color accent, String symbol) {
        JPanel card = new HoverCard(accent);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(MUTED);

        JLabel icon = new JLabel(symbol, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setOpaque(false);
        icon.setForeground(accent);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        icon.setPreferredSize(new Dimension(42, 42));

        top.add(label, BorderLayout.CENTER);
        top.add(icon, BorderLayout.EAST);

        JLabel number = new JLabel(value);
        number.setFont(new Font("Segoe UI", Font.BOLD, 30));
        number.setForeground(TEXT);
        if (value != null && value.matches("\\d{1,9}")) {
            animateCount(number, Integer.parseInt(value));
        }

        JLabel detail = new JLabel("Tổng dữ liệu hiện có");
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detail.setForeground(MUTED);

        JPanel lower = new JPanel();
        lower.setOpaque(false);
        lower.setLayout(new BoxLayout(lower, BoxLayout.Y_AXIS));
        lower.add(number);
        lower.add(Box.createVerticalStrut(5));
        lower.add(detail);

        card.add(top, BorderLayout.NORTH);
        card.add(lower, BorderLayout.CENTER);
        return card;
    }

    private void showDashboard() {
        content.removeAll();
        content.setLayout(new BorderLayout(0, 20));
        currentTable = "";

        JPanel welcome = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(49, 94, 251),
                        w, h, new Color(102, 74, 220)));
                g2.fillRoundRect(0, 0, w, h, 24, 24);

                // vòng tròn trang trí mờ
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, 24, 24);
                g2.setClip(clip);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillOval(w - 230, -90, 280, 280);
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillOval(w - 360, h - 110, 220, 220);
                g2.dispose();
            }
        };
        welcome.setOpaque(false);
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        welcome.setBorder(new EmptyBorder(24, 27, 24, 27));

        JLabel hello = new JLabel("Chào mừng trở lại, Admin!");
        hello.setFont(new Font("Segoe UI", Font.BOLD, 24));
        hello.setForeground(Color.WHITE);

        JLabel description = new JLabel("Theo dõi và quản lý dữ liệu học sinh, giáo viên, lớp học, khóa học.");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        description.setForeground(new Color(235, 239, 255));

        welcome.add(hello);
        welcome.add(Box.createVerticalStrut(8));
        welcome.add(description);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 8, 8));
        cards.setOpaque(false);
        cards.add(createCard("Tổng số học sinh", countRows("hoc_sinh"), BLUE, "HS"));
        cards.add(createCard("Tổng số giáo viên", countRows("giao_vien"), new Color(18, 153, 122), "GV"));
        cards.add(createCard("Tổng số lớp học", countRows("lop"), new Color(225, 148, 38), "LH"));
        cards.add(createCard("Tổng số khóa học", countRows("khoa_hoc"), new Color(135, 87, 205), "KH"));

        JPanel recent = new JPanel(new BorderLayout(0, 13));
        recent.setBackground(Color.WHITE);
        recent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(18, 18, 18, 18)));

        JLabel recentTitle = new JLabel("Học sinh gần đây");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        recentTitle.setForeground(TEXT);
        recent.add(recentTitle, BorderLayout.NORTH);

        String recentTable = findTable("hoc_sinh");
        if (recentTable != null) {
            recent.add(makeTableScroll(readTable(recentTable, 8)), BorderLayout.CENTER);
        } else {
            recent.add(new JLabel("Chưa tìm thấy bảng hoc_sinh."), BorderLayout.CENTER);
        }

        body.add(cards, BorderLayout.NORTH);
        body.add(recent, BorderLayout.CENTER);

        statusLabel = new JLabel("  ●  Đã kết nối database: btl");
        statusLabel.setForeground(MUTED);

        content.add(welcome, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);
        content.revalidate();
        content.repaint();
        content.fadeIn();
    }

    private String countRows(String wantedTable) {
        String realName = findTable(wantedTable);
        if (realName == null) return "0";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM `" + realName + "`")) {
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            return "Lỗi";
        }
        return "0";
    }

    // Tìm bảng không phân biệt hoa thường
    private String findTable(String wantedName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    if (name.equalsIgnoreCase(wantedName)) return name;
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    // ================= TRANG MỖI MỤC (HỌC SINH, GIÁO VIÊN, ...) =================
    private void showSection(String title, String tableName, String desc, String joinTarget) {
        currentTitle = title;
        content.removeAll();
        content.setLayout(new BorderLayout(0, 15));

        JLabel heading = new JLabel("Quản lý " + title.toLowerCase());
        heading.setFont(new Font("Segoe UI", Font.BOLD, 25));
        heading.setForeground(TEXT);

        JLabel sub = new JLabel(desc);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(MUTED);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(heading);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(sub);

        searchField = new SearchField("Tìm theo mã, họ tên hoặc thông tin bất kỳ...");
        searchField.setPreferredSize(new Dimension(320, 38));

        JButton search = toolButton("Tìm kiếm", BLUE, Color.WHITE, null);
        search.addActionListener(e -> searchData());
        searchField.addActionListener(e -> searchData());

        JButton refresh = toolButton("Làm mới", Color.WHITE, TEXT, new Color(205, 212, 226));
        refresh.addActionListener(e -> {
            searchField.setText("");
            refreshData();
        });

        JButton add = toolButton("+ Thêm", new Color(18, 153, 122), Color.WHITE, null);
        add.addActionListener(e -> addRecord());

        JButton edit = toolButton("Sửa", new Color(255, 247, 224),
                new Color(150, 93, 10), new Color(240, 214, 150));
        edit.addActionListener(e -> editRecord());

        JButton delete = toolButton("Xóa", new Color(255, 235, 235),
                new Color(190, 40, 50), new Color(244, 190, 195));
        delete.addActionListener(e -> deleteRecord());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(searchField);
        left.add(search);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(refresh);
        right.add(add);
        right.add(edit);
        right.add(delete);

        JPanel tools = new JPanel(new BorderLayout());
        tools.setBackground(Color.WHITE);
        tools.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 16, 14, 16)));
        tools.add(left, BorderLayout.WEST);
        tools.add(right, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(titleBox, BorderLayout.NORTH);
        top.add(tools, BorderLayout.SOUTH);

        tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createLineBorder(BORDER));

        statusLabel = new JLabel("");
        statusLabel.setForeground(MUTED);
        statusLabel.setBorder(new EmptyBorder(8, 2, 0, 0));

        content.add(top, BorderLayout.NORTH);
        content.add(tablePanel, BorderLayout.CENTER);

        joinTable = null;
        namePos = -1;
        String real = findTable(tableName);
        if (real == null) {
            currentTable = "";
            table = null;
            JLabel msg = new JLabel("Chưa có bảng \"" + tableName
                    + "\" trong database btl. Hãy chạy file schema.sql trước.",
                    SwingConstants.CENTER);
            msg.setForeground(MUTED);
            tablePanel.add(msg, BorderLayout.CENTER);
            statusLabel.setText("  Không tìm thấy bảng: " + tableName);
        } else {
            String err = joinTarget == null ? null : setupJoin(real, joinTarget);
            if (err != null) {
                currentTable = "";
                table = null;
                JLabel msg = new JLabel("<html><div style='text-align:center'>"
                        + err.replace("\n", "<br>") + "</div></html>", SwingConstants.CENTER);
                msg.setForeground(MUTED);
                tablePanel.add(msg, BorderLayout.CENTER);
                statusLabel.setText("  Không ghép được dữ liệu họ tên");
            } else {
                loadSelectedTable(real);
            }
        }

        content.revalidate();
        content.repaint();
        content.fadeIn();
    }

    // Tìm cột liên kết giữa bảng phiếu lương và bảng người nhận (nhân viên / giáo viên)
    private String setupJoin(String realP, String wanted) {
        String realJ = findTable(wanted);
        if (realJ == null) return "Chưa có bảng \"" + wanted + "\" trong database btl.";

        try {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> pCols = new ArrayList<String>();
            List<String> jCols = new ArrayList<String>();
            String jPk = null;

            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, realP, "%")) {
                while (rs.next()) pCols.add(rs.getString("COLUMN_NAME"));
            }
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, realJ, "%")) {
                while (rs.next()) jCols.add(rs.getString("COLUMN_NAME"));
            }
            try (ResultSet rs = meta.getPrimaryKeys(conn.getCatalog(), null, realJ)) {
                if (rs.next()) jPk = rs.getString("COLUMN_NAME");
            }
            if (jPk == null && !jCols.isEmpty()) jPk = jCols.get(0);

            String fk = null;
            for (String c : pCols) {
                if (c.equalsIgnoreCase(jPk)) { fk = c; break; }
            }
            if (fk == null) {
                for (String c : pCols) {
                    if (c.toLowerCase().contains(wanted.toLowerCase())) { fk = c; break; }
                }
            }
            if (fk == null) {
                return "Bảng " + realP + " chưa có cột liên kết tới bảng " + realJ
                        + " (cần cột tên " + jPk + ").\nCác cột hiện có: " + pCols;
            }

            String nameCol = null;
            for (String c : jCols) {
                if (c.equalsIgnoreCase("ho_ten")) { nameCol = c; break; }
            }
            if (nameCol == null) {
                for (String c : jCols) {
                    if (c.toLowerCase().contains("ten")) { nameCol = c; break; }
                }
            }
            if (nameCol == null) nameCol = jCols.size() > 1 ? jCols.get(1) : jPk;

            joinTable = realJ;
            joinFk = fk;
            joinPk = jPk;
            joinName = nameCol;
            namePos = pCols.indexOf(fk) + 1;
            return null;

        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    // Vị trí cột thật (trong bảng gốc) -> vị trí trong bảng đang hiển thị
    private int viewIndex(int colIdx) {
        return (namePos >= 0 && colIdx >= namePos) ? colIdx + 1 : colIdx;
    }

    private void loadSelectedTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) return;

        currentTable = tableName;
        columns.clear();
        primaryKeys.clear();

        try {
            DatabaseMetaData meta = conn.getMetaData();

            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tableName, "%")) {
                while (rs.next()) columns.add(rs.getString("COLUMN_NAME"));
            }

            try (ResultSet rs = meta.getPrimaryKeys(conn.getCatalog(), null, tableName)) {
                while (rs.next()) primaryKeys.add(rs.getString("COLUMN_NAME"));
            }

            showTableData(tableName, "");

        } catch (SQLException e) {
            showError("Không lấy được cấu trúc bảng", e);
        }
    }

    private void showTableData(String tableName, String keyword) {
        try {
            StringBuilder sql = new StringBuilder();
            boolean searching = keyword != null && !keyword.trim().isEmpty();
            boolean join = joinTable != null && namePos >= 0;
            int terms = columns.size() + (join ? 1 : 0);

            if (!join) {
                sql.append("SELECT * FROM `" + tableName + "`");
            } else {
                sql.append("SELECT ");
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("p.`").append(columns.get(i)).append("`");
                    if (i == namePos - 1) {
                        sql.append(", j.`").append(joinName).append("` AS `Họ và tên`");
                    }
                }
                sql.append(" FROM `").append(tableName).append("` p JOIN `").append(joinTable)
                   .append("` j ON p.`").append(joinFk).append("` = j.`").append(joinPk).append("`");
            }

            if (searching && !columns.isEmpty()) {
                sql.append(" WHERE ");
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) sql.append(" OR ");
                    sql.append("CAST(").append(join ? "p." : "").append("`")
                       .append(columns.get(i)).append("` AS CHAR) LIKE ?");
                }
                if (join) {
                    sql.append(" OR CAST(j.`").append(joinName).append("` AS CHAR) LIKE ?");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                if (searching) {
                    for (int i = 0; i < terms; i++) {
                        ps.setString(i + 1, "%" + keyword.trim() + "%");
                    }
                }

                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int n = meta.getColumnCount();
                    String[] headers = new String[n];

                    for (int i = 1; i <= n; i++) {
                        headers[i - 1] = label(meta.getColumnLabel(i));
                    }

                    model = new DefaultTableModel(headers, 0) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };

                    while (rs.next()) {
                        Object[] row = new Object[n];
                        for (int i = 1; i <= n; i++) row[i - 1] = rs.getObject(i);
                        model.addRow(row);
                    }

                    table = new JTable(model);
                    styleTable(table);

                    tablePanel.removeAll();
                    if (model.getRowCount() == 0) {
                        JLabel empty = new JLabel(searching
                                ? "Không có bản ghi nào khớp từ khóa. Hãy đổi từ khóa hoặc bấm Làm mới."
                                : "Bảng này chưa có dữ liệu. Bấm + Thêm để tạo bản ghi đầu tiên.",
                                SwingConstants.CENTER);
                        empty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                        empty.setForeground(MUTED);
                        tablePanel.add(empty, BorderLayout.CENTER);
                    } else {
                        tablePanel.add(makeTableScroll(table), BorderLayout.CENTER);
                    }
                    tablePanel.revalidate();
                    tablePanel.repaint();

                    String message = "Bảng: " + tableName;
                    if (searching) message = "Từ khóa: \"" + keyword.trim() + "\"";

                    statusLabel.setText("  " + message + "  |  " + model.getRowCount() + " bản ghi");
                }
            }

        } catch (SQLException e) {
            showError("Không đọc được dữ liệu bảng " + tableName, e);
        }
    }

    private JScrollPane makeTableScroll(JTable t) {
        JScrollPane scroll = new JScrollPane(t);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
        return scroll;
    }

    private JTable readTable(String tableName, int limit) {
        try {
            String sql = "SELECT * FROM `" + tableName + "`";
            if (limit > 0) sql += " LIMIT " + limit;

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                ResultSetMetaData meta = rs.getMetaData();
                int n = meta.getColumnCount();
                String[] headers = new String[n];
                for (int i = 1; i <= n; i++) headers[i - 1] = label(meta.getColumnLabel(i));

                DefaultTableModel data = new DefaultTableModel(headers, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 1; i <= n; i++) row[i - 1] = rs.getObject(i);
                    data.addRow(row);
                }

                JTable result = new JTable(data);
                styleTable(result);
                return result;
            }
        } catch (SQLException e) {
            showError("Không đọc được dữ liệu bảng " + tableName, e);
            return new JTable();
        }
    }

    // Định dạng giá trị hiển thị trong ô (số lớn có dấu chấm ngăn cách: 9.000.000)
    private String format(Object v) {
        if (v == null) return "—";
        java.text.NumberFormat vn = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        vn.setMaximumFractionDigits(2);
        if (v instanceof Integer || v instanceof Long || v instanceof java.math.BigInteger) {
            long n = ((Number) v).longValue();
            return Math.abs(n) >= 10000 ? vn.format(n) : String.valueOf(n);
        }
        if (v instanceof java.math.BigDecimal) {
            java.math.BigDecimal d = ((java.math.BigDecimal) v).stripTrailingZeros();
            return d.abs().compareTo(new java.math.BigDecimal(10000)) >= 0
                    ? vn.format(d) : d.toPlainString();
        }
        String text = v.toString();
        if (v instanceof java.sql.Timestamp && text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    // Vẽ ô: đệm hai bên, dòng chẵn lẻ xen kẽ màu, dòng rê chuột và dòng chọn sáng lên
    private class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable tb, Object v,
                boolean sel, boolean focus, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(tb, format(v), sel, false, r, c);
            l.setBorder(new EmptyBorder(0, 16, 0, 16));
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setVerticalAlignment(SwingConstants.CENTER);
            Object hv = tb.getClientProperty("hoverRow");
            boolean hover = hv instanceof Integer && ((Integer) hv) == r;

            if (sel) l.setBackground(new Color(224, 232, 255));
            else if (hover) l.setBackground(new Color(238, 243, 255));
            else l.setBackground(r % 2 == 0 ? Color.WHITE : new Color(249, 250, 254));

            l.setForeground(v == null ? new Color(170, 177, 194) : TEXT);
            return l;
        }
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setForeground(TEXT);
        t.setBackground(Color.WHITE);
        t.setRowHeight(46);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setSelectionBackground(new Color(224, 232, 255));
        t.setSelectionForeground(TEXT);
        t.putClientProperty("hoverRow", -1);
        t.setDefaultRenderer(Object.class, new CellRenderer());

        // Tiêu đề cột: nền xanh nhạt, chữ đậm, gạch chân xanh
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable tb, Object v,
                    boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setOpaque(true);
                l.setBackground(new Color(240, 244, 252));
                l.setForeground(new Color(60, 72, 100));
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(196, 208, 246)),
                        new EmptyBorder(0, 16, 0, 16)));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });
        t.getTableHeader().setPreferredSize(new Dimension(0, 46));
        t.getTableHeader().setReorderingAllowed(false);

        // Rê chuột: làm sáng dòng đang trỏ tới
        t.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = t.rowAtPoint(e.getPoint());
                Object old = t.getClientProperty("hoverRow");
                if (!(old instanceof Integer) || ((Integer) old) != r) {
                    t.putClientProperty("hoverRow", r);
                    t.repaint();
                }
            }
        });
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                t.putClientProperty("hoverRow", -1);
                t.repaint();
            }
        });

        // Độ rộng cột theo nội dung; bảng ít cột thì tự dãn kín chiều ngang
        FontMetrics fm = t.getFontMetrics(t.getFont());
        FontMetrics hf = t.getFontMetrics(new Font("Segoe UI", Font.BOLD, 13));
        int total = 0;
        for (int i = 0; i < t.getColumnCount(); i++) {
            int w = hf.stringWidth(String.valueOf(t.getColumnModel().getColumn(i).getHeaderValue())) + 40;
            for (int r = 0; r < Math.min(t.getRowCount(), 200); r++) {
                w = Math.max(w, fm.stringWidth(format(t.getValueAt(r, i))) + 40);
            }
            w = Math.max(100, Math.min(320, w));
            t.getColumnModel().getColumn(i).setPreferredWidth(w);
            total += w;
        }
        t.setAutoResizeMode(total > 1000 ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private void searchData() {
        if (currentTable == null || currentTable.isEmpty()) return;
        showTableData(currentTable, searchField.getText().trim());
    }

    // ================= THÊM / SỬA / XÓA =================
    // Form nhập liệu: 2 cặp "nhãn - ô nhập" trên mỗi hàng

    private boolean kiemTraSoDienThoai(List<JTextField> fields) {
        // Chỉ kiểm tra bảng học sinh và giáo viên
        if (!currentTable.equalsIgnoreCase("hoc_sinh")
                && !currentTable.equalsIgnoreCase("giao_vien")) {
            return true;
        }

        // Tìm cột số điện thoại
        int viTriSDT = -1;

        for (int i = 0; i < columns.size(); i++) {
            String tenCot = columns.get(i).toLowerCase();

            if (tenCot.equals("sdt")
                    || tenCot.contains("so_dien_thoai")
                    || tenCot.contains("sodienthoai")) {
                viTriSDT = i;
                break;
            }
        }

        // Nếu bảng không có cột số điện thoại thì bỏ qua
        if (viTriSDT == -1) {
            return true;
        }

        String sdt = fields.get(viTriSDT).getText().trim();

        if (!sdt.matches("0[0-9]{9}")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0",
                    "Số điện thoại không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );

            fields.get(viTriSDT).requestFocus();
            return false;
        }

        return true;
    }

    private List<JTextField> createInputFields(Object[] oldValues, boolean lockKeys) {
        List<JTextField> fields = new ArrayList<JTextField>();

        inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(18, 22, 18, 22)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(9, 8, 9, 8);
        g.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            String lower = col.toLowerCase();
            String hint = "";
            if (lower.contains("thoi_gian")) hint = "yyyy-MM-dd HH:mm:ss";
            else if (lower.contains("ngay")) hint = "yyyy-MM-dd";

            JLabel lb = new JLabel(label(col));
            lb.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lb.setForeground(TEXT);

            JTextField field = new SearchField(hint);
            field.setPreferredSize(new Dimension(230, 38));
            if (oldValues != null && i < oldValues.length && oldValues[i] != null) {
                field.setText(oldValues[i].toString());
            }
            if (lockKeys && primaryKeys.contains(col)) {
                field.setEnabled(false); // không cho sửa mã
                field.setBackground(new Color(238, 241, 247));
                field.setDisabledTextColor(TEXT);
            }

            int row = i / 2, base = (i % 2) * 2;
            g.gridy = row;

            g.gridx = base;
            g.weightx = 0;
            g.fill = GridBagConstraints.NONE;
            inputPanel.add(lb, g);

            g.gridx = base + 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            inputPanel.add(field, g);

            fields.add(field);
        }
        return fields;
    }

    // Hộp thoại Thêm / Sửa: thanh tiêu đề gradient + form + nút bo góc
    private int showInputDialog(String title) {
        final int[] result = {JOptionPane.CANCEL_OPTION};
        String verb = title.startsWith("Sửa") ? "SỬA" : "THÊM";

        JDialog dlg = new JDialog(this, title, true);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(BG);

        JLabel head = new JLabel(verb + " " + currentTitle.toUpperCase(), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(28, 62, 150),
                        getWidth(), getHeight(), new Color(84, 66, 206)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        head.setOpaque(false);
        head.setForeground(Color.WHITE);
        head.setFont(new Font("Segoe UI", Font.BOLD, 22));
        head.setBorder(new EmptyBorder(20, 0, 20, 0));

        JScrollPane scroll = new JScrollPane(inputPanel);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));

        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.setBorder(new EmptyBorder(22, 26, 8, 26));
        formWrap.add(scroll, BorderLayout.CENTER);

        RoundButton save = new RoundButton("Lưu", 12, new Color(18, 153, 122), Color.WHITE, null);
        RoundButton cancel = new RoundButton("Hủy", 12, new Color(71, 80, 100), Color.WHITE, null);
        for (RoundButton b : new RoundButton[]{save, cancel}) {
            b.setFixedHeight(42);
            b.setBorder(new EmptyBorder(0, 44, 0, 44));
            b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        }
        save.addActionListener(e -> {
            result[0] = JOptionPane.OK_OPTION;
            dlg.dispose();
        });
        cancel.addActionListener(e -> dlg.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 14));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(cancel);

        dlg.add(head, BorderLayout.NORTH);
        dlg.add(formWrap, BorderLayout.CENTER);
        dlg.add(buttons, BorderLayout.SOUTH);

        dlg.getRootPane().setDefaultButton(save);
        dlg.getRootPane().registerKeyboardAction(e -> dlg.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        int rows = (columns.size() + 1) / 2;
        int height = Math.min(720, 190 + rows * 56 + 90);
        dlg.setSize(900, height);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        return result[0];
    }

    private void addRecord() {
        if (currentTable.isEmpty()) return;

        List<JTextField> fields = createInputFields(null, false);

        if (showInputDialog("Thêm bản ghi") != JOptionPane.OK_OPTION) {
            return;
        }

        // Kiểm tra số điện thoại trước khi lưu
        if (!kiemTraSoDienThoai(fields)) {
            return;
        }

        try {
            StringBuilder sql = new StringBuilder("INSERT INTO `" + currentTable + "` (");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("`").append(columns.get(i)).append("`");
            }
            sql.append(") VALUES (");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(")");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < fields.size(); i++) {
                    String value = fields.get(i).getText().trim();
                    if (value.isEmpty()) ps.setNull(i + 1, java.sql.Types.NULL);
                    else ps.setString(i + 1, value);
                }
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Thêm dữ liệu thành công!");
            refreshData();

        } catch (SQLException e) {
            showError("Không thêm được. Kiểm tra mã bị trùng, cột bắt buộc và kiểu dữ liệu\n"
                    + "(ngày nhập dạng yyyy-MM-dd).", e);
        }
    }

    private void editRecord() {
        if (table == null || table.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn dòng muốn sửa.");
            return;
        }
        if (primaryKeys.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bảng không có khóa chính nên không thể sửa an toàn.");
            return;
        }

        int row = table.convertRowIndexToModel(table.getSelectedRow());
        Object[] oldValues = new Object[columns.size()];
        for (int i = 0; i < oldValues.length; i++) oldValues[i] = table.getValueAt(row, viewIndex(i));

        List<JTextField> fields = createInputFields(oldValues, true);

        if (showInputDialog("Sửa bản ghi") != JOptionPane.OK_OPTION) {
            return;
        }

        // Kiểm tra số điện thoại trước khi cập nhật
        if (!kiemTraSoDienThoai(fields)) {
            return;
        }

        try {
            StringBuilder sql = new StringBuilder("UPDATE `" + currentTable + "` SET ");
            boolean first = true;

            for (String col : columns) {
                if (primaryKeys.contains(col)) continue;
                if (!first) sql.append(",");
                sql.append("`").append(col).append("`=?");
                first = false;
            }

            if (first) {
                JOptionPane.showMessageDialog(this, "Bảng không có cột nào để sửa.");
                return;
            }

            sql.append(" WHERE ");
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) sql.append(" AND ");
                sql.append("`").append(primaryKeys.get(i)).append("`=?");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int p = 1;

                for (int i = 0; i < columns.size(); i++) {
                    if (primaryKeys.contains(columns.get(i))) continue;
                    String value = fields.get(i).getText().trim();
                    if (value.isEmpty()) ps.setNull(p++, java.sql.Types.NULL);
                    else ps.setString(p++, value);
                }

                for (String key : primaryKeys) {
                    ps.setObject(p++, oldValues[columns.indexOf(key)]);
                }
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Sửa dữ liệu thành công!");
            refreshData();

        } catch (SQLException e) {
            showError("Không sửa được dữ liệu", e);
        }
    }

    private void deleteRecord() {
        if (table == null || table.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn dòng muốn xóa.");
            return;
        }
        if (primaryKeys.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bảng không có khóa chính nên không thể xóa an toàn.");
            return;
        }

        int answer = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa bản ghi này?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;

        int row = table.convertRowIndexToModel(table.getSelectedRow());

        try {
            StringBuilder sql = new StringBuilder("DELETE FROM `" + currentTable + "` WHERE ");
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) sql.append(" AND ");
                sql.append("`").append(primaryKeys.get(i)).append("`=?");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int p = 1;
                for (String key : primaryKeys) {
                    ps.setObject(p++, table.getValueAt(row, viewIndex(columns.indexOf(key))));
                }
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Xóa dữ liệu thành công!");
            refreshData();

        } catch (SQLException e) {
            showError("Không xóa được. Có thể bản ghi đang được bảng khác tham chiếu.", e);
        }
    }

    // Làm mới: tải lại bảng đang xem, nếu đang ở Tổng quan thì tải lại tổng quan
    private void refreshData() {
        if (!currentTable.isEmpty() && tablePanel != null && tablePanel.isShowing()) {
            loadSelectedTable(currentTable);
        } else {
            showDashboard();
        }
    }

    private void showError(String title, SQLException e) {
        JOptionPane.showMessageDialog(this, title + "\n" + e.getMessage(),
                "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
    }

    // ===== Thanh cuộn mảnh, bo tròn =====
    private class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(198, 206, 224);
            trackColor = Color.WHITE;
        }

        private JButton zeroButton() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d);
            b.setMinimumSize(d);
            b.setMaximumSize(d);
            return b;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == javax.swing.JScrollBar.VERTICAL
                    ? new Dimension(12, 48) : new Dimension(48, 12);
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(Color.WHITE);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean strong = isThumbRollover() || isDragging;
            g2.setColor(strong ? new Color(150, 162, 190) : new Color(198, 206, 224));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 10, 10);
            g2.dispose();
        }
    }

    // ===== Nút bo góc tự vẽ, có hiệu ứng chuyển màu mượt khi rê chuột / nhấn / chọn =====
    private class RoundButton extends JButton {
        private final int arc;
        private final Color borderColor;
        private final Color baseBg, baseFg;
        private boolean hoverEnabled = true, hovering = false, pressing = false;
        private boolean menuMode = false, active = false;
        private Color menuHoverBg, menuActiveBg;
        private int fixedHeight = 0;

        private final float[] curBg, curFg;
        private float bar = 0f;   // thanh nhấn bên trái của mục menu đang chọn
        private float off = 0f;   // độ trượt nhẹ sang phải khi rê chuột (menu)
        private final Timer anim;

        RoundButton(String text, int arc, Color bg, Color fg, Color borderColor) {
            super(text);
            this.arc = arc;
            this.borderColor = borderColor;
            this.baseBg = bg;
            this.baseFg = fg;
            this.curBg = rgb(bg);
            this.curFg = rgb(fg);
            setBackground(bg);
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(0, 18, 0, 18));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            anim = new Timer(15, e -> step());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { hovering = true; retarget(); }
                @Override
                public void mouseExited(MouseEvent e) { hovering = false; pressing = false; retarget(); }
                @Override
                public void mousePressed(MouseEvent e) { pressing = true; retarget(); }
                @Override
                public void mouseReleased(MouseEvent e) { pressing = false; retarget(); }
            });
        }

        void setHover(boolean on) { hoverEnabled = on; }
        void setFixedHeight(int h) { fixedHeight = h; }

        void setMenuMode(Color hoverBg, Color activeBg) {
            this.menuMode = true;
            this.menuHoverBg = hoverBg;
            this.menuActiveBg = activeBg;
        }

        void setActiveState(boolean on) {
            this.active = on;
            retarget();
        }

        private void retarget() {
            if (!anim.isRunning()) anim.start();
        }

        private Color targetBg() {
            if (menuMode) {
                if (active) return menuActiveBg;
                if (hovering) return menuHoverBg;
                return baseBg;
            }
            if (hoverEnabled && hovering) {
                int d = pressing ? 30 : 14;
                return new Color(Math.max(0, baseBg.getRed() - d),
                        Math.max(0, baseBg.getGreen() - d),
                        Math.max(0, baseBg.getBlue() - d));
            }
            return baseBg;
        }

        private Color targetFg() {
            if (menuMode && (active || hovering)) return Color.WHITE;
            return baseFg;
        }

        private void step() {
            float[] tb = rgb(targetBg());
            float[] tf = rgb(targetFg());
            float tBar = (menuMode && active) ? 1f : 0f;
            float tOff = (menuMode && hovering && !active) ? 4f : 0f;
            boolean done = true;

            for (int i = 0; i < 3; i++) {
                curBg[i] += (tb[i] - curBg[i]) * 0.26f;
                curFg[i] += (tf[i] - curFg[i]) * 0.26f;
                if (Math.abs(tb[i] - curBg[i]) > 0.6f || Math.abs(tf[i] - curFg[i]) > 0.6f) done = false;
            }
            bar += (tBar - bar) * 0.26f;
            off += (tOff - off) * 0.26f;
            if (Math.abs(tBar - bar) > 0.02f || Math.abs(tOff - off) > 0.05f) done = false;

            if (done) {
                for (int i = 0; i < 3; i++) { curBg[i] = tb[i]; curFg[i] = tf[i]; }
                bar = tBar;
                off = tOff;
                anim.stop();
            }
            setForeground(new Color(clamp255(curFg[0]), clamp255(curFg[1]), clamp255(curFg[2])));
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return fixedHeight > 0 ? new Dimension(Math.max(d.width, 92), fixedHeight) : d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color c = new Color(clamp255(curBg[0]), clamp255(curBg[1]), clamp255(curBg[2]));
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            if (borderColor != null) {
                g2.setColor(hovering && !menuMode ? mix(borderColor, BLUE, 0.55f) : borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }

            // thanh nhấn bên trái cho mục menu đang chọn
            if (menuMode && bar > 0.02f) {
                int bh = Math.round(getHeight() * 0.5f * bar);
                g2.setColor(new Color(255, 255, 255, Math.round(230 * bar)));
                g2.fillRoundRect(0, (getHeight() - bh) / 2, 4, bh, 4, 4);
            }

            int shift = Math.round(off);
            int py = (pressing && !menuMode) ? 1 : 0;

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText();
            int x;
            Icon ic = getIcon();
            if (ic != null) {
                int ix = getInsets().left + shift;
                ic.paintIcon(this, g2, ix, (getHeight() - ic.getIconHeight()) / 2);
                x = ix + ic.getIconWidth() + getIconTextGap();
            } else if (getHorizontalAlignment() == SwingConstants.LEFT) {
                x = getInsets().left + shift;
            } else {
                x = (getWidth() - fm.stringWidth(t)) / 2;
            }
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + py;
            g2.setColor(getForeground());
            g2.drawString(t, x, y);
            g2.dispose();
        }
    }

    // ===== Biểu tượng vẽ bằng code (không phụ thuộc font) =====
    private class MenuIcon implements Icon {
        private final String type;

        MenuIcon(String type) { this.type = type; }

        @Override public int getIconWidth() { return 20; }
        @Override public int getIconHeight() { return 20; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(c.getForeground());
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case "home":      // ngôi nhà
                    g2.drawPolyline(new int[]{2, 10, 18}, new int[]{10, 2, 10}, 3);
                    g2.drawPolyline(new int[]{4, 4, 16, 16}, new int[]{9, 18, 18, 9}, 4);
                    g2.drawRect(8, 12, 4, 6);
                    break;
                case "cap":       // học sinh: mũ tốt nghiệp
                    g2.drawPolygon(new int[]{10, 19, 10, 1}, new int[]{3, 8, 13, 8}, 4);
                    g2.drawPolyline(new int[]{5, 5, 10, 15, 15}, new int[]{10, 14, 17, 14, 10}, 5);
                    break;
                case "board":     // giáo viên: bảng đen
                    g2.drawRoundRect(2, 3, 16, 10, 2, 2);
                    g2.drawLine(10, 13, 10, 17);
                    g2.drawLine(6, 18, 14, 18);
                    g2.drawLine(6, 8, 14, 8);
                    break;
                case "briefcase": // nhân viên: cặp công sở
                    g2.drawRoundRect(2, 6, 16, 11, 3, 3);
                    g2.drawRoundRect(7, 3, 6, 3, 2, 2);
                    g2.drawLine(2, 11, 18, 11);
                    break;
                case "money":     // phiếu lương: tờ tiền
                    g2.drawRoundRect(1, 5, 18, 11, 3, 3);
                    g2.drawOval(7, 7, 6, 6);
                    g2.drawLine(4, 8, 4, 8);
                    g2.drawLine(16, 13, 16, 13);
                    break;
                case "group":     // lớp học: nhóm người
                    g2.drawOval(3, 4, 5, 5);
                    g2.drawOval(12, 4, 5, 5);
                    g2.drawArc(1, 11, 9, 9, 0, 180);
                    g2.drawArc(10, 11, 9, 9, 0, 180);
                    break;
                case "calendar":  // lịch học
                    g2.drawRoundRect(2, 4, 16, 14, 3, 3);
                    g2.drawLine(2, 9, 18, 9);
                    g2.drawLine(6, 2, 6, 6);
                    g2.drawLine(14, 2, 14, 6);
                    break;
                case "book":      // khóa học: sách mở
                    g2.drawPolyline(new int[]{10, 3, 3, 10}, new int[]{5, 4, 15, 17}, 4);
                    g2.drawPolyline(new int[]{10, 17, 17, 10}, new int[]{5, 4, 15, 17}, 4);
                    g2.drawLine(10, 5, 10, 17);
                    break;
                case "doc":       // đề thi: tờ đề có dòng chữ
                    g2.drawRoundRect(4, 2, 12, 16, 3, 3);
                    g2.drawLine(7, 7, 13, 7);
                    g2.drawLine(7, 10, 13, 10);
                    g2.drawLine(7, 13, 11, 13);
                    break;
                case "exam":      // kết quả thi: giấy có dấu tích
                    g2.drawRoundRect(4, 2, 12, 16, 3, 3);
                    g2.drawPolyline(new int[]{7, 9, 13}, new int[]{10, 13, 7}, 3);
                    break;
                default:
                    g2.drawOval(4, 4, 12, 12);
            }
            g2.dispose();
        }
    }

    // ===== Ô tìm kiếm bo góc, có chữ gợi ý, viền chuyển màu mượt khi focus =====
    private class SearchField extends JTextField {
        private final String hint;
        private final Color idle = new Color(205, 212, 226);
        private float fp = 0f;
        private boolean focused = false;
        private Timer ft;

        SearchField(String hint) {
            this.hint = hint;
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setForeground(TEXT);
            setBorder(makeBorder(idle));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) { focused = true; animateBorder(); }
                @Override
                public void focusLost(FocusEvent e) { focused = false; animateBorder(); }
            });
        }

        private void animateBorder() {
            if (ft != null) ft.stop();
            ft = new Timer(15, e -> {
                float target = focused ? 1f : 0f;
                fp += (target - fp) * 0.3f;
                if (Math.abs(target - fp) < 0.03f) {
                    fp = target;
                    ft.stop();
                }
                setBorder(makeBorder(mix(idle, BLUE, fp)));
                repaint();
            });
            ft.start();
        }

        private javax.swing.border.Border makeBorder(Color c) {
            return BorderFactory.createCompoundBorder(
                    new LineBorder(c, 1, true), new EmptyBorder(0, 12, 0, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new AdminDashboard());
    }
}