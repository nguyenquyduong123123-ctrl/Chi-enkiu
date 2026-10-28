

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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Giao diện quản lý trung tâm đào tạo - phong cách S-Link
 * (header trắng, sidebar trắng, tông đỏ, thẻ lớp học màu hồng).
 */
public class MainDashboardUI extends JFrame {

    // ===== Thông tin người dùng đăng nhập (đổi tại đây) =====
    private static final String USER_NAME = "Trần Trung Hiếu";

    // ===== Bảng màu =====
    private static final Color COLOR_PRIMARY = new Color(229, 28, 35);       // Đỏ chủ đạo
    private static final Color COLOR_PINK = new Color(254, 226, 226);        // Nền thẻ lớp
    private static final Color COLOR_BG = new Color(244, 244, 245);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(229, 231, 235);
    private static final Color COLOR_TEXT_MAIN = new Color(31, 41, 55);
    private static final Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    private static final Color COLOR_NAV_ICON = new Color(75, 85, 99);
    private static final Color COLOR_HOVER = new Color(247, 247, 247);
    private static final Color COLOR_ACTIVE_BG = new Color(240, 240, 241);
    private static final Color COLOR_SELECT = new Color(254, 226, 226);
    private static final Color COLOR_ZEBRA = new Color(250, 250, 250);
    private static final Color COLOR_BLUE = new Color(22, 119, 255);
    private static final Color COLOR_GREEN = new Color(82, 196, 26);
    private static final Color COLOR_TEAL = new Color(94, 200, 210);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebar;
    private JLabel lblSection;
    private boolean sidebarCollapsed = false;
    private final List<NavButton> navButtons = new ArrayList<>();

    public MainDashboardUI() {
        setTitle("HỆ THỐNG QUẢN LÝ TRUNG TÂM ĐÀO TẠO");
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
        mainContentPanel.add(createHocVienPanel(), "HocVien");
        mainContentPanel.add(createLopHocPanel(), "LopHoc");
        mainContentPanel.add(createNhanSuPanel(), "NhanSu");
        mainContentPanel.add(createTaiChinhPanel(), "TaiChinh");

        add(mainContentPanel, BorderLayout.CENTER);
    }

    private static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    // =====================================================================
    //  ICON VẼ BẰNG VECTOR (không phụ thuộc font emoji)
    // =====================================================================
    private enum IconType { HOME, USERS, BOOK, TEACHER, WALLET, BELL, GRID, CALENDAR, CHAT, COPY }

    private static void drawIcon(Graphics g, IconType type, int x, int y, int size, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        double s = size / 20.0;
        g2.scale(s, s);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case HOME: {
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
                g2.drawRoundRect(8, 11, 4, 5, 1, 1);
                break;
            }
            case USERS:
                g2.drawOval(7, 3, 6, 6);
                g2.drawArc(3, 12, 14, 12, 0, 180);
                break;
            case BOOK:
                g2.drawRoundRect(3, 4, 14, 12, 3, 3);
                g2.drawLine(10, 5, 10, 15);
                g2.drawLine(5, 8, 8, 8);
                g2.drawLine(12, 8, 15, 8);
                break;
            case TEACHER:
                g2.drawRoundRect(2, 3, 16, 10, 2, 2);
                g2.drawLine(10, 13, 10, 17);
                g2.drawLine(6, 17, 14, 17);
                break;
            case WALLET:
                g2.drawRoundRect(2, 5, 16, 11, 4, 4);
                g2.drawOval(7, 7, 6, 6);
                break;
            case BELL: {
                Path2D p = new Path2D.Float();
                p.moveTo(4, 14);
                p.curveTo(6, 12, 6, 10, 6, 8);
                p.curveTo(6, 4, 14, 4, 14, 8);
                p.curveTo(14, 10, 14, 12, 16, 14);
                p.closePath();
                g2.draw(p);
                g2.drawLine(8, 17, 12, 17);
                break;
            }
            case GRID:
                g2.drawRoundRect(3, 3, 5, 5, 2, 2);
                g2.drawRoundRect(12, 3, 5, 5, 2, 2);
                g2.drawRoundRect(3, 12, 5, 5, 2, 2);
                g2.drawRoundRect(12, 12, 5, 5, 2, 2);
                break;
            case CALENDAR:
                g2.drawRoundRect(3, 4, 14, 13, 3, 3);
                g2.drawLine(3, 8, 17, 8);
                g2.drawLine(7, 2, 7, 5);
                g2.drawLine(13, 2, 13, 5);
                break;
            case CHAT:
                g2.drawOval(3, 3, 14, 12);
                g2.fillOval(6, 8, 2, 2);
                g2.fillOval(9, 8, 2, 2);
                g2.fillOval(12, 8, 2, 2);
                break;
            case COPY:
                g2.drawRoundRect(4, 3, 9, 10, 2, 2);
                g2.drawRoundRect(8, 7, 8, 10, 2, 2);
                break;
            default:
                break;
        }
        g2.dispose();
    }

    private static class VIcon implements Icon {
        private final IconType type;
        private final int size;
        private final Color color;

        VIcon(IconType type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) { drawIcon(g, type, x, y, size, color); }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // =====================================================================
    //  CÁC COMPONENT TỰ VẼ
    // =====================================================================

    /** Panel bo góc, viền mảnh, bóng nhẹ phía dưới. */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private final boolean shadow;

        RoundedPanel(LayoutManager lm, int radius, Color fill, boolean shadow) {
            super(lm);
            this.radius = radius;
            this.fill = fill;
            this.shadow = shadow;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (shadow) {
                g2.setColor(new Color(15, 23, 42, 12));
                g2.fillRoundRect(0, 2, w - 1, h - 2, radius, radius);
            }
            int bh = shadow ? h - 3 : h - 1;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, bh, radius, radius);
            g2.setColor(COLOR_BORDER);
            g2.drawRoundRect(0, 0, w - 1, bh, radius, radius);
            g2.dispose();
        }
    }

    /** Nút bo góc, hiệu ứng hover / nhấn, có thể kèm icon. */
    private static class RoundedButton extends JButton {
        private final Color base;
        private final int arc;

        RoundedButton(String text, Color base) {
            this(text, base, null, 12);
        }

        RoundedButton(String text, Color base, IconType icon, int arc) {
            super(text);
            this.base = base;
            this.arc = arc;
            setFont(font(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(125, 38));
            if (icon != null) {
                setIcon(new VIcon(icon, 16, Color.WHITE));
                setIconTextGap(8);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = base;
            if (getModel().isPressed()) c = base.darker().darker();
            else if (getModel().isRollover()) c = base.darker();
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Nút điều hướng sidebar: icon + chữ, mục đang chọn chữ đỏ nền xám nhạt. */
    private static class NavButton extends JButton {
        private boolean active;
        private boolean collapsed;
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
            setRolloverEnabled(true);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 46, 0, 0));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setPreferredSize(new Dimension(200, 46));
        }

        void setActive(boolean a) {
            active = a;
            setForeground(a ? COLOR_PRIMARY : COLOR_TEXT_MAIN);
            setFont(font(a ? Font.BOLD : Font.PLAIN, 14));
            repaint();
        }

        void setCollapsed(boolean c) {
            collapsed = c;
            setText(c ? "" : fullText);
            setToolTipText(c ? fullText : null);
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
            int ix = collapsed ? (getWidth() - 20) / 2 : 16;
            drawIcon(g, icon, ix, (getHeight() - 20) / 2, 20, active ? COLOR_PRIMARY : COLOR_NAV_ICON);
            super.paintComponent(g);
        }
    }

    /** Nút tròn nhỏ (nút thu gọn sidebar). */
    private static class CircleButton extends JButton {
        CircleButton(String text) {
            super(text);
            setFont(font(Font.BOLD, 18));
            setForeground(COLOR_TEXT_MUTED);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setMargin(new Insets(0, 0, 2, 0));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? COLOR_HOVER : Color.WHITE);
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setColor(COLOR_BORDER);
            g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Nút vuông có icon (thanh trên cùng), tuỳ chọn huy hiệu số đỏ. */
    private static class IconButton extends JButton {
        private final IconType type;
        private final String badge;

        IconButton(IconType type, String badge) {
            this.type = type;
            this.badge = badge;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(badge != null ? 62 : 40, 46));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? COLOR_HOVER : Color.WHITE);
            g2.fillRoundRect(0, 6, 40, 40, 12, 12);
            g2.setColor(new Color(235, 235, 237));
            g2.drawRoundRect(0, 6, 39, 39, 12, 12);
            drawIcon(g2, type, 10, 16, 20, COLOR_NAV_ICON);
            if (badge != null) {
                g2.setFont(font(Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int pw = fm.stringWidth(badge) + 12;
                int px = 40 - pw / 2;
                g2.setColor(COLOR_PRIMARY);
                g2.fillRoundRect(px, 0, pw, 18, 18, 18);
                g2.setColor(Color.WHITE);
                g2.drawString(badge, px + 6, (18 - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    /** Ô nhập liệu bo góc, đổi màu viền khi focus, có gợi ý (placeholder). */
    private static class RoundedTextField extends JTextField {
        private final String hint;

        RoundedTextField(String hint) {
            this.hint = hint;
            setOpaque(false);
            setFont(font(Font.PLAIN, 13));
            setForeground(COLOR_TEXT_MAIN);
            setCaretColor(COLOR_PRIMARY);
            setBorder(new EmptyBorder(6, 12, 6, 12));
            setPreferredSize(new Dimension(0, 38));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();

            super.paintComponent(g);

            Graphics2D g3 = (Graphics2D) g.create();
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getText().isEmpty() && hint != null && !hint.isEmpty()) {
                g3.setColor(new Color(156, 163, 175));
                g3.setFont(getFont());
                FontMetrics fm = g3.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g3.drawString(hint, getInsets().left, y);
            }
            boolean focus = isFocusOwner();
            g3.setColor(focus ? COLOR_PRIMARY : new Color(209, 213, 219));
            g3.setStroke(new BasicStroke(focus ? 1.6f : 1f));
            g3.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g3.dispose();
        }
    }

    /** Hình tròn / bo góc chứa chữ (logo, avatar). */
    private static class Badge extends JComponent {
        private final String text;
        private final Color bg, fg;
        private final Font f;
        private final int arc;

        Badge(String text, Color bg, Color fg, Font f, int size, int arc) {
            this.text = text;
            this.bg = bg;
            this.fg = fg;
            this.f = f;
            this.arc = arc;
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setColor(fg);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

    /** Hình tròn / bo góc chứa icon vector. */
    private static class IconBadge extends JComponent {
        private final IconType type;
        private final Color bg, fg;
        private final int size, arc;

        IconBadge(IconType type, Color bg, Color fg, int size, int arc) {
            this.type = type;
            this.bg = bg;
            this.fg = fg;
            this.size = size;
            this.arc = arc;
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            int is = size / 2;
            drawIcon(g, type, (getWidth() - is) / 2, (getHeight() - is) / 2, is, fg);
        }
    }

    /** Nhãn dạng "viên thuốc" (trạng thái, tag). */
    private static class Pill extends JLabel {
        private final Color bg;

        Pill(String text) {
            this(text, pillBg(text), pillFg(text));
        }

        Pill(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            setForeground(fg);
            setFont(font(Font.BOLD, 12));
            setBorder(new EmptyBorder(3, 12, 3, 12));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static Color pillBg(String t) {
        if (t.contains("Đã")) return new Color(220, 252, 231);
        if (t.contains("Chưa")) return new Color(254, 243, 199);
        return new Color(229, 231, 235);
    }

    private static Color pillFg(String t) {
        if (t.contains("Đã")) return new Color(22, 101, 52);
        if (t.contains("Chưa")) return new Color(146, 64, 14);
        return new Color(55, 65, 81);
    }

    /** Biểu đồ cột vẽ bằng Graphics2D. */
    private static class BarChart extends JComponent {
        private final String[] labels;
        private final double[] values;

        BarChart(String[] labels, double[] values) {
            this.labels = labels;
            this.values = values;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int top = 24, bottom = 26;
            double max = 0;
            for (double v : values) max = Math.max(max, v);
            int n = values.length;
            int slot = w / n;
            int barW = Math.min(36, (int) (slot * 0.55));
            g2.setFont(font(Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();

            g2.setColor(new Color(243, 244, 246));
            g2.drawLine(0, h - bottom, w, h - bottom);

            for (int i = 0; i < n; i++) {
                int bh = (int) ((h - top - bottom) * values[i] / max);
                int x = i * slot + (slot - barW) / 2;
                int y = h - bottom - bh;
                boolean last = i == n - 1;
                g2.setColor(last ? COLOR_PRIMARY : new Color(254, 202, 202));
                g2.fillRoundRect(x, y, barW, bh, 10, 10);
                g2.fillRect(x, y + bh / 2, barW, bh - bh / 2);

                String v = String.valueOf((int) values[i]);
                g2.setColor(last ? COLOR_TEXT_MAIN : COLOR_TEXT_MUTED);
                g2.drawString(v, x + (barW - fm.stringWidth(v)) / 2, y - 6);
                g2.setColor(COLOR_TEXT_MUTED);
                g2.drawString(labels[i], x + (barW - fm.stringWidth(labels[i])) / 2, h - 8);
            }
            g2.dispose();
        }
    }

    /** Thanh tiến trình xanh dương. */
    private static class ProgressLine extends JComponent {
        private final int percent;

        ProgressLine(int percent) {
            this.percent = percent;
            setPreferredSize(new Dimension(100, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = (getHeight() - 6) / 2;
            g2.setColor(new Color(229, 231, 235));
            g2.fillRoundRect(0, y, getWidth(), 6, 6, 6);
            g2.setColor(COLOR_BLUE);
            g2.fillRoundRect(0, y, getWidth() * percent / 100, 6, 6, 6);
            g2.dispose();
        }
    }

    /** Đường kẻ đứt nét. */
    private static class DashedLine extends JComponent {
        DashedLine() {
            setPreferredSize(new Dimension(100, 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(248, 180, 180));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[]{3f, 3f}, 0f));
            g2.drawLine(0, 0, getWidth(), 0);
            g2.dispose();
        }
    }

    /** Banner chào mừng nền hồng có sọc chéo. */
    private static class BannerPanel extends JPanel {
        private final String line1, line2;

        BannerPanel(String line1, String line2) {
            this.line1 = line1;
            this.line2 = line2;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, 18, 18);
            g2.setPaint(new GradientPaint(0, 0, new Color(254, 205, 211), w, 0, new Color(255, 238, 238)));
            g2.fill(shape);
            g2.setClip(shape);

            g2.setColor(new Color(229, 28, 35, 40));
            g2.fillPolygon(new int[]{0, 80, 200, 120}, new int[]{h, h, 0, 0}, 4);
            g2.setColor(new Color(229, 28, 35, 22));
            g2.fillPolygon(new int[]{95, 130, 250, 215}, new int[]{h, h, 0, 0}, 4);

            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 54));
            g2.setColor(COLOR_PRIMARY);
            g2.drawString("\u270C", 215, h / 2 + 18);

            g2.setFont(font(Font.BOLD, 27));
            g2.setColor(COLOR_PRIMARY);
            g2.drawString(line1, 300, h / 2 - 6);
            g2.drawString(line2, 300, h / 2 + 30);
            g2.dispose();
        }
    }

    /** Thẻ thông báo / sự kiện: có mũi tên chuyển mục, ngày và nhãn. */
    private static class NotifyCard extends RoundedPanel {
        private int idx = 0;
        private final String[] msgs;
        private final String[] dates;
        private final JLabel lblMsg = new JLabel();
        private final JLabel lblDate = new JLabel();

        NotifyCard(String title, String[] msgs, String[] dates, String badge, Color badgeColor) {
            super(new BorderLayout(0, 6), 16, COLOR_CARD, true);
            this.msgs = msgs;
            this.dates = dates;
            setBorder(new EmptyBorder(14, 18, 16, 18));

            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(font(Font.BOLD, 16));
            lblTitle.setForeground(COLOR_TEXT_MAIN);
            JPanel arrows = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
            arrows.setOpaque(false);
            arrows.add(createArrow("\u2039", () -> step(-1)));
            arrows.add(createArrow("\u203A", () -> step(1)));
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.add(lblTitle, BorderLayout.WEST);
            head.add(arrows, BorderLayout.EAST);

            lblMsg.setFont(font(Font.PLAIN, 14));
            lblMsg.setForeground(COLOR_TEXT_MAIN);
            lblMsg.setVerticalAlignment(SwingConstants.TOP);
            lblDate.setFont(font(Font.PLAIN, 13));
            lblDate.setForeground(COLOR_TEXT_MUTED);

            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            foot.add(lblDate, BorderLayout.WEST);
            foot.add(new Pill(badge, badgeColor, Color.WHITE), BorderLayout.EAST);

            add(head, BorderLayout.NORTH);
            add(lblMsg, BorderLayout.CENTER);
            add(foot, BorderLayout.SOUTH);
            refresh();
        }

        private void step(int d) {
            idx = (idx + d + msgs.length) % msgs.length;
            refresh();
        }

        private void refresh() {
            lblMsg.setText("<html><body style='width:170px'>" + msgs[idx] + "</body></html>");
            lblDate.setText(dates[idx]);
        }
    }

    private static JLabel createArrow(String s, Runnable action) {
        JLabel l = new JLabel(s);
        l.setFont(font(Font.BOLD, 20));
        l.setForeground(COLOR_TEXT_MAIN);
        l.setCursor(new Cursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
        });
        return l;
    }

    /** Thẻ lớp học màu hồng, có tag góc phải trên. */
    private static class CourseCard extends JPanel {
        private final String tag;
        private final Color tagColor;

        CourseCard(String tag, Color tagColor) {
            super(new GridBagLayout());
            this.tag = tag;
            this.tagColor = tagColor;
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, COLOR_PINK, getWidth(), getHeight(), new Color(254, 236, 236)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
            g2.setFont(font(Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(tag) + 24;
            g2.setColor(tagColor);
            g2.fillRoundRect(getWidth() - tw, 14, tw + 20, 26, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(tag, getWidth() - tw + 12, 14 + (26 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Panel cuộn được, luôn co giãn theo chiều rộng khung nhìn. */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        ScrollablePanel(LayoutManager lm) {
            super(lm);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(24, r.height - 40); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // =====================================================================
    //  THANH TRÊN CÙNG
    // =====================================================================
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        bar.setPreferredSize(new Dimension(0, 72));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(0, 14, 0, 0));
        left.add(new Badge("C", COLOR_PRIMARY, Color.WHITE, font(Font.BOLD, 20), 40, 40));
        JLabel lblLogo = new JLabel("Center Admin");
        lblLogo.setFont(font(Font.BOLD, 21));
        lblLogo.setForeground(COLOR_TEXT_MAIN);
        left.add(lblLogo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 13));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 12));
        right.add(new IconButton(IconType.GRID, null));
        right.add(new IconButton(IconType.BELL, "99+"));
        String first = USER_NAME.substring(USER_NAME.lastIndexOf(' ') + 1);
        right.add(new Badge(first.substring(0, 1).toUpperCase(), new Color(156, 163, 175), Color.WHITE,
                font(Font.BOLD, 20), 46, 46));
        JLabel lblUser = new JLabel(USER_NAME);
        lblUser.setFont(font(Font.PLAIN, 15));
        lblUser.setForeground(COLOR_TEXT_MAIN);
        right.add(lblUser);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // =====================================================================
    //  1. SIDEBAR
    // =====================================================================
    private JPanel createSidebar() {
        sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));

        lblSection = new JLabel("DASHBOARD & QUẢN LÝ");
        lblSection.setFont(font(Font.BOLD, 12));
        lblSection.setForeground(new Color(156, 163, 175));
        lblSection.setBorder(new EmptyBorder(26, 26, 12, 10));

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(new EmptyBorder(0, 12, 10, 12));

        NavButton btnDash = createNavButton("Trang chủ", IconType.HOME, "Dashboard");
        NavButton btnHV = createNavButton("Quản lý học viên", IconType.USERS, "HocVien");
        NavButton btnLop = createNavButton("Khóa học & lớp", IconType.BOOK, "LopHoc");
        NavButton btnNS = createNavButton("Giáo viên & nhân viên", IconType.TEACHER, "NhanSu");
        NavButton btnTC = createNavButton("Học phí & lương", IconType.WALLET, "TaiChinh");

        NavButton[] all = {btnDash, btnHV, btnLop, btnNS, btnTC};
        for (int i = 0; i < all.length; i++) {
            navPanel.add(all[i]);
            if (i < all.length - 1) navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        CircleButton toggle = new CircleButton("\u2039");
        toggle.addActionListener(e -> toggleSidebar(toggle));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(8, 14, 16, 14));
        bottom.add(toggle, BorderLayout.WEST);

        sidebar.add(lblSection, BorderLayout.NORTH);
        sidebar.add(navPanel, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);

        setNavActive(btnDash);
        return sidebar;
    }

    private void toggleSidebar(JButton toggle) {
        sidebarCollapsed = !sidebarCollapsed;
        sidebar.setPreferredSize(new Dimension(sidebarCollapsed ? 84 : 260, 0));
        lblSection.setVisible(!sidebarCollapsed);
        for (NavButton b : navButtons) b.setCollapsed(sidebarCollapsed);
        toggle.setText(sidebarCollapsed ? "\u203A" : "\u2039");
        getContentPane().revalidate();
        getContentPane().repaint();
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
    //  2. TRANG TỔNG QUAN
    // =====================================================================
    private JPanel createDashboardOverviewPanel() {
        ScrollablePanel content = new ScrollablePanel(new BorderLayout());
        content.setBackground(COLOR_BG);
        content.setBorder(new EmptyBorder(0, 28, 30, 28));

        JPanel stack = new JPanel(new GridBagLayout());
        stack.setOpaque(false);

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String first = USER_NAME.substring(USER_NAME.lastIndexOf(' ') + 1);

        // --- Hàng 1: banner + thông báo + sự kiện ---
        JPanel topRow = new JPanel(new GridBagLayout());
        topRow.setOpaque(false);
        topRow.setPreferredSize(new Dimension(10, 140));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;

        g.gridx = 0;
        g.weightx = 0.50;
        g.insets = new Insets(0, 0, 0, 16);
        topRow.add(new BannerPanel("Xin chào " + first, "Mừng bạn quay lại"), g);

        g.gridx = 1;
        g.weightx = 0.25;
        topRow.add(new NotifyCard("Thông báo",
                new String[]{"Lớp Tiếng Nhật N5 sắp khai giảng", "Hạn nộp học phí đợt 2 là 05/11",
                        "Có 3 học viên đăng ký mới hôm nay"},
                new String[]{today, today, today}, "Mới", COLOR_PRIMARY), g);

        g.gridx = 2;
        g.weightx = 0.25;
        g.insets = new Insets(0, 0, 0, 0);
        topRow.add(new NotifyCard("Sự kiện",
                new String[]{"Khai giảng lớp IELTS tháng 11", "Buổi thi thử JLPT N4", "Họp giáo viên đầu tháng"},
                new String[]{today, "02/11/2026", "05/11/2026"}, "Lịch học", COLOR_TEAL), g);

        addRow(stack, createPageTopSpacer(), 0, 0);
        addRow(stack, topRow, 1, 30);

        // --- Hàng 2: lớp đang mở ---
        addRow(stack, createClassSection(), 2, 30);

        // --- Hàng 3: thống kê ---
        addRow(stack, createSectionTitle("Thống kê tháng này"), 3, 14);
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 18, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.add(createStatCard("Tổng học viên", "1,250", "+48 trong tháng này", IconType.USERS, new Color(37, 99, 235)));
        cardsGrid.add(createStatCard("Lớp đang mở", "32", "+3 lớp mới", IconType.BOOK, new Color(16, 185, 129)));
        cardsGrid.add(createStatCard("Giáo viên / nhân viên", "45", "Không đổi", IconType.TEACHER, new Color(245, 158, 11)));
        cardsGrid.add(createStatCard("Doanh thu tháng", "450.000.000 đ", "+12% so với tháng trước", IconType.WALLET, COLOR_PRIMARY));
        addRow(stack, cardsGrid, 4, 30);

        // --- Hàng 4: bảng đăng ký + biểu đồ ---
        JPanel recentCard = createCard("Đăng ký mới gần đây");
        String[] cols = {"Thời gian", "Học viên", "Khóa học", "Số tiền", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        model.addRow(new Object[]{"10:30 hôm nay", "Nguyễn Văn A", "Tiếng Nhật N5", "3.500.000 đ", "Đã nộp"});
        model.addRow(new Object[]{"09:15 hôm nay", "Trần Thị B", "Tiếng Nhật N4", "4.200.000 đ", "Đã nộp"});
        model.addRow(new Object[]{"08:40 hôm nay", "Lê Minh C", "Tiếng Anh IELTS", "6.800.000 đ", "Chưa nộp"});
        model.addRow(new Object[]{"Hôm qua", "Phạm Thu D", "Tiếng Hàn sơ cấp", "3.900.000 đ", "Đã nộp"});
        recentCard.add(createScroll(createCustomTable(model)), BorderLayout.CENTER);

        JPanel chartCard = createCard("Doanh thu 6 tháng gần nhất (triệu đồng)");
        chartCard.add(new BarChart(
                new String[]{"T5", "T6", "T7", "T8", "T9", "T10"},
                new double[]{320, 365, 340, 410, 400, 450}), BorderLayout.CENTER);

        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);
        split.setPreferredSize(new Dimension(10, 320));
        GridBagConstraints sg = new GridBagConstraints();
        sg.fill = GridBagConstraints.BOTH;
        sg.weighty = 1;
        sg.gridx = 0;
        sg.weightx = 0.62;
        sg.insets = new Insets(0, 0, 0, 9);
        split.add(recentCard, sg);
        sg.gridx = 1;
        sg.weightx = 0.38;
        sg.insets = new Insets(0, 9, 0, 0);
        split.add(chartCard, sg);
        addRow(stack, split, 5, 0);

        content.add(stack, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(COLOR_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(COLOR_BG);
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    private Component createPageTopSpacer() {
        return Box.createRigidArea(new Dimension(1, 24));
    }

    private void addRow(JPanel stack, Component c, int y, int bottomGap) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, bottomGap, 0);
        stack.add(c, g);
    }

    private JLabel createSectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 18));
        l.setForeground(COLOR_TEXT_MAIN);
        return l;
    }

    // ----- Khu vực "Lớp đang mở" -----
    private JPanel createClassSection() {
        JPanel section = new JPanel(new BorderLayout(0, 14));
        section.setOpaque(false);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(createSectionTitle("Lớp đang mở"), BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);

        RoundedButton btnQR = new RoundedButton("Điểm danh QR", COLOR_PRIMARY, IconType.GRID, 8);
        btnQR.setPreferredSize(new Dimension(150, 34));
        btnQR.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Chức năng điểm danh QR đang được phát triển.", "Điểm danh QR", JOptionPane.INFORMATION_MESSAGE));

        JLabel semester = new JLabel("HK1 26-27  \u25BE");
        semester.setFont(font(Font.PLAIN, 14));
        semester.setForeground(COLOR_TEXT_MAIN);

        JLabel viewAll = new JLabel("Xem tất cả");
        viewAll.setFont(font(Font.PLAIN, 14));
        viewAll.setForeground(COLOR_PRIMARY);
        viewAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewAll.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                setNavActive(navButtons.get(2));
                cardLayout.show(mainContentPanel, "LopHoc");
            }
        });

        right.add(btnQR);
        right.add(createArrow("\u2039", () -> { }));
        right.add(createArrow("\u203A", () -> { }));
        right.add(semester);
        right.add(viewAll);
        head.add(right, BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(1, 3, 18, 0));
        cards.setOpaque(false);
        cards.add(createCourseCard("03", "JP-N5", "Tiếng Nhật N5", "Hiện tại", COLOR_GREEN,
                new String[]{"24 / 29", "12 / 24", "3"}, "13:00 04/11", "12 / 24 buổi", 50, "GV: Nguyễn Thị Lan"));
        cards.add(createCourseCard("04", "JP-N4", "Tiếng Nhật N4", "Sắp tới", COLOR_PRIMARY,
                new String[]{"0 / 30", "0 / 24", "0"}, "18:00 28/10", "0 / 24 buổi", 0, "GV: Trần Văn Minh"));
        cards.add(createCourseCard("07", "IELTS-F", "IELTS Foundation", "Hiện tại", COLOR_GREEN,
                new String[]{"22 / 25", "19 / 25", "5"}, "13:00 29/10", "19 / 25 buổi", 76, "GV: Lê Hoài Anh"));

        section.add(head, BorderLayout.NORTH);
        section.add(cards, BorderLayout.CENTER);
        return section;
    }

    private JPanel createCourseCard(String order, String code, String name, String tag, Color tagColor,
                                    String[] stats, String next, String progressText, int percent,
                                    String teacher) {
        CourseCard card = new CourseCard(tag, tagColor);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTH;

        // Dòng mã lớp
        JPanel codeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        codeRow.setOpaque(false);
        JLabel lblOrder = new JLabel(order, new VIcon(IconType.COPY, 14, COLOR_PRIMARY), SwingConstants.LEFT);
        lblOrder.setHorizontalTextPosition(SwingConstants.LEFT);
        lblOrder.setIconTextGap(5);
        lblOrder.setFont(font(Font.PLAIN, 13));
        lblOrder.setForeground(COLOR_PRIMARY);
        JLabel lblCode = new JLabel("\u2022  " + code);
        lblCode.setFont(font(Font.PLAIN, 13));
        lblCode.setForeground(COLOR_PRIMARY);
        codeRow.add(lblOrder);
        codeRow.add(lblCode);
        g.gridy = 0;
        g.insets = new Insets(0, -6, 0, 0);
        card.add(codeRow, g);

        // Tên lớp
        JLabel lblName = new JLabel(name);
        lblName.setFont(font(Font.BOLD, 21));
        lblName.setForeground(COLOR_TEXT_MAIN);
        g.gridy = 1;
        g.insets = new Insets(4, 0, 0, 0);
        card.add(lblName, g);

        // 3 ô thống kê
        JPanel statRow = new JPanel(new GridLayout(1, 3, 8, 0));
        statRow.setOpaque(false);
        statRow.add(createStatBox(IconType.USERS, stats[0], "Sĩ số"));
        statRow.add(createStatBox(IconType.BOOK, stats[1], "Số buổi đã học"));
        statRow.add(createStatBox(IconType.CHAT, stats[2], "Thảo luận"));
        g.gridy = 2;
        g.insets = new Insets(12, 0, 0, 0);
        card.add(statRow, g);

        g.gridy = 3;
        g.insets = new Insets(14, 0, 0, 0);
        card.add(createInfoRow("Tiếp theo:", next), g);

        g.gridy = 4;
        g.insets = new Insets(8, 0, 0, 0);
        card.add(createInfoRow("Tiến trình:", progressText), g);

        JPanel progRow = new JPanel(new BorderLayout(10, 0));
        progRow.setOpaque(false);
        JLabel lblPercent = new JLabel(percent + "%");
        lblPercent.setFont(font(Font.PLAIN, 12));
        lblPercent.setForeground(COLOR_TEXT_MUTED);
        progRow.add(new ProgressLine(percent), BorderLayout.CENTER);
        progRow.add(lblPercent, BorderLayout.EAST);
        g.gridy = 5;
        g.insets = new Insets(6, 0, 0, 0);
        card.add(progRow, g);

        g.gridy = 6;
        g.insets = new Insets(12, 0, 0, 0);
        card.add(new DashedLine(), g);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel lblTeacher = new JLabel(teacher);
        lblTeacher.setFont(font(Font.PLAIN, 14));
        lblTeacher.setForeground(COLOR_TEXT_MAIN);
        bottom.add(lblTeacher, BorderLayout.WEST);
        bottom.add(new Badge("GV", Color.WHITE, COLOR_TEXT_MUTED, font(Font.BOLD, 11), 30, 30), BorderLayout.EAST);
        g.gridy = 7;
        g.weighty = 1;
        g.insets = new Insets(10, 0, 0, 0);
        card.add(bottom, g);
        return card;
    }

    private JPanel createStatBox(IconType icon, String text, String tip) {
        RoundedPanel box = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 8, 10), 8, Color.WHITE, false);
        JLabel l = new JLabel(text, new VIcon(icon, 18, COLOR_TEXT_MUTED), SwingConstants.LEFT);
        l.setIconTextGap(8);
        l.setFont(font(Font.PLAIN, 14));
        l.setForeground(COLOR_TEXT_MAIN);
        box.add(l);
        box.setToolTipText(tip);
        return box;
    }

    private JPanel createInfoRow(String left, String right) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel l = new JLabel(left);
        l.setFont(font(Font.PLAIN, 14));
        l.setForeground(COLOR_TEXT_MUTED);
        JLabel r = new JLabel(right);
        r.setFont(font(Font.PLAIN, 14));
        r.setForeground(COLOR_TEXT_MAIN);
        row.add(l, BorderLayout.WEST);
        row.add(r, BorderLayout.EAST);
        return row;
    }

    private JPanel createStatCard(String title, String value, String trend, IconType icon, Color accent) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(14, 0), 16, COLOR_CARD, true);
        card.setBorder(new EmptyBorder(18, 18, 20, 18));

        Color tint = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30);
        IconBadge iconBadge = new IconBadge(icon, tint, accent, 52, 52);
        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(iconBadge, BorderLayout.NORTH);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(font(Font.PLAIN, 13));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(font(Font.BOLD, 21));
        lblVal.setForeground(COLOR_TEXT_MAIN);

        JLabel lblTrend = new JLabel(trend);
        lblTrend.setFont(font(Font.PLAIN, 12));
        lblTrend.setForeground(trend.startsWith("+") ? new Color(22, 163, 74) : COLOR_TEXT_MUTED);

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 2));
        text.setOpaque(false);
        text.add(lblTitle);
        text.add(lblVal);
        text.add(lblTrend);

        card.add(iconWrap, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    // =====================================================================
    //  3. TRANG QUẢN LÝ HỌC VIÊN
    // =====================================================================
    private JPanel createHocVienPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 22));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(28, 30, 28, 30));

        JPanel north = new JPanel(new BorderLayout(0, 22));
        north.setOpaque(false);
        north.add(createPageHeader("Quản lý học viên", "Thêm, sửa, xóa và tra cứu hồ sơ học viên"),
                BorderLayout.NORTH);

        // --- Form ---
        JPanel formCard = createCard("Thông tin học viên");
        RoundedTextField txtMaHV = new RoundedTextField("VD: HV003");
        RoundedTextField txtHoTen = new RoundedTextField("Nguyễn Văn A");
        RoundedTextField txtSDT = new RoundedTextField("09xxxxxxxx");
        RoundedTextField txtEmail = new RoundedTextField("email@example.com");

        JPanel formGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        formGrid.setOpaque(false);
        formGrid.add(createFieldGroup("Mã học viên", txtMaHV));
        formGrid.add(createFieldGroup("Họ và tên", txtHoTen));
        formGrid.add(createFieldGroup("Số điện thoại", txtSDT));
        formGrid.add(createFieldGroup("Email", txtEmail));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        RoundedButton btnAdd = new RoundedButton("Thêm mới", COLOR_PRIMARY);
        RoundedButton btnUpdate = new RoundedButton("Cập nhật", new Color(13, 148, 136));
        RoundedButton btnDelete = new RoundedButton("Xóa", new Color(51, 65, 85));
        RoundedButton btnClear = new RoundedButton("Làm mới", new Color(148, 163, 184));
        btnPanel.add(btnClear);
        btnPanel.add(btnDelete);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnAdd);

        JPanel formBody = new JPanel(new BorderLayout(0, 16));
        formBody.setOpaque(false);
        formBody.add(formGrid, BorderLayout.CENTER);
        formBody.add(btnPanel, BorderLayout.SOUTH);
        formCard.add(formBody, BorderLayout.CENTER);
        north.add(formCard, BorderLayout.CENTER);

        // --- Bảng ---
        JPanel tableCard = createCard(null);
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        JLabel lblList = new JLabel("Danh sách học viên");
        lblList.setFont(font(Font.BOLD, 16));
        lblList.setForeground(COLOR_TEXT_MAIN);
        RoundedTextField txtSearch = new RoundedTextField("Tìm theo tên, mã, SĐT...");
        txtSearch.setPreferredSize(new Dimension(300, 38));
        tableHeader.add(lblList, BorderLayout.WEST);
        tableHeader.add(txtSearch, BorderLayout.EAST);
        tableCard.add(tableHeader, BorderLayout.NORTH);

        String[] columns = {"Mã HV", "Họ tên", "Số điện thoại", "Email", "Ngày đăng ký"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        model.addRow(new Object[]{"HV001", "Nguyễn Văn A", "0912345678", "nva@gmail.com", "2026-01-10"});
        model.addRow(new Object[]{"HV002", "Trần Thị B", "0987654321", "ttb@gmail.com", "2026-02-15"});

        JTable table = createCustomTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        tableCard.add(createScroll(table), BorderLayout.CENTER);

        // --- Sự kiện ---
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() {
                String s = txtSearch.getText().trim();
                sorter.setRowFilter(s.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(s)));
            }
            @Override public void insertUpdate(DocumentEvent e) { apply(); }
            @Override public void removeUpdate(DocumentEvent e) { apply(); }
            @Override public void changedUpdate(DocumentEvent e) { apply(); }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && viewRow >= 0) {
                int r = table.convertRowIndexToModel(viewRow);
                txtMaHV.setText(String.valueOf(model.getValueAt(r, 0)));
                txtHoTen.setText(String.valueOf(model.getValueAt(r, 1)));
                txtSDT.setText(String.valueOf(model.getValueAt(r, 2)));
                txtEmail.setText(String.valueOf(model.getValueAt(r, 3)));
            }
        });

        Runnable clearForm = () -> {
            txtMaHV.setText("");
            txtHoTen.setText("");
            txtSDT.setText("");
            txtEmail.setText("");
            table.clearSelection();
            txtMaHV.requestFocus();
        };

        btnClear.addActionListener(e -> clearForm.run());

        btnAdd.addActionListener(e -> {
            if (txtMaHV.getText().trim().isEmpty() || txtHoTen.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập mã và họ tên học viên.",
                        "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            model.addRow(new Object[]{txtMaHV.getText().trim(), txtHoTen.getText().trim(),
                    txtSDT.getText().trim(), txtEmail.getText().trim(), LocalDate.now().toString()});
            clearForm.run();
        });

        btnUpdate.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Hãy chọn một học viên trong danh sách để cập nhật.",
                        "Chưa chọn học viên", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int r = table.convertRowIndexToModel(viewRow);
            model.setValueAt(txtMaHV.getText().trim(), r, 0);
            model.setValueAt(txtHoTen.getText().trim(), r, 1);
            model.setValueAt(txtSDT.getText().trim(), r, 2);
            model.setValueAt(txtEmail.getText().trim(), r, 3);
        });

        btnDelete.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Hãy chọn một học viên trong danh sách để xóa.",
                        "Chưa chọn học viên", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int ok = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa học viên này?",
                    "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                model.removeRow(table.convertRowIndexToModel(viewRow));
                clearForm.run();
            }
        });

        panel.add(north, BorderLayout.NORTH);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    // =====================================================================
    //  4. CÁC TRANG CÒN LẠI
    // =====================================================================
    private JPanel createLopHocPanel() { return createPlaceholderPanel("Khóa học & lớp học", IconType.BOOK); }
    private JPanel createNhanSuPanel() { return createPlaceholderPanel("Giáo viên & nhân viên", IconType.TEACHER); }
    private JPanel createTaiChinhPanel() { return createPlaceholderPanel("Học phí & phiếu lương", IconType.WALLET); }

    private JPanel createPlaceholderPanel(String title, IconType icon) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_BG);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);

        IconBadge badge = new IconBadge(icon, COLOR_PINK, COLOR_PRIMARY, 88, 88);
        badge.setMaximumSize(new Dimension(88, 88));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(title);
        lbl.setFont(font(Font.BOLD, 22));
        lbl.setForeground(COLOR_TEXT_MAIN);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Chức năng này đang được phát triển");
        sub.setFont(font(Font.PLAIN, 14));
        sub.setForeground(COLOR_TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(badge);
        box.add(Box.createRigidArea(new Dimension(0, 18)));
        box.add(lbl);
        box.add(Box.createRigidArea(new Dimension(0, 6)));
        box.add(sub);
        panel.add(box);
        return panel;
    }

    // =====================================================================
    //  TIỆN ÍCH TẠO COMPONENT
    // =====================================================================
    private JPanel createPageHeader(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 3));
        texts.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(font(Font.BOLD, 24));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(font(Font.PLAIN, 13));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        texts.add(lblTitle);
        texts.add(lblSub);
        p.add(texts, BorderLayout.WEST);
        return p;
    }

    private JPanel createCard(String title) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 16, COLOR_CARD, true);
        card.setBorder(new EmptyBorder(18, 20, 20, 20));
        if (title != null && !title.isEmpty()) {
            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(font(Font.BOLD, 16));
            lblTitle.setForeground(COLOR_TEXT_MAIN);
            card.add(lblTitle, BorderLayout.NORTH);
        }
        return card;
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(font(Font.BOLD, 12));
        label.setForeground(COLOR_TEXT_MUTED);
        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private JScrollPane createScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JTable createCustomTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(44);
        table.setFont(font(Font.PLAIN, 13));
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(243, 244, 246));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(COLOR_SELECT);
        table.setSelectionForeground(COLOR_TEXT_MAIN);
        table.setFocusable(false);

        table.setDefaultRenderer(Object.class, new ZebraRenderer());
        for (int i = 0; i < model.getColumnCount(); i++) {
            if ("Trạng thái".equals(model.getColumnName(i))) {
                table.getColumnModel().getColumn(i).setCellRenderer(new StatusRenderer());
            }
        }

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new HeaderRenderer());
        return table;
    }

    private static Color rowBackground(int row, boolean selected) {
        if (selected) return COLOR_SELECT;
        return row % 2 == 0 ? Color.WHITE : COLOR_ZEBRA;
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setBackground(rowBackground(row, sel));
            setForeground(COLOR_TEXT_MAIN);
            return c;
        }
    }

    private static class StatusRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int row, int col) {
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setBackground(rowBackground(row, sel));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(0, 12, 0, 0);
            wrap.add(new Pill(v == null ? "" : v.toString()), gbc);
            return wrap;
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setFont(font(Font.BOLD, 12));
            setBackground(new Color(245, 245, 246));
            setForeground(COLOR_TEXT_MUTED);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                    new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    // =====================================================================
    //  CHẠY CHƯƠNG TRÌNH
    // =====================================================================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new MainDashboardUI().setVisible(true));
    }
}