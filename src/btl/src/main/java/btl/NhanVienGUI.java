package btl;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NhanVienGUI extends JFrame {

    // Cấu hình thông tin kết nối MySQL
    private static final String DB_URL = "jdbc:mysql://localhost:3306/BTL";
    private static final String USER = "root";
    private static final String PASS = "Dung@0512";

    // Palette Màu
    private static final Color COLOR_PRIMARY = new Color(79, 70, 229);       // Indigo
    private static final Color COLOR_PRIMARY_HOVER = new Color(67, 56, 202);
    private static final Color COLOR_WARNING = new Color(217, 119, 6);        // Amber (Sửa)
    private static final Color COLOR_DANGER = new Color(225, 29, 72);         // Rose (Xóa)
    private static final Color COLOR_SIDEBAR = new Color(30, 41, 59);         // Dark Slate
    private static final Color COLOR_SIDEBAR_BTN = new Color(51, 65, 85);
    private static final Color COLOR_SIDEBAR_ACTIVE = new Color(79, 70, 229);
    private static final Color COLOR_BG = new Color(248, 250, 252);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);

    private Font fontTitle = new Font("Segoe UI", Font.BOLD, 18);
    private Font fontMain = new Font("Segoe UI", Font.PLAIN, 13);
    private Font fontBold = new Font("Segoe UI", Font.BOLD, 13);

    // Sidebar & CardLayout
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private RoundButton navBtnHD, navBtnLop, navBtnPL, navBtnTraCuu;

    // Components Tab Hóa Đơn
    private JTextField txtMaHD, txtMaNVHD, txtMaGD, txtKhoanThu, txtSearchHD;
    private JComboBox<String> cbHinhThuc;
    private JTable tableHD;
    private DefaultTableModel modelHD;
    private TableRowSorter<DefaultTableModel> sorterHD;

    // Components Tab Lớp Học
    private JTextField txtIDLop, txtMaKHLop, txtMaNVLop, txtNienKhoa, txtTenLop, txtSiSo, txtSearchLop;
    private JTable tableLop;
    private DefaultTableModel modelLop;
    private TableRowSorter<DefaultTableModel> sorterLop;

    // Components Tab Phiếu Lương
    private JTextField txtMaPL, txtIDGVPL, txtMaNVPL, txtTuNgay, txtDenNgay, txtTongLuong, txtSearchPL;
    private JTable tablePL;
    private DefaultTableModel modelPL;
    private TableRowSorter<DefaultTableModel> sorterPL;

    // Components Tab Tra cứu (Khóa học & Học viên)
    private JTextField txtSearchKH, txtSearchHV;
    private JTable tableKH, tableHV;
    private DefaultTableModel modelKH, modelHV;

    public NhanVienGUI() {
        setTitle("Hệ Thống Quản Lý Dành Cho Nhân Viên");
        setSize(1150, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout());

        // Setup Sidebar
        add(createSidebar(), BorderLayout.WEST);

        // Setup Main Content Panel
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(COLOR_BG);
        mainContentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        mainContentPanel.add(createTabHoaDon(), "TabHD");
        mainContentPanel.add(createTabLop(), "TabLop");
        mainContentPanel.add(createTabPhieuLuong(), "TabPL");
        mainContentPanel.add(createTabTraCuu(), "TabTraCuu");

        add(mainContentPanel, BorderLayout.CENTER);

        // Load dữ liệu ban đầu
        loadDataHoaDon();
        loadDataLop();
        loadDataPhieuLuong();
        loadDataKhoaHoc();
        loadDataHocVien();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    // =========================================================================
    // THANH CÔNG CỤ BÊN TRÁI (SIDEBAR)
    // =========================================================================
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(COLOR_SIDEBAR);
        sidebar.setLayout(new BorderLayout());

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(new EmptyBorder(25, 20, 25, 20));
        JLabel lblTitle = new JLabel("WORKFLOW");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Hệ thống quản lý nhân viên");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(148, 163, 184));

        pnlHeader.add(lblTitle, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        sidebar.add(pnlHeader, BorderLayout.NORTH);

        JPanel pnlMenu = new JPanel();
        pnlMenu.setOpaque(false);
        pnlMenu.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));
        pnlMenu.setBorder(new EmptyBorder(10, 15, 10, 15));

        navBtnHD = createNavButton("1. Quản Lý Hóa Đơn");
        navBtnLop = createNavButton("2. Quản Lý Lớp Học");
        navBtnPL = createNavButton("3. Lập Phiếu Lương");
        navBtnTraCuu = createNavButton("4. Tra Cứu Thông Tin");

        pnlMenu.add(navBtnHD);
        pnlMenu.add(navBtnLop);
        pnlMenu.add(navBtnPL);
        pnlMenu.add(navBtnTraCuu);

        sidebar.add(pnlMenu, BorderLayout.CENTER);

        setNavActive(navBtnHD);
        navBtnHD.addActionListener(e -> { cardLayout.show(mainContentPanel, "TabHD"); setNavActive(navBtnHD); });
        navBtnLop.addActionListener(e -> { cardLayout.show(mainContentPanel, "TabLop"); setNavActive(navBtnLop); });
        navBtnPL.addActionListener(e -> { cardLayout.show(mainContentPanel, "TabPL"); setNavActive(navBtnPL); });
        navBtnTraCuu.addActionListener(e -> { cardLayout.show(mainContentPanel, "TabTraCuu"); setNavActive(navBtnTraCuu); });

        return sidebar;
    }

    private RoundButton createNavButton(String text) {
        RoundButton btn = new RoundButton(text, 12);
        btn.setPreferredSize(new Dimension(210, 42));
        btn.setFont(fontBold);
        btn.setForeground(new Color(226, 232, 240));
        btn.setBackground(COLOR_SIDEBAR_BTN);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 15, 0, 0));
        return btn;
    }

    private void setNavActive(RoundButton activeBtn) {
        RoundButton[] btns = {navBtnHD, navBtnLop, navBtnPL, navBtnTraCuu};
        for (RoundButton b : btns) {
            if (b == activeBtn) {
                b.setBackground(COLOR_SIDEBAR_ACTIVE);
                b.setForeground(Color.WHITE);
            } else {
                b.setBackground(COLOR_SIDEBAR_BTN);
                b.setForeground(new Color(226, 232, 240));
            }
        }
    }

    // =========================================================================
    // TAB 1: QUẢN LÝ HÓA ĐƠN (CRUD FULL)
    // =========================================================================
    private JPanel createTabHoaDon() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        // Form Card
        RoundPanel pnlForm = new RoundPanel(16);
        pnlForm.setBackground(COLOR_CARD);
        pnlForm.setLayout(new BorderLayout(15, 15));
        pnlForm.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("Quản Lý Hóa Đơn / Thu Học Phí");
        lblTitle.setFont(fontTitle);
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        pnlForm.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlFields = new JPanel(new GridLayout(3, 4, 15, 12));
        pnlFields.setOpaque(false);

        txtMaHD = new RoundTextField(10);
        txtMaNVHD = new RoundTextField(10);
        txtMaGD = new RoundTextField(10);
        txtKhoanThu = new RoundTextField(10);
        cbHinhThuc = new JComboBox<>(new String[]{"Tien mat", "Chuyen khoan", "The"});
        cbHinhThuc.setFont(fontMain);
        cbHinhThuc.setBackground(Color.WHITE);

        pnlFields.add(createFieldGroup("Mã Hóa Đơn:", txtMaHD));
        pnlFields.add(createFieldGroup("Mã NV Lập:", txtMaNVHD));
        pnlFields.add(createFieldGroup("Mã Ghi Danh:", txtMaGD));
        pnlFields.add(createFieldGroup("Khoản Thu (VNĐ):", txtKhoanThu));
        pnlFields.add(createFieldGroup("Hình Thức:", cbHinhThuc));

        // Nút bấm tác vụ (Thêm, Sửa, Xóa)
        RoundButton btnThem = new RoundButton("Thêm Mới", 10);
        btnThem.setFont(fontBold); btnThem.setBackground(COLOR_PRIMARY); btnThem.setForeground(Color.WHITE);

        RoundButton btnSua = new RoundButton("Cập Nhật", 10);
        btnSua.setFont(fontBold); btnSua.setBackground(COLOR_WARNING); btnSua.setForeground(Color.WHITE);

        RoundButton btnXoa = new RoundButton("Xóa HD", 10);
        btnXoa.setFont(fontBold); btnXoa.setBackground(COLOR_DANGER); btnXoa.setForeground(Color.WHITE);

        JPanel pnlAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlAction.setOpaque(false);
        pnlAction.add(btnThem);
        pnlAction.add(btnSua);
        pnlAction.add(btnXoa);

        pnlFields.add(new JLabel());
        pnlFields.add(pnlAction);

        pnlForm.add(pnlFields, BorderLayout.CENTER);
        panel.add(pnlForm, BorderLayout.NORTH);

        // Bảng dữ liệu + Tìm kiếm
        modelHD = new DefaultTableModel(new String[]{"Mã HD", "Mã NV", "Mã Ghi Danh", "Khoản Thu", "Ngày Lập", "Hình Thức"}, 0);
        tableHD = createStyledTable(modelHD);
        sorterHD = new TableRowSorter<>(modelHD);
        tableHD.setRowSorter(sorterHD);

        txtSearchHD = new RoundTextField(10);
        setupSearchFilter(txtSearchHD, sorterHD);

        panel.add(createTableCardWithSearch("Danh Sách Hóa Đơn", txtSearchHD, tableHD), BorderLayout.CENTER);

        // Sự kiện Click chọn dòng trên bảng
        tableHD.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tableHD.getSelectedRow();
                if (row >= 0) {
                    row = tableHD.convertRowIndexToModel(row);
                    txtMaHD.setText(modelHD.getValueAt(row, 0).toString());
                    txtMaNVHD.setText(modelHD.getValueAt(row, 1).toString());
                    txtMaGD.setText(modelHD.getValueAt(row, 2).toString());
                    txtKhoanThu.setText(modelHD.getValueAt(row, 3).toString());
                    cbHinhThuc.setSelectedItem(modelHD.getValueAt(row, 5).toString());
                }
            }
        });

        // Xử lý Sự kiện CRUD
        btnThem.addActionListener(e -> {
            String sql = "INSERT INTO hoa_don (ma_hoa_don, ma_nhan_vien, ma_ghi_danh, khoan_thu, ngay_lap, hinh_thuc) VALUES (?, ?, ?, ?, ?, ?)";
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, txtMaHD.getText());
                pstmt.setString(2, txtMaNVHD.getText());
                pstmt.setString(3, txtMaGD.getText());
                pstmt.setInt(4, Integer.parseInt(txtKhoanThu.getText()));
                pstmt.setString(5, now);
                pstmt.setString(6, cbHinhThuc.getSelectedItem().toString());
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Đã thêm hóa đơn thành công!");
                loadDataHoaDon();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSua.addActionListener(e -> {
            String sql = "UPDATE hoa_don SET ma_nhan_vien=?, ma_ghi_danh=?, khoan_thu=?, hinh_thuc=? WHERE ma_hoa_don=?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, txtMaNVHD.getText());
                pstmt.setString(2, txtMaGD.getText());
                pstmt.setInt(3, Integer.parseInt(txtKhoanThu.getText()));
                pstmt.setString(4, cbHinhThuc.getSelectedItem().toString());
                pstmt.setString(5, txtMaHD.getText());
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Cập nhật hóa đơn thành công!");
                    loadDataHoaDon();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy Mã HD để cập nhật!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnXoa.addActionListener(e -> {
            if (txtMaHD.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc nhập Mã HD cần xóa!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa hóa đơn này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String sql = "DELETE FROM hoa_don WHERE ma_hoa_don=?";
                try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, txtMaHD.getText());
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Đã xóa hóa đơn!");
                    loadDataHoaDon();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return panel;
    }

    private void loadDataHoaDon() {
        modelHD.setRowCount(0);
        String sql = "SELECT ma_hoa_don, ma_nhan_vien, ma_ghi_danh, khoan_thu, ngay_lap, hinh_thuc FROM hoa_don";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                modelHD.addRow(new Object[]{
                        rs.getString("ma_hoa_don"), rs.getString("ma_nhan_vien"),
                        rs.getString("ma_ghi_danh"), rs.getInt("khoan_thu"),
                        rs.getTimestamp("ngay_lap"), rs.getString("hinh_thuc")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // =========================================================================
    // TAB 2: QUẢN LÝ LỚP HỌC (CRUD FULL)
    // =========================================================================
    private JPanel createTabLop() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        RoundPanel pnlForm = new RoundPanel(16);
        pnlForm.setBackground(COLOR_CARD);
        pnlForm.setLayout(new BorderLayout(15, 15));
        pnlForm.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("Quản Lý Thông Tin Lớp Học");
        lblTitle.setFont(fontTitle);
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        pnlForm.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlFields = new JPanel(new GridLayout(3, 4, 15, 12));
        pnlFields.setOpaque(false);

        txtIDLop = new RoundTextField(10);
        txtMaKHLop = new RoundTextField(10);
        txtMaNVLop = new RoundTextField(10);
        txtTenLop = new RoundTextField(10);
        txtNienKhoa = new RoundTextField(10);
        txtSiSo = new RoundTextField(10);

        pnlFields.add(createFieldGroup("ID Lớp:", txtIDLop));
        pnlFields.add(createFieldGroup("Mã Khóa Học:", txtMaKHLop));
        pnlFields.add(createFieldGroup("Mã NV QL:", txtMaNVLop));
        pnlFields.add(createFieldGroup("Tên Lớp:", txtTenLop));
        pnlFields.add(createFieldGroup("Niên Khóa:", txtNienKhoa));
        pnlFields.add(createFieldGroup("Sĩ Số:", txtSiSo));

        RoundButton btnThem = new RoundButton("Thêm Lớp", 10);
        btnThem.setFont(fontBold); btnThem.setBackground(COLOR_PRIMARY); btnThem.setForeground(Color.WHITE);

        RoundButton btnSua = new RoundButton("Cập Nhật", 10);
        btnSua.setFont(fontBold); btnSua.setBackground(COLOR_WARNING); btnSua.setForeground(Color.WHITE);

        RoundButton btnXoa = new RoundButton("Xóa Lớp", 10);
        btnXoa.setFont(fontBold); btnXoa.setBackground(COLOR_DANGER); btnXoa.setForeground(Color.WHITE);

        JPanel pnlAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlAction.setOpaque(false);
        pnlAction.add(btnThem);
        pnlAction.add(btnSua);
        pnlAction.add(btnXoa);

        pnlFields.add(new JLabel());
        pnlFields.add(pnlAction);

        pnlForm.add(pnlFields, BorderLayout.CENTER);
        panel.add(pnlForm, BorderLayout.NORTH);

        modelLop = new DefaultTableModel(new String[]{"ID Lớp", "Mã Khóa Học", "Mã NV QL", "Niên Khóa", "Tên Lớp", "Sĩ Số"}, 0);
        tableLop = createStyledTable(modelLop);
        sorterLop = new TableRowSorter<>(modelLop);
        tableLop.setRowSorter(sorterLop);

        txtSearchLop = new RoundTextField(10);
        setupSearchFilter(txtSearchLop, sorterLop);

        panel.add(createTableCardWithSearch("Danh Sách Lớp Học", txtSearchLop, tableLop), BorderLayout.CENTER);

        tableLop.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tableLop.getSelectedRow();
                if (row >= 0) {
                    row = tableLop.convertRowIndexToModel(row);
                    txtIDLop.setText(modelLop.getValueAt(row, 0).toString());
                    txtMaKHLop.setText(modelLop.getValueAt(row, 1).toString());
                    txtMaNVLop.setText(modelLop.getValueAt(row, 2).toString());
                    txtNienKhoa.setText(modelLop.getValueAt(row, 3).toString());
                    txtTenLop.setText(modelLop.getValueAt(row, 4).toString());
                    txtSiSo.setText(modelLop.getValueAt(row, 5).toString());
                }
            }
        });

        btnThem.addActionListener(e -> {
            String sql = "INSERT INTO Lop (id_lop, ma_khoa_hoc, ma_nhan_vien, nien_khoa, ten_lop, si_so) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, txtIDLop.getText());
                pstmt.setString(2, txtMaKHLop.getText());
                pstmt.setString(3, txtMaNVLop.getText());
                pstmt.setString(4, txtNienKhoa.getText());
                pstmt.setString(5, txtTenLop.getText());
                pstmt.setInt(6, Integer.parseInt(txtSiSo.getText()));
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Đã thêm lớp học thành công!");
                loadDataLop();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSua.addActionListener(e -> {
            String sql = "UPDATE Lop SET ma_khoa_hoc=?, ma_nhan_vien=?, nien_khoa=?, ten_lop=?, si_so=? WHERE id_lop=?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, txtMaKHLop.getText());
                pstmt.setString(2, txtMaNVLop.getText());
                pstmt.setString(3, txtNienKhoa.getText());
                pstmt.setString(4, txtTenLop.getText());
                pstmt.setInt(5, Integer.parseInt(txtSiSo.getText()));
                pstmt.setString(6, txtIDLop.getText());
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Cập nhật lớp học thành công!");
                    loadDataLop();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy ID Lớp để cập nhật!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnXoa.addActionListener(e -> {
            if (txtIDLop.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc nhập ID Lớp cần xóa!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa lớp học này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String sql = "DELETE FROM Lop WHERE id_lop=?";
                try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, txtIDLop.getText());
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Đã xóa lớp học!");
                    loadDataLop();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return panel;
    }

    private void loadDataLop() {
        modelLop.setRowCount(0);
        String sql = "SELECT id_lop, ma_khoa_hoc, ma_nhan_vien, nien_khoa, ten_lop, si_so FROM Lop";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                modelLop.addRow(new Object[]{
                        rs.getString("id_lop"), rs.getString("ma_khoa_hoc"),
                        rs.getString("ma_nhan_vien"), rs.getString("nien_khoa"),
                        rs.getString("ten_lop"), rs.getInt("si_so")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // =========================================================================
    // TAB 3: LẬP PHIẾU LƯƠNG (CRUD FULL)
    // =========================================================================
    private JPanel createTabPhieuLuong() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        RoundPanel pnlForm = new RoundPanel(16);
        pnlForm.setBackground(COLOR_CARD);
        pnlForm.setLayout(new BorderLayout(15, 15));
        pnlForm.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("Lập & Quản Lý Phiếu Lương Giáo Viên");
        lblTitle.setFont(fontTitle);
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        pnlForm.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlFields = new JPanel(new GridLayout(3, 4, 15, 12));
        pnlFields.setOpaque(false);

        txtMaPL = new RoundTextField(10);
        txtIDGVPL = new RoundTextField(10);
        txtMaNVPL = new RoundTextField(10);
        txtTongLuong = new RoundTextField(10);
        txtTuNgay = new RoundTextField(10);
        txtDenNgay = new RoundTextField(10);

        pnlFields.add(createFieldGroup("Mã Phiếu Lương:", txtMaPL));
        pnlFields.add(createFieldGroup("ID Giáo Viên:", txtIDGVPL));
        pnlFields.add(createFieldGroup("Mã NV Lập:", txtMaNVPL));
        pnlFields.add(createFieldGroup("Tổng Lương (VNĐ):", txtTongLuong));
        pnlFields.add(createFieldGroup("Từ Ngày (yyyy-MM-dd):", txtTuNgay));
        pnlFields.add(createFieldGroup("Đến Ngày (yyyy-MM-dd):", txtDenNgay));

        RoundButton btnThem = new RoundButton("Lập Phiếu", 10);
        btnThem.setFont(fontBold); btnThem.setBackground(COLOR_PRIMARY); btnThem.setForeground(Color.WHITE);

        RoundButton btnSua = new RoundButton("Cập Nhật", 10);
        btnSua.setFont(fontBold); btnSua.setBackground(COLOR_WARNING); btnSua.setForeground(Color.WHITE);

        RoundButton btnXoa = new RoundButton("Xóa Phiếu", 10);
        btnXoa.setFont(fontBold); btnXoa.setBackground(COLOR_DANGER); btnXoa.setForeground(Color.WHITE);

        JPanel pnlAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlAction.setOpaque(false);
        pnlAction.add(btnThem);
        pnlAction.add(btnSua);
        pnlAction.add(btnXoa);

        pnlFields.add(new JLabel());
        pnlFields.add(pnlAction);

        pnlForm.add(pnlFields, BorderLayout.CENTER);
        panel.add(pnlForm, BorderLayout.NORTH);

        modelPL = new DefaultTableModel(new String[]{"Mã PL", "ID GV", "Mã NV", "Từ Ngày", "Đến Ngày", "Ngày Trả", "Tổng Lương"}, 0);
        tablePL = createStyledTable(modelPL);
        sorterPL = new TableRowSorter<>(modelPL);
        tablePL.setRowSorter(sorterPL);

        txtSearchPL = new RoundTextField(10);
        setupSearchFilter(txtSearchPL, sorterPL);

        panel.add(createTableCardWithSearch("Danh Sách Phiếu Lương", txtSearchPL, tablePL), BorderLayout.CENTER);

        tablePL.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tablePL.getSelectedRow();
                if (row >= 0) {
                    row = tablePL.convertRowIndexToModel(row);
                    txtMaPL.setText(modelPL.getValueAt(row, 0).toString());
                    txtIDGVPL.setText(modelPL.getValueAt(row, 1).toString());
                    txtMaNVPL.setText(modelPL.getValueAt(row, 2).toString());
                    txtTuNgay.setText(modelPL.getValueAt(row, 3).toString().split(" ")[0]);
                    txtDenNgay.setText(modelPL.getValueAt(row, 4).toString().split(" ")[0]);
                    txtTongLuong.setText(modelPL.getValueAt(row, 6).toString());
                }
            }
        });

        btnThem.addActionListener(e -> {
            String sql = "INSERT INTO Phieu_luong (ma_phieu_luong, id_giao_vien, ma_nhan_vien, tu_ngay, den_ngay, ngay_tra, tong_luong) VALUES (?, ?, ?, ?, ?, ?, ?)";
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, txtMaPL.getText());
                pstmt.setString(2, txtIDGVPL.getText());
                pstmt.setString(3, txtMaNVPL.getText());
                pstmt.setString(4, txtTuNgay.getText() + " 00:00:00");
                pstmt.setString(5, txtDenNgay.getText() + " 23:59:59");
                pstmt.setString(6, now);
                pstmt.setInt(7, Integer.parseInt(txtTongLuong.getText()));
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Đã lập phiếu lương thành công!");
                loadDataPhieuLuong();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSua.addActionListener(e -> {
            String sql = "UPDATE Phieu_luong SET id_giao_vien=?, ma_nhan_vien=?, tu_ngay=?, den_ngay=?, tong_luong=? WHERE ma_phieu_luong=?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, txtIDGVPL.getText());
                pstmt.setString(2, txtMaNVPL.getText());
                pstmt.setString(3, txtTuNgay.getText() + " 00:00:00");
                pstmt.setString(4, txtDenNgay.getText() + " 23:59:59");
                pstmt.setInt(5, Integer.parseInt(txtTongLuong.getText()));
                pstmt.setString(6, txtMaPL.getText());
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Cập nhật phiếu lương thành công!");
                    loadDataPhieuLuong();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy Mã PL để cập nhật!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnXoa.addActionListener(e -> {
            if (txtMaPL.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc nhập Mã PL cần xóa!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa phiếu lương này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String sql = "DELETE FROM Phieu_luong WHERE ma_phieu_luong=?";
                try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, txtMaPL.getText());
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Đã xóa phiếu lương!");
                    loadDataPhieuLuong();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return panel;
    }

    private void loadDataPhieuLuong() {
        modelPL.setRowCount(0);
        String sql = "SELECT ma_phieu_luong, id_giao_vien, ma_nhan_vien, tu_ngay, den_ngay, ngay_tra, tong_luong FROM Phieu_luong";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                modelPL.addRow(new Object[]{
                        rs.getString("ma_phieu_luong"), rs.getString("id_giao_vien"),
                        rs.getString("ma_nhan_vien"), rs.getTimestamp("tu_ngay"),
                        rs.getTimestamp("den_ngay"), rs.getTimestamp("ngay_tra"),
                        rs.getInt("tong_luong")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // =========================================================================
    // TAB 4: TRA CỨU KHÓA HỌC & HỌC VIÊN (SELECT TRA CỨU TƯ VẤN)
    // =========================================================================
    private JPanel createTabTraCuu() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
        panel.setOpaque(false);

        // Bảng Khóa Học
        modelKH = new DefaultTableModel(new String[]{"Mã KH", "Tên Khóa Học", "Học Phí (VNĐ)"}, 0);
        tableKH = createStyledTable(modelKH);
        TableRowSorter<DefaultTableModel> sorterKH = new TableRowSorter<>(modelKH);
        tableKH.setRowSorter(sorterKH);
        txtSearchKH = new RoundTextField(10);
        setupSearchFilter(txtSearchKH, sorterKH);

        // Bảng Học Viên
        modelHV = new DefaultTableModel(new String[]{"Mã HV", "Họ Tên", "SĐT", "Email"}, 0);
        tableHV = createStyledTable(modelHV);
        TableRowSorter<DefaultTableModel> sorterHV = new TableRowSorter<>(modelHV);
        tableHV.setRowSorter(sorterHV);
        txtSearchHV = new RoundTextField(10);
        setupSearchFilter(txtSearchHV, sorterHV);

        panel.add(createTableCardWithSearch("Tra Cứu Khóa Học & Học Phí", txtSearchKH, tableKH));
        panel.add(createTableCardWithSearch("Tra Cứu Thông Tin Học Viên", txtSearchHV, tableHV));

        return panel;
    }

    private void loadDataKhoaHoc() {
        modelKH.setRowCount(0);
        String sql = "SELECT ma_khoa_hoc, ten_khoa_hoc, hoc_phi FROM khoa_hoc";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                modelKH.addRow(new Object[]{ rs.getString("ma_khoa_hoc"), rs.getString("ten_khoa_hoc"), rs.getInt("hoc_phi") });
            }
        } catch (SQLException ex) { /* Nếu chưa có bảng sẽ bỏ qua lỗi */ }
    }

    private void loadDataHocVien() {
        modelHV.setRowCount(0);
        String sql = "SELECT ma_hoc_vien, ho_ten, so_dien_thoai, email FROM hoc_vien";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                modelHV.addRow(new Object[]{ rs.getString("ma_hoc_vien"), rs.getString("ho_ten"), rs.getString("so_dien_thoai"), rs.getString("email") });
            }
        } catch (SQLException ex) { /* Nếu chưa có bảng sẽ bỏ qua lỗi */ }
    }

    // =========================================================================
    // HELPER UI METHODS & CUSTOM COMPONENTS
    // =========================================================================

    private void setupSearchFilter(JTextField txtSearch, TableRowSorter<DefaultTableModel> sorter) {
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = txtSearch.getText();
                if (text.trim().length() == 0) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });
    }

    private JPanel createFieldGroup(String labelText, JComponent inputComponent) {
        JPanel pnl = new JPanel(new BorderLayout(0, 5));
        pnl.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(fontMain);
        label.setForeground(COLOR_TEXT_MUTED);
        pnl.add(label, BorderLayout.NORTH);
        pnl.add(inputComponent, BorderLayout.CENTER);
        return pnl;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(fontMain);
        table.setRowHeight(32);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(new Color(238, 242, 255));
        table.setSelectionForeground(COLOR_PRIMARY);
        table.setShowVerticalLines(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(fontBold);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(COLOR_TEXT_MAIN);
        header.setPreferredSize(new Dimension(0, 36));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        return table;
    }

    private RoundPanel createTableCardWithSearch(String title, JTextField txtSearch, JTable table) {
        RoundPanel card = new RoundPanel(16);
        card.setBackground(COLOR_CARD);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(12, 15, 12, 15));

        JPanel pnlHeader = new JPanel(new BorderLayout(10, 0));
        pnlHeader.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(fontBold);
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JPanel pnlSearch = new JPanel(new BorderLayout(5, 0));
        pnlSearch.setOpaque(false);
        JLabel lblFind = new JLabel("Tìm kiếm:");
        lblFind.setFont(fontMain);
        lblFind.setForeground(COLOR_TEXT_MUTED);
        txtSearch.setPreferredSize(new Dimension(180, 30));
        pnlSearch.add(lblFind, BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);

        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(pnlSearch, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        card.add(pnlHeader, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    // Class Custom Vẽ Giao Diện
    class RoundPanel extends JPanel {
        private int cornerRadius;
        public RoundPanel(int radius) { super(); this.cornerRadius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
            g2.dispose();
        }
    }

    class RoundButton extends JButton {
        private int cornerRadius;
        public RoundButton(String label, int radius) {
            super(label); this.cornerRadius = radius;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if (getBackground().equals(COLOR_PRIMARY)) setBackground(COLOR_PRIMARY_HOVER); }
                @Override public void mouseExited(MouseEvent e) { if (getBackground().equals(COLOR_PRIMARY_HOVER)) setBackground(COLOR_PRIMARY); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class RoundTextField extends JTextField {
        private int cornerRadius;
        public RoundTextField(int radius) {
            this.cornerRadius = radius; setOpaque(false); setFont(fontMain);
            setBorder(new EmptyBorder(6, 10, 6, 10));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
            g2.setColor(new Color(203, 213, 225));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NhanVienGUI().setVisible(true));
    }
}