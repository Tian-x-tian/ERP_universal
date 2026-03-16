package com.erp.business.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.erp.common.core.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis Plus 配置。
 */
@Configuration
public class MyBatisPlusConfig {
    private static final String TENANT_COLUMN_NAME = "tenant_id";

    private static final Set<String> GLOBAL_TABLE_CANDIDATES = new HashSet<>(Arrays.asList(
            "sys_tenant",
            "sys_menu",
            "sys_dict_type",
            "sys_dict_data",
            "sys_config"));

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, Boolean> tenantColumnCache = new ConcurrentHashMap<>();

    public MyBatisPlusConfig(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 注册多租户与分页拦截器。
     *
     * @return MyBatis Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                String tenantId = normalizeTenantId(TenantContextHolder.getTenantId());
                if (!StringUtils.hasText(tenantId)) {
                    throw new IllegalStateException("Tenant id is missing in current request context.");
                }
                return new StringValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return TENANT_COLUMN_NAME;
            }

            @Override
            public boolean ignoreTable(String tableName) {
                if (tableName == null) {
                    return false;
                }
                String normalizedTableName = normalizeTableName(tableName);
                if (!GLOBAL_TABLE_CANDIDATES.contains(normalizedTableName)) {
                    return false;
                }
                return !hasTenantColumn(normalizedTableName);
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 判断数据表是否包含租户字段。
     *
     * @param tableName 表名
     * @return true 表示存在 tenant_id 列
     */
    private boolean hasTenantColumn(String tableName) {
        return tenantColumnCache.computeIfAbsent(tableName, this::queryTenantColumn);
    }

    /**
     * 查询元数据确认租户字段是否存在。
     *
     * @param tableName 表名
     * @return true 表示存在 tenant_id 列
     */
    private boolean queryTenantColumn(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                TENANT_COLUMN_NAME);
        return count != null && count > 0;
    }

    /**
     * 规范化表名。
     *
     * @param tableName 原始表名
     * @return 规范化表名
     */
    private String normalizeTableName(String tableName) {
        String normalized = tableName.replace("`", "").trim().toLowerCase(Locale.ROOT);
        int separatorIndex = normalized.lastIndexOf('.');
        if (separatorIndex >= 0 && separatorIndex < normalized.length() - 1) {
            normalized = normalized.substring(separatorIndex + 1);
        }
        return normalized;
    }

    /**
     * 规范化租户编号。
     *
     * @param tenantId 原始租户编号
     * @return 规范化租户编号
     */
    private String normalizeTenantId(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }
}
