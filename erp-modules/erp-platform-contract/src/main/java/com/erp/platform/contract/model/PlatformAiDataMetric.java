package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * AI 只读数据集指标项，用于面板指标卡渲染。
 */
public class PlatformAiDataMetric implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 指标编码 */
    private String key;
    /** 指标名称 */
    private String label;
    /** 指标值（统一以字符串承载，避免精度问题） */
    private String value;
    /** 指标单位 */
    private String unit;
    /** 展示语义（normal/success/warning/danger） */
    private String tone;

    public PlatformAiDataMetric() {
    }

    public PlatformAiDataMetric(String key, String label, String value, String unit, String tone) {
        this.key = key;
        this.label = label;
        this.value = value;
        this.unit = unit;
        this.tone = tone;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }
}
