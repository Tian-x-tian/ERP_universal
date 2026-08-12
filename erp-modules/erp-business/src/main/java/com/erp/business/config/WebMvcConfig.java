package com.erp.business.config;

import com.erp.business.audit.OperationLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OperationLogInterceptor operationLogInterceptor;

    public WebMvcConfig(OperationLogInterceptor operationLogInterceptor) {
        this.operationLogInterceptor = operationLogInterceptor;
    }

    /**
     * 注册 MVC 拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(operationLogInterceptor).addPathPatterns("/**");
    }
}
