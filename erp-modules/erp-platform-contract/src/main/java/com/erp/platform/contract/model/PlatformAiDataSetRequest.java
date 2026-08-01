package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 只读数据集查询请求。
 *
 * <p>各业务服务对外只暴露一个数据集调度端点，由 datasetKey 分发到具体实现，
 * 避免为每个 AI 只读工具单独开一个内部接口。</p>
 */
public class PlatformAiDataSetRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 数据集编码 */
    private String datasetKey;
    /** 查询参数 */
    private Map<String, Object> params = new LinkedHashMap<>();

    public PlatformAiDataSetRequest() {
    }

    public PlatformAiDataSetRequest(String datasetKey, Map<String, Object> params) {
        this.datasetKey = datasetKey;
        this.params = params == null ? new LinkedHashMap<>() : params;
    }

    public String getDatasetKey() {
        return datasetKey;
    }

    public void setDatasetKey(String datasetKey) {
        this.datasetKey = datasetKey;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? new LinkedHashMap<>() : params;
    }
}
