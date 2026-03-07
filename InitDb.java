import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InitDb {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://192.168.0.22:3306/?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8";
        String user = "root";
        String password = "123456";

        System.out.println("Connecting to MySQL...");
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Creating erp_system database if not exists...");
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS erp_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            stmt.executeUpdate("USE erp_system");

            System.out.println("Reading init_system.sql...");
            String sqlContent = new String(Files.readAllBytes(Paths.get("erp-modules/erp-system/src/main/resources/sql/init_system.sql")), "UTF-8");
            
            String[] statements = sqlContent.split(";");
            System.out.println("Executing " + statements.length + " statements...");
            for (String statement : statements) {
                if (statement.trim().isEmpty()) continue;
                try {
                    stmt.execute(statement);
                } catch (Exception e) {
                    System.err.println("Error executing: " + statement.substring(0, Math.min(50, statement.length())) + "... -> " + e.getMessage());
                }
            }
            System.out.println("Database initialization completed!");
        }
    }
}
