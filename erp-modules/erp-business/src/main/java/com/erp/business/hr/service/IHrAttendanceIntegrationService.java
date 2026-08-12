package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrAttendanceFieldMapping;
import com.erp.business.hr.domain.HrAttendanceSyncLog;
import com.erp.business.hr.domain.vo.HrAttendanceCallbackBody;
import com.erp.business.hr.domain.vo.HrAttendancePushBody;

import java.util.List;

/**
 * 出勤管理同步服务接口。
 */
public interface IHrAttendanceIntegrationService {

    /**
     * 查询映射配置。
     *
     * @return 映射配置列表
     */
    List<HrAttendanceFieldMapping> listMappings();

    /**
     * 保存映射配置。
     *
     * @param mappings 映射配置
     * @return 最新配置
     */
    List<HrAttendanceFieldMapping> saveMappings(List<HrAttendanceFieldMapping> mappings);

    /**
     * 发起出勤推送。
     *
     * @param body 推送参数
     * @return 生成的同步日志
     */
    List<HrAttendanceSyncLog> pushAttendance(HrAttendancePushBody body);

    /**
     * 处理出勤回传。
     *
     * @param body 回传参数
     * @return 最新日志
     */
    HrAttendanceSyncLog callback(HrAttendanceCallbackBody body);

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
    Page<HrAttendanceSyncLog> selectLogPage(String periodCode, String syncStatus, Long employeeId, Long pageNum, Long pageSize);

    /**
     * 重试指定日志。
     *
     * @param logId 日志ID
     * @return 最新日志
     */
    HrAttendanceSyncLog retry(Long logId);
}

