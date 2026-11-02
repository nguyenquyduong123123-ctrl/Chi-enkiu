import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Khung giao diện dùng chung cho cổng Giáo viên và cổng Nhân viên
 * (cùng phong cách với StudentDashboardUI): thanh trên, menu trái, thẻ thống kê,
 * bảng dữ liệu và các hàm truy vấn MySQL qua DBConnection.getConnection().
 *
 * Cách dùng trong lớp con:
 *   1. đọc hồ sơ người dùng từ DB
 *   2. addPage(...) cho từng mục menu
 *   3. build(...) để dựng giao diện
 */
public abstract class PortalFrame extends JFrame {

    /* ===================== MÀU SẮC ===================== */

    protected static final Color C_BG = new Color(244, 247, 251);
    protected static final Color C_CARD = Color.WHITE;
    protected static final Color C_BORDER = new Color(226, 232, 240);
    protected static final Color C_TEXT = new Color(15, 23, 42);
    protected static final Color C_MUTED = new Color(100, 116, 139);
    protected static final Color C_HOVER = new Color(241, 245, 249);
    protected static final Color C_SELECT = new Color(219, 234, 254);
    protected static final Color C_SUCCESS = new Color(16, 185, 129);
    protected static final Color C_SUCCESS_DARK = new Color(5, 150, 105);
    protected static final Color C_WARNING = new Color(245, 158, 11);
    protected static final Color C_DANGER = new Color(220, 38, 38);
    protected static final Color C_STRIPE = new Color(248, 250, 252);

    // Màu chủ đạo của cổng (lớp con truyền vào build())
    protected Color accent = new Color(37, 99, 235);
    protected Color accentDark = new Color(29, 78, 216);
    protected Color accentSoft = new Color(232, 240, 254);
    protected Color accentLight = new Color(224, 242, 254);

    /* ===================== KIỂU DỮ LIỆU PHỤ ===================== */

    @FunctionalInterface
    protected interface RowMapper {
        Object[] map(ResultSet rs) throws SQLException;
    }

    protected enum IconType { HOME, BOOK, CALENDAR, AWARD, WALLET, USER, GROUP, STAR, LIST }

    private static class Page {
        final String key, label;
        final IconType icon;

        Page(String key, String label, IconType icon) {
            this.key = key;
            this.label = label;
            this.icon = icon;
        }
    }

    private final List<Page> pages = new ArrayList<>();
    private final Map<String, Supplier<JPanel>> builders = new HashMap<>();
    private final Map<String, JPanel> currentPanels = new HashMap<>();
    private final List<NavButton> navButtons = new ArrayList<>();
    private CardLayout cardLayout;
    private JPanel mainContentPanel;

    /* ===================== DỰNG KHUNG ===================== */

    /** Đăng ký một mục menu. Mỗi lần bấm menu, panel được dựng lại nên dữ liệu luôn mới. */
    protected void addPage(String key, String label, IconType icon, Supplier<JPanel> builder) {
        pages.add(new Page(key, label, icon));
        builders.put(key, builder);
    }

    protected void build(String windowTitle, String badge, String brand, String sectionLabel,
                         String footerText, String userName, String userSub, Color accentColor) {
        this.accent = accentColor;
        this.accentDark = accentColor.darker();
        this.accentSoft = tint(accentColor, 0.12);
        this.accentLight = tint(accentColor, 0.20);

        setTitle(windowTitle);
        setSize(1320, 800);
        setMinimumSize(new Dimension(1100, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(C_BG);

        add(createTopBar(badge, brand, userName, userSub), BorderLayout.NORTH);
        add(createSidebar(sectionLabel, footerText), BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);

        if (!pages.isEmpty()) {
            navButtons.get(0).setActive(true);
            showPage(pages.get(0).key);
        }
    }

    protected void showPage(String key) {
        JPanel old = currentPanels.get(key);
        if (old != null) mainContentPanel.remove(old);
        JPanel fresh = builders.get(key).get();
        currentPanels.put(key, fresh);
        mainContentPanel.add(fresh, key);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
        cardLayout.show(mainContentPanel, key);
    }

    protected void onLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có muốn đăng xuất khỏi hệ thống?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginForm().setVisible(true);
        }
    }

    private static Color tint(Color c, double f) {
        return new Color(255 - (int) ((255 - c.getRed()) * f),
                255 - (int) ((255 - c.getGreen()) * f),
                255 - (int) ((255 - c.getBlue()) * f));
    }

    /* ===================== TIỆN ÍCH ===================== */

    protected static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    protected static String nz(String s) {
        return s == null ? "" : s;
    }

    protected static String vnd(long n) {
        return String.format(Locale.US, "%,d", n).replace(',', '.') + " VNĐ";
    }

    protected static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** "2026-11-02 18:00:00.0" -> "2026-11-02 18:00" */
    protected static String dateTime(String s) {
        if (s == null) return "";
        return s.length() >= 16 ? s.substring(0, 16) : s;
    }

    /** "2026-11-02 18:00:00.0" -> "2026-11-02" */
    protected static String dateOnly(String s) {
        if (s == null) return "";
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }

    protected static String stars(int n) {
        int k = Math.max(0, Math.min(5, n));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < k ? "★" : "☆");
        return sb + "  (" + n + ")";
    }

    protected static String buoiLabel(String s) {
        if ("nghi".equalsIgnoreCase(s)) return "Nghỉ";
        if ("day".equalsIgnoreCase(s)) return "Có dạy";
        return nz(s);
    }

    protected static String ketQuaLabel(String s) {
        if ("dat".equalsIgnoreCase(s)) return "Đạt";
        if ("truot".equalsIgnoreCase(s)) return "Trượt";
        return s == null ? "Chưa chấm" : s;
    }

    protected static String initialOf(String name) {
        String t = name == null ? "" : name.trim();
        if (t.isEmpty()) return "?";
        String last = t.substring(t.lastIndexOf(' ') + 1);
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase();
    }

    protected void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    /** Trả về chỉ số dòng (theo model) đang chọn; -1 và nhắc người dùng nếu chưa chọn. */
    protected int selectedModelRow(JTable t) {
        int viewRow = t.getSelectedRow();
        if (viewRow < 0) {
            warn("Vui lòng chọn một dòng trong bảng trước!");
            return -1;
        }
        return t.convertRowIndexToModel(viewRow);
    }

    protected static DocumentListener onChange(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }

    @SuppressWarnings("unchecked")
    protected TableRowSorter<DefaultTableModel> sorterOf(JTable t) {
        return (TableRowSorter<DefaultTableModel>) t.getRowSorter();
    }

    /* ===================== TRUY VẤN CSDL ===================== */

    protected void showError(String msg, Exception e) {
        JOptionPane.showMessageDialog(this, msg + "\n" + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /** Đọc nhiều dòng vào model. Luôn đóng Connection/Statement/ResultSet. */
    protected void loadRows(DefaultTableModel model, String sql, RowMapper mapper, String... params) {
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

    /** Giá trị cột đầu tiên của dòng đầu tiên; null nếu không có dòng hoặc lỗi. */
    protected String queryString(String sql, String... params) {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return null;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        } catch (SQLException e) {
            showError("Lỗi truy vấn dữ liệu:", e);
            return null;
        }
    }

    protected long queryLong(String sql, String... params) {
        String s = queryString(sql, params);
        if (s == null) return 0;
        try {
            return new java.math.BigDecimal(s.trim()).longValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** INSERT / UPDATE / DELETE. Trả về số dòng bị ảnh hưởng, hoặc -1 nếu lỗi. */
    protected int execUpdate(String sql, String... params) {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return -1;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            showError("Lỗi cập nhật dữ liệu:", e);
            return -1;
        }
    }

    /* ===================== ICON ===================== */

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
            case GROUP:
                g2.drawOval(3, 4, 6, 6);
                g2.drawOval(11, 4, 6, 6);
                g2.drawArc(1, 12, 10, 8, 0, 180);
                g2.drawArc(9, 12, 10, 8, 0, 180);
                break;
            case STAR:
                Path2D star = new Path2D.Float();
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? 8.0 : 3.6;
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    double px = 10 + r * Math.cos(a), py = 10.5 + r * Math.sin(a);
                    if (i == 0) star.moveTo(px, py);
                    else star.lineTo(px, py);
                }
                star.closePath();
                g2.draw(star);
                break;
            case LIST:
                g2.drawLine(8, 5, 17, 5);
                g2.drawLine(8, 10, 17, 10);
                g2.drawLine(8, 15, 17, 15);
                g2.fillOval(3, 4, 3, 3);
                g2.fillOval(3, 9, 3, 3);
                g2.fillOval(3, 14, 3, 3);
                break;
        }
        g2.dispose();
    }

    /* ===================== THÀNH PHẦN GIAO DIỆN ===================== */

    protected class PrimaryButton extends JButton {
        private final Color color;

        PrimaryButton(String text) {
            this(text, null, 210);
        }

        PrimaryButton(String text, Color color, int width) {
            super(text);
            this.color = color;
            setFont(font(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(width, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = color != null ? color : accent;
            if (!isEnabled()) g2.setColor(new Color(148, 163, 184));
            else if (getModel().isPressed()) g2.setColor(base.darker().darker());
            else if (getModel().isRollover()) g2.setColor(base.darker());
            else g2.setColor(base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    protected static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private Color stripe; // dải màu bên trái (thẻ thống kê)

        RoundedPanel(LayoutManager lm, int radius, Color fill) {
            super(lm);
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        RoundedPanel stripe(Color c) {
            this.stripe = c;
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
            if (stripe != null) {
                Shape oldClip = g2.getClip();
                g2.clip(new RoundRectangle2D.Float(0, 0, w, h - 1, radius, radius));
                g2.setColor(stripe);
                g2.fillRect(0, 0, 5, h);
                g2.setClip(oldClip);
            }
            g2.setColor(C_BORDER);
            g2.drawRoundRect(0, 0, w, h - 1, radius, radius);
            g2.dispose();
        }
    }

    protected class GradientBanner extends JPanel {
        GradientBanner() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, accentDark, getWidth(), getHeight(), accent.brighter()));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(new Color(255, 255, 255, 28));
            g2.fillOval(getWidth() - 150, -60, 220, 220);
            g2.fillOval(getWidth() - 260, getHeight() - 70, 140, 140);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class NavButton extends JButton {
        private boolean active;
        private final IconType icon;

        NavButton(String text, IconType icon) {
            super(text);
            this.icon = icon;
            setFont(font(Font.PLAIN, 14));
            setForeground(C_TEXT);
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
            setForeground(a ? accent : C_TEXT);
            setFont(font(a ? Font.BOLD : Font.PLAIN, 14));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (active) {
                g2.setColor(accentSoft);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent);
                g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
            } else if (getModel().isRollover()) {
                g2.setColor(C_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.dispose();
            drawIcon(g, icon, 16, (getHeight() - 20) / 2, 20, active ? accent : C_MUTED);
            super.paintComponent(g);
        }
    }

    protected static class Badge extends JComponent {
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

    /* ===================== THANH TRÊN + MENU TRÁI ===================== */

    private JPanel createTopBar(String badge, String brand, String userName, String userSub) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        bar.setPreferredSize(new Dimension(0, 68));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(0, 14, 0, 0));
        left.add(new Badge(badge, accent, Color.WHITE, 40));

        JLabel lblLogo = new JLabel(brand);
        lblLogo.setFont(font(Font.BOLD, 20));
        lblLogo.setForeground(C_TEXT);
        left.add(lblLogo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 11));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 14));
        right.add(new Badge(initialOf(userName), accentLight, accent, 42));

        JPanel userDetail = new JPanel(new GridLayout(2, 1));
        userDetail.setOpaque(false);
        JLabel lblUser = new JLabel(userName);
        lblUser.setFont(font(Font.BOLD, 14));
        lblUser.setForeground(C_TEXT);
        JLabel lblSub = new JLabel(userSub);
        lblSub.setFont(font(Font.PLAIN, 12));
        lblSub.setForeground(C_MUTED);
        userDetail.add(lblUser);
        userDetail.add(lblSub);
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
        btnLogout.addActionListener(e -> onLogout());
        right.add(btnLogout);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createSidebar(String sectionLabel, String footerText) {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(250, 0));
        side.setBackground(Color.WHITE);
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        JLabel lblSection = new JLabel(sectionLabel);
        lblSection.setFont(font(Font.BOLD, 11));
        lblSection.setForeground(C_MUTED);
        lblSection.setBorder(new EmptyBorder(24, 24, 12, 10));

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(new EmptyBorder(0, 12, 10, 12));

        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            NavButton button = new NavButton(p.label, p.icon);
            button.addActionListener(e -> {
                for (NavButton b : navButtons) b.setActive(b == button);
                showPage(p.key);
            });
            navButtons.add(button);
            navPanel.add(button);
            if (i < pages.size() - 1) navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        JLabel footer = new JLabel(footerText);
        footer.setFont(font(Font.PLAIN, 11));
        footer.setForeground(C_MUTED);
        footer.setBorder(new EmptyBorder(10, 24, 16, 10));

        side.add(lblSection, BorderLayout.NORTH);
        side.add(navPanel, BorderLayout.CENTER);
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    /* ===================== BỐ CỤC TRANG ===================== */

    protected JPanel pagePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));
        return panel;
    }

    protected JLabel pageTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(font(Font.BOLD, 22));
        title.setForeground(C_TEXT);
        return title;
    }

    protected JPanel createBanner(String title, String subtitle) {
        JPanel banner = new GradientBanner();
        banner.setBorder(new EmptyBorder(22, 26, 22, 26));
        JLabel lbl = new JLabel("<html><span style='font-size:20px'><b>" + esc(title) + "</b></span><br>"
                + "<span style='font-size:12px;color:#E0F2FE'>" + esc(subtitle) + "</span></html>");
        lbl.setFont(font(Font.PLAIN, 14));
        lbl.setForeground(Color.WHITE);
        banner.add(lbl, BorderLayout.WEST);
        return banner;
    }

    protected RoundedPanel createCard(String title) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 16, C_CARD);
        card.setBorder(new EmptyBorder(18, 20, 20, 20));
        if (title != null) {
            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(font(Font.BOLD, 16));
            lblTitle.setForeground(C_TEXT);
            card.add(lblTitle, BorderLayout.NORTH);
        }
        return card;
    }

    protected JPanel createStatCard(String title, String val, String sub, Color color) {
        RoundedPanel card = new RoundedPanel(new GridLayout(3, 1, 0, 2), 14, Color.WHITE).stripe(color);
        card.setBorder(new EmptyBorder(14, 20, 14, 14));

        JLabel t = new JLabel(title);
        t.setFont(font(Font.PLAIN, 12));
        t.setForeground(C_MUTED);

        JLabel v = new JLabel(val);
        v.setFont(font(Font.BOLD, 19));
        v.setForeground(color);

        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 12));
        s.setForeground(C_MUTED);

        card.add(t);
        card.add(v);
        card.add(s);
        return card;
    }

    protected JPanel statsRow(JComponent... cards) {
        JPanel stats = new JPanel(new GridLayout(1, cards.length, 16, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 112));
        for (JComponent c : cards) stats.add(c);
        return stats;
    }

    /** Trang tổng quan: banner + hàng thẻ thống kê + hai thẻ bảng cạnh nhau. */
    protected JPanel overviewLayout(JPanel banner, JPanel stats, JComponent left, JComponent right) {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        centerGrid.setOpaque(false);
        centerGrid.add(left);
        centerGrid.add(right);

        JPanel topStack = new JPanel(new BorderLayout(0, 16));
        topStack.setOpaque(false);
        topStack.add(banner, BorderLayout.NORTH);
        topStack.add(stats, BorderLayout.SOUTH);

        panel.add(topStack, BorderLayout.NORTH);
        panel.add(centerGrid, BorderLayout.CENTER);
        return panel;
    }

    /** Trang hồ sơ: avatar + lưới thông tin {nhãn, giá trị}. */
    protected JPanel createProfilePage(String title, String cardTitle, String name, String subLine, String[][] items) {
        JPanel panel = pagePanel();
        RoundedPanel card = createCard(cardTitle);

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        head.setOpaque(false);
        head.add(new Badge(initialOf(name), accentLight, accent, 72));
        JLabel lblName = new JLabel("<html><span style='font-size:18px'><b>" + esc(name) + "</b></span><br>"
                + "<span style='font-size:12px;color:#64748B'>" + esc(subLine) + "</span></html>");
        lblName.setFont(font(Font.PLAIN, 14));
        head.add(lblName);

        JPanel infoGrid = new JPanel(new GridLayout((items.length + 1) / 2, 2, 18, 14));
        infoGrid.setOpaque(false);
        infoGrid.setBorder(new EmptyBorder(16, 0, 0, 0));
        for (String[] it : items) infoGrid.add(createProfileItem(it[0], it[1]));
        if (items.length % 2 == 1) infoGrid.add(new JPanel() {{ setOpaque(false); }});

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(head, BorderLayout.NORTH);
        body.add(infoGrid, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);

        panel.add(pageTitle(title), BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfileItem(String label, String val) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(4, 0, 8, 0)));
        JLabel lbl = new JLabel(label);
        lbl.setFont(font(Font.PLAIN, 12));
        lbl.setForeground(C_MUTED);
        JLabel v = new JLabel(val == null || val.isEmpty() ? "—" : val);
        v.setFont(font(Font.BOLD, 15));
        v.setForeground(C_TEXT);
        p.add(lbl);
        p.add(v);
        return p;
    }

    /* ===================== BẢNG DỮ LIỆU ===================== */

    protected JTable loadTable(String[] cols, String sql, RowMapper mapper, int statusCol, int scoreCol,
                               String... params) {
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        loadRows(model, sql, mapper, params);
        return createCustomTable(model, statusCol, scoreCol);
    }

    /** Thẻ chứa bảng (tùy chọn: ô tìm kiếm phía trên, vùng nút phía dưới). */
    protected RoundedPanel tableCard(String cardTitle, JTable table, String emptyText,
                                     boolean searchable, JComponent footer) {
        RoundedPanel card = createCard(cardTitle);
        JComponent scroll = createScroll(table, emptyText);
        JComponent body = scroll;
        if (searchable) {
            JPanel center = new JPanel(new BorderLayout(0, 10));
            center.setOpaque(false);
            center.add(createSearchField(table), BorderLayout.NORTH);
            center.add(scroll, BorderLayout.CENTER);
            body = center;
        }
        card.add(body, BorderLayout.CENTER);
        if (footer != null) card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    /** Trang danh sách đơn giản: tiêu đề + một thẻ bảng có ô tìm kiếm. */
    protected JPanel createListPage(String title, String cardTitle, String[] cols, String sql, RowMapper mapper,
                                    int statusCol, int scoreCol, String emptyText, boolean searchable,
                                    String... params) {
        JPanel panel = pagePanel();
        JTable table = loadTable(cols, sql, mapper, statusCol, scoreCol, params);
        panel.add(pageTitle(title), BorderLayout.NORTH);
        panel.add(tableCard(cardTitle, table, emptyText, searchable, null), BorderLayout.CENTER);
        return panel;
    }

    protected JTextField newSearchBox() {
        JTextField tf = new JTextField();
        tf.setFont(font(Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        tf.putClientProperty("JTextField.placeholderText", "Tìm kiếm trong bảng...");
        tf.setToolTipText("Gõ để lọc danh sách");
        return tf;
    }

    protected JTextField createSearchField(JTable table) {
        JTextField tf = newSearchBox();
        TableRowSorter<DefaultTableModel> sorter = sorterOf(table);
        tf.getDocument().addDocumentListener(onChange(() -> {
            String t = tf.getText().trim();
            sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t)));
        }));
        return tf;
    }

    /** Bọc bảng trong scroll; nếu không có dòng nào thì hiện thông báo trống thân thiện. */
    protected JComponent createScroll(JTable table, String emptyText) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        if (table.getModel().getRowCount() > 0) return sp;

        JLabel empty = new JLabel(emptyText, SwingConstants.CENTER);
        empty.setFont(font(Font.PLAIN, 14));
        empty.setForeground(C_MUTED);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        wrap.add(table.getTableHeader(), BorderLayout.NORTH);
        wrap.add(empty, BorderLayout.CENTER);
        return wrap;
    }

    /** statusCol: cột trạng thái tô màu; scoreCol: cột điểm tô xanh/đỏ (-1 = không dùng). */
    protected JTable createCustomTable(DefaultTableModel model, int statusCol, int scoreCol) {
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
        table.setSelectionBackground(C_SELECT);
        table.setSelectionForeground(C_TEXT);
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
            setForeground(C_TEXT);
            setHorizontalAlignment(SwingConstants.LEFT);
            if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : C_STRIPE);

            String s = v == null ? "" : v.toString();
            if (col == statusCol && !s.isEmpty()) {
                String low = s.toLowerCase();
                Color c;
                if (low.contains("hủy") || low.contains("huỷ") || low.contains("nghỉ")
                        || low.contains("chưa") || low.contains("trượt")) c = C_DANGER;
                else if (low.contains("hoàn") || low.contains("đã") || low.contains("xong")
                        || low.contains("đạt") || low.contains("có dạy")) c = C_SUCCESS_DARK;
                else c = new Color(37, 99, 235);
                setFont(font(Font.BOLD, 12));
                setForeground(c);
                setText("●  " + s);
            } else if (col == scoreCol) {
                try {
                    int n = Integer.parseInt(s.trim());
                    setFont(font(Font.BOLD, 13));
                    setForeground(n >= 80 ? C_SUCCESS_DARK : C_DANGER);
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
            setForeground(C_MUTED);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                    new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }
}
