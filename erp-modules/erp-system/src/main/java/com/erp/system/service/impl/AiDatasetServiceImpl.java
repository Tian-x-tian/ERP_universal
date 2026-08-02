package com.erp.system.service.impl;

import com.erp.platform.contract.model.PlatformAiDataSet;
import com.erp.platform.contract.model.PlatformAiDataSetRequest;
import com.erp.system.mapper.AiDatasetMapper;
import com.erp.system.service.IAiDatasetService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 平台域 AI 只读数据集服务实现。
 */
@Service
public class AiDatasetServiceImpl implements IAiDatasetService {
    private static final String KEY_NOTICE_OVERVIEW = "notice_overview";
    private static final String KEY_OPERATION_TREND = "operation_trend";
    private static final String KEY_AI_USAGE_TREND = "ai_usage_trend";

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
            case KEY_NOTICE_OVERVIEW -> buildNoticeOverview(params);
            case KEY_OPERATION_TREND -> buildOperationTrend(params);
            case KEY_AI_USAGE_TREND -> buildAiUsageTrend(params);
            default -> emptyDataSet(datasetKey, "平台域不支持的数据集：" + datasetKey);
        };
    }

    /**
     * 列出本模块支持的数据集编码。
     *
     * @return 数据集编码列表
     */
    @Override
    public List<String> supportedKeys() {
        return Arrays.asList(KEY_NOTICE_OVERVIEW, KEY_OPERATION_TREND, KEY_AI_USAGE_TREND);
    }

    /**
     * 构造系统消息分布数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildNoticeOverview(Map<String, Object> params) {
        Long receiverUserId = resolveScopedUserId(params);
        int limit = resolveLimit(params, 15);
        List<Map<String, Object>> typeRecords = aiDatasetMapper.selectNoticeOverview(receiverUserId, limit);
        List<Map<String, Object>> sourceRecords = aiDatasetMapper.selectUnreadNoticeSource(receiverUserId, 5);

        PlatformAiDataSet dataSet = newDataSet(KEY_NOTICE_OVERVIEW,
                receiverUserId == null ? "全租户消息分布" : "我的消息分布");
        dataSet.addColumn("noticeType", "消息类型", "text")
                .addColumn("totalCount", "消息总数", "number")
                .addColumn("unreadCount", "未读数", "number")
                .addColumn("failedCount", "送达失败", "number");
        dataSet.setChartHint("pie");
        dataSet.setChartCategoryKey("noticeType");
        dataSet.setChartValueKey("unreadCount");

        long total = 0L;
        long unread = 0L;
        long failed = 0L;
        for (Map<String, Object> record : typeRecords) {
            long totalCount = asLong(record.get("total_count"));
            long unreadCount = asLong(record.get("unread_count"));
            long failedCount = asLong(record.get("failed_count"));
            total += totalCount;
            unread += unreadCount;
            failed += failedCount;
            dataSet.addRow(asString(record.get("notice_type")), totalCount, unreadCount, failedCount);
        }

        dataSet.addMetric("unread", "未读消息", String.valueOf(unread), "条", unread > 0 ? "warning" : "success");
        dataSet.addMetric("total", "消息总数", String.valueOf(total), "条", "normal");
        dataSet.addMetric("failed", "送达失败", String.valueOf(failed), "条", failed > 0 ? "danger" : "normal");

        StringBuilder summary = new StringBuilder();
        summary.append(unread == 0 ? "当前没有未读消息。" : "当前有 " + unread + " 条未读消息。");
        if (!sourceRecords.isEmpty()) {
            summary.append(" 未读来源集中在：");
            for (int index = 0; index < sourceRecords.size(); index++) {
                if (index > 0) {
                    summary.append("、");
                }
                summary.append(asString(sourceRecords.get(index).get("source_name")))
                        .append("(")
                        .append(asLong(sourceRecords.get(index).get("unread_count")))
                        .append(")");
            }
            summary.append("。");
        }
        dataSet.setSummary(summary.toString());
        return dataSet;
    }

    /**
     * 构造操作日志趋势数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildOperationTrend(Map<String, Object> params) {
        int days = resolveDays(params, 14);
        List<Map<String, Object>> records = aiDatasetMapper.selectOperationTrend(sinceDate(days));

        PlatformAiDataSet dataSet = newDataSet(KEY_OPERATION_TREND, "近 " + days + " 天系统写操作趋势");
        dataSet.addColumn("operationDate", "日期", "date")
                .addColumn("totalCount", "操作次数", "number")
                .addColumn("failedCount", "失败次数", "number")
                .addColumn("avgCostMs", "平均耗时(ms)", "number");
        dataSet.setChartHint("line");
        dataSet.setChartCategoryKey("operationDate");
        dataSet.setChartValueKey("totalCount");

        long total = 0L;
        long failed = 0L;
        for (Map<String, Object> record : records) {
            long totalCount = asLong(record.get("total_count"));
            long failedCount = asLong(record.get("failed_count"));
            total += totalCount;
            failed += failedCount;
            dataSet.addRow(record.get("operation_date"), totalCount, failedCount, asLong(record.get("avg_cost_ms")));
        }
        dataSet.addMetric("total", "操作总数", String.valueOf(total), "次", "normal");
        dataSet.addMetric("failed", "失败次数", String.valueOf(failed), "次", failed > 0 ? "warning" : "success");
        dataSet.setSummary(total == 0
                ? "近 " + days + " 天没有写操作记录。"
                : "近 " + days + " 天共 " + total + " 次写操作，其中失败 " + failed + " 次。");
        return dataSet;
    }

    /**
     * 构造 AI 使用量数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildAiUsageTrend(Map<String, Object> params) {
        int days = resolveDays(params, 14);
        List<Map<String, Object>> records = aiDatasetMapper.selectAiUsageTrend(sinceDate(days));

        PlatformAiDataSet dataSet = newDataSet(KEY_AI_USAGE_TREND, "近 " + days + " 天 AI 使用量");
        dataSet.addColumn("statDate", "日期", "date")
                .addColumn("requestCount", "调用次数", "number")
                .addColumn("failedCount", "失败次数", "number")
                .addColumn("totalTokens", "Token 消耗", "number")
                .addColumn("avgDurationMs", "平均耗时(ms)", "number");
        dataSet.setChartHint("line");
        dataSet.setChartCategoryKey("statDate");
        dataSet.setChartValueKey("requestCount");

        long requests = 0L;
        long tokens = 0L;
        long failed = 0L;
        for (Map<String, Object> record : records) {
            long requestCount = asLong(record.get("request_count"));
            long totalTokens = asLong(record.get("total_tokens"));
            long failedCount = asLong(record.get("failed_count"));
            requests += requestCount;
            tokens += totalTokens;
            failed += failedCount;
            dataSet.addRow(record.get("stat_date"), requestCount, failedCount, totalTokens,
                    asLong(record.get("avg_duration_ms")));
        }
        dataSet.addMetric("requests", "调用次数", String.valueOf(requests), "次", "normal");
        dataSet.addMetric("tokens", "Token 消耗", String.valueOf(tokens), "个", "normal");
        dataSet.addMetric("failed", "失败次数", String.valueOf(failed), "次", failed > 0 ? "warning" : "success");
        dataSet.setSummary(requests == 0
                ? "近 " + days + " 天没有 AI 调用记录。"
                : "近 " + days + " 天共调用 AI " + requests + " 次，消耗 " + tokens + " 个 token。");
        return dataSet;
    }

    /**
     * 解析统计范围对应的用户ID。
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
