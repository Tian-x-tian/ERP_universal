package com.erp.workflow.service.impl;

import com.erp.platform.contract.model.PlatformAiDataSet;
import com.erp.platform.contract.model.PlatformAiDataSetRequest;
import com.erp.workflow.mapper.AiDatasetMapper;
import com.erp.workflow.service.IAiDatasetService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 流程域 AI 只读数据集服务实现。
 */
@Service
public class AiDatasetServiceImpl implements IAiDatasetService {
    private static final String KEY_TODO_BACKLOG = "todo_backlog";
    private static final String KEY_TODO_AGING = "todo_aging";
    private static final String KEY_APPROVAL_DURATION = "approval_duration";
    private static final String KEY_INSTANCE_STATS = "process_instance_stats";
    private static final String KEY_USER_WORKLOAD = "user_workload";
    private static final String KEY_APPROVAL_TREND = "approval_trend";

    private static final int MAX_ROW_LIMIT = 50;
    private static final int MAX_DAY_RANGE = 180;

    private final AiDatasetMapper aiDatasetMapper;

    public AiDatasetServiceImpl(AiDatasetMapper aiDatasetMapper) {
        this.aiDatasetMapper = aiDatasetMapper;
    }

    /**
     * 按数据集编码执行只读统计。
     *
     * @param request 数据集请求
     * @return 数据集结果
     */
    @Override
    public PlatformAiDataSet query(PlatformAiDataSetRequest request) {
        String datasetKey = request == null ? null : trimToNull(request.getDatasetKey());
        Map<String, Object> params = request == null ? Map.of() : request.getParams();
        if (!StringUtils.hasText(datasetKey)) {
            return emptyDataSet(null, "未指定数据集编码");
        }
        return switch (datasetKey) {
            case KEY_TODO_BACKLOG -> buildTodoBacklog(params);
            case KEY_TODO_AGING -> buildTodoAging(params);
            case KEY_APPROVAL_DURATION -> buildApprovalDuration(params);
            case KEY_INSTANCE_STATS -> buildInstanceStats(params);
            case KEY_USER_WORKLOAD -> buildUserWorkload(params);
            case KEY_APPROVAL_TREND -> buildApprovalTrend(params);
            default -> emptyDataSet(datasetKey, "流程域不支持的数据集：" + datasetKey);
        };
    }

    /**
     * 列出本模块支持的数据集编码。
     *
     * @return 数据集编码列表
     */
    @Override
    public List<String> supportedKeys() {
        return Arrays.asList(KEY_TODO_BACKLOG, KEY_TODO_AGING, KEY_APPROVAL_DURATION,
                KEY_INSTANCE_STATS, KEY_USER_WORKLOAD, KEY_APPROVAL_TREND);
    }

    /**
     * 构造待办积压数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildTodoBacklog(Map<String, Object> params) {
        Long assigneeUserId = resolveScopedUserId(params);
        List<Map<String, Object>> records = aiDatasetMapper.selectTodoBacklog(assigneeUserId);

        PlatformAiDataSet dataSet = newDataSet(KEY_TODO_BACKLOG,
                assigneeUserId == null ? "全租户待办积压分布" : "我的待办积压分布");
        dataSet.addColumn("priorityLabel", "优先级", "text")
                .addColumn("totalCount", "待办总数", "number")
                .addColumn("pendingCount", "待处理", "number")
                .addColumn("processingCount", "处理中", "number")
                .addColumn("overdueCount", "已超期", "number");
        dataSet.setChartHint("bar");
        dataSet.setChartCategoryKey("priorityLabel");
        dataSet.setChartValueKey("totalCount");

        long total = 0L;
        long overdue = 0L;
        long high = 0L;
        for (Map<String, Object> record : records) {
            String priority = asString(record.get("priority"));
            long totalCount = asLong(record.get("total_count"));
            long overdueCount = asLong(record.get("overdue_count"));
            total += totalCount;
            overdue += overdueCount;
            if ("H".equalsIgnoreCase(priority)) {
                high += totalCount;
            }
            dataSet.addRow(priorityLabel(priority),
                    totalCount,
                    asLong(record.get("pending_count")),
                    asLong(record.get("processing_count")),
                    overdueCount);
        }

        dataSet.addMetric("total", "待办总数", String.valueOf(total), "条", "normal");
        dataSet.addMetric("high", "高优先级", String.valueOf(high), "条", high > 0 ? "warning" : "normal");
        dataSet.addMetric("overdue", "已超期", String.valueOf(overdue), "条", overdue > 0 ? "danger" : "success");
        dataSet.setSummary(total == 0
                ? "当前没有未办结待办。"
                : "共 " + total + " 条未办结待办，其中高优先级 " + high + " 条、已超期 " + overdue + " 条。");
        return dataSet;
    }

    /**
     * 构造待办滞留数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildTodoAging(Map<String, Object> params) {
        Long assigneeUserId = resolveScopedUserId(params);
        int limit = resolveLimit(params, 10);
        List<Map<String, Object>> records = aiDatasetMapper.selectTodoAging(assigneeUserId, limit);

        PlatformAiDataSet dataSet = newDataSet(KEY_TODO_AGING, "滞留最久的待办");
        dataSet.addColumn("processName", "流程", "text")
                .addColumn("nodeName", "节点", "text")
                .addColumn("businessNo", "业务单号", "text")
                .addColumn("priorityLabel", "优先级", "text")
                .addColumn("agingHours", "滞留小时", "number")
                .addColumn("dueTime", "截止时间", "date");

        long maxAging = 0L;
        for (Map<String, Object> record : records) {
            long agingHours = asLong(record.get("aging_hours"));
            maxAging = Math.max(maxAging, agingHours);
            dataSet.addRow(asString(record.get("process_name")),
                    asString(record.get("node_name")),
                    asString(record.get("business_no")),
                    priorityLabel(asString(record.get("priority"))),
                    agingHours,
                    record.get("due_time"));
        }
        dataSet.addMetric("count", "在办待办", String.valueOf(records.size()), "条", "normal");
        dataSet.addMetric("maxAging", "最长滞留", String.valueOf(maxAging), "小时",
                maxAging >= 72 ? "danger" : maxAging >= 24 ? "warning" : "normal");
        dataSet.setSummary(records.isEmpty()
                ? "当前没有滞留中的待办。"
                : "滞留最久的待办已挂起 " + maxAging + " 小时，建议优先清理。");
        dataSet.setTruncated(records.size() >= limit);
        return dataSet;
    }

    /**
     * 构造审批节点耗时数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildApprovalDuration(Map<String, Object> params) {
        int days = resolveDays(params, 30);
        int limit = resolveLimit(params, 10);
        List<Map<String, Object>> records = aiDatasetMapper.selectApprovalDuration(sinceDate(days), limit);

        PlatformAiDataSet dataSet = newDataSet(KEY_APPROVAL_DURATION, "近 " + days + " 天审批节点耗时");
        dataSet.addColumn("nodeName", "节点", "text")
                .addColumn("finishedCount", "办结数", "number")
                .addColumn("avgMinutes", "平均耗时(分钟)", "number")
                .addColumn("maxMinutes", "最长耗时(分钟)", "number");
        dataSet.setChartHint("bar");
        dataSet.setChartCategoryKey("nodeName");
        dataSet.setChartValueKey("avgMinutes");

        String slowestNode = null;
        BigDecimal slowestAvg = BigDecimal.ZERO;
        for (Map<String, Object> record : records) {
            BigDecimal avgMinutes = asDecimal(record.get("avg_minutes"));
            String nodeName = asString(record.get("node_name"));
            if (avgMinutes.compareTo(slowestAvg) > 0) {
                slowestAvg = avgMinutes;
                slowestNode = nodeName;
            }
            dataSet.addRow(nodeName,
                    asLong(record.get("finished_count")),
                    avgMinutes,
                    asLong(record.get("max_minutes")));
        }
        dataSet.addMetric("nodes", "统计节点数", String.valueOf(records.size()), "个", "normal");
        if (slowestNode != null) {
            dataSet.addMetric("slowest", "最慢节点", slowestNode, null, "warning");
        }
        dataSet.setSummary(records.isEmpty()
                ? "近 " + days + " 天没有已办结的审批任务。"
                : "最慢节点是「" + slowestNode + "」，平均耗时 " + slowestAvg + " 分钟。");
        return dataSet;
    }

    /**
     * 构造流程实例状态分布数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildInstanceStats(Map<String, Object> params) {
        int days = resolveDays(params, 30);
        int limit = resolveLimit(params, 15);
        List<Map<String, Object>> records = aiDatasetMapper.selectInstanceStats(sinceDate(days), limit);

        PlatformAiDataSet dataSet = newDataSet(KEY_INSTANCE_STATS, "近 " + days + " 天流程实例分布");
        dataSet.addColumn("processName", "流程", "text")
                .addColumn("totalCount", "发起总数", "number")
                .addColumn("runningCount", "进行中", "number")
                .addColumn("finishedCount", "已完成", "number")
                .addColumn("rejectedCount", "已驳回", "number")
                .addColumn("abortedCount", "已撤销", "number");
        dataSet.setChartHint("bar");
        dataSet.setChartCategoryKey("processName");
        dataSet.setChartValueKey("totalCount");

        long total = 0L;
        long running = 0L;
        long rejected = 0L;
        for (Map<String, Object> record : records) {
            long totalCount = asLong(record.get("total_count"));
            long runningCount = asLong(record.get("running_count"));
            long rejectedCount = asLong(record.get("rejected_count"));
            total += totalCount;
            running += runningCount;
            rejected += rejectedCount;
            dataSet.addRow(asString(record.get("process_name")),
                    totalCount,
                    runningCount,
                    asLong(record.get("finished_count")),
                    rejectedCount,
                    asLong(record.get("aborted_count")));
        }
        dataSet.addMetric("total", "发起总数", String.valueOf(total), "笔", "normal");
        dataSet.addMetric("running", "进行中", String.valueOf(running), "笔", "normal");
        dataSet.addMetric("rejectRate", "驳回率",
                total == 0 ? "0" : BigDecimal.valueOf(rejected * 100.0 / total).setScale(1, RoundingMode.HALF_UP).toPlainString(),
                "%", rejected * 5 > total ? "warning" : "normal");
        dataSet.setSummary(total == 0
                ? "近 " + days + " 天没有流程发起记录。"
                : "近 " + days + " 天共发起 " + total + " 笔流程，其中 " + running + " 笔仍在进行中。");
        return dataSet;
    }

    /**
     * 构造人员在办负载数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildUserWorkload(Map<String, Object> params) {
        int limit = resolveLimit(params, 10);
        List<Map<String, Object>> records = aiDatasetMapper.selectUserWorkload(limit);

        PlatformAiDataSet dataSet = newDataSet(KEY_USER_WORKLOAD, "人员在办任务负载排行");
        dataSet.addColumn("assigneeName", "办理人", "text")
                .addColumn("openTaskCount", "在办任务", "number")
                .addColumn("overdueCount", "已超期", "number");
        dataSet.setChartHint("bar");
        dataSet.setChartCategoryKey("assigneeName");
        dataSet.setChartValueKey("openTaskCount");

        long total = 0L;
        for (Map<String, Object> record : records) {
            String nickName = asString(record.get("assignee_nick_name"));
            String userName = asString(record.get("assignee_user_name"));
            String displayName = StringUtils.hasText(nickName) ? nickName
                    : StringUtils.hasText(userName) ? userName
                            : "用户#" + asString(record.get("assignee_user_id"));
            long openTaskCount = asLong(record.get("open_task_count"));
            total += openTaskCount;
            dataSet.addRow(displayName, openTaskCount, asLong(record.get("overdue_count")));
        }
        dataSet.addMetric("people", "在办人数", String.valueOf(records.size()), "人", "normal");
        dataSet.addMetric("total", "在办任务", String.valueOf(total), "条", "normal");
        dataSet.setSummary(records.isEmpty()
                ? "当前没有在办的流程任务。"
                : "共 " + records.size() + " 人手上有在办任务，合计 " + total + " 条。");
        return dataSet;
    }

    /**
     * 构造审批动作趋势数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildApprovalTrend(Map<String, Object> params) {
        int days = resolveDays(params, 14);
        List<Map<String, Object>> records = aiDatasetMapper.selectApprovalTrend(sinceDate(days));

        PlatformAiDataSet dataSet = newDataSet(KEY_APPROVAL_TREND, "近 " + days + " 天审批动作趋势");
        dataSet.addColumn("actionDate", "日期", "date")
                .addColumn("totalCount", "动作总数", "number")
                .addColumn("approveCount", "通过", "number")
                .addColumn("rejectCount", "驳回", "number");
        dataSet.setChartHint("line");
        dataSet.setChartCategoryKey("actionDate");
        dataSet.setChartValueKey("totalCount");

        long total = 0L;
        long approve = 0L;
        long reject = 0L;
        for (Map<String, Object> record : records) {
            long totalCount = asLong(record.get("total_count"));
            long approveCount = asLong(record.get("approve_count"));
            long rejectCount = asLong(record.get("reject_count"));
            total += totalCount;
            approve += approveCount;
            reject += rejectCount;
            dataSet.addRow(record.get("action_date"), totalCount, approveCount, rejectCount);
        }
        dataSet.addMetric("total", "动作总数", String.valueOf(total), "次", "normal");
        dataSet.addMetric("approve", "通过", String.valueOf(approve), "次", "success");
        dataSet.addMetric("reject", "驳回", String.valueOf(reject), "次", reject > 0 ? "warning" : "normal");
        dataSet.setSummary(total == 0
                ? "近 " + days + " 天没有审批动作记录。"
                : "近 " + days + " 天共产生 " + total + " 次审批动作，通过 " + approve + " 次、驳回 " + reject + " 次。");
        return dataSet;
    }

    /**
     * 解析统计范围对应的用户ID。
     *
     * <p>scope=mine 时按传入的 userId 过滤；scope=tenant 时统计全租户。</p>
     *
     * @param params 查询参数
     * @return 用户ID，为空表示全租户
     */
    private Long resolveScopedUserId(Map<String, Object> params) {
        String scope = asString(params.get("scope"));
        if ("tenant".equalsIgnoreCase(scope)) {
            return null;
        }
        Object userId = params.get("userId");
        if (userId == null) {
            return null;
        }
        long parsed = asLong(userId);
        return parsed <= 0 ? null : parsed;
    }

    /**
     * 解析行数上限。
     *
     * @param params       查询参数
     * @param defaultLimit 默认值
     * @return 行数上限
     */
    private int resolveLimit(Map<String, Object> params, int defaultLimit) {
        long limit = asLong(params.get("limit"));
        if (limit <= 0) {
            return defaultLimit;
        }
        return (int) Math.min(limit, MAX_ROW_LIMIT);
    }

    /**
     * 解析统计天数。
     *
     * @param params      查询参数
     * @param defaultDays 默认值
     * @return 统计天数
     */
    private int resolveDays(Map<String, Object> params, int defaultDays) {
        long days = asLong(params.get("days"));
        if (days <= 0) {
            return defaultDays;
        }
        return (int) Math.min(days, MAX_DAY_RANGE);
    }

    /**
     * 计算统计起始时间。
     *
     * @param days 统计天数
     * @return 起始时间
     */
    private Date sinceDate(int days) {
        LocalDate startDate = LocalDate.now().minusDays(Math.max(0, days - 1L));
        return Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 构造带标题的空白数据集。
     *
     * @param key   数据集编码
     * @param title 数据集标题
     * @return 数据集
     */
    private PlatformAiDataSet newDataSet(String key, String title) {
        PlatformAiDataSet dataSet = new PlatformAiDataSet();
        dataSet.setKey(key);
        dataSet.setTitle(title);
        return dataSet;
    }

    /**
     * 构造异常提示数据集。
     *
     * @param key     数据集编码
     * @param message 提示文案
     * @return 数据集
     */
    private PlatformAiDataSet emptyDataSet(String key, String message) {
        PlatformAiDataSet dataSet = newDataSet(key, "无可用数据");
        dataSet.setMessage(message);
        dataSet.setSummary(message);
        return dataSet;
    }

    /**
     * 翻译优先级标签。
     *
     * @param priority 优先级编码
     * @return 优先级标签
     */
    private String priorityLabel(String priority) {
        if ("H".equalsIgnoreCase(priority)) {
            return "高";
        }
        if ("M".equalsIgnoreCase(priority)) {
            return "中";
        }
        if ("L".equalsIgnoreCase(priority)) {
            return "低";
        }
        return StringUtils.hasText(priority) ? priority : "未设置";
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
