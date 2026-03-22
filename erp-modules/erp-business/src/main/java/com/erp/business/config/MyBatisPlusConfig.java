package com.erp.business.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.erp.common.mybatis.TenantMybatisPlusConfigurationSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Set;

/**
 * MyBatis Plus 配置。
 */
@Configuration
public class MyBatisPlusConfig extends TenantMybatisPlusConfigurationSupport {
    private static final Set<String> GLOBAL_TABLE_CANDIDATES = Set.of(
            "sys_tenant",
            "sys_menu",
            "sys_dict_type",
            "sys_dict_data",
            "sys_config");

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

    /**
     * 返回业务模块的全局平台表。
     *
     * @return 全局平台表集合
     */
    @Override
    protected Set<String> globalTableCandidates() {
        return GLOBAL_TABLE_CANDIDATES;
    }
}
