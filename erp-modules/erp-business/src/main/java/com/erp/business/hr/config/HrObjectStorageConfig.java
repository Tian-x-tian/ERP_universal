package com.erp.business.hr.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * HR 对象存储配置注册。
 */
@Configuration
@EnableConfigurationProperties(HrObjectStorageProperties.class)
public class HrObjectStorageConfig {
}
