import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            System.getenv().getOrDefault("CSMS_DB_URL", "jdbc:mysql://localhost:3306/cloudstorage");
    private static final String USER =
            System.getenv().getOrDefault("CSMS_DB_USER", "root");
    private static final String PASSWORD =
            System.getenv().getOrDefault("CSMS_DB_PASS", "password");

    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver Not Found");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void closeConnection(Connection con) {
        if (con != null) {
            try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null;
        } catch (SQLException e) {
            return false;
        }
    }
}
