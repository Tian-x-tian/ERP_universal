package com.erp.system.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.erp.common.core.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * MyBatis Plus 配置
 */
@Configuration
public class MyBatisPlusConfig {
    /**
     * 无 tenant_id 字段的系统表。
     * 这些表由业务逻辑自行控制访问范围，不由多租户插件自动拼接租户条件。
     */
    private static final Set<String> IGNORE_TENANT_TABLES = new HashSet<>(Arrays.asList(
            "sys_tenant",
            "sys_user_role",
            "sys_role_menu",
            "sys_menu",
            "sys_dict_type",
            "sys_dict_data",
            "sys_config"));

    /**
     * 新多租户插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                String tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null) {
                    tenantId = "000000";
                }
                return new StringValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                if (tableName == null) {
                    return false;
                }
                return IGNORE_TENANT_TABLES.contains(tableName.toLowerCase(Locale.ROOT));
            }
        }));
        return interceptor;
    }
}
