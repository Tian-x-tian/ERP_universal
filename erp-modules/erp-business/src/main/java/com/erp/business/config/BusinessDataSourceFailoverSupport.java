package com.erp.business.config;

import org.springframework.util.StringUtils;

/**
 * 业务模块数据源选择辅助类。
 */
public final class BusinessDataSourceFailoverSupport {

    private BusinessDataSourceFailoverSupport() {
    }

    /**
     * 根据启用开关选择最终可用的数据源配置。
     *
     * @param primary 主数据源配置
     * @param shared 共用系统数据源配置
     * @param enabled 是否启用业务独立数据源
     * @return 最终选中的数据源配置
     */
    public static DataSourceProfile selectActiveProfile(DataSourceProfile primary,
            DataSourceProfile shared,
            boolean enabled) {
        if (enabled && primary != null && primary.isConfigured()) {
            return primary;
        }
        if (shared != null && shared.isConfigured()) {
            return shared;
        }
        if (primary != null && primary.isConfigured()) {
            return primary;
        }
        throw new IllegalStateException("No datasource profile is configured.");
    }

    /**
     * 数据源连接配置。
     */
    public static final class DataSourceProfile {
        private final String name;
        private final String url;
        private final String username;
        private final String password;
        private final String driverClassName;

        /**
         * 创建数据源连接配置。
         *
         * @param name 配置名称
         * @param url JDBC 地址
         * @param username 用户名
         * @param password 密码
         * @param driverClassName 驱动类名
         */
        public DataSourceProfile(String name, String url, String username, String password, String driverClassName) {
            this.name = normalize(name);
            this.url = normalize(url);
            this.username = normalize(username);
            this.password = password;
            this.driverClassName = normalize(driverClassName);
        }

        /**
         * 判断当前配置是否已完整填写。
         *
         * @return true 表示配置完整
         */
        public boolean isConfigured() {
            return StringUtils.hasText(url) && StringUtils.hasText(username) && StringUtils.hasText(driverClassName);
        }

        /**
         * 获取配置名称。
         *
         * @return 配置名称
         */
        public String getName() {
            return name;
        }

        /**
         * 获取 JDBC 地址。
         *
         * @return JDBC 地址
         */
        public String getUrl() {
            return url;
        }

        /**
         * 获取用户名。
         *
         * @return 用户名
         */
        public String getUsername() {
            return username;
        }

        /**
         * 获取密码。
         *
         * @return 密码
         */
        public String getPassword() {
            return password;
        }

        /**
         * 获取驱动类名。
         *
         * @return 驱动类名
         */
        public String getDriverClassName() {
            return driverClassName;
        }

        /**
         * 规范化字符串配置。
         *
         * @param value 原始值
         * @return 去空白后的值
         */
        private String normalize(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            return value.trim();
        }
    }
}
