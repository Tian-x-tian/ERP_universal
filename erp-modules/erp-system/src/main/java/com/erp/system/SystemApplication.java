package com.erp.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * ERP 多模块统一启动入口。
 */
public class SystemApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemApplication.class);
    private static final String MODULE_DESCRIPTOR_PATH = "META-INF/erp-unified-module.properties";

    /**
     * 按模块描述文件顺序启动多模块应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        throw new UnsupportedOperationException("SystemApplication unified launcher is deprecated and disabled. Use module entrypoints (GatewayApplication/AuthApplication/SystemModuleApplication/BusinessApplication).");
    }

    /**
     * 从类路径加载并解析启用中的模块描述。
     *
     * @return 按 module.order 升序排序的模块定义集合
     */
    private static List<ModuleDefinition> loadEnabledModules() {
        List<ModuleDefinition> modules = new ArrayList<>();
        Enumeration<URL> resources;
        try {
            resources = Thread.currentThread().getContextClassLoader().getResources(MODULE_DESCRIPTOR_PATH);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read module descriptors from " + MODULE_DESCRIPTOR_PATH, ex);
        }
        while (resources.hasMoreElements()) {
            URL descriptorUrl = resources.nextElement();
            ModuleDefinition module = parseModuleDescriptor(descriptorUrl);
            if (module.isEnabled()) {
                modules.add(module);
            }
        }
        modules.sort(Comparator.comparingInt(ModuleDefinition::getOrder));
        return modules;
    }

    /**
     * 解析单个模块描述文件。
     *
     * @param descriptorUrl 描述文件 URL
     * @return 模块定义
     */
    private static ModuleDefinition parseModuleDescriptor(URL descriptorUrl) {
        Properties properties = new Properties();
        try (InputStream inputStream = descriptorUrl.openStream()) {
            properties.load(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse descriptor: " + descriptorUrl, ex);
        }

        String moduleName = requireProperty(properties, "module.name", descriptorUrl);
        String applicationClassName = requireProperty(properties, "module.application-class", descriptorUrl);
        String configLocation = requireProperty(properties, "module.config-location", descriptorUrl);
        WebApplicationType webType = parseWebType(
                properties.getProperty("module.web-application-type", "SERVLET"),
                descriptorUrl);
        int order = parseOrder(properties.getProperty("module.order", "100"), descriptorUrl);
        boolean enabled = Boolean.parseBoolean(properties.getProperty("module.enabled", "true"));
        String serverPort = trimToNull(properties.getProperty("module.server-port"));

        return new ModuleDefinition(moduleName, applicationClassName, configLocation, webType, order, enabled,
                serverPort);
    }

    /**
     * 启动指定模块应用。
     *
     * @param module 模块定义
     * @param args   启动参数
     * @return 模块应用上下文
     */
    private static ConfigurableApplicationContext startModule(ModuleDefinition module, String[] args) {
        Class<?> applicationCls;
        try {
            applicationCls = Class.forName(module.getApplicationClassName());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Module application class not found: " + module.getApplicationClassName(),
                    ex);
        }
        SpringApplication application = new SpringApplication(applicationCls);
        application.setWebApplicationType(module.getWebType());
        Map<String, Object> defaultProperties = new HashMap<>();
        defaultProperties.put("spring.config.location", module.getConfigLocation());
        defaultProperties.put("spring.main.allow-bean-definition-overriding", "true");
        // 统一入口共用 classpath 时，默认关闭 Gateway 自动配置，避免污染非网关模块。
        defaultProperties.put("spring.cloud.gateway.enabled", "false");
        application.setDefaultProperties(defaultProperties);
        application.addInitializers(context -> applyModuleOverrides(context.getEnvironment(), module));
        ConfigurableApplicationContext context = application.run(args);
        LOGGER.info("{} started with config {}", module.getModuleName(), module.getConfigLocation());
        return context;
    }

    /**
     * 将模块级强制配置置于最高优先级，避免被跨模块导入配置覆盖。
     *
     * @param environment 模块环境
     * @param module      模块定义
     */
    private static void applyModuleOverrides(ConfigurableEnvironment environment, ModuleDefinition module) {
        Map<String, Object> overrides = new HashMap<>();
        if (module.getServerPort() != null) {
            overrides.put("server.port", module.getServerPort());
        }
        if (overrides.isEmpty()) {
            return;
        }
        environment.getPropertySources().addFirst(
                new MapPropertySource(module.getModuleName() + "-overrides", overrides));
    }

    /**
     * 注册统一关闭钩子，确保多模块进程停止时按逆序释放资源。
     *
     * @param contexts 已启动模块上下文集合
     */
    private static void registerShutdownHook(List<ConfigurableApplicationContext> contexts) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeContexts(contexts), "erp-unified-shutdown"));
    }

    /**
     * 关闭已启动模块上下文。
     *
     * @param contexts 已启动模块上下文集合
     */
    private static void closeContexts(List<ConfigurableApplicationContext> contexts) {
        List<ConfigurableApplicationContext> reversed = new ArrayList<>(contexts);
        Collections.reverse(reversed);
        for (ConfigurableApplicationContext context : reversed) {
            if (context != null && context.isActive()) {
                try {
                    context.close();
                } catch (Exception ex) {
                    LOGGER.warn("Failed to close context: {}", context.getId(), ex);
                }
            }
        }
    }

    /**
     * 获取并校验描述文件中的必填属性。
     *
     * @param properties    属性对象
     * @param key           属性键
     * @param descriptorUrl 描述文件 URL
     * @return 属性值
     */
    private static String requireProperty(Properties properties, String key, URL descriptorUrl) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing property '" + key + "' in descriptor: " + descriptorUrl);
        }
        return value.trim();
    }

    /**
     * 解析模块 Web 类型。
     *
     * @param value         Web 类型字符串
     * @param descriptorUrl 描述文件 URL
     * @return WebApplicationType
     */
    private static WebApplicationType parseWebType(String value, URL descriptorUrl) {
        try {
            return WebApplicationType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid module.web-application-type in descriptor: " + descriptorUrl,
                    ex);
        }
    }

    /**
     * 解析模块启动顺序。
     *
     * @param value         顺序字符串
     * @param descriptorUrl 描述文件 URL
     * @return 启动顺序值
     */
    private static int parseOrder(String value, URL descriptorUrl) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid module.order in descriptor: " + descriptorUrl, ex);
        }
    }

    /**
     * 去除首尾空格并将空串转为 null。
     *
     * @param value 原始值
     * @return 规范化后的值
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 统一启动模块定义。
     */
    private static final class ModuleDefinition {
        private final String moduleName;
        private final String applicationClassName;
        private final String configLocation;
        private final WebApplicationType webType;
        private final int order;
        private final boolean enabled;
        private final String serverPort;

        /**
         * 创建模块定义。
         *
         * @param moduleName           模块名称
         * @param applicationClassName 启动类全限定名
         * @param configLocation       配置文件路径
         * @param webType              Web 应用类型
         * @param order                启动顺序
         * @param enabled              是否启用
         * @param serverPort           强制端口
         */
        private ModuleDefinition(String moduleName,
                String applicationClassName,
                String configLocation,
                WebApplicationType webType,
                int order,
                boolean enabled,
                String serverPort) {
            this.moduleName = moduleName;
            this.applicationClassName = applicationClassName;
            this.configLocation = configLocation;
            this.webType = webType;
            this.order = order;
            this.enabled = enabled;
            this.serverPort = serverPort;
        }

        /**
         * 获取模块名称。
         *
         * @return 模块名称
         */
        private String getModuleName() {
            return moduleName;
        }

        /**
         * 获取启动类全限定名。
         *
         * @return 启动类全限定名
         */
        private String getApplicationClassName() {
            return applicationClassName;
        }

        /**
         * 获取配置文件路径。
         *
         * @return 配置文件路径
         */
        private String getConfigLocation() {
            return configLocation;
        }

        /**
         * 获取 Web 应用类型。
         *
         * @return Web 应用类型
         */
        private WebApplicationType getWebType() {
            return webType;
        }

        /**
         * 获取启动顺序。
         *
         * @return 启动顺序
         */
        private int getOrder() {
            return order;
        }

        /**
         * 获取是否启用。
         *
         * @return true 表示启用，false 表示禁用
         */
        private boolean isEnabled() {
            return enabled;
        }

        /**
         * 获取强制端口。
         *
         * @return 强制端口
         */
        private String getServerPort() {
            return serverPort;
        }
    }
}
