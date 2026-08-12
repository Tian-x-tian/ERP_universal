package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeePosition;
import com.erp.business.hr.domain.vo.HrEmployeePositionBody;
import com.erp.business.hr.domain.vo.HrEmployeePositionQuery;

import java.util.List;

/**
 * 员工任职服务接口。
 */
public interface IHrEmployeePositionService {

    /**
     * 分页查询员工任职。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    Page<HrEmployeePosition> selectPage(HrEmployeePositionQuery query);

    /**
     * 查询员工任职列表。
     *
     * @param employeeId 员工ID
     * @return 任职列表
     */
    List<HrEmployeePosition> listByEmployeeId(Long employeeId);

    /**
     * 新增员工任职。
     *
     * @param body 保存参数
     * @return 任职记录
     */
    HrEmployeePosition createPosition(HrEmployeePositionBody body);

    /**
     * 更新员工任职。
     *
     * @param positionId 任职ID
     * @param body 保存参数
     * @return 任职记录
     */
    HrEmployeePosition updatePosition(Long positionId, HrEmployeePositionBody body);
}
