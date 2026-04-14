package com.erp.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.erp.common.core.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis Plus 多租户配置基类。
 */
public abstract class TenantMybatisPlusConfigurationSupport {
    private static final String TENANT_COLUMN_NAME = "tenant_id";

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, Boolean> tenantColumnCache = new ConcurrentHashMap<>();

    /**
     * 创建 MyBatis 多租户配置。
     *
     * @param dataSource 数据源
     */
    protected TenantMybatisPlusConfigurationSupport(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 注册多租户与分页拦截器。
     *
     * @return MyBatis Plus 拦截器
     */
    protected MybatisPlusInterceptor buildInterceptor() {
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
                    return true;
                }
                String normalizedTableName = normalizeTableName(tableName);
                if (globalTableCandidates().contains(normalizedTableName)) {
                    return true;
                }
                return !hasTenantColumn(normalizedTableName);
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 允许模块补充无 tenant_id 的平台表。
     *
     * @return 平台表集合
     */
    protected Set<String> globalTableCandidates() {
        return Collections.emptySet();
    }

    /**
     * 判断数据表是否包含租户字段。
     *
     * @param tableName 表名
     * @return true 表示存在 tenant_id 列
     */
    protected boolean hasTenantColumn(String tableName) {
        return tenantColumnCache.computeIfAbsent(tableName, this::queryTenantColumn);
    }

    /**
     * 查询元数据确认租户字段是否存在。
     *
     * @param tableName 表名
     * @return true 表示存在 tenant_id 列
     */
    protected boolean queryTenantColumn(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
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
    protected String normalizeTableName(String tableName) {
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
    protected String normalizeTenantId(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    /**
     * 构建默认全局表集合。
     *
     * @param tables 表名数组
     * @return 表集合
     */
    protected Set<String> buildGlobalTableSet(String... tables) {
        Set<String> candidates = new HashSet<>();
        if (tables == null) {
            return candidates;
        }
        for (String table : tables) {
            if (StringUtils.hasText(table)) {
                candidates.add(table.trim().toLowerCase(Locale.ROOT));
            }
        }
        return candidates;
    }
}
