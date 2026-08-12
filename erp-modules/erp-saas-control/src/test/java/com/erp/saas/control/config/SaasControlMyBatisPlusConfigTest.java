package com.erp.saas.control.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaasControlMyBatisPlusConfigTest {

    @Test
    void shouldConfigurePaginationWithoutTenantLineInterceptor() {
        MybatisPlusInterceptor interceptor = new SaasControlMyBatisPlusConfig().mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors())
                .hasSize(1)
                .allMatch(PaginationInnerInterceptor.class::isInstance)
                .noneMatch(TenantLineInnerInterceptor.class::isInstance);
    }
}
