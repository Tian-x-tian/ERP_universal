package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrSalaryFieldMapping;
import com.erp.business.hr.domain.HrSalarySyncLog;
import com.erp.business.hr.domain.vo.HrSalaryCallbackBody;
import com.erp.business.hr.domain.vo.HrSalaryPushBody;

import java.util.List;

/**
 * 薪酬核算同步服务接口。
 */
public interface IHrSalaryIntegrationService {

    /**
     * 查询映射配置。
     *
     * @return 映射配置列表
     */
    List<HrSalaryFieldMapping> listMappings();

    /**
     * 保存映射配置。
     *
     * @param mappings 映射配置
     * @return 最新配置
     */
    List<HrSalaryFieldMapping> saveMappings(List<HrSalaryFieldMapping> mappings);

    /**
     * 发起薪酬推送。
     *
     * @param body 推送参数
     * @return 生成的同步日志
     */
    List<HrSalarySyncLog> pushSalary(HrSalaryPushBody body);

    /**
     * 处理薪酬回传。
     *
     * @param body 回传参数
     * @return 最新日志
     */
    HrSalarySyncLog callback(HrSalaryCallbackBody body);

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
    Page<HrSalarySyncLog> selectLogPage(String periodCode, String syncStatus, Long employeeId, Long pageNum, Long pageSize);

    /**
     * 重试指定日志。
     *
     * @param logId 日志ID
     * @return 最新日志
     */
    HrSalarySyncLog retry(Long logId);
}
