package com.erp.business.hr.service;

import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.vo.HrEmployeeArchiveBody;

import java.util.List;

/**
 * 员工扩展档案服务接口。
 */
public interface IHrEmployeeArchiveService {

    /**
     * 新增员工扩展档案。
     *
     * @param archiveBody 档案参数
     * @return 落库后的档案对象
     */
    HrEmployeeArchive createArchive(HrEmployeeArchiveBody archiveBody);

    /**
     * 更新员工扩展档案，不存在时自动补建。
     *
     * @param employeeId  员工ID
     * @param archiveBody 档案参数
     * @return 更新后的档案对象
     */
    HrEmployeeArchive updateArchive(Long employeeId, HrEmployeeArchiveBody archiveBody);

    /**
     * 审批通过后生效扩展档案。
     *
     * @param employeeId 员工ID
     * @param archiveBody 档案参数
     * @param operator 操作人
     * @return 更新后的档案对象
     */
    HrEmployeeArchive applyApprovedArchive(Long employeeId, HrEmployeeArchiveBody archiveBody, String operator);

    /**
     * 按员工ID查询扩展档案。
     *
     * @param employeeId 员工ID
     * @return 档案对象
     */
    HrEmployeeArchive getArchiveByEmployeeId(Long employeeId);

    /**
     * 按员工ID集合批量查询扩展档案。
     *
     * @param employeeIds 员工ID集合
     * @return 档案列表
     */
    List<HrEmployeeArchive> listArchivesByEmployeeIds(List<Long> employeeIds);
}
