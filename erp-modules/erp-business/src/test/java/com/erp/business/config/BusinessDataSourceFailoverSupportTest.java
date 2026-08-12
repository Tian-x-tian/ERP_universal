package com.erp.business.config;

import com.erp.business.config.BusinessDataSourceFailoverSupport.DataSourceProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 数据源选择辅助类单元测试。
 */
class BusinessDataSourceFailoverSupportTest {

    /**
     * 验证默认未启用业务独立数据源时共用系统数据源。
     */
    @Test
    void shouldUseSharedProfileWhenBusinessDatasourceIsDisabled() {
        DataSourceProfile primary = new DataSourceProfile("primary", "jdbc:mysql://primary/db", "root", "123", "driver");
        DataSourceProfile shared = new DataSourceProfile("shared", "jdbc:mysql://shared/db", "root", "123", "driver");

        DataSourceProfile selected = BusinessDataSourceFailoverSupport.selectActiveProfile(
                primary,
                shared,
                false);

        Assertions.assertEquals("shared", selected.getName());
    }

    /**
     * 验证显式启用业务独立数据源后优先使用业务数据源。
     */
    @Test
    void shouldUsePrimaryProfileWhenBusinessDatasourceIsEnabled() {
        DataSourceProfile primary = new DataSourceProfile("primary", "jdbc:mysql://primary/db", "root", "123", "driver");
        DataSourceProfile shared = new DataSourceProfile("shared", "jdbc:mysql://shared/db", "root", "123", "driver");

        DataSourceProfile selected = BusinessDataSourceFailoverSupport.selectActiveProfile(
                primary,
                shared,
                true);

        Assertions.assertEquals("primary", selected.getName());
    }

    /**
     * 验证未启用业务独立数据源且共用配置缺失时仍可退回业务配置。
     */
    @Test
    void shouldFallbackToPrimaryWhenSharedProfileIsMissing() {
        DataSourceProfile primary = new DataSourceProfile("primary", "jdbc:mysql://same/db", "root", "123", "driver");

        DataSourceProfile selected = BusinessDataSourceFailoverSupport.selectActiveProfile(
                primary,
                null,
                false);

        Assertions.assertEquals("primary", selected.getName());
    }
}
