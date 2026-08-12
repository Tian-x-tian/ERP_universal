package com.erp.system.service;

import com.erp.platform.contract.model.PlatformAiDataSet;
import com.erp.platform.contract.model.PlatformAiDataSetRequest;
import com.erp.platform.contract.model.PlatformAiQuotaUsage;

import java.util.List;

/**
 * 平台域 AI 只读数据集服务。
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

    /**
     * 查询当日 AI 用量。
     *
     * @param userId 当前用户ID
     * @return 用量统计
     */
    PlatformAiQuotaUsage queryQuotaUsage(Long userId);
}
