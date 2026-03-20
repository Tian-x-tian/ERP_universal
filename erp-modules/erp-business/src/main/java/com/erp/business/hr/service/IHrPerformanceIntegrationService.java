package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrPerformanceFieldMapping;
import com.erp.business.hr.domain.HrPerformanceSyncLog;
import com.erp.business.hr.domain.vo.HrPerformanceCallbackBody;
import com.erp.business.hr.domain.vo.HrPerformancePushBody;

import java.util.List;

/**
 * 绩效考核同步服务接口。
 */
public interface IHrPerformanceIntegrationService {

    /**
     * 查询映射配置。
     *
     * @return 映射配置列表
     */
    List<HrPerformanceFieldMapping> listMappings();

    /**
     * 保存映射配置。
     *
     * @param mappings 映射配置
     * @return 最新配置
     */
    List<HrPerformanceFieldMapping> saveMappings(List<HrPerformanceFieldMapping> mappings);

    /**
     * 发起绩效推送。
     *
     * @param body 推送参数
     * @return 生成的同步日志
     */
    List<HrPerformanceSyncLog> pushPerformance(HrPerformancePushBody body);

    /**
     * 处理绩效回传。
     *
     * @param body 回传参数
     * @return 最新日志
     */
    HrPerformanceSyncLog callback(HrPerformanceCallbackBody body);

    /**
     * 分页查询同步日志。
     *
     * @param periodCode 期间
     * @param syncStatus 状态
     * @param employeeId 员工ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<HrPerformanceSyncLog> selectLogPage(String periodCode, String syncStatus, Long employeeId, Long pageNum, Long pageSize);

    /**
     * 重试指定日志。
     *
     * @param logId 日志ID
     * @return 最新日志
     */
    HrPerformanceSyncLog retry(Long logId);
}

