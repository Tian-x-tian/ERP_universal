package com.erp.business.config;

import com.erp.business.config.BusinessDataSourceFailoverSupport.DataSourceProfile;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 业务模块数据源配置。
 */
@Configuration
public class BusinessDataSourceConfig {
    private static final String PRIMARY_PREFIX = "erp.business.datasource";
    private static final String SHARED_PREFIX = "spring.datasource";
    private static final String ENABLED_KEY = "erp.business.datasource.enabled";

    /**
     * 创建业务模块数据源。
     * 默认共用系统库，显式启用后才切换到业务库配置。
     *
     * @param environment Spring 环境配置
     * @return 可用数据源
     */
    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource(Environment environment) {
        DataSourceProfile primaryProfile = buildProfile(environment, PRIMARY_PREFIX, "business-primary");
        DataSourceProfile sharedProfile = buildProfile(environment, SHARED_PREFIX, "system-shared");
        DataSourceProfile activeProfile = BusinessDataSourceFailoverSupport.selectActiveProfile(
                primaryProfile,
                sharedProfile,
                environment.getProperty(ENABLED_KEY, Boolean.class, false));
        return createDataSource(activeProfile);
    }

    /**
     * 根据配置前缀构造数据源描述。
     *
     * @param environment Spring 环境配置
     * @param prefix 配置前缀
     * @param profileName 配置名称
     * @return 数据源描述
     */
    private DataSourceProfile buildProfile(Environment environment, String prefix, String profileName) {
        return new DataSourceProfile(
                profileName,
                environment.getProperty(prefix + ".url"),
                environment.getProperty(prefix + ".username"),
                environment.getProperty(prefix + ".password"),
                environment.getProperty(prefix + ".driver-class-name", "com.mysql.cj.jdbc.Driver"));
    }

    /**
     * 根据配置描述创建 Hikari 数据源实例。
     *
     * @param profile 数据源描述
     * @return Hikari 数据源
     */
    private HikariDataSource createDataSource(DataSourceProfile profile) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(profile.getUrl());
        dataSource.setUsername(profile.getUsername());
        dataSource.setPassword(profile.getPassword());
        dataSource.setDriverClassName(profile.getDriverClassName());
        dataSource.setPoolName("erp-business-" + resolvePoolName(profile.getName()));
        dataSource.setInitializationFailTimeout(3000);
        dataSource.setConnectionTimeout(5000);
        initializeAtStartup(dataSource);
        return dataSource;
    }

    /**
     * 在模块启动阶段预热连接池，避免首次打开库存页面才初始化数据库连接。
     *
     * @param dataSource Hikari 数据源
     */
    private void initializeAtStartup(HikariDataSource dataSource) {
        try (var ignored = dataSource.getConnection()) {
            // 启动阶段主动完成一次连接初始化。
        } catch (SQLException ex) {
            throw new IllegalStateException("Business datasource initialization failed.", ex);
        }
    }

    /**
     * 将配置名称转换为连接池名称片段。
     *
     * @param profileName 配置名称
     * @return 连接池名称片段
     */
    private String resolvePoolName(String profileName) {
        if (!StringUtils.hasText(profileName)) {
            return "datasource";
        }
        return profileName.trim().replaceAll("[^a-zA-Z0-9_-]", "-");
    }
}
