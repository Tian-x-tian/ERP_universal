package com.erp.saas.control.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.Ordered;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaasControlSqlUpgradeRunnerTest {

    @Test
    void shouldExposeSafeDefaults() {
        SaasControlSqlUpgradeRunner runner = new SaasControlSqlUpgradeRunner(new TrackingDataSource());

        assertThat((String) ReflectionTestUtils.getField(runner, "upgradeLocation"))
                .isEqualTo("classpath:sql/upgrade/control/*.sql");
        assertThat((String) ReflectionTestUtils.getField(runner, "HISTORY_TABLE_NAME"))
                .isEqualTo("saas_sql_upgrade_log");
        assertThat(((Ordered) runner).getOrder()).isEqualTo(100);
    }

    @Test
    void shouldExecuteInFilenameOrderRecordChecksumAndSkipSameScript() throws Exception {
        TrackingDataSource dataSource = new TrackingDataSource();
        Resource b = script("20260801_02_b.sql", "SELECT 'marker-b';");
        Resource a = script("20260801_01_a.sql", "SELECT 'marker-a';");
        SaasControlSqlUpgradeRunner runner = runner(dataSource, b, a);

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(dataSource.executedScripts()).containsExactly("SELECT 'marker-a'", "SELECT 'marker-b'");
        assertThat(dataSource.checksum("20260801_01_a.sql")).isEqualTo(sha256("SELECT 'marker-a';"));
        assertThat(dataSource.checksum("20260801_02_b.sql")).isEqualTo(sha256("SELECT 'marker-b';"));
        assertThat(dataSource.status("20260801_01_a.sql")).isEqualTo("1");

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(dataSource.executedScripts()).hasSize(2);
        assertThat(dataSource.history()).hasSize(2);
    }

    @Test
    void shouldFailWhenExistingFilenameHasDifferentChecksum() throws Exception {
        TrackingDataSource dataSource = new TrackingDataSource();
        runner(dataSource, script("20260801_01_foundation.sql", "SELECT 1;"))
                .run(new DefaultApplicationArguments(new String[0]));

        SaasControlSqlUpgradeRunner changed = runner(dataSource,
                script("20260801_01_foundation.sql", "SELECT 2;"));

        assertThatThrownBy(() -> changed.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("checksum")
                .hasMessageContaining("20260801_01_foundation.sql");
    }

    @Test
    void shouldFailClosedWhenChecksumCannotBeCalculated() {
        TrackingDataSource dataSource = new TrackingDataSource();
        Resource unreadable = new AbstractResource() {
            @Override
            public String getDescription() {
                return "unreadable test script";
            }

            @Override
            public String getFilename() {
                return "20260801_01_unreadable.sql";
            }

            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public boolean isReadable() {
                return true;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("expected checksum failure");
            }
        };

        assertThatThrownBy(() -> runner(dataSource, unreadable)
                .run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("checksum");
        assertThat(dataSource.historyLookupCount()).isZero();
    }

    private SaasControlSqlUpgradeRunner runner(TrackingDataSource dataSource, Resource... resources) {
        ResourcePatternResolver resolver = new ResourcePatternResolver() {
            @Override
            public Resource[] getResources(String locationPattern) {
                return resources;
            }

            @Override
            public Resource getResource(String location) {
                return resources.length == 0 ? script("empty.sql", "") : resources[0];
            }

            @Override
            public ClassLoader getClassLoader() {
                return getClass().getClassLoader();
            }
        };
        return new SaasControlSqlUpgradeRunner(dataSource, resolver);
    }

    private Resource script(String filename, String sql) {
        return new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8), filename) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private String sha256(String value) throws Exception {
        return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static final class TrackingDataSource implements DataSource {
        private final Map<String, HistoryRow> history = new LinkedHashMap<>();
        private final List<String> executedScripts = new ArrayList<>();
        private int historyLookupCount;

        Map<String, HistoryRow> history() {
            return history;
        }

        List<String> executedScripts() {
            return executedScripts;
        }

        String checksum(String scriptName) {
            return history.get(scriptName).checksum();
        }

        String status(String scriptName) {
            return history.get(scriptName).status();
        }

        int historyLookupCount() {
            return historyLookupCount;
        }

        @Override
        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "createStatement" -> statement();
                        case "prepareStatement" -> preparedStatement((String) args[0]);
                        case "close", "commit", "rollback", "setAutoCommit" -> null;
                        case "getAutoCommit" -> true;
                        case "isClosed" -> false;
                        case "unwrap" -> proxy;
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private Statement statement() {
            return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(), new Class[]{Statement.class},
                    (proxy, method, args) -> {
                        if (("execute".equals(method.getName()) || "executeUpdate".equals(method.getName()))
                                && args != null && args.length > 0) {
                            String sql = String.valueOf(args[0]);
                            if (!sql.startsWith("CREATE TABLE")) {
                                executedScripts.add(sql);
                            }
                            return "execute".equals(method.getName());
                        }
                        if ("getWarnings".equals(method.getName()) || "close".equals(method.getName())) {
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private PreparedStatement preparedStatement(String sql) {
            List<Object> parameters = new ArrayList<>();
            return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                    new Class[]{PreparedStatement.class}, (proxy, method, args) -> {
                        if (method.getName().startsWith("set") && args != null && args.length >= 2) {
                            int index = (Integer) args[0];
                            while (parameters.size() < index) {
                                parameters.add(null);
                            }
                            parameters.set(index - 1, args[1]);
                            return null;
                        }
                        if ("executeQuery".equals(method.getName())) {
                            historyLookupCount++;
                            HistoryRow row = history.get(String.valueOf(parameters.get(0)));
                            return resultSet(row);
                        }
                        if ("executeUpdate".equals(method.getName())) {
                            if (sql.startsWith("INSERT")) {
                                history.put(String.valueOf(parameters.get(0)),
                                        new HistoryRow(String.valueOf(parameters.get(1)), "1",
                                                String.valueOf(parameters.get(2))));
                            }
                            return 1;
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private ResultSet resultSet(HistoryRow row) {
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class[]{ResultSet.class},
                    new java.lang.reflect.InvocationHandler() {
                        private boolean consumed;

                        @Override
                        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                            return switch (method.getName()) {
                                case "next" -> !consumed && row != null ? (consumed = true) : false;
                                case "getString" -> {
                                    String column = String.valueOf(args[0]);
                                    yield "status".equalsIgnoreCase(column) || "2".equals(column)
                                            ? row.status() : row.checksum();
                                }
                                case "close" -> null;
                                default -> defaultValue(method.getReturnType());
                            };
                        }
                    });
        }

        private static Object defaultValue(Class<?> type) {
            if (type == null || !type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            return null;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    private record HistoryRow(String checksum, String status, String remark) {
    }
}
