package ConnectDB;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionOracle {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = ConnectionOracle.class
                .getResourceAsStream("/Resources/db.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("Không tìm thấy db.properties");
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc db.properties: " + e);
        }
    }

    public static Connection getOracleConnection() throws ClassNotFoundException, SQLException {
        String host = props.getProperty("db.host", "localhost");
        String port = props.getProperty("db.port", "1521");
        String sid  = props.getProperty("db.sid",  "orcl");
        String user = props.getProperty("db.username");
        String pass = props.getProperty("db.password");

        Class.forName("oracle.jdbc.OracleDriver");
        String url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
        return DriverManager.getConnection(url, user, pass);
    }
}
