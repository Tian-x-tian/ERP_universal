package com.erp.business.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 业务模块启动期增量脚本执行器。
 */
@Component
public class BusinessSchemaUpgradeRunner implements ApplicationRunner {

    private final DataSource dataSource;

    public BusinessSchemaUpgradeRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 应用启动后自动执行业务模块增量脚本，优先补齐历史库缺失对象。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("sql/upgrade_business.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }
}
