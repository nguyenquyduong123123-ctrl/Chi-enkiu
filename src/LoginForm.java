import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Màn hình chọn cổng truy cập S-LINK.
 * Không cần đăng nhập: mỗi cổng dùng một tài khoản mặc định.
 */
public class LoginForm extends JFrame {

    // ===== Tài khoản mặc định cho từng cổng =====
    private static final String DEFAULT_STAFF_ID   = "NV01";
    private static final String DEFAULT_TEACHER_ID = "GV01";
    private static final String DEFAULT_STUDENT_ID = "SV01";

    // ===== Bảng màu (đồng bộ với AdminDashboard) =====
    private static final Color NAVY        = new Color(15, 23, 62);
    private static final Color BLUE        = new Color(59, 91, 240);
    private static final Color PURPLE      = new Color(124, 58, 237);
    private static final Color COLOR_STAFF   = new Color(79, 70, 229);
    private static final Color COLOR_TEACHER = new Color(14, 165, 233);
    private static final Color COLOR_STUDENT = new Color(16, 185, 129);
    private static final Color COLOR_ADMIN   = new Color(245, 158, 11);
    private static final Color COLOR_BG      = new Color(248, 250, 252);
    private static final Color COLOR_TEXT    = new Color(15, 23, 42);
    private static final Color COLOR_MUTED   = new Color(100, 116, 139);
    private static final Color COLOR_BORDER  = new Color(226, 232, 240);

    private static final String FONT_FAMILY = pickFont("Helvetica Neue", "Arial");

    private boolean launching = false;

    public LoginForm() {
        setTitle("S-LINK | Cổng truy cập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout());

        // ---------- HEADER ----------
        add(new HeaderPanel(), BorderLayout.NORTH);

        // ---------- BODY ----------
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(22, 28, 6, 28));

        PortalCard staff = new PortalCard("NV", "Cổng nhân viên",
                "Quản lý hóa đơn, lớp học và học viên", COLOR_STAFF,
                () -> new StaffDashboardUI(DEFAULT_STAFF_ID));
        PortalCard teacher = new PortalCard("GV", "Cổng giáo viên",
                "Lịch giảng dạy, lớp phụ trách, phiếu lương", COLOR_TEACHER,
                () -> new TeacherDashboardUI(DEFAULT_TEACHER_ID));
        PortalCard student = new PortalCard("HV", "Cổng học viên",
                "Lịch học, kết quả thi và đánh giá", COLOR_STUDENT,
                () -> new StudentDashboardUI(DEFAULT_STUDENT_ID));
        PortalCard admin = new PortalCard("AD", "Cổng quản trị Admin",
                "Toàn quyền quản trị dữ liệu hệ thống", COLOR_ADMIN,
                AdminDashboard::new);

        for (PortalCard c : Arrays.asList(staff, teacher, student, admin)) {
            body.add(c);
        }
        add(body, BorderLayout.CENTER);

        // ---------- FOOTER ----------
        JLabel footer = new JLabel("Phím tắt: 1 · 2 · 3 · 4     |     S-LINK © 2026", SwingConstants.CENTER);
        footer.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        footer.setForeground(COLOR_MUTED);
        footer.setBorder(new EmptyBorder(6, 0, 18, 0));
        add(footer, BorderLayout.SOUTH);

        // ---------- PHÍM TẮT 1-4 ----------
        bindKey("1", staff);
        bindKey("2", teacher);
        bindKey("3", student);
        bindKey("4", admin);

        getContentPane().setPreferredSize(new Dimension(460, 650));
        pack();
        setLocationRelativeTo(null);
    }

    private void bindKey(String key, PortalCard card) {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), "go" + key);
        root.getActionMap().put("go" + key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { card.fire(); }
        });
    }

    /** Mở một cổng. Chỉ đóng form này khi cổng mới đã mở thành công. */
    private void openPortal(Supplier<JFrame> factory) {
        if (launching) return;               // chặn bấm nhiều lần
        launching = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingUtilities.invokeLater(() -> {
            try {
                JFrame next = factory.get();
                if (next == null) throw new IllegalStateException("Không tạo được giao diện.");
                next.setLocationRelativeTo(null);
                next.setVisible(true);
                dispose();
            } catch (Throwable ex) {
                launching = false;
                setCursor(Cursor.getDefaultCursor());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Không thể mở cổng này.\nChi tiết: " + ex.getMessage()
                                + "\n\nHãy kiểm tra kết nối database (DBConnection).",
                        "Lỗi mở cổng", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // =====================================================================
    //  HEADER: nền gradient xanh → tím giống banner của Admin
    // =====================================================================
    private static class HeaderPanel extends JPanel {
        HeaderPanel() {
            setPreferredSize(new Dimension(460, 190));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            applyHints(g2);
            int w = getWidth(), h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, BLUE, w, h, PURPLE));
            g2.fillRect(0, 0, w, h);

            // vòng tròn trang trí mờ
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fill(new Ellipse2D.Float(w - 120, -70, 220, 220));
            g2.setColor(new Color(255, 255, 255, 16));
            g2.fill(new Ellipse2D.Float(-60, h - 80, 170, 170));

            // logo
            int size = 60, lx = (w - size) / 2, ly = 28;
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fill(new RoundRectangle2D.Float(lx, ly + 4, size, size, 18, 18));
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(lx, ly, size, size, 18, 18));
            g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 32));
            g2.setColor(BLUE);
            drawCentered(g2, "S", lx, ly, size, size);

            // tiêu đề
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 26));
            drawCentered(g2, "HỆ THỐNG S-LINK", 0, 100, w, 34);

            g2.setColor(new Color(255, 255, 255, 215));
            g2.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
            drawCentered(g2, "Chọn cổng truy cập để tiếp tục", 0, 134, w, 24);

            g2.dispose();
        }
    }

    // =====================================================================
    //  THẺ CỔNG: hiệu ứng hover mượt, hỗ trợ bàn phím
    // =====================================================================
    private class PortalCard extends JComponent {
        private final String badge, title, desc;
        private final Color accent;
        private final Supplier<JFrame> factory;
        private float hover = 0f;      // 0 → 1
        private boolean pressed = false;
        private final Timer anim;

        PortalCard(String badge, String title, String desc, Color accent, Supplier<JFrame> factory) {
            this.badge = badge;
            this.title = title;
            this.desc = desc;
            this.accent = accent;
            this.factory = factory;

            setPreferredSize(new Dimension(404, 94));
            setMinimumSize(new Dimension(404, 94));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 94));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            setOpaque(false);

            final boolean[] over = {false};
            anim = new Timer(14, e -> {
                float target = (over[0] || isFocusOwner()) ? 1f : 0f;
                hover += (target - hover) * 0.28f;
                if (Math.abs(target - hover) < 0.01f) {
                    hover = target;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over[0] = true; anim.start(); }
                @Override public void mouseExited(MouseEvent e)  { over[0] = false; pressed = false; anim.start(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; requestFocusInWindow(); repaint(); }
                @Override public void mouseReleased(MouseEvent e) {
                    boolean click = pressed && contains(e.getPoint());
                    pressed = false;
                    repaint();
                    if (click) fire();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { anim.start(); }
                @Override public void focusLost(FocusEvent e)   { anim.start(); }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) fire();
                }
            });
        }

        void fire() { openPortal(factory); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            applyHints(g2);

            int pad = 7;
            float lift = hover * 2f - (pressed ? 1.5f : 0f);
            float x = pad, y = pad - lift;
            float w = getWidth() - pad * 2f, h = getHeight() - pad * 2f - 4;

            // bóng đổ
            for (int i = 7; i >= 1; i--) {
                int a = (int) ((4 + 7 * hover) * (8 - i) / 7f);
                g2.setColor(new Color(15, 23, 42, Math.max(0, a)));
                g2.fill(new RoundRectangle2D.Float(x - i + 1, y - i + 4 + lift, w + (i - 1) * 2, h + (i - 1) * 2, 16 + i, 16 + i));
            }

            // nền thẻ
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, 16, 16));

            // viền
            g2.setStroke(new BasicStroke(hasFocus() ? 2f : 1.2f));
            g2.setColor(lerp(COLOR_BORDER, accent, Math.max(hover, hasFocus() ? 1f : 0f)));
            g2.draw(new RoundRectangle2D.Float(x, y, w, h, 16, 16));

            // huy hiệu (giống HS/GV/LH/KH của dashboard)
            float bs = 50, bx = x + 20, by = y + (h - bs) / 2f;
            g2.setColor(lerp(withAlpha(accent, 34), accent, hover));
            g2.fill(new RoundRectangle2D.Float(bx, by, bs, bs, 14, 14));
            g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
            g2.setColor(lerp(accent, Color.WHITE, hover));
            drawCentered(g2, badge, (int) bx, (int) by, (int) bs, (int) bs);

            // chữ
            int tx = (int) (bx + bs + 18);
            g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
            g2.setColor(COLOR_TEXT);
            g2.drawString(title, tx, (int) (y + h / 2f - 3));
            g2.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
            g2.setColor(COLOR_MUTED);
            g2.drawString(desc, tx, (int) (y + h / 2f + 17));

            // mũi tên
            float ax = x + w - 30 + 4 * hover, ay = y + h / 2f;
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(lerp(new Color(148, 163, 184), accent, hover));
            g2.drawLine((int) ax, (int) ay - 6, (int) ax + 6, (int) ay);
            g2.drawLine((int) ax + 6, (int) ay, (int) ax, (int) ay + 6);

            g2.dispose();
        }
    }

    // =====================================================================
    //  Tiện ích vẽ
    // =====================================================================
    private static void applyHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static void drawCentered(Graphics2D g2, String s, int x, int y, int w, int h) {
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(s)) / 2;
        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(s, tx, ty);
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static String pickFont(String... candidates) {
        java.util.Set<String> available = new java.util.HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : candidates) if (available.contains(f)) return f;
        return Font.SANS_SERIF;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}