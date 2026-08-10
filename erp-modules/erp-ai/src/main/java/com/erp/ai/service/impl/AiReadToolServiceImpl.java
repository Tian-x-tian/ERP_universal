package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.context.AiInvocationScope;
import com.erp.ai.model.AiPanelCardVO;
import com.erp.ai.model.AiReadToolResult;
import com.erp.ai.model.AiStructuredBlock;
import com.erp.ai.model.AiToolDefinition;
import com.erp.ai.security.service.SecurityUserResolver;
import com.erp.ai.service.AiReadToolService;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.common.client.internal.InternalBusinessClient;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.platform.contract.model.PlatformAiDataColumn;
import com.erp.platform.contract.model.PlatformAiDataMetric;
import com.erp.platform.contract.model.PlatformAiDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI 只读工具服务实现。
 *
 * <p>工具注册表是本类的核心：每个工具声明所属模块、后端数据集编码、所需权限与参数结构，
 * 执行时按模块分发到对应的内部客户端，再把统一的数据集结构转成“模型可读文本 + 前端可渲染区块”。</p>
 */
@Service
public class AiReadToolServiceImpl implements AiReadToolService {
    private static final Logger log = LoggerFactory.getLogger(AiReadToolServiceImpl.class);

    private static final String MODULE_WORKFLOW = "workflow";
    private static final String MODULE_SYSTEM = "system";
    private static final String MODULE_BUSINESS = "business";

    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_DAYS = "days";
    private static final String PARAM_LIMIT = "limit";

    private static final String SCOPE_CACHE_KEY = "ai:read-tools";

    /** 工具注册表，key 为模型可见的工具名 */
    private static final Map<String, ReadToolDescriptor> TOOL_REGISTRY = buildRegistry();

    private final InternalWorkflowClient internalWorkflowClient;
    private final InternalSystemClient internalSystemClient;
    private final InternalBusinessClient internalBusinessClient;
    private final AiTenantConfigService aiTenantConfigService;
    private final SecurityUserResolver securityUserResolver;
    private final ErpAiProperties erpAiProperties;

    public AiReadToolServiceImpl(InternalWorkflowClient internalWorkflowClient,
            InternalSystemClient internalSystemClient,
            InternalBusinessClient internalBusinessClient,
            AiTenantConfigService aiTenantConfigService,
            SecurityUserResolver securityUserResolver,
            ErpAiProperties erpAiProperties) {
        this.internalWorkflowClient = internalWorkflowClient;
        this.internalSystemClient = internalSystemClient;
        this.internalBusinessClient = internalBusinessClient;
        this.aiTenantConfigService = aiTenantConfigService;
        this.securityUserResolver = securityUserResolver;
        this.erpAiProperties = erpAiProperties;
    }

    /**
     * 构造当前用户可调用的只读工具定义。
     *
     * @return 工具定义列表
     */
    @Override
    public List<AiToolDefinition> buildAvailableTools() {
        if (!erpAiProperties.isReadToolsEnabled()) {
            return Collections.emptyList();
        }
        return AiInvocationScope.compute(SCOPE_CACHE_KEY, () -> {
            List<AiToolDefinition> tools = new ArrayList<>();
            for (ReadToolDescriptor descriptor : TOOL_REGISTRY.values()) {
                if (!hasAnyPermission(descriptor.permissions())) {
                    continue;
                }
                AiToolDefinition definition = new AiToolDefinition();
                definition.setName(descriptor.name());
                definition.setDescription(descriptor.description());
                definition.setParameters(buildSchema(descriptor.parameters()));
                tools.add(definition);
            }
            return tools;
        });
    }

    /**
     * 列出当前用户可用的面板卡片。
     *
     * @return 卡片列表
     */
    @Override
    public List<AiPanelCardVO> listAvailableCards() {
        List<AiPanelCardVO> cards = new ArrayList<>();
        if (!erpAiProperties.isReadToolsEnabled()) {
            return cards;
        }
        for (ReadToolDescriptor descriptor : TOOL_REGISTRY.values()) {
            if (!hasAnyPermission(descriptor.permissions())) {
                continue;
            }
            cards.add(new AiPanelCardVO(descriptor.name(),
                    resolveToolLabel(descriptor.name()),
                    descriptor.description(),
                    descriptor.parameters()));
        }
        return cards;
    }

    /**
     * 解析只读工具的展示名。
     *
     * @param toolName 工具名称
     * @return 展示名
     */
    @Override
    public String resolveToolLabel(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return "数据查询";
        }
        return switch (toolName.trim()) {
            case "query_todo_backlog" -> "待办积压分布";
            case "query_todo_aging" -> "待办滞留明细";
            case "query_approval_duration" -> "审批节点耗时";
            case "query_process_instance_stats" -> "流程实例分布";
            case "query_user_workload" -> "人员负载排行";
            case "query_approval_trend" -> "审批动作趋势";
            case "query_notice_overview" -> "消息分布";
            case "query_operation_trend" -> "系统操作趋势";
            case "query_ai_usage_trend" -> "AI 使用量";
            case "query_stock_overview" -> "库存概览";
            case "query_stock_warning" -> "库存预警";
            case "query_hr_headcount" -> "在岗人数";
            case "query_hr_warning" -> "HR 预警";
            default -> "数据查询";
        };
    }

    /**
     * 判断给定名称是否为只读工具。
     *
     * @param toolName 工具名称
     * @return true 表示是只读工具
     */
    @Override
    public boolean isReadTool(String toolName) {
        return StringUtils.hasText(toolName) && TOOL_REGISTRY.containsKey(toolName.trim());
    }

    /**
     * 执行只读工具。
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @return 执行结果
     */
    @Override
    public AiReadToolResult execute(String toolName, Map<String, Object> arguments) {
        String normalizedName = toolName == null ? null : toolName.trim();
        ReadToolDescriptor descriptor = normalizedName == null ? null : TOOL_REGISTRY.get(normalizedName);
        if (descriptor == null) {
            return AiReadToolResult.failure(normalizedName, "未识别的查询工具");
        }
        if (!erpAiProperties.isReadToolsEnabled()) {
            return AiReadToolResult.failure(normalizedName, "只读工具已被关闭");
        }
        // 执行前重新判权，避免模型拿到过期的工具清单后越权取数。
        if (!hasAnyPermission(descriptor.permissions())) {
            return AiReadToolResult.failure(normalizedName, "当前账号没有查看该数据的权限");
        }

        try {
            Map<String, Object> params = normalizeParams(descriptor, arguments);
            PlatformAiDataSet dataSet = dispatch(descriptor, params);
            if (dataSet == null) {
                return AiReadToolResult.failure(normalizedName, "数据服务未返回结果");
            }
            truncateRows(dataSet);
            return AiReadToolResult.success(normalizedName,
                    renderForModel(dataSet),
                    new AiStructuredBlock(normalizedName, dataSet));
        } catch (Exception ex) {
            log.warn("AI 只读工具执行失败: tool={}, reason={}", normalizedName, ex.getMessage());
            return AiReadToolResult.failure(normalizedName, "数据服务暂时不可用");
        }
    }

    /**
     * 按模块把数据集查询分发到对应的内部客户端。
     *
     * @param descriptor 工具描述
     * @param params     查询参数
     * @return 数据集
     */
    private PlatformAiDataSet dispatch(ReadToolDescriptor descriptor, Map<String, Object> params) {
        return switch (descriptor.module()) {
            case MODULE_WORKFLOW -> internalWorkflowClient.queryAiDataset(descriptor.datasetKey(), params);
            case MODULE_SYSTEM -> internalSystemClient.queryAiDataset(descriptor.datasetKey(), params);
            case MODULE_BUSINESS -> internalBusinessClient.queryAiDataset(descriptor.datasetKey(), params);
            default -> null;
        };
    }

    /**
     * 规范化模型传入的参数，只保留工具声明过的参数并补齐当前用户身份。
     *
     * @param descriptor 工具描述
     * @param arguments  模型原始参数
     * @return 规范化参数
     */
    private Map<String, Object> normalizeParams(ReadToolDescriptor descriptor, Map<String, Object> arguments) {
        Map<String, Object> params = new LinkedHashMap<>();
        Map<String, Object> safeArguments = arguments == null ? Collections.emptyMap() : arguments;
        for (String parameter : descriptor.parameters()) {
            Object value = safeArguments.get(parameter);
            if (value == null) {
                continue;
            }
            if (PARAM_SCOPE.equals(parameter)) {
                String scope = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
                params.put(PARAM_SCOPE, "tenant".equals(scope) ? "tenant" : "mine");
                continue;
            }
            params.put(parameter, value);
        }
        // userId 始终由服务端注入，绝不接受模型传入，避免越权查看他人数据。
        if (descriptor.parameters().contains(PARAM_SCOPE)) {
            params.putIfAbsent(PARAM_SCOPE, "mine");
            params.put("userId", securityUserResolver.getCurrentUserId());
        }
        return params;
    }

    /**
     * 按配置的行数上限裁剪数据集。
     *
     * @param dataSet 数据集
     */
    private void truncateRows(PlatformAiDataSet dataSet) {
        int maxRows = Math.max(1, erpAiProperties.getMaxDatasetRows());
        List<Map<String, Object>> rows = dataSet.getRows();
        if (rows != null && rows.size() > maxRows) {
            dataSet.setRows(new ArrayList<>(rows.subList(0, maxRows)));
            dataSet.setTruncated(true);
        }
    }

    /**
     * 将数据集渲染成回灌给模型的紧凑文本。
     *
     * <p>刻意使用竖线分隔的纯文本而不是 JSON：同样的信息量下 token 更省，
     * 也不会诱导模型把原始结构直接抄进回复里。</p>
     *
     * @param dataSet 数据集
     * @return 紧凑文本
     */
    private String renderForModel(PlatformAiDataSet dataSet) {
        StringBuilder builder = new StringBuilder();
        builder.append("[数据集] ").append(nullToDash(dataSet.getTitle())).append('\n');
        if (StringUtils.hasText(dataSet.getSummary())) {
            builder.append("结论: ").append(dataSet.getSummary()).append('\n');
        }
        if (StringUtils.hasText(dataSet.getMessage())) {
            builder.append("说明: ").append(dataSet.getMessage()).append('\n');
        }

        List<PlatformAiDataMetric> metrics = dataSet.getMetrics();
        if (metrics != null && !metrics.isEmpty()) {
            builder.append("指标: ");
            for (int index = 0; index < metrics.size(); index++) {
                PlatformAiDataMetric metric = metrics.get(index);
                if (index > 0) {
                    builder.append("; ");
                }
                builder.append(metric.getLabel()).append('=').append(nullToDash(metric.getValue()));
                if (StringUtils.hasText(metric.getUnit())) {
                    builder.append(metric.getUnit());
                }
            }
            builder.append('\n');
        }

        List<PlatformAiDataColumn> columns = dataSet.getColumns();
        List<Map<String, Object>> rows = dataSet.getRows();
        if (columns != null && !columns.isEmpty() && rows != null && !rows.isEmpty()) {
            builder.append("表格(");
            for (int index = 0; index < columns.size(); index++) {
                if (index > 0) {
                    builder.append('|');
                }
                builder.append(columns.get(index).getLabel());
            }
            builder.append("):\n");
            for (Map<String, Object> row : rows) {
                for (int index = 0; index < columns.size(); index++) {
                    if (index > 0) {
                        builder.append('|');
                    }
                    builder.append(nullToDash(row.get(columns.get(index).getKey())));
                }
                builder.append('\n');
            }
            if (dataSet.isTruncated()) {
                builder.append("（结果已按上限截断，如需更多请缩小范围）\n");
            }
        } else if (rows == null || rows.isEmpty()) {
            builder.append("表格: 无数据行\n");
        }
        return builder.toString().trim();
    }

    /**
     * 判断当前用户是否拥有任一权限。
     *
     * @param permissions 权限候选集
     * @return true 表示拥有任一权限
     */
    private boolean hasAnyPermission(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }
        for (String permission : permissions) {
            if (aiTenantConfigService.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构造工具的 JSON Schema 参数结构。
     *
     * @param parameters 参数名集合
     * @return JSON Schema
     */
    private Map<String, Object> buildSchema(List<String> parameters) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String parameter : parameters) {
            properties.put(parameter, buildParameterSchema(parameter));
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("additionalProperties", Boolean.FALSE);
        return schema;
    }

    /**
     * 构造单个参数的 Schema 片段。
     *
     * @param parameter 参数名
     * @return 参数 Schema
     */
    private Map<String, Object> buildParameterSchema(String parameter) {
        Map<String, Object> property = new LinkedHashMap<>();
        switch (parameter) {
            case PARAM_SCOPE -> {
                property.put("type", "string");
                property.put("enum", Arrays.asList("mine", "tenant"));
                property.put("description", "统计范围：mine=只看当前用户，tenant=看全租户，默认 mine");
            }
            case PARAM_DAYS -> {
                property.put("type", "integer");
                property.put("description", "统计最近多少天，默认按工具自身设定，最大 180");
            }
            case PARAM_LIMIT -> {
                property.put("type", "integer");
                property.put("description", "返回的最大行数，默认按工具自身设定，最大 50");
            }
            default -> {
                property.put("type", "string");
                property.put("description", parameter);
            }
        }
        return property;
    }

    /**
     * 构造只读工具注册表。
     *
     * @return 工具注册表
     */
    private static Map<String, ReadToolDescriptor> buildRegistry() {
        List<ReadToolDescriptor> descriptors = List.of(
                new ReadToolDescriptor("query_todo_backlog", MODULE_WORKFLOW, "todo_backlog",
                        "查询待办积压分布（按优先级统计未办结待办、超期数）。问“我的待办情况/积压多少/有没有超期”时调用。",
                        List.of(PARAM_SCOPE),
                        List.of("workflow:todo:list", "workflow:todo:handle")),
                new ReadToolDescriptor("query_todo_aging", MODULE_WORKFLOW, "todo_aging",
                        "查询滞留最久的待办明细。问“哪些待办压得最久/该先处理哪条”时调用。",
                        List.of(PARAM_SCOPE, PARAM_LIMIT),
                        List.of("workflow:todo:list", "workflow:todo:handle")),
                new ReadToolDescriptor("query_approval_duration", MODULE_WORKFLOW, "approval_duration",
                        "查询各审批节点的平均与最长耗时。问“审批慢在哪/哪个节点最耗时”时调用。",
                        List.of(PARAM_DAYS, PARAM_LIMIT),
                        List.of("workflow:instance:report", "workflow:instance:list", "workflow:todo:handle")),
                new ReadToolDescriptor("query_process_instance_stats", MODULE_WORKFLOW, "process_instance_stats",
                        "查询流程实例的发起量与状态分布（进行中/完成/驳回/撤销）。问“最近流程跑得怎么样/驳回率”时调用。",
                        List.of(PARAM_DAYS, PARAM_LIMIT),
                        List.of("workflow:instance:list", "workflow:instance:report")),
                new ReadToolDescriptor("query_user_workload", MODULE_WORKFLOW, "user_workload",
                        "查询各办理人手上的在办任务量排行。问“谁的活最多/负载是否均衡”时调用。",
                        List.of(PARAM_LIMIT),
                        List.of("workflow:instance:report", "workflow:instance:list")),
                new ReadToolDescriptor("query_approval_trend", MODULE_WORKFLOW, "approval_trend",
                        "查询近期每日审批动作趋势（通过/驳回）。问“最近审批量走势”时调用。",
                        List.of(PARAM_DAYS),
                        List.of("workflow:instance:report", "workflow:instance:list")),
                new ReadToolDescriptor("query_notice_overview", MODULE_SYSTEM, "notice_overview",
                        "查询系统消息按类型的未读分布与送达失败数。问“未读消息都是什么/哪类消息最多”时调用。",
                        List.of(PARAM_SCOPE, PARAM_LIMIT),
                        List.of("system:message:list", "system:message:read")),
                new ReadToolDescriptor("query_operation_trend", MODULE_SYSTEM, "operation_trend",
                        "查询近期系统写操作量、失败次数与平均耗时。问“系统最近忙不忙/有没有异常”时调用。",
                        List.of(PARAM_DAYS),
                        List.of("system:oper:list")),
                new ReadToolDescriptor("query_ai_usage_trend", MODULE_SYSTEM, "ai_usage_trend",
                        "查询近期 AI 调用量与 token 消耗。问“AI 用得多不多/花了多少 token”时调用。",
                        List.of(PARAM_DAYS),
                        List.of("system:ai:audit:list", "system:ai:ops:view")),
                new ReadToolDescriptor("query_stock_overview", MODULE_BUSINESS, "stock_overview",
                        "查询各仓库库存概览（品类数、即时/可用/冻结/在途库存）。问“库存情况/哪个仓存货多”时调用。",
                        List.of(PARAM_LIMIT),
                        List.of("business:inventory:ledger:list", "business:inventory:report:list")),
                new ReadToolDescriptor("query_stock_warning", MODULE_BUSINESS, "stock_warning",
                        "查询未关闭的库存预警分布。问“库存有什么预警/要不要补货”时调用。",
                        List.of(PARAM_LIMIT),
                        List.of("business:inventory:warning:list")),
                new ReadToolDescriptor("query_hr_headcount", MODULE_BUSINESS, "hr_headcount",
                        "查询在岗人数与岗位分布。问“公司多少人/各岗位人数”时调用。",
                        List.of(PARAM_LIMIT),
                        List.of("business:hr:employee:list")),
                new ReadToolDescriptor("query_hr_warning", MODULE_BUSINESS, "hr_warning",
                        "查询未关闭的 HR 预警（合同到期、试用期等）。问“HR 有什么风险/谁的合同快到期”时调用。",
                        List.of(PARAM_LIMIT),
                        List.of("business:hr:warning:list")));

        Map<String, ReadToolDescriptor> registry = new LinkedHashMap<>();
        for (ReadToolDescriptor descriptor : descriptors) {
            registry.put(descriptor.name(), descriptor);
        }
        return Collections.unmodifiableMap(registry);
    }

    private String nullToDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    /**
     * 只读工具描述。
     *
     * @param name        模型可见的工具名
     * @param module      所属模块
     * @param datasetKey  后端数据集编码
     * @param description 给模型看的用途说明
     * @param parameters  允许的参数名
     * @param permissions 所需权限候选集（拥有其一即可）
     */
    private record ReadToolDescriptor(String name,
            String module,
            String datasetKey,
            String description,
            List<String> parameters,
            List<String> permissions) {
    }
}
