package com.erp.business.common.service;

import com.erp.platform.contract.model.PlatformAiDataSet;
import com.erp.platform.contract.model.PlatformAiDataSetRequest;

import java.util.List;

/**
 * 业务域 AI 只读数据集服务。
 */
public interface IAiDatasetService {

    /**
     * 按数据集编码执行只读统计。
     *
     * @param request 数据集请求
     * @return 数据集结果
     */
    PlatformAiDataSet query(PlatformAiDataSetRequest request);

    /**
     * 列出本模块支持的数据集编码。
     *
     * @return 数据集编码列表
     */
    List<String> supportedKeys();
}
