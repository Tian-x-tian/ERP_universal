import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InitDb {
    public static void main(String[] args) throws Exception {
        String database = readSetting("INIT_DB_NAME", "erp_system");
        String url = readSetting("INIT_DB_URL",
                "jdbc:mysql://127.0.0.1:3306/?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8");
        String user = readSetting("INIT_DB_USER", "root");
        String password = readSetting("INIT_DB_PASSWORD", "");
        List<String> scriptPaths = resolveScriptPaths(args);

        System.out.println("Connecting to MySQL...");
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Creating " + database + " database if not exists...");
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            stmt.executeUpdate("USE `" + database + "`");

            for (String scriptPath : scriptPaths) {
                executeSqlScript(stmt, scriptPath);
            }
            System.out.println("Database initialization completed!");
        }
    }

    private static void executeSqlScript(Statement stmt, String scriptPath) throws Exception {
        System.out.println("Reading " + scriptPath + "...");
        String sqlContent = new String(Files.readAllBytes(Paths.get(scriptPath)), "UTF-8");
        String[] statements = sqlContent.split(";");
        System.out.println("Executing " + statements.length + " statements from " + scriptPath + "...");
        for (String statement : statements) {
            if (statement == null || statement.trim().isEmpty()) {
                continue;
            }
            try {
                stmt.execute(statement);
            } catch (Exception e) {
                String preview = statement.trim().replaceAll("\\s+", " ");
                System.err.println("Error executing: " + preview.substring(0, Math.min(80, preview.length())) + "... -> " + e.getMessage());
            }
        }
    }

    private static List<String> resolveScriptPaths(String[] args) {
        if (args != null && args.length > 0) {
            return Arrays.asList(args);
        }
        List<String> defaultPaths = new ArrayList<>();
        defaultPaths.add("erp-modules/erp-system/src/main/resources/sql/upgrade_system.sql");
        defaultPaths.add("erp-modules/erp-business/src/main/resources/sql/upgrade_business.sql");
        return defaultPaths;
    }

    private static String readSetting(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            return systemValue.trim();
        }
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return defaultValue;
    }
}
