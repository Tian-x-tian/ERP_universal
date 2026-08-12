package com.erp.business.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.erp.common.mybatis.TenantGlobalTables;
import com.erp.common.mybatis.TenantMybatisPlusConfigurationSupport;
import com.erp.common.mybatis.TenantSchemaReadinessFilter;
import com.erp.common.mybatis.TenantSchemaReadinessGate;
import com.erp.common.mybatis.TenantSchemaValidationRunner;
import com.erp.common.mybatis.TenantSchemaValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Set;

/**
 * MyBatis Plus 配置。
 */
@Configuration
public class MyBatisPlusConfig extends TenantMybatisPlusConfigurationSupport {

    public MyBatisPlusConfig(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * 注册多租户与分页拦截器。
     *
     * @return MyBatis Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return buildInterceptor();
    }

    @Bean
    @ConditionalOnProperty(name = "erp.tenant.schema-validation.enabled", havingValue = "true", matchIfMissing = true)
    public TenantSchemaReadinessGate tenantSchemaReadinessGate() {
        return new TenantSchemaReadinessGate();
    }

    @Bean
    @ConditionalOnProperty(name = "erp.tenant.schema-validation.enabled", havingValue = "true", matchIfMissing = true)
    public TenantSchemaReadinessFilter tenantSchemaReadinessFilter(TenantSchemaReadinessGate readinessGate) {
        return new TenantSchemaReadinessFilter(readinessGate);
    }

    @Bean
    @ConditionalOnProperty(name = "erp.tenant.schema-validation.enabled", havingValue = "true", matchIfMissing = true)
    public TenantSchemaValidationRunner tenantSchemaValidationRunner(
            DataSource dataSource, TenantSchemaReadinessGate readinessGate) {
        return new TenantSchemaValidationRunner(
                new TenantSchemaValidator(new JdbcTemplate(dataSource), TenantGlobalTables.TABLES),
                readinessGate);
    }

    /**
     * 返回业务模块的全局平台表。
     *
     * @return 全局平台表集合
     */
    @Override
    protected Set<String> globalTableCandidates() {
        return TenantGlobalTables.TABLES;
    }
}
