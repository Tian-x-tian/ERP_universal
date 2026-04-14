package com.erp.business.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 业务模块增量脚本执行器单元测试。
 */
class BusinessSchemaUpgradeRunnerTest {

    private static final String TEST_MARKER = "business_schema_upgrade_runner_test";

    /**
     * 验证已执行过的版本化脚本不会在二次启动时重复执行。
     *
     * @throws Exception 执行器运行异常
     */
    @Test
    void shouldSkipPreviouslyExecutedVersionedScriptOnSecondRun() throws Exception {
        TrackingDataSource dataSource = new TrackingDataSource();
        BusinessSchemaUpgradeRunner runner = new BusinessSchemaUpgradeRunner(dataSource);
        ReflectionTestUtils.setField(runner, "upgradeEnabled", true);
        ReflectionTestUtils.setField(runner, "legacyScriptLocation", "classpath:sql/upgrade_business.sql");
        ReflectionTestUtils.setField(runner, "versionedScriptLocation", "classpath*:sql/upgrade/business/*.sql");

        runner.run(new DefaultApplicationArguments(new String[0]));
        int firstRunMarkerCount = dataSource.countExecutedStatementsContaining(TEST_MARKER);

        runner.run(new DefaultApplicationArguments(new String[0]));
        int secondRunMarkerCount = dataSource.countExecutedStatementsContaining(TEST_MARKER);

        Assertions.assertTrue(firstRunMarkerCount > 0);
        Assertions.assertEquals(firstRunMarkerCount, secondRunMarkerCount);
    }

    /**
     * 用于跟踪 SQL 执行情况的轻量级数据源。
     */
    private static final class TrackingDataSource implements DataSource {
        private final List<String> executedStatements = new CopyOnWriteArrayList<>();
        private final Set<String> executedScripts = ConcurrentHashMap.newKeySet();

        /**
         * 统计包含指定片段的 SQL 语句执行次数。
         *
         * @param marker 关键片段
         * @return 执行次数
         */
        int countExecutedStatementsContaining(String marker) {
            int count = 0;
            for (String executedStatement : executedStatements) {
                if (executedStatement != null && executedStatement.contains(marker)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Connection getConnection() {
            return createConnectionProxy();
        }

        @Override
        public Connection getConnection(String username, String password) {
            return createConnectionProxy();
        }

        private Connection createConnectionProxy() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[]{Connection.class},
                    (proxy, method, args) -> {
                        String methodName = method.getName();
                        if ("createStatement".equals(methodName)) {
                            return createStatementProxy();
                        }
                        if ("prepareStatement".equals(methodName)) {
                            return createPreparedStatementProxy((String) args[0]);
                        }
                        if ("close".equals(methodName) || "commit".equals(methodName)
                                || "rollback".equals(methodName) || "setAutoCommit".equals(methodName)) {
                            return null;
                        }
                        if ("getAutoCommit".equals(methodName)) {
                            return Boolean.TRUE;
                        }
                        if ("isClosed".equals(methodName)) {
                            return Boolean.FALSE;
                        }
                        if ("unwrap".equals(methodName)) {
                            return proxy;
                        }
                        if ("isWrapperFor".equals(methodName)) {
                            return Boolean.FALSE;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private Statement createStatementProxy() {
            return (Statement) Proxy.newProxyInstance(
                    Statement.class.getClassLoader(),
                    new Class[]{Statement.class},
                    (proxy, method, args) -> {
                        String methodName = method.getName();
                        if ("execute".equals(methodName) || "executeUpdate".equals(methodName)) {
                            executedStatements.add((String) args[0]);
                            return "execute".equals(methodName) ? Boolean.TRUE : 1;
                        }
                        if ("close".equals(methodName)) {
                            return null;
                        }
                        if ("unwrap".equals(methodName)) {
                            return proxy;
                        }
                        if ("isWrapperFor".equals(methodName)) {
                            return Boolean.FALSE;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private PreparedStatement createPreparedStatementProxy(String sql) {
            List<Object> parameters = new ArrayList<>();
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        String methodName = method.getName();
                        if (methodName.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer index) {
                            ensureParameterCapacity(parameters, index);
                            parameters.set(index - 1, args[1]);
                            return null;
                        }
                        if ("executeQuery".equals(methodName)) {
                            String scriptName = parameters.isEmpty() ? null : String.valueOf(parameters.get(0));
                            boolean exists = scriptName != null && executedScripts.contains(scriptName);
                            return createResultSetProxy(exists);
                        }
                        if ("execute".equals(methodName) || "executeUpdate".equals(methodName)) {
                            if (sql != null && sql.toLowerCase().contains("insert") && !parameters.isEmpty()) {
                                executedScripts.add(String.valueOf(parameters.get(0)));
                            }
                            return "execute".equals(methodName) ? Boolean.TRUE : 1;
                        }
                        if ("close".equals(methodName)) {
                            return null;
                        }
                        if ("unwrap".equals(methodName)) {
                            return proxy;
                        }
                        if ("isWrapperFor".equals(methodName)) {
                            return Boolean.FALSE;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private ResultSet createResultSetProxy(boolean exists) {
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class[]{ResultSet.class},
                    new java.lang.reflect.InvocationHandler() {
                        private boolean consumed;

                        @Override
                        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                            String methodName = method.getName();
                            if ("next".equals(methodName)) {
                                if (exists && !consumed) {
                                    consumed = true;
                                    return Boolean.TRUE;
                                }
                                return Boolean.FALSE;
                            }
                            if ("close".equals(methodName)) {
                                return null;
                            }
                            if ("getInt".equals(methodName)) {
                                return exists ? 1 : 0;
                            }
                            if ("unwrap".equals(methodName)) {
                                return proxy;
                            }
                            if ("isWrapperFor".equals(methodName)) {
                                return Boolean.FALSE;
                            }
                            return defaultValue(method.getReturnType());
                        }
                    });
        }

        private void ensureParameterCapacity(List<Object> parameters, int index) {
            while (parameters.size() < index) {
                parameters.add(null);
            }
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == null || !returnType.isPrimitive()) {
                return null;
            }
            if (boolean.class.equals(returnType)) {
                return Boolean.FALSE;
            }
            if (int.class.equals(returnType)) {
                return 0;
            }
            if (long.class.equals(returnType)) {
                return 0L;
            }
            if (double.class.equals(returnType)) {
                return 0D;
            }
            if (float.class.equals(returnType)) {
                return 0F;
            }
            if (short.class.equals(returnType)) {
                return (short) 0;
            }
            if (byte.class.equals(returnType)) {
                return (byte) 0;
            }
            if (char.class.equals(returnType)) {
                return (char) 0;
            }
            return null;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("Parent logger is not supported in test data source.");
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
