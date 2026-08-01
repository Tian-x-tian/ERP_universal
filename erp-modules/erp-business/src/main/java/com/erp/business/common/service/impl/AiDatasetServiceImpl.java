package com.erp.business.common.service.impl;

import com.erp.business.common.mapper.AiDatasetMapper;
import com.erp.business.common.service.IAiDatasetService;
import com.erp.platform.contract.model.PlatformAiDataSet;
import com.erp.platform.contract.model.PlatformAiDataSetRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 业务域 AI 只读数据集服务实现。
 */
@Service
public class AiDatasetServiceImpl implements IAiDatasetService {
    private static final String KEY_STOCK_OVERVIEW = "stock_overview";
    private static final String KEY_STOCK_WARNING = "stock_warning";
    private static final String KEY_HR_HEADCOUNT = "hr_headcount";
    private static final String KEY_HR_WARNING = "hr_warning";

    private static final int MAX_ROW_LIMIT = 50;

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
            case KEY_STOCK_OVERVIEW -> buildStockOverview(params);
            case KEY_STOCK_WARNING -> buildStockWarning(params);
            case KEY_HR_HEADCOUNT -> buildHrHeadcount(params);
            case KEY_HR_WARNING -> buildHrWarning(params);
            default -> emptyDataSet(datasetKey, "业务域不支持的数据集：" + datasetKey);
        };
    }

    /**
     * 列出本模块支持的数据集编码。
     *
     * @return 数据集编码列表
     */
    @Override
    public List<String> supportedKeys() {
        return Arrays.asList(KEY_STOCK_OVERVIEW, KEY_STOCK_WARNING, KEY_HR_HEADCOUNT, KEY_HR_WARNING);
    }

    /**
     * 构造库存概览数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildStockOverview(Map<String, Object> params) {
        int limit = resolveLimit(params, 10);
        List<Map<String, Object>> records = aiDatasetMapper.selectStockOverview(limit);
        List<Map<String, Object>> lowStockRecords = aiDatasetMapper.selectLowStockItems(5);

        PlatformAiDataSet dataSet = newDataSet(KEY_STOCK_OVERVIEW, "各仓库库存概览");
        dataSet.addColumn("warehouseName", "仓库", "text")
                .addColumn("itemKinds", "物料品类数", "number")
                .addColumn("onHandQty", "即时库存", "number")
                .addColumn("availableQty", "可用库存", "number")
                .addColumn("frozenQty", "冻结库存", "number")
                .addColumn("inTransitQty", "在途库存", "number");
        dataSet.setChartHint("bar");
        dataSet.setChartCategoryKey("warehouseName");
        dataSet.setChartValueKey("availableQty");

        BigDecimal totalOnHand = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalFrozen = BigDecimal.ZERO;
        for (Map<String, Object> record : records) {
            BigDecimal onHandQty = asDecimal(record.get("on_hand_qty"));
            BigDecimal availableQty = asDecimal(record.get("available_qty"));
            BigDecimal frozenQty = asDecimal(record.get("frozen_qty"));
            totalOnHand = totalOnHand.add(onHandQty);
            totalAvailable = totalAvailable.add(availableQty);
            totalFrozen = totalFrozen.add(frozenQty);
            String warehouseName = asString(record.get("warehouse_name"));
            dataSet.addRow(StringUtils.hasText(warehouseName) ? warehouseName : "仓库#" + asString(record.get("warehouse_id")),
                    asLong(record.get("item_kinds")),
                    onHandQty,
                    availableQty,
                    frozenQty,
                    asDecimal(record.get("in_transit_qty")));
        }

        dataSet.addMetric("warehouses", "统计仓库", String.valueOf(records.size()), "个", "normal");
        dataSet.addMetric("onHand", "即时库存合计", totalOnHand.stripTrailingZeros().toPlainString(), null, "normal");
        dataSet.addMetric("frozen", "冻结库存合计", totalFrozen.stripTrailingZeros().toPlainString(), null,
                totalFrozen.signum() > 0 ? "warning" : "normal");

        StringBuilder summary = new StringBuilder();
        summary.append(records.isEmpty()
                ? "当前没有库存余额数据。"
                : "共 " + records.size() + " 个仓库有库存，可用合计 " + totalAvailable.stripTrailingZeros().toPlainString() + "。");
        if (!lowStockRecords.isEmpty()) {
            summary.append(" 可用库存最低的物料：");
            for (int index = 0; index < lowStockRecords.size(); index++) {
                if (index > 0) {
                    summary.append("、");
                }
                String itemName = asString(lowStockRecords.get(index).get("item_name"));
                summary.append(StringUtils.hasText(itemName) ? itemName : "物料#" + asString(lowStockRecords.get(index).get("item_id")))
                        .append("(")
                        .append(asDecimal(lowStockRecords.get(index).get("available_qty")).stripTrailingZeros().toPlainString())
                        .append(")");
            }
            summary.append("。");
        }
        dataSet.setSummary(summary.toString());
        return dataSet;
    }

    /**
     * 构造库存预警数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildStockWarning(Map<String, Object> params) {
        int limit = resolveLimit(params, 15);
        List<Map<String, Object>> records = aiDatasetMapper.selectStockWarning(limit);

        PlatformAiDataSet dataSet = newDataSet(KEY_STOCK_WARNING, "未关闭库存预警分布");
        dataSet.addColumn("warningType", "预警类型", "text")
                .addColumn("totalCount", "未关闭数", "number")
                .addColumn("newCount", "待处理", "number");
        dataSet.setChartHint("pie");
        dataSet.setChartCategoryKey("warningType");
        dataSet.setChartValueKey("totalCount");

        long total = 0L;
        long pending = 0L;
        for (Map<String, Object> record : records) {
            long totalCount = asLong(record.get("total_count"));
            long newCount = asLong(record.get("new_count"));
            total += totalCount;
            pending += newCount;
            dataSet.addRow(asString(record.get("warning_type")), totalCount, newCount);
        }
        dataSet.addMetric("total", "未关闭预警", String.valueOf(total), "条", total > 0 ? "warning" : "success");
        dataSet.addMetric("pending", "待处理", String.valueOf(pending), "条", pending > 0 ? "danger" : "success");
        dataSet.setSummary(total == 0
                ? "当前没有未关闭的库存预警。"
                : "共 " + total + " 条未关闭库存预警，其中 " + pending + " 条尚未处理。");
        return dataSet;
    }

    /**
     * 构造在岗人数数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildHrHeadcount(Map<String, Object> params) {
        int limit = resolveLimit(params, 15);
        List<Map<String, Object>> records = aiDatasetMapper.selectHeadcountByPost(limit);
        Map<String, Object> summaryRecord = aiDatasetMapper.selectHeadcountSummary();
        Map<String, Object> safeSummary = summaryRecord == null ? Collections.emptyMap() : summaryRecord;

        PlatformAiDataSet dataSet = newDataSet(KEY_HR_HEADCOUNT, "在岗人数分布");
        dataSet.addColumn("postName", "岗位", "text")
                .addColumn("headcount", "在岗人数", "number");
        dataSet.setChartHint("bar");
        dataSet.setChartCategoryKey("postName");
        dataSet.setChartValueKey("headcount");

        for (Map<String, Object> record : records) {
            dataSet.addRow(asString(record.get("post_name")), asLong(record.get("headcount")));
        }

        long active = asLong(safeSummary.get("active_count"));
        long inactive = asLong(safeSummary.get("inactive_count"));
        dataSet.addMetric("active", "在岗人数", String.valueOf(active), "人", "normal");
        dataSet.addMetric("inactive", "非在岗", String.valueOf(inactive), "人", "normal");
        dataSet.setSummary(active == 0
                ? "当前没有在岗员工数据。"
                : "当前在岗 " + active + " 人，覆盖 " + records.size() + " 个岗位。");
        return dataSet;
    }

    /**
     * 构造 HR 预警数据集。
     *
     * @param params 查询参数
     * @return 数据集
     */
    private PlatformAiDataSet buildHrWarning(Map<String, Object> params) {
        int limit = resolveLimit(params, 15);
        List<Map<String, Object>> records = aiDatasetMapper.selectHrWarning(limit);
        List<Map<String, Object>> detailRecords = aiDatasetMapper.selectHrWarningDetail(5);

        PlatformAiDataSet dataSet = newDataSet(KEY_HR_WARNING, "未关闭 HR 预警分布");
        dataSet.addColumn("warningType", "预警类型", "text")
                .addColumn("totalCount", "未关闭数", "number")
                .addColumn("newCount", "待处理", "number");
        dataSet.setChartHint("pie");
        dataSet.setChartCategoryKey("warningType");
        dataSet.setChartValueKey("totalCount");

        long total = 0L;
        long pending = 0L;
        for (Map<String, Object> record : records) {
            long totalCount = asLong(record.get("total_count"));
            long newCount = asLong(record.get("new_count"));
            total += totalCount;
            pending += newCount;
            dataSet.addRow(asString(record.get("warning_type")), totalCount, newCount);
        }
        dataSet.addMetric("total", "未关闭预警", String.valueOf(total), "条", total > 0 ? "warning" : "success");
        dataSet.addMetric("pending", "待处理", String.valueOf(pending), "条", pending > 0 ? "danger" : "success");

        StringBuilder summary = new StringBuilder();
        summary.append(total == 0 ? "当前没有未关闭的 HR 预警。" : "共 " + total + " 条未关闭 HR 预警。");
        if (!detailRecords.isEmpty()) {
            summary.append(" 最近到期：");
            for (int index = 0; index < detailRecords.size(); index++) {
                if (index > 0) {
                    summary.append("、");
                }
                summary.append(asString(detailRecords.get(index).get("warning_title")));
            }
            summary.append("。");
        }
        dataSet.setSummary(summary.toString());
        return dataSet;
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
