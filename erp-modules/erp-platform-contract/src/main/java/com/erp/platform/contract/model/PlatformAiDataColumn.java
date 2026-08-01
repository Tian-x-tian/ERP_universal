package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * AI 只读数据集列定义。
 */
public class PlatformAiDataColumn implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 列编码，对应行数据的 key */
    private String key;
    /** 列展示名称 */
    private String label;
    /** 列类型（text/number/date/percent） */
    private String type;

    public PlatformAiDataColumn() {
    }

    public PlatformAiDataColumn(String key, String label, String type) {
        this.key = key;
        this.label = label;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
