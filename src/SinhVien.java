

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Bộ component giao diện dùng chung (phong cách S-Link).
 * Các lớp Dashboard của từng vai trò dùng: import static btl.UIKit.*;
 */
public final class SinhVien {

    private SinhVien() {}

    // =====================================================================
    //  MÀU SẮC & FONT
    // =====================================================================
    static final Color COLOR_PRIMARY = new Color(229, 28, 35);
    static final Color COLOR_PINK = new Color(254, 226, 226);
    static final Color COLOR_BG = new Color(244, 244, 245);
    static final Color COLOR_CARD = Color.WHITE;
    static final Color COLOR_BORDER = new Color(229, 231, 235);
    static final Color COLOR_TEXT_MAIN = new Color(31, 41, 55);
    static final Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    static final Color COLOR_NAV_ICON = new Color(75, 85, 99);
    static final Color COLOR_HOVER = new Color(247, 247, 247);
    static final Color COLOR_ACTIVE_BG = new Color(240, 240, 241);
    static final Color COLOR_SELECT = new Color(254, 226, 226);
    static final Color COLOR_ZEBRA = new Color(250, 250, 250);
    static final Color COLOR_BLUE = new Color(22, 119, 255);
    static final Color COLOR_GREEN = new Color(82, 196, 26);
    static final Color COLOR_TEAL = new Color(94, 200, 210);
    static final Color COLOR_GRAY = new Color(156, 163, 175);

    static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    // =====================================================================
    //  ICON VECTOR
    // =====================================================================
    enum IconType {
        HOME, USERS, BOOK, TEACHER, WALLET, BELL, GRID, CALENDAR, CHAT, COPY, PROFILE, SCORE, LOGOUT
    }

    static void drawIcon(Graphics g, IconType type, int x, int y, int size, Color color) {
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
            case PROFILE:
                g2.drawRoundRect(2, 4, 16, 12, 3, 3);
                g2.drawOval(5, 7, 4, 4);
                g2.drawLine(11, 8, 15, 8);
                g2.drawLine(11, 12, 15, 12);
                g2.drawLine(5, 14, 9, 14);
                break;
            case SCORE:
                g2.drawLine(3, 17, 17, 17);
                g2.drawRoundRect(4, 10, 3, 7, 1, 1);
                g2.drawRoundRect(9, 6, 3, 11, 1, 1);
                g2.drawRoundRect(14, 3, 3, 14, 1, 1);
                break;
            case LOGOUT: {
                Path2D door = new Path2D.Float();
                door.moveTo(8, 3);
                door.lineTo(4, 3);
                door.lineTo(4, 17);
                door.lineTo(8, 17);
                g2.draw(door);
                g2.drawLine(8, 10, 17, 10);
                g2.drawLine(14, 7, 17, 10);
                g2.drawLine(14, 13, 17, 10);
                break;
            }
            default:
                break;
        }
        g2.dispose();
    }

    static class VIcon implements Icon {
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
    //  COMPONENT TỰ VẼ
    // =====================================================================

    /** Panel bo góc, viền mảnh, bóng nhẹ phía dưới. */
    static class RoundedPanel extends JPanel {
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
    static class RoundedButton extends JButton {
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
    static class NavButton extends JButton {
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

    /** Nút tròn nhỏ (thu gọn sidebar). */
    static class CircleButton extends JButton {
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

    /** Nút vuông có icon (thanh trên cùng), tuỳ chọn huy hiệu đỏ. */
    static class IconButton extends JButton {
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

    /** Ô nhập bo góc, viền đỏ khi focus, có placeholder; nền xám khi chỉ đọc. */
    static class RoundedTextField extends JTextField {
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
            g2.setColor(isEditable() ? Color.WHITE : new Color(249, 250, 251));
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
            boolean focus = isFocusOwner() && isEditable();
            g3.setColor(focus ? COLOR_PRIMARY : new Color(209, 213, 219));
            g3.setStroke(new BasicStroke(focus ? 1.6f : 1f));
            g3.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g3.dispose();
        }
    }

    /** Hình tròn / bo góc chứa chữ (logo, avatar). */
    static class Badge extends JComponent {
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
    static class IconBadge extends JComponent {
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
    static class Pill extends JLabel {
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

    private static boolean isOk(String t) {
        return !t.contains("Chưa") && (t.contains("Đã") || t.contains("Đạt") || t.contains("Đang học"));
    }

    static Color pillBg(String t) {
        if (t.contains("Chưa")) return new Color(254, 243, 199);
        if (isOk(t)) return new Color(220, 252, 231);
        return new Color(229, 231, 235);
    }

    static Color pillFg(String t) {
        if (t.contains("Chưa")) return new Color(146, 64, 14);
        if (isOk(t)) return new Color(22, 101, 52);
        return new Color(55, 65, 81);
    }

    /** Biểu đồ cột vẽ bằng Graphics2D. */
    static class BarChart extends JComponent {
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

                String v = values[i] % 1 == 0 ? String.valueOf((int) values[i]) : String.format("%.1f", values[i]);
                g2.setColor(last ? COLOR_TEXT_MAIN : COLOR_TEXT_MUTED);
                g2.drawString(v, x + (barW - fm.stringWidth(v)) / 2, y - 6);
                g2.setColor(COLOR_TEXT_MUTED);
                g2.drawString(labels[i], x + (barW - fm.stringWidth(labels[i])) / 2, h - 8);
            }
            g2.dispose();
        }
    }

    /** Thanh tiến trình xanh dương. */
    static class ProgressLine extends JComponent {
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
    static class DashedLine extends JComponent {
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
    static class BannerPanel extends JPanel {
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

    /** Thẻ thông báo / sự kiện: mũi tên chuyển mục, ngày và nhãn. */
    static class NotifyCard extends RoundedPanel {
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

    static JLabel createArrow(String s, Runnable action) {
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
    static class CourseCard extends JPanel {
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
    static class ScrollablePanel extends JPanel implements Scrollable {
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
    //  HÀM TIỆN ÍCH DỰNG GIAO DIỆN
    // =====================================================================

    /** Bọc một "stack" (GridBagLayout xếp dọc) thành trang cuộn được. */
    static JPanel scrollPage(JPanel stack) {
        ScrollablePanel content = new ScrollablePanel(new BorderLayout());
        content.setBackground(COLOR_BG);
        content.setBorder(new EmptyBorder(0, 28, 30, 28));
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

    static void addRow(JPanel stack, Component c, int y, int bottomGap) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, bottomGap, 0);
        stack.add(c, g);
    }

    static JLabel createSectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 18));
        l.setForeground(COLOR_TEXT_MAIN);
        return l;
    }

    static JPanel createPageHeader(String title, String subtitle) {
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

    static JPanel createCard(String title) {
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

    static JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(font(Font.BOLD, 12));
        label.setForeground(COLOR_TEXT_MUTED);
        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    static JPanel createInfoRow(String left, String right) {
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

    static JPanel createStatCard(String title, String value, String trend, IconType icon, Color accent) {
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

    static JPanel createStatBox(IconType icon, String text, String tip) {
        RoundedPanel box = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 8, 10), 8, Color.WHITE, false);
        JLabel l = new JLabel(text, new VIcon(icon, 18, COLOR_TEXT_MUTED), SwingConstants.LEFT);
        l.setIconTextGap(8);
        l.setFont(font(Font.PLAIN, 14));
        l.setForeground(COLOR_TEXT_MAIN);
        box.add(l);
        box.setToolTipText(tip);
        return box;
    }

    /**
     * Dữ liệu một lớp: {thứ tự, mã lớp, tên, tag, ô1, ô2, ô3, tiếp theo, tiến trình, phần trăm, giáo viên}
     */
    static JPanel createCourseCard(String[] d) {
        Color tagColor = "Hiện tại".equals(d[3]) ? COLOR_GREEN
                : "Sắp tới".equals(d[3]) ? COLOR_PRIMARY : COLOR_GRAY;
        return createCourseCard(d[0], d[1], d[2], d[3], tagColor,
                new String[]{d[4], d[5], d[6]}, d[7], d[8], Integer.parseInt(d[9]), d[10]);
    }

    static JPanel createCourseCard(String order, String code, String name, String tag, Color tagColor,
                                   String[] stats, String next, String progressText, int percent,
                                   String teacher) {
        CourseCard card = new CourseCard(tag, tagColor);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTH;

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

        JLabel lblName = new JLabel(name);
        lblName.setFont(font(Font.BOLD, 21));
        lblName.setForeground(COLOR_TEXT_MAIN);
        g.gridy = 1;
        g.insets = new Insets(4, 0, 0, 0);
        card.add(lblName, g);

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

    // =====================================================================
    //  BẢNG DỮ LIỆU
    // =====================================================================
    static JScrollPane createScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Bảng chỉ đọc; cột tên "Trạng thái" hoặc "Kết quả" hiển thị dạng nhãn màu. */
    static DefaultTableModel createReadOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static JTable createCustomTable(DefaultTableModel model) {
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
            String n = model.getColumnName(i);
            if ("Trạng thái".equals(n) || "Kết quả".equals(n)) {
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

    static class ZebraRenderer extends DefaultTableCellRenderer {
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

    static class StatusRenderer implements TableCellRenderer {
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

    static class HeaderRenderer extends DefaultTableCellRenderer {
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
}