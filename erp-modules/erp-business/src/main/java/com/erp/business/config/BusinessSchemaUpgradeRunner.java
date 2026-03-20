package com.erp.business.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 业务模块启动期增量脚本执行器。
 */
@Component
public class BusinessSchemaUpgradeRunner implements ApplicationRunner {
    private static final String LEGACY_SCRIPT_PATH = "sql/upgrade_business.sql";
    private static final String VERSIONED_SCRIPT_LOCATION = "classpath*:sql/upgrade/business/*.sql";

    private final DataSource dataSource;
    private final PathMatchingResourcePatternResolver resourcePatternResolver;

    public BusinessSchemaUpgradeRunner(DataSource dataSource) {
        this.dataSource = dataSource;
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
    }

    /**
     * 应用启动后自动执行业务模块增量脚本，优先补齐历史库缺失对象。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        ClassPathResource legacyScript = new ClassPathResource(LEGACY_SCRIPT_PATH);
        if (legacyScript.exists()) {
            populator.addScript(legacyScript);
        }
        for (Resource resource : loadVersionedScripts()) {
            populator.addScript(resource);
        }
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }

    /**
     * 加载版本化业务增量脚本并按文件名排序。
     *
     * @return 版本化脚本列表
     * @throws Exception 资源加载异常
     */
    private List<Resource> loadVersionedScripts() throws Exception {
        Resource[] resources = resourcePatternResolver.getResources(VERSIONED_SCRIPT_LOCATION);
        List<Resource> scriptList = new ArrayList<>();
        for (Resource resource : resources) {
            if (resource != null && resource.exists() && resource.isReadable()) {
                scriptList.add(resource);
            }
        }
        scriptList.sort(Comparator.comparing(this::safeFileName));
        return scriptList;
    }

    /**
     * 安全读取脚本文件名。
     *
     * @param resource 资源对象
     * @return 文件名
     */
    private String safeFileName(Resource resource) {
        String fileName = resource == null ? null : resource.getFilename();
        return StringUtils.hasText(fileName) ? fileName : "unknown.sql";
    }
}
