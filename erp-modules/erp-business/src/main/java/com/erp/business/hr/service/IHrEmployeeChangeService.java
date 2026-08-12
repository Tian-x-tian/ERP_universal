package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeChange;
import com.erp.business.hr.domain.vo.HrEmployeeArchiveBody;
import com.erp.business.hr.domain.vo.HrEmployeeChangeQuery;
import com.erp.business.hr.domain.vo.HrEmployeeChangeSubmitBody;

import java.util.List;
import java.util.Map;

/**
 * 员工异动服务接口。
 */
public interface IHrEmployeeChangeService {

    /**
     * 分页查询异动记录。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    Page<HrEmployeeChange> selectPage(HrEmployeeChangeQuery query);

    /**
     * 按员工查询异动记录。
     *
     * @param employeeId 员工ID
     * @return 异动记录列表
     */
    List<HrEmployeeChange> listByEmployeeId(Long employeeId);

    /**
     * 创建待审批异动记录。
     *
     * @param employeeId 员工ID
     * @param body 提交参数
     * @return 异动记录
     */
    HrEmployeeChange createChange(Long employeeId, HrEmployeeChangeSubmitBody body);

    /**
     * 审批通过后生效异动。
     *
     * @param changeId 异动ID
     * @param archiveBody 审批后的扩展档案
     * @param approvedBy 审批人
     */
    void approveChange(Long changeId, HrEmployeeArchiveBody archiveBody, String approvedBy);

    /**
     * 审批驳回后回写状态。
     *
     * @param changeId 异动ID
     * @param rejectedBy 审批人
     */
    void rejectChange(Long changeId, String rejectedBy);

    /**
     * 根据异动ID查询记录。
     *
     * @param changeId 异动ID
     * @return 异动记录
     */
    HrEmployeeChange getById(Long changeId);

    /**
     * 从快照中解析目标员工数据。
     *
     * @param change 变更记录
     * @return 目标员工数据
     */
    Map<String, Object> readAfterEmployee(HrEmployeeChange change);

    /**
     * 从快照中解析目标档案数据。
     *
     * @param change 变更记录
     * @return 目标档案数据
     */
    HrEmployeeArchiveBody readAfterArchive(HrEmployeeChange change);
}
