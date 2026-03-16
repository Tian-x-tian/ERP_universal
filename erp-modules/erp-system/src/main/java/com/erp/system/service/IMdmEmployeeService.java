package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmEmployee;

import java.util.List;

/**
 * MDM 员工主数据服务接口。
 */
public interface IMdmEmployeeService extends IService<MdmEmployee> {

    /**
     * 查询员工列表。
     *
     * @param empCode 员工编码
     * @param empName 员工名称
     * @param status  状态
     * @return 员工列表
     */
    List<MdmEmployee> selectEmployeeList(String empCode, String empName, String status);

    /**
     * 新增员工。
     *
     * @param employee 员工对象
     * @return true 表示成功
     */
    boolean createEmployee(MdmEmployee employee);

    /**
     * 修改员工。
     *
     * @param employee 员工对象
     * @return true 表示成功
     */
    boolean updateEmployee(MdmEmployee employee);

    /**
     * 员工离职。
     *
     * @param employeeId 员工ID
     * @return true 表示成功
     */
    boolean leaveEmployee(Long employeeId, Integer versionNo);

    /**
     * 删除员工（逻辑删除）。
     *
     * @param employeeId 员工ID
     * @return true 表示成功
     */
    boolean removeEmployee(Long employeeId, Integer versionNo);
}
