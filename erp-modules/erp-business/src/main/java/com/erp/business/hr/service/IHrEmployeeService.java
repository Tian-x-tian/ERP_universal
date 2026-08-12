package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.vo.HrEmployeeAggregateQuery;
import com.erp.business.hr.domain.vo.HrEmployeeDetailVO;
import com.erp.business.hr.domain.vo.HrEmployeeListVO;

/**
 * HR 员工聚合服务接口。
 */
public interface IHrEmployeeService {

    /**
     * 分页查询 HR 员工台账。
     *
     * @param query 查询参数
     * @return 聚合分页结果
     */
    Page<HrEmployeeListVO> selectEmployeePage(HrEmployeeAggregateQuery query);

    /**
     * 查询 HR 员工详情。
     *
     * @param employeeId 员工ID
     * @return 聚合详情
     */
    HrEmployeeDetailVO getEmployeeDetail(Long employeeId);

    /**
     * 查询各状态下的人员数量统计。
     *
     * @param query 查询参数（可选）
     * @return 状态到总数的映射
     */
    java.util.Map<String, Long> selectEmployeeStatusStats(HrEmployeeAggregateQuery query);
}
