

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginForm extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnGoRegister;

    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_BG = new Color(248, 250, 252);

    public LoginForm() {
        setTitle("ĐĂNG NHẬP HỆ THỐNG TRUNG TÂM S-LINK");
        setSize(420, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_BG);
        mainPanel.setBorder(new EmptyBorder(30, 35, 30, 35));

        JLabel lblTitle = new JLabel("CỔNG THÔNG TIN S-LINK", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(COLOR_PRIMARY);

        JPanel formGrid = new JPanel(new GridLayout(4, 1, 0, 8));
        formGrid.setOpaque(false);
        formGrid.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel lblUser = new JLabel("Mã tài khoản (Mã SV / GV / NV):");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel lblPass = new JLabel("Mật khẩu / Số điện thoại:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        formGrid.add(lblUser);
        formGrid.add(txtUsername);
        formGrid.add(lblPass);
        formGrid.add(txtPassword);

        JPanel btnStack = new JPanel(new GridLayout(2, 1, 0, 10));
        btnStack.setOpaque(false);

        btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(COLOR_PRIMARY);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(0, 42));

        btnGoRegister = new JButton("Chưa có tài khoản? Đăng ký học viên mới");
        btnGoRegister.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnGoRegister.setForeground(COLOR_PRIMARY);
        btnGoRegister.setContentAreaFilled(false);
        btnGoRegister.setBorderPainted(false);
        btnGoRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnStack.add(btnLogin);
        btnStack.add(btnGoRegister);

        mainPanel.add(lblTitle, BorderLayout.NORTH);
        mainPanel.add(formGrid, BorderLayout.CENTER);
        mainPanel.add(btnStack, BorderLayout.SOUTH);

        add(mainPanel);

        btnLogin.addActionListener(e -> handleLogin());
        btnGoRegister.addActionListener(e -> {
            this.dispose();
            new RegisterForm().setVisible(true);
        });
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Mã tài khoản!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối CSDL MySQL!", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String sqlSV = "SELECT * FROM Hoc_sinh WHERE ma_sinh_vien = ?";
            PreparedStatement pstSV = conn.prepareStatement(sqlSV);
            pstSV.setString(1, username);
            ResultSet rsSV = pstSV.executeQuery();

            if (rsSV.next()) {
                String hoTen = rsSV.getString("ho_ten");
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công!\nXin chào học viên: " + hoTen);
                new StudentDashboardUI(username).setVisible(true);
                this.dispose();
                return;
            }

            JOptionPane.showMessageDialog(this, "Mã sinh viên không tồn tại trong CSDL MySQL!", "Lỗi Đăng nhập", JOptionPane.ERROR_MESSAGE);
            rsSV.close(); pstSV.close(); conn.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}