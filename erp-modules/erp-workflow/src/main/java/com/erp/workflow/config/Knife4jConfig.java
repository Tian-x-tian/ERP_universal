package com.erp.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 配置类
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP 企业级管理工作流 API 文档")
                        .version("1.0.0")
                        .description("基于 Spring Boot 3 + Spring Cloud Alibaba 的多租户 ERP 工作流")
                        .contact(new Contact().name("Antigravity").email("admin@erp.com")));
    }

    @Bean
    public GroupedOpenApi workflowApi() {
        return GroupedOpenApi.builder()
                .group("核心工作流模块")
                .pathsToMatch("/workflow/**")
                .build();
    }
}

