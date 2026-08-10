package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 只读数据集，是所有 AI 只读工具（read tool）的统一返回结构。
 *
 * <p>同一份数据集有两个消费方：一是序列化成紧凑文本回灌给模型继续推理，
 * 二是原样通过 SSE 推给前端渲染成指标卡 / 表格 / 图表。</p>
 */
public class PlatformAiDataSet implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 数据集编码，与只读工具名一致 */
    private String key;
    /** 数据集标题 */
    private String title;
    /** 服务端生成的一句话结论 */
    private String summary;
    /** 列定义 */
    private List<PlatformAiDataColumn> columns = new ArrayList<>();
    /** 行数据 */
    private List<Map<String, Object>> rows = new ArrayList<>();
    /** 指标卡 */
    private List<PlatformAiDataMetric> metrics = new ArrayList<>();
    /** 建议图表类型（none/bar/line/pie） */
    private String chartHint = "none";
    /** 图表分类轴取值列 */
    private String chartCategoryKey;
    /** 图表数值轴取值列 */
    private String chartValueKey;
    /** 是否因为行数上限被截断 */
    private boolean truncated;
    /** 空结果或异常时的说明文案 */
    private String message;

    /**
     * 追加一列。
     *
     * @param key   列编码
     * @param label 列名称
     * @param type  列类型
     * @return 当前数据集
     */
    public PlatformAiDataSet addColumn(String key, String label, String type) {
        this.columns.add(new PlatformAiDataColumn(key, label, type));
        return this;
    }

    /**
     * 追加一个指标。
     *
     * @param key   指标编码
     * @param label 指标名称
     * @param value 指标值
     * @param unit  指标单位
     * @param tone  展示语义
     * @return 当前数据集
     */
    public PlatformAiDataSet addMetric(String key, String label, String value, String unit, String tone) {
        this.metrics.add(new PlatformAiDataMetric(key, label, value, unit, tone));
        return this;
    }

    /**
     * 追加一行数据，按列定义顺序取值。
     *
     * @param values 与列定义等长的取值数组
     * @return 当前数据集
     */
    public PlatformAiDataSet addRow(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (values != null) {
            for (int index = 0; index < values.length && index < columns.size(); index++) {
                row.put(columns.get(index).getKey(), values[index]);
            }
        }
        this.rows.add(row);
        return this;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<PlatformAiDataColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<PlatformAiDataColumn> columns) {
        this.columns = columns == null ? new ArrayList<>() : columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }

    public List<PlatformAiDataMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<PlatformAiDataMetric> metrics) {
        this.metrics = metrics == null ? new ArrayList<>() : metrics;
    }

    public String getChartHint() {
        return chartHint;
    }

    public void setChartHint(String chartHint) {
        this.chartHint = chartHint;
    }

    public String getChartCategoryKey() {
        return chartCategoryKey;
    }

    public void setChartCategoryKey(String chartCategoryKey) {
        this.chartCategoryKey = chartCategoryKey;
    }

    public String getChartValueKey() {
        return chartValueKey;
    }

    public void setChartValueKey(String chartValueKey) {
        this.chartValueKey = chartValueKey;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
