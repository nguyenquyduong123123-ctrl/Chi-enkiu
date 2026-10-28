import java.sql.Connection;

import java.sql.DriverManager;



public class DBConnection {

    // Đã trỏ chính xác về database BTL

    private static final String URL = "jdbc:mysql://localhost:3306/BTL?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private static final String USER = "root";

    private static final String PASSWORD = ""; // Nhập mật khẩu MySQL của bạn nếu có



    public static Connection getConnection() {

        Connection conn = null;

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            conn = DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (Exception e) {

            System.err.println("Lỗi kết nối CSDL BTL: " + e.getMessage());

        }

        return conn;

    }

}