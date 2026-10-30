import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * CỔNG GIÁO VIÊN S-LINK (bản làm lại, độc lập - không cần PortalFrame)
 *
 * Mở bằng: new TeacherDashboardUI("GV01").setVisible(true);
 *
 * Cấu hình CSDL qua biến môi trường (hoặc sửa mặc định bên dưới):
 *   DB_URL  (mặc định jdbc:mysql://localhost:3306/btl ...)   DB_USER (root)   DB_PASS (rỗng)
 * Cần mysql-connector-j trong classpath. Chạy được trên JDK 8+.
 *
 * CÁC LỖI NGHIỆP VỤ ĐÃ SỬA so với bản cũ:
 *  1. Trạng thái buổi học: CSDL chỉ cho 'Dạy' / 'Nghỉ' (CHECK) nhưng code cũ dùng 'day' / 'nghi'
 *     -> mọi truy vấn đếm/đổi trạng thái đều sai hoặc vi phạm ràng buộc. Nay dùng đúng giá trị.
 *  2. Không cho đổi trạng thái buổi học đã diễn ra (trước hôm nay); chỉ đổi buổi của chính giáo viên.
 *  3. "Tổng số buổi dạy" cũ đếm cả buổi tương lai -> nay chỉ đếm buổi 'Dạy' đã diễn ra.
 *  4. Kết quả thi: cột tinh_trang chỉ được UPDATE một lần trong btl.sql, bài làm mới sẽ NULL
 *     -> nay tự tính ĐẠT/TRƯỢT theo đúng quy tắc khi thiếu dữ liệu.
 *  5. Sắp xếp bảng theo giá trị thật (ngày, số) thay vì chuỗi đã format (trước đây "10/01" < "9/01").
 *  6. Không còn Long.parseLong trên chuỗi (dễ NumberFormatException); kết nối DB dùng chung, tự nối lại.
 *  7. Không tìm thấy giáo viên -> không mở cửa sổ với dữ liệu rỗng nữa.
 */
public class TeacherDashboardUI extends JFrame {

    /* ===================== CẤU HÌNH & THEME ===================== */

    static final String DB_URL = env("DB_URL", "jdbc:mysql://localhost:3306/btl?useUnicode=true"
            + "&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh");
    static final String DB_USER = env("DB_USER", "root");
    static final String DB_PASS = env("DB_PASS", "123456");

    static final String DAY = "Dạy", OFF = "Nghỉ";

    static final Color BG = new Color(0xF1F5F9), INK = new Color(0x0F172A), MUTED = new Color(0x64748B),
            LINE = new Color(0xE2E8F0), TEAL = new Color(0x0D9488), TEAL_D = new Color(0x0F766E),
            GREEN = new Color(0x16A34A), RED = new Color(0xDC2626), AMBER = new Color(0xF59E0B),
            BLUE = new Color(0x2563EB), VIOLET = new Color(0x7C3AED);

    static final Locale VI = Locale.forLanguageTag("vi");
    static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter DD = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static String env(String k, String d) {
        String v = System.getenv(k);
        return v == null || v.isEmpty() ? d : v;
    }

    static Font f(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    static Color mix(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static float ease(float t) {
        float u = 1 - t;
        return 1 - u * u * u;
    }

    /* ===================== CSDL ===================== */

    private static Connection shared;
    private static long lastErr;

    static synchronized Connection conn() throws SQLException {
        if (shared == null || shared.isClosed() || !shared.isValid(2)) {
            shared = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }
        return shared;
    }

    static void dbError(Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastErr < 4000) return;
        lastErr = now;
        JOptionPane.showMessageDialog(null, "Lỗi CSDL: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    static List<Object[]> query(String sql, Object... p) {
        List<Object[]> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] r = new Object[n];
                    for (int i = 0; i < n; i++) {
                        Object v = rs.getObject(i + 1);
                        if (v instanceof LocalDateTime) v = Timestamp.valueOf((LocalDateTime) v);
                        r[i] = v;
                    }
                    out.add(r);
                }
            }
        } catch (SQLException e) {
            dbError(e);
        }
        return out;
    }

    static Object one(String sql, Object... p) {
        List<Object[]> r = query(sql, p);
        return r.isEmpty() ? null : r.get(0)[0];
    }

    static long num(String sql, Object... p) {
        Object o = one(sql, p);
        return o instanceof Number ? ((Number) o).longValue() : 0;
    }

    static int update(String sql, Object... p) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            return ps.executeUpdate();
        } catch (SQLException e) {
            dbError(e);
            return -1;
        }
    }

    /* ===================== ĐỊNH DẠNG ===================== */

    static String s(Object o) {
        return o == null ? "" : o.toString();
    }

    static int n(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    static String vnd(Number v) {
        return NumberFormat.getInstance(VI).format(v.longValue()) + " ₫";
    }

    static String stars(int n) {
        n = Math.max(0, Math.min(5, n));
        return "★★★★★".substring(0, n) + "☆☆☆☆☆".substring(0, 5 - n);
    }

    static String when(Timestamp t) {
        LocalDateTime d = t.toLocalDateTime();
        return d.toLocalTime().equals(LocalTime.MIDNIGHT) ? d.format(DD) : d.format(DT);
    }

    /** Quy tắc đạt/trượt giống hệt lệnh UPDATE trong btl.sql. */
    static String result(String lv, int dh, int tv, int ng, int tong) {
        boolean ok;
        switch (lv) {
            case "N5": ok = dh + tv >= 38 && ng >= 19 && tong >= 80; break;
            case "N4": ok = dh + tv >= 38 && ng >= 19 && tong >= 90; break;
            case "N3": ok = dh >= 19 && tv >= 19 && ng >= 19 && tong >= 95; break;
            case "N2": ok = dh >= 19 && tv >= 19 && ng >= 19 && tong >= 90; break;
            case "N1": ok = dh >= 19 && tv >= 19 && ng >= 19 && tong >= 100; break;
            default: ok = false;
        }
        return ok ? "ĐẠT" : "TRƯỢT";
    }

    /* ===================== HOẠT ẢNH ===================== */

    static Timer anim(int ms, Consumer<Float> step, Runnable done) {
        final long t0 = System.nanoTime();
        final Timer t = new Timer(15, null);
        t.addActionListener(e -> {
            float p = Math.min(1f, (System.nanoTime() - t0) / 1e6f / ms);
            step.accept(p);
            if (p >= 1f) {
                t.stop();
                if (done != null) done.run();
            }
        });
        t.start();
        return t;
    }

    /** Giá trị 0..1 trượt mượt về mục tiêu (dùng cho hover / chọn). */
    static class Tween {
        float v, target;
        final Timer t;

        Tween(JComponent c) {
            t = new Timer(15, e -> {
                float d = target - v;
                if (Math.abs(d) < 0.02f) {
                    v = target;
                    ((Timer) e.getSource()).stop();
                } else v += d * 0.25f;
                c.repaint();
            });
        }

        void to(float x) {
            target = x;
            t.start();
        }
    }

    static Tween hover(JComponent c) {
        Tween tw = new Tween(c);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                tw.to(1);
            }

            public void mouseExited(MouseEvent e) {
                tw.to(0);
            }
        });
        return tw;
    }

    /* ===================== THÀNH PHẦN GIAO DIỆN ===================== */

    static void shadowCard(Graphics2D g, int w, int h) {
        for (int i = 6; i >= 1; i--) {
            g.setColor(new Color(15, 23, 42, 4));
            g.fillRoundRect(6 - i, 7 - i, w - 12 + 2 * i, h - 12 + 2 * i, 22 + i, 22 + i);
        }
        g.setColor(Color.WHITE);
        g.fillRoundRect(6, 4, w - 12, h - 12, 22, 22);
        g.setColor(LINE);
        g.drawRoundRect(6, 4, w - 13, h - 13, 22, 22);
    }

    static class Card extends JPanel {
        Card(String title, JComponent tools) {
            super(new BorderLayout(0, 14));
            setOpaque(false);
            setBorder(new EmptyBorder(26, 30, 24, 30));
            JPanel h = new JPanel(new BorderLayout());
            h.setOpaque(false);
            JLabel l = new JLabel(title);
            l.setFont(f(Font.BOLD, 16));
            l.setForeground(INK);
            h.add(l, BorderLayout.WEST);
            if (tools != null) h.add(tools, BorderLayout.EAST);
            add(h, BorderLayout.NORTH);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            shadowCard(g, getWidth(), getHeight());
            g.dispose();
        }
    }

    static class Btn extends JButton {
        final Color col;
        final Tween tw;

        Btn(String text, Color col) {
            super(text);
            this.col = col;
            tw = hover(this);
            setFont(f(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(11, 24, 11, 24));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int off = getModel().isPressed() ? 1 : 0, h = getHeight() - 4;
            g.setColor(alpha(col, (int) (60 * tw.v)));
            g.fillRoundRect(2, 5 + off, getWidth() - 4, h, h, h);
            g.setColor(isEnabled() ? mix(col, Color.BLACK, tw.v * 0.14f) : new Color(0x94A3B8));
            g.fillRoundRect(0, off, getWidth(), h, h, h);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    static class Slim extends BasicScrollBarUI {
        protected void configureScrollBarColors() {
        }

        JButton nb() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        protected JButton createDecreaseButton(int o) {
            return nb();
        }

        protected JButton createIncreaseButton(int o) {
            return nb();
        }

        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(new Color(0xCBD5E1));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUI(new Slim());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getHorizontalScrollBar().setUI(new Slim());
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        return sp;
    }

    /** Bảng dữ liệu: zebra, hover, pill trạng thái, sao đánh giá, sắp xếp theo giá trị thật. */
    static class DataTable extends JTable {
        final DefaultTableModel m;
        final TableRowSorter<TableModel> sorter;
        final String sql, empty;
        final Object[] params;
        final Function<Object[], Object[]> fmt;
        int hover = -1;

        DataTable(String[] cols, String sql, Function<Object[], Object[]> fmt, String empty, Object... params) {
            super(new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) {
                    return false;
                }

                public Class<?> getColumnClass(int c) {
                    Object v = getRowCount() > 0 ? getValueAt(0, c) : null;
                    return v == null ? Object.class : v.getClass();
                }
            });
            this.m = (DefaultTableModel) getModel();
            this.sql = sql;
            this.fmt = fmt;
            this.empty = empty;
            this.params = params;
            sorter = new TableRowSorter<TableModel>(m);
            setRowSorter(sorter);
            setRowHeight(48);
            setShowGrid(false);
            setIntercellSpacing(new Dimension(0, 0));
            setFont(f(0, 13));
            setForeground(INK);
            setSelectionBackground(new Color(0xCCFBF1));
            setSelectionForeground(INK);
            setFillsViewportHeight(true);
            getTableHeader().setReorderingAllowed(false);
            getTableHeader().setPreferredSize(new Dimension(0, 42));
            getTableHeader().setDefaultRenderer((t, v, sel, foc, r, c) -> {
                JLabel l = new JLabel(s(v).toUpperCase(VI));
                l.setOpaque(true);
                l.setBackground(new Color(0xF8FAFC));
                l.setForeground(MUTED);
                l.setFont(f(Font.BOLD, 11));
                l.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(0, 14, 0, 14)));
                return l;
            });
            setDefaultRenderer(Object.class, new Cell());
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    int r = rowAtPoint(e.getPoint());
                    if (r != hover) {
                        hover = r;
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }
            });
            reload();
        }

        void reload() {
            m.setRowCount(0);
            for (Object[] r : query(sql, params)) m.addRow(fmt == null ? r : fmt.apply(r));
            repaint();
        }

        @Override
        public void paint(Graphics g0) {
            super.paint(g0);
            if (getRowCount() == 0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setFont(f(0, 14));
                g.setColor(MUTED);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(empty, (getWidth() - fm.stringWidth(empty)) / 2, getHeight() / 2 - 10);
                g.dispose();
            }
        }

        class Cell extends DefaultTableCellRenderer {
            String pill;
            Color pc;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                String col = t.getColumnName(c), txt;
                Color fg = INK;
                int align = SwingConstants.LEFT;
                pill = null;
                if (v instanceof Timestamp) txt = when((Timestamp) v);
                else if (v instanceof Number && col.equals("Đánh Giá")) {
                    txt = stars(((Number) v).intValue());
                    fg = AMBER;
                } else if (v instanceof Number && col.equals("Tổng Lương")) {
                    txt = vnd((Number) v);
                    fg = TEAL_D;
                    align = SwingConstants.RIGHT;
                } else if (v instanceof Number || v instanceof BigDecimal) {
                    txt = s(v);
                    align = SwingConstants.CENTER;
                } else txt = s(v);
                if (txt.equals(DAY) || txt.equals("ĐẠT")) {
                    pill = txt;
                    pc = GREEN;
                } else if (txt.equals(OFF) || txt.equals("TRƯỢT")) {
                    pill = txt;
                    pc = RED;
                }
                super.getTableCellRendererComponent(t, pill != null ? "" : txt, sel, foc, r, c);
                setForeground(fg);
                setHorizontalAlignment(align);
                setBorder(new EmptyBorder(0, 14, 0, 14));
                setOpaque(true);
                setBackground(sel ? t.getSelectionBackground()
                        : r == hover ? new Color(0xF0FDFA) : r % 2 == 0 ? Color.WHITE : new Color(0xFAFBFC));
                return this;
            }

            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                if (pill == null) return;
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setFont(f(Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(pill) + 26, h = 26, x = 14, y = (getHeight() - h) / 2;
                g.setColor(alpha(pc, 32));
                g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
                g.setColor(pc);
                g.fillOval(x + 9, y + h / 2 - 3, 6, 6);
                g.drawString(pill, x + 20, y + (h + fm.getAscent() - fm.getDescent()) / 2);
                g.dispose();
            }
        }
    }

    static JTextField search(DataTable t) {
        JTextField tf = new JTextField(18) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setColor(new Color(0xF1F5F9));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                if (isFocusOwner()) {
                    g.setColor(TEAL);
                    g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                }
                g.dispose();
                super.paintComponent(g0);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D h = (Graphics2D) g0.create();
                    aa(h);
                    h.setColor(MUTED);
                    h.setFont(getFont());
                    FontMetrics fm = h.getFontMetrics();
                    h.drawString("Tìm kiếm...", getInsets().left,
                            (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    h.dispose();
                }
            }
        };
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(9, 18, 9, 18));
        tf.setFont(f(0, 13));
        tf.setForeground(INK);
        tf.setCaretColor(TEAL);
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                tf.repaint();
            }

            public void focusLost(FocusEvent e) {
                tf.repaint();
            }
        });
        tf.getDocument().addDocumentListener(new DocumentListener() {
            void go() {
                String q = tf.getText().trim();
                t.sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
            }

            public void insertUpdate(DocumentEvent e) {
                go();
            }

            public void removeUpdate(DocumentEvent e) {
                go();
            }

            public void changedUpdate(DocumentEvent e) {
                go();
            }
        });
        return tf;
    }

    /** Thanh bên có "viên thuốc" chọn trượt mượt giữa các mục. */
    static void icon(Graphics2D g, int k, int x, int y, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (k) {
            case 0:
                g.drawPolyline(new int[]{x + 1, x + 9, x + 17}, new int[]{y + 9, y + 1, y + 9}, 3);
                g.drawRoundRect(x + 3, y + 8, 12, 9, 3, 3);
                break;
            case 1:
                g.drawRoundRect(x + 2, y + 1, 14, 16, 3, 3);
                g.drawLine(x + 6, y + 6, x + 12, y + 6);
                g.drawLine(x + 6, y + 10, x + 12, y + 10);
                break;
            case 2:
                g.drawRoundRect(x + 1, y + 3, 16, 14, 4, 4);
                g.drawLine(x + 1, y + 8, x + 17, y + 8);
                g.drawLine(x + 6, y + 1, x + 6, y + 5);
                g.drawLine(x + 12, y + 1, x + 12, y + 5);
                break;
            case 3:
                g.drawOval(x + 6, y + 2, 6, 6);
                g.drawArc(x + 2, y + 11, 14, 12, 0, 180);
                break;
            case 4:
                g.drawOval(x + 3, y + 1, 12, 12);
                g.drawLine(x + 6, y + 13, x + 4, y + 18);
                g.drawLine(x + 12, y + 13, x + 14, y + 18);
                break;
            case 5:
                Path2D p = new Path2D.Float();
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? 9 : 4;
                    double px = x + 9 + r * Math.cos(a), py = y + 9.5 + r * Math.sin(a);
                    if (i == 0) p.moveTo(px, py);
                    else p.lineTo(px, py);
                }
                p.closePath();
                g.draw(p);
                break;
            case 6:
                g.drawRoundRect(x + 1, y + 4, 16, 12, 4, 4);
                g.drawLine(x + 2, y + 4, x + 12, y + 1);
                g.fillOval(x + 12, y + 9, 3, 3);
                break;
            default:
                g.drawOval(x + 5, y + 1, 8, 8);
                g.drawArc(x + 2, y + 11, 14, 12, 0, 180);
        }
    }

    static class Avatar extends JComponent {
        final String text;

        Avatar(String text, int size) {
            this.text = text;
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0x2DD4BF), w, h, TEAL_D));
            g.fillOval(0, 0, w, h);
            g.setColor(Color.WHITE);
            g.setFont(f(Font.BOLD, h / 3 + 2));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (w - fm.stringWidth(text)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    static class Tile extends JPanel {
        Tile(String k, String v) {
            super(new GridLayout(2, 1, 0, 4));
            setOpaque(false);
            setBorder(new EmptyBorder(14, 18, 14, 18));
            JLabel a = new JLabel(k), b = new JLabel(v.isEmpty() ? "—" : v);
            a.setFont(f(0, 12));
            a.setForeground(MUTED);
            b.setFont(f(Font.BOLD, 15));
            b.setForeground(INK);
            add(a);
            add(b);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(new Color(0xF8FAFC));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.setColor(LINE);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.dispose();
        }
    }

    /** Banner chào mừng với các vòng tròn trôi nhẹ. */
    static class Banner extends JPanel {
        final String title, sub;
        float phase;
        Timer tm;

        Banner(String title, String sub) {
            this.title = title;
            this.sub = sub;
            setOpaque(false);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            tm = new Timer(33, e -> {
                phase += 0.03f;
                repaint();
            });
            tm.start();
        }

        @Override
        public void removeNotify() {
            if (tm != null) tm.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            Shape rr = new RoundRectangle2D.Float(6, 4, w - 12, h - 10, 26, 26);
            g.setPaint(new GradientPaint(0, 0, TEAL_D, w, h, new Color(0x14B8A6)));
            g.fill(rr);
            g.setClip(rr);
            g.setColor(new Color(255, 255, 255, 26));
            g.fillOval((int) (w - 240 + 20 * Math.sin(phase)), (int) (-90 + 12 * Math.cos(phase)), 320, 320);
            g.fillOval((int) (w - 460 + 26 * Math.cos(phase * 0.8)), (int) (h - 70 + 10 * Math.sin(phase)), 210, 210);
            g.setColor(new Color(255, 255, 255, 18));
            g.fillOval((int) (w * 0.45 + 30 * Math.sin(phase * 0.6)), -60, 150, 150);
            g.setColor(Color.WHITE);
            g.setFont(f(Font.BOLD, 26));
            g.drawString(title, 36, 56);
            g.setColor(new Color(255, 255, 255, 215));
            g.setFont(f(0, 14));
            g.drawString(sub, 36, 84);
            LocalDate d = LocalDate.now();
            String wd = d.getDayOfWeek().getDisplayName(TextStyle.FULL, VI);
            String ds = Character.toUpperCase(wd.charAt(0)) + wd.substring(1) + ", " + d.format(DD);
            g.setFont(f(Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int pw = fm.stringWidth(ds) + 28;
            g.setColor(new Color(255, 255, 255, 45));
            g.fillRoundRect(36, 98, pw, 28, 28, 28);
            g.setColor(Color.WHITE);
            g.drawString(ds, 50, 98 + (28 + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Thẻ thống kê: số đếm tăng dần + vòng tiến độ. */
    static class Stat extends JPanel {
        final String title, fmt, sub;
        final double target;
        final Color col;
        double cur;
        float prog;

        Stat(String title, double target, String fmt, String sub, Color col) {
            this.title = title;
            this.target = target;
            this.fmt = fmt;
            this.sub = sub;
            this.col = col;
            setOpaque(false);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (target >= 0) anim(900, p -> {
                float e = ease(p);
                cur = target * e;
                prog = e;
                repaint();
            }, null);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            shadowCard(g, w, h);
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(alpha(col, 35));
            g.drawOval(w - 74, 28, 38, 38);
            g.setColor(col);
            g.drawArc(w - 74, 28, 38, 38, 90, -(int) (300 * prog));
            g.setFont(f(Font.BOLD, 13));
            g.setColor(MUTED);
            g.drawString(title, 30, 42);
            g.setFont(f(Font.BOLD, 30));
            g.setColor(INK);
            g.drawString(target < 0 ? "Chưa có" : String.format(VI, fmt, cur), 30, 84);
            g.setFont(f(0, 12));
            g.setColor(MUTED);
            g.drawString(sub, 30, 108);
            g.dispose();
        }
    }

    static class Toast extends JComponent {
        final String text;
        final Color col;
        float a;

        Toast(String text, Color col) {
            this.text = text;
            this.col = col;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a))));
            int w = getWidth(), h = getHeight() - 8;
            g.setColor(new Color(0, 0, 0, 40));
            g.fillRoundRect(2, 6, w - 4, h, h, h);
            g.setColor(INK);
            g.fillRoundRect(0, 0, w, h, h, h);
            g.setColor(col);
            g.fillOval(18, h / 2 - 5, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(f(Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 38, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Khung nội dung: trượt lên + hiện dần khi đổi trang. */
    static class FadePanel extends JPanel {
        float alpha = 1f;

        FadePanel() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(26, 32, 26, 32));
        }

        void swap(JComponent c) {
            removeAll();
            add(c, BorderLayout.CENTER);
            alpha = 0f;
            revalidate();
            anim(420, p -> {
                alpha = ease(p);
                repaint();
            }, null);
        }

        @Override
        public void paint(Graphics g0) {
            if (alpha >= 1f) {
                super.paint(g0);
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.001f, alpha)));
            g.translate(0, (1 - alpha) * 22);
            super.paint(g);
            g.dispose();
        }
    }

    /* ===================== KHUNG CHÍNH ===================== */

    private final String idGiaoVien;
    private String hoTen = "", soDienThoai = "", chungChi = "", quocTich = "";
    private boolean found;
    private final FadePanel content = new FadePanel();
    private Side side;
    private final String[] names = {"Tổng quan", "Lớp phụ trách", "Lịch dạy", "Học sinh của tôi",
            "Điểm thi học sinh", "Phản hồi học sinh", "Phiếu lương", "Thông tin cá nhân"};
    private final List<Supplier<JComponent>> pages = new ArrayList<>();

    public TeacherDashboardUI() {
        this("GV01");
    }

    public TeacherDashboardUI(String idGiaoVien) {
        super("Cổng Giáo Viên S-Link");
        this.idGiaoVien = idGiaoVien;
        loadProfile();
        setTitle("Cổng Giáo Viên S-Link - " + hoTen);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1340, 840);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        pages.addAll(Arrays.<Supplier<JComponent>>asList(this::overviewPage, this::classPage, this::schedulePage,
                this::studentPage, this::examPage, this::feedbackPage, this::payslipPage, this::profilePage));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        setContentPane(root);
        side = new Side();
        root.add(side, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        if (found) showPage(0);
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b && found);
    }

    private void loadProfile() {
        List<Object[]> r = query("SELECT ho_ten, so_dien_thoai, chung_chi, quoc_tich FROM Giao_vien "
                + "WHERE id_giao_vien = ?", idGiaoVien);
        if (r.isEmpty()) {
            hoTen = idGiaoVien;
            JOptionPane.showMessageDialog(null, "Không tìm thấy giáo viên có mã " + idGiaoVien + " trong CSDL.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        found = true;
        hoTen = s(r.get(0)[0]).isEmpty() ? idGiaoVien : s(r.get(0)[0]);
        soDienThoai = s(r.get(0)[1]);
        chungChi = s(r.get(0)[2]);
        quocTich = s(r.get(0)[3]);
    }

    private String initials() {
        String[] p = hoTen.trim().split("\\s+");
        String a = p[p.length - 1].substring(0, 1);
        return (p.length > 1 ? p[p.length - 2].substring(0, 1) + a : a).toUpperCase(VI);
    }

    void showPage(int i) {
        side.setActive(i);
        content.swap(pages.get(i).get());
    }

    void toast(String msg, Color c) {
        JLayeredPane lp = getLayeredPane();
        Toast t = new Toast(msg, c);
        FontMetrics fm = lp.getFontMetrics(f(Font.BOLD, 14));
        int w = fm.stringWidth(msg) + 62, h = 54;
        t.setBounds((lp.getWidth() + 264 - w) / 2, lp.getHeight() - 100, w, h);
        lp.add(t, JLayeredPane.POPUP_LAYER);
        anim(250, p -> {
            t.a = p;
            t.repaint();
        }, () -> {
            Timer hold = new Timer(2300, e -> anim(350, p -> {
                t.a = 1 - p;
                t.repaint();
            }, () -> {
                lp.remove(t);
                lp.repaint();
            }));
            hold.setRepeats(false);
            hold.start();
        });
    }

    class Nav extends JComponent {
        final int idx;
        final String text;
        final Tween hov, sel;

        Nav(int idx, String text) {
            this.idx = idx;
            this.text = text;
            hov = hover(this);
            sel = new Tween(this);
            setPreferredSize(new Dimension(0, 48));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    showPage(idx);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            float k = Math.max(hov.v * 0.7f, sel.v);
            g.setColor(new Color(255, 255, 255, (int) (22 * hov.v * (1 - sel.v))));
            g.fillRoundRect(14, 0, getWidth() - 28, getHeight(), 14, 14);
            Color c = mix(new Color(0x94A3B8), Color.WHITE, k);
            icon(g, idx, 34 + (int) (3 * hov.v), 15, mix(new Color(0x94A3B8), new Color(0x5EEAD4), k));
            g.setColor(c);
            g.setFont(f(sel.v > 0.5f ? Font.BOLD : Font.PLAIN, 14));
            g.drawString(text, 66 + (int) (3 * hov.v), 29);
            g.dispose();
        }
    }

    class Side extends JPanel {
        final List<Nav> navs = new ArrayList<>();
        int active = 0;
        float indY;
        int targetY;
        final Timer pt;

        Side() {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(264, 0));

            JPanel brand = new JPanel() {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    aa(g);
                    g.setPaint(new GradientPaint(24, 26, new Color(0x2DD4BF), 68, 70, TEAL_D));
                    g.fillRoundRect(24, 26, 44, 44, 14, 14);
                    g.setColor(Color.WHITE);
                    g.setFont(f(Font.BOLD, 24));
                    g.drawString("S", craft(g, "S", 24, 44), 58);
                    g.setFont(f(Font.BOLD, 20));
                    g.drawString("S-Link", 82, 47);
                    g.setColor(new Color(0x5EEAD4));
                    g.setFont(f(Font.BOLD, 10));
                    g.drawString("GÓC GIẢNG DẠY", 83, 63);
                    g.dispose();
                }

                int craft(Graphics2D g, String t, int x, int w) {
                    return x + (w - g.getFontMetrics().stringWidth(t)) / 2;
                }
            };
            brand.setOpaque(false);
            brand.setPreferredSize(new Dimension(0, 100));

            JPanel box = new JPanel();
            box.setOpaque(false);
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setBorder(new EmptyBorder(10, 0, 0, 0));
            for (int i = 0; i < names.length; i++) {
                Nav nv = new Nav(i, names[i]);
                navs.add(nv);
                box.add(nv);
                box.add(Box.createVerticalStrut(4));
            }

            JPanel user = new JPanel(new BorderLayout(12, 0)) {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    aa(g);
                    g.setColor(new Color(255, 255, 255, 20));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                    g.dispose();
                }
            };
            user.setOpaque(false);
            user.setBorder(new EmptyBorder(12, 14, 12, 14));
            user.add(new Avatar(initials(), 44), BorderLayout.WEST);
            JPanel tx = new JPanel(new GridLayout(2, 1));
            tx.setOpaque(false);
            JLabel n1 = new JLabel(hoTen), n2 = new JLabel(idGiaoVien + (chungChi.isEmpty() ? "" : "  |  " + chungChi));
            n1.setFont(f(Font.BOLD, 13));
            n1.setForeground(Color.WHITE);
            n2.setFont(f(0, 11));
            n2.setForeground(new Color(0x94A3B8));
            tx.add(n1);
            tx.add(n2);
            user.add(tx, BorderLayout.CENTER);
            JButton btnLogout = new JButton("Đăng xuất hệ thống");
            btnLogout.setFont(f(Font.BOLD, 12));
            btnLogout.setForeground(Color.BLUE);
            btnLogout.setBackground(RED); // Dùng màu đỏ hoặc màu tùy chỉnh
            btnLogout.setFocusPainted(false);
            btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnLogout.addActionListener(e -> {
                int r = JOptionPane.showOptionDialog(TeacherDashboardUI.this, 
                        "Bạn có muốn đăng xuất khỏi hệ thống?", "Xác nhận",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        new Object[]{"Đăng xuất", "Ở lại"}, "Ở lại");
                if (r == 0) {
                    dispose(); // Đóng cửa sổ TeacherDashboard hiện tại
                    new LoginForm().setVisible(true); // Mở lại LoginForm
                }
            });

            JPanel foot = new JPanel();
            foot.setOpaque(false);
            foot.setLayout(new BoxLayout(foot, BoxLayout.Y_AXIS));
            foot.setBorder(new EmptyBorder(0, 14, 16, 14));
            foot.add(user);
            foot.add(Box.createVerticalStrut(10));
            foot.add(btnLogout);
            btnLogout.setAlignmentX(Component.CENTER_ALIGNMENT);

            add(brand, BorderLayout.NORTH);
            add(box, BorderLayout.CENTER);
            add(foot, BorderLayout.SOUTH);
          
            pt = new Timer(15, e -> {
                float d = targetY - indY;
                if (Math.abs(d) < 0.5f) {
                    indY = targetY;
                    ((Timer) e.getSource()).stop();
                } else indY += d * 0.22f;
                repaint();
            });
        }

        int yOf(Nav n) {
            return SwingUtilities.convertPoint(n.getParent(), n.getLocation(), this).y;
        }

        void setActive(int i) {
            active = i;
            for (Nav nv : navs) nv.sel.to(nv.idx == i ? 1 : 0);
            targetY = yOf(navs.get(i));
            pt.start();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0x0F172A), 0, h, new Color(0x134E4A)));
            g.fillRect(0, 0, w, h);
            targetY = yOf(navs.get(active));
            if (!pt.isRunning()) indY = targetY;
            g.setColor(new Color(255, 255, 255, 34));
            g.fill(new RoundRectangle2D.Float(14, indY, w - 28, 48, 14, 14));
            g.setColor(new Color(0x2DD4BF));
            g.fill(new RoundRectangle2D.Float(14, indY + 12, 4, 24, 4, 4));
            g.dispose();
        }
    }

    /* ===================== TRANG ===================== */

    static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 20));
        p.setOpaque(false);
        return p;
    }

    static JPanel head(String t, String sub) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel a = new JLabel(t), b = new JLabel(sub);
        a.setFont(f(Font.BOLD, 26));
        a.setForeground(INK);
        b.setFont(f(0, 14));
        b.setForeground(MUTED);
        p.add(a);
        p.add(b);
        return p;
    }

    static JPanel listPage(String title, String sub, String cardTitle, DataTable t, JComponent footer) {
        JPanel p = page();
        p.add(head(title, sub), BorderLayout.NORTH);
        Card c = new Card(cardTitle, search(t));
        c.add(scroll(t), BorderLayout.CENTER);
        if (footer != null) c.add(footer, BorderLayout.SOUTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private static final String MY_CLASSES = "SELECT id_lop FROM Quanli WHERE id_giao_vien = ?";

    private JComponent overviewPage() {
        long soLop = num("SELECT COUNT(*) FROM Quanli WHERE id_giao_vien = ?", idGiaoVien);
        long soHs = num("SELECT COUNT(*) FROM Hoc_sinh WHERE id_giao_vien = ?", idGiaoVien);
        long sapToi = num("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                + "AND ngay_day >= CURDATE()", idGiaoVien, DAY);
        Object avgO = one("SELECT AVG(rating) FROM Danh_gia_va_phan_hoi WHERE id_lop IN (" + MY_CLASSES + ")",
                idGiaoVien);
        double avg = avgO instanceof Number ? ((Number) avgO).doubleValue() : -1;
        Object lg = one("SELECT tong_luong FROM Phieu_luong WHERE id_giao_vien = ? ORDER BY ngay_tra DESC LIMIT 1",
                idGiaoVien);

        JPanel p = page();
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        Banner b = new Banner("Chào mừng " + hoTen + "!",
                "Quản lý lớp học, lịch dạy và theo dõi kết quả học sinh của bạn.");
        b.setPreferredSize(new Dimension(100, 136));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 136));
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(100, 132));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        stats.add(new Stat("Lớp phụ trách", soLop, "%.0f lớp", "Mã GV: " + idGiaoVien, TEAL));
        stats.add(new Stat("Học sinh phụ trách", soHs, "%.0f học sinh", "Đang theo học", BLUE));
        stats.add(new Stat("Buổi dạy sắp tới", sapToi, "%.0f buổi", "Từ hôm nay trở đi", AMBER));
        stats.add(new Stat("Đánh giá trung bình", avg, "%.1f / 5",
                "Lương gần nhất: " + (lg instanceof Number ? vnd((Number) lg) : "chưa có"), VIOLET));
        b.setAlignmentX(0f);
        stats.setAlignmentX(0f);
        top.add(b);
        top.add(Box.createVerticalStrut(10));
        top.add(stats);
        p.add(top, BorderLayout.NORTH);

        DataTable t1 = new DataTable(new String[]{"Ngày dạy", "Ca", "Phòng", "Lớp", "Trạng thái"},
                "SELECT lh.ngay_day, lh.ca_hoc, lh.phong_hoc, COALESCE(l.ten_lop, lh.id_lop), lh.trang_thai_buoi "
                        + "FROM Lich_hoc lh LEFT JOIN Lop l ON lh.id_lop = l.id_lop "
                        + "WHERE lh.id_giao_vien = ? AND lh.ngay_day >= CURDATE() ORDER BY lh.ngay_day LIMIT 6",
                null, "Chưa có lịch dạy sắp tới", idGiaoVien);
        DataTable t2 = new DataTable(new String[]{"Học sinh", "Lớp", "Đánh Giá", "Nội dung"},
                "SELECT hs.ho_ten, l.ten_lop, d.rating, d.mo_ta FROM Danh_gia_va_phan_hoi d "
                        + "JOIN Hoc_sinh hs ON d.ma_sinh_vien = hs.ma_sinh_vien JOIN Lop l ON d.id_lop = l.id_lop "
                        + "WHERE d.id_lop IN (" + MY_CLASSES + ") ORDER BY d.ngay_gui DESC LIMIT 6",
                null, "Chưa có phản hồi nào", idGiaoVien);
        JPanel grid = new JPanel(new GridLayout(1, 2, 14, 0));
        grid.setOpaque(false);
        Card c1 = new Card("Lịch dạy sắp tới", null), c2 = new Card("Phản hồi mới nhất từ học sinh", null);
        c1.add(scroll(t1), BorderLayout.CENTER);
        c2.add(scroll(t2), BorderLayout.CENTER);
        grid.add(c1);
        grid.add(c2);
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private JComponent classPage() {
        return listPage("Lớp học tôi phụ trách", "Các lớp bạn đang được phân công", "Danh sách lớp",
                new DataTable(new String[]{"Mã Lớp", "Tên Lớp", "Khóa Học", "Trình Độ", "Niên Khóa", "Sĩ Số"},
                        "SELECT l.id_lop, l.ten_lop, k.ten_khoa_hoc, k.trinh_do, l.nien_khoa, l.si_so "
                                + "FROM Quanli q JOIN Lop l ON q.id_lop = l.id_lop "
                                + "LEFT JOIN Khoa_hoc k ON l.ma_khoa_hoc = k.ma_khoa_hoc "
                                + "WHERE q.id_giao_vien = ? ORDER BY l.id_lop",
                        null, "Bạn chưa được phân công lớp nào", idGiaoVien), null);
    }

    private JComponent studentPage() {
        return listPage("Học sinh của tôi", "Học sinh do bạn phụ trách", "Danh sách học sinh",
                new DataTable(new String[]{"Mã SV", "Họ Tên", "Ngày Sinh", "Trình Độ", "Số Điện Thoại"},
                        "SELECT ma_sinh_vien, ho_ten, ngay_sinh, trinh_do, so_dien_thoai FROM Hoc_sinh "
                                + "WHERE id_giao_vien = ? ORDER BY ma_sinh_vien",
                        null, "Chưa có học sinh nào được phân công cho bạn", idGiaoVien), null);
    }

    private JComponent schedulePage() {
        DataTable t = new DataTable(
                new String[]{"Mã Lịch", "Lớp", "Ngày Dạy", "Ca Học", "Phòng", "Nội Dung", "Trạng Thái"},
                "SELECT lh.ma_lich_hoc, COALESCE(l.ten_lop, lh.id_lop), lh.ngay_day, lh.ca_hoc, lh.phong_hoc, "
                        + "lh.noi_dung, lh.trang_thai_buoi FROM Lich_hoc lh LEFT JOIN Lop l ON lh.id_lop = l.id_lop "
                        + "WHERE lh.id_giao_vien = ? ORDER BY lh.ngay_day",
                null, "Chưa có lịch dạy nào", idGiaoVien);
        Btn off = new Btn("Báo nghỉ buổi này", RED), on = new Btn("Khôi phục buổi dạy", GREEN);
        off.addActionListener(e -> changeSession(t, OFF));
        on.addActionListener(e -> changeSession(t, DAY));
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        foot.setOpaque(false);
        JLabel hint = new JLabel("Chọn một buổi trong bảng. Chỉ đổi được buổi từ hôm nay trở đi.");
        hint.setFont(f(0, 12));
        hint.setForeground(MUTED);
        foot.add(hint);
        foot.add(off);
        foot.add(on);
        SwingUtilities.invokeLater(() -> {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < t.getRowCount(); i++) {
                Object v = t.getValueAt(i, 2);
                if (v instanceof Timestamp && !((Timestamp) v).toLocalDateTime().toLocalDate().isBefore(today)) {
                    t.scrollRectToVisible(t.getCellRect(Math.min(t.getRowCount() - 1, i + 8), 0, true));
                    t.scrollRectToVisible(t.getCellRect(i, 0, true));
                    break;
                }
            }
        });
        return listPage("Lịch dạy & Trạng thái buổi học", "Báo nghỉ hoặc khôi phục buổi dạy của bạn",
                "Chi tiết các buổi dạy", t, foot);
    }

    private void changeSession(DataTable t, String newStatus) {
        int vr = t.getSelectedRow();
        if (vr < 0) {
            toast("Hãy chọn một buổi học trong bảng trước", AMBER);
            return;
        }
        int r = t.convertRowIndexToModel(vr);
        String ma = s(t.m.getValueAt(r, 0)), lop = s(t.m.getValueAt(r, 1)), cur = s(t.m.getValueAt(r, 6));
        Object ngayO = t.m.getValueAt(r, 2);
        if (cur.equals(newStatus)) {
            toast("Buổi học này đã ở trạng thái \"" + cur + "\" rồi", AMBER);
            return;
        }
        if (ngayO instanceof Timestamp
                && ((Timestamp) ngayO).toLocalDateTime().toLocalDate().isBefore(LocalDate.now())) {
            toast("Không thể thay đổi buổi học đã diễn ra", RED);
            return;
        }
        String ngay = ngayO instanceof Timestamp ? when((Timestamp) ngayO) : s(ngayO);
        int ok = JOptionPane.showConfirmDialog(this, "Chuyển buổi " + ma + " (lớp " + lop + ", " + ngay
                + ")\nsang trạng thái: " + newStatus + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        int n = update("UPDATE Lich_hoc SET trang_thai_buoi = ? WHERE ma_lich_hoc = ? AND id_giao_vien = ?",
                newStatus, ma, idGiaoVien);
        if (n > 0) {
            t.reload();
            for (int i = 0; i < t.getRowCount(); i++)
                if (ma.equals(s(t.getValueAt(i, 0)))) {
                    t.setRowSelectionInterval(i, i);
                    break;
                }
            toast(newStatus.equals(OFF) ? "Đã báo nghỉ buổi " + ma : "Đã khôi phục buổi dạy " + ma,
                    newStatus.equals(OFF) ? RED : GREEN);
        } else if (n == 0) toast("Không tìm thấy buổi học thuộc quyền của bạn", RED);
    }

    private JComponent examPage() {
        DataTable t = new DataTable(new String[]{"Mã Bài Làm", "Học Sinh", "Trình Độ", "Mã Đề", "Đọc Hiểu",
                "Từ Vựng", "Nghe", "Tổng Điểm", "Kết Quả"},
                "SELECT k.ma_bai_lam, hs.ho_ten, k.trinh_do, k.ma_de_thi, k.diem_doc_hieu, k.diem_tu_vung, "
                        + "k.diem_nghe, k.tong_diem, k.tinh_trang FROM Ket_qua_thi k "
                        + "JOIN Hoc_sinh hs ON k.ma_sinh_vien = hs.ma_sinh_vien "
                        + "WHERE hs.id_giao_vien = ? ORDER BY k.ma_bai_lam",
                r -> {
                    Object[] o = r.clone();
                    if (o[8] == null || s(o[8]).isEmpty())
                        o[8] = result(s(o[2]), n(o[4]), n(o[5]), n(o[6]), n(o[7]));
                    return o;
                }, "Chưa có kết quả thi nào", idGiaoVien);
        int pass = 0;
        for (int i = 0; i < t.m.getRowCount(); i++) if ("ĐẠT".equals(t.m.getValueAt(i, 8))) pass++;
        int tot = t.m.getRowCount();
        String title = "Bài làm của học sinh" + (tot == 0 ? "" : "  •  " + pass + "/" + tot + " đạt ("
                + Math.round(100.0 * pass / tot) + "%)");
        return listPage("Kết quả thi của học sinh", "Bài làm của các học sinh do bạn phụ trách", title, t, null);
    }

    private JComponent feedbackPage() {
        Object a = one("SELECT AVG(rating) FROM Danh_gia_va_phan_hoi WHERE id_lop IN (" + MY_CLASSES + ")",
                idGiaoVien);
        String title = "Đánh giá từ học sinh" + (a instanceof Number
                ? String.format(VI, "  •  Trung bình %.1f / 5", ((Number) a).doubleValue()) : "");
        return listPage("Phản hồi & đánh giá của học sinh", "Góp ý từ các lớp bạn phụ trách", title,
                new DataTable(new String[]{"Mã", "Học Sinh", "Lớp", "Đánh Giá", "Nội Dung", "Ngày Gửi"},
                        "SELECT d.ma_danh_gia, hs.ho_ten, l.ten_lop, d.rating, d.mo_ta, d.ngay_gui "
                                + "FROM Danh_gia_va_phan_hoi d JOIN Hoc_sinh hs ON d.ma_sinh_vien = hs.ma_sinh_vien "
                                + "JOIN Lop l ON d.id_lop = l.id_lop WHERE d.id_lop IN (" + MY_CLASSES + ") "
                                + "ORDER BY d.ngay_gui DESC",
                        null, "Chưa có phản hồi nào từ học sinh", idGiaoVien), null);
    }

    private JComponent payslipPage() {
        long sum = num("SELECT COALESCE(SUM(tong_luong), 0) FROM Phieu_luong WHERE id_giao_vien = ?", idGiaoVien);
        return listPage("Phiếu lương của tôi", "Lịch sử nhận lương", "Lịch sử nhận lương  •  Tổng đã nhận: " + vnd(sum),
                new DataTable(new String[]{"Mã Phiếu", "Từ Ngày", "Đến Ngày", "Ngày Trả", "Tổng Lương"},
                        "SELECT ma_phieu_luong, tu_ngay, den_ngay, ngay_tra, tong_luong FROM Phieu_luong "
                                + "WHERE id_giao_vien = ? ORDER BY ngay_tra DESC",
                        null, "Chưa có phiếu lương nào", idGiaoVien), null);
    }

    private JComponent profilePage() {
        long soLop = num("SELECT COUNT(*) FROM Quanli WHERE id_giao_vien = ?", idGiaoVien);
        long soHs = num("SELECT COUNT(*) FROM Hoc_sinh WHERE id_giao_vien = ?", idGiaoVien);
        long daDay = num("SELECT COUNT(*) FROM Lich_hoc WHERE id_giao_vien = ? AND trang_thai_buoi = ? "
                + "AND ngay_day < NOW()", idGiaoVien, DAY);
        JPanel p = page();
        p.add(head("Thông tin cá nhân", "Hồ sơ giáo viên của bạn"), BorderLayout.NORTH);
        Card c = new Card("Hồ sơ giáo viên", null);
        JPanel id = new JPanel(new BorderLayout(20, 0));
        id.setOpaque(false);
        id.add(new Avatar(initials(), 84), BorderLayout.WEST);
        JPanel tx = new JPanel(new GridLayout(2, 1, 0, 2));
        tx.setOpaque(false);
        JLabel a = new JLabel(hoTen), b = new JLabel(idGiaoVien + "  •  " + chungChi);
        a.setFont(f(Font.BOLD, 24));
        a.setForeground(INK);
        b.setFont(f(0, 14));
        b.setForeground(MUTED);
        tx.add(a);
        tx.add(b);
        id.add(tx, BorderLayout.CENTER);
        id.setBorder(new EmptyBorder(0, 0, 22, 0));
        JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
        grid.setOpaque(false);
        String[][] kv = {{"Mã giáo viên", idGiaoVien}, {"Họ và tên", hoTen}, {"Số điện thoại", soDienThoai},
                {"Chứng chỉ", chungChi}, {"Quốc tịch", quocTich}, {"Số lớp phụ trách", String.valueOf(soLop)},
                {"Học sinh phụ trách", String.valueOf(soHs)}, {"Số buổi đã dạy", String.valueOf(daDay)}};
        for (String[] r : kv) grid.add(new Tile(r[0], r[1]));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(id, BorderLayout.NORTH);
        JPanel g2 = new JPanel(new BorderLayout());
        g2.setOpaque(false);
        g2.add(grid, BorderLayout.NORTH);
        wrap.add(g2, BorderLayout.CENTER);
        c.add(wrap, BorderLayout.CENTER);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("OptionPane.messageFont", f(0, 14));
        UIManager.put("OptionPane.buttonFont", f(Font.BOLD, 13));
        SwingUtilities.invokeLater(() -> new TeacherDashboardUI(args.length > 0 ? args[0] : "GV01").setVisible(true));
    }
}