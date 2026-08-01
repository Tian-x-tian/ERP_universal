package com.erp.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.erp.common.core.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * MyBatis Plus 多租户配置基类。
 */
public abstract class TenantMybatisPlusConfigurationSupport {
    private static final String TENANT_COLUMN_NAME = "tenant_id";

    /**
     * 创建 MyBatis 多租户配置。
     *
     * @param dataSource 数据源
     */
    protected TenantMybatisPlusConfigurationSupport(DataSource dataSource) {
    }

    /**
     * 注册多租户与分页拦截器。
     *
     * @return MyBatis Plus 拦截器
     */
    protected MybatisPlusInterceptor buildInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(buildTenantLineHandler()));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 构建默认租户处理器。
     *
     * @return 租户处理器
     */
    protected TenantLineHandler buildTenantLineHandler() {
        return new TenantLineHandler() {
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
                if (!StringUtils.hasText(tableName)) {
                    return false;
                }
                String normalizedTableName = normalizeTableName(tableName);
                return globalTableCandidates().contains(normalizedTableName);
            }
        };
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
     * 规范化表名。
     *
     * @param tableName 原始表名
     * @return 规范化表名
     */
    protected String normalizeTableName(String tableName) {
        return tableName.replace("`", "").trim().toLowerCase(Locale.ROOT);
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
                candidates.add(normalizeTableName(table));
            }
        }
        return candidates;
    }
}
