package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.service.IMdmEmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MDM 员工主数据控制层。
 */
@RestController
@RequestMapping("/system/mdm/employee")
public class MdmEmployeeController {
    private final IMdmEmployeeService employeeService;

    public MdmEmployeeController(IMdmEmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * 查询员工列表。
     *
     * @param empCode 员工编码
     * @param empName 员工名称
     * @param status  状态
     * @return 员工列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:employee:list')")
    public R<List<MdmEmployee>> list(@RequestParam(value = "empCode", required = false) String empCode,
            @RequestParam(value = "empName", required = false) String empName,
            @RequestParam(value = "status", required = false) String status) {
        return R.success(employeeService.selectEmployeeList(empCode, empName, status));
    }

    /**
     * 查询员工详情。
     *
     * @param employeeId 员工ID
     * @return 员工详情
     */
    @GetMapping("/{employeeId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:employee:query')")
    public R<MdmEmployee> getInfo(@PathVariable("employeeId") Long employeeId) {
        MdmEmployee employee = employeeService.getById(employeeId);
        if (employee == null || "2".equals(employee.getDelFlag())) {
            return R.failed("员工不存在");
        }
        return R.success(employee);
    }

    /**
     * 新增员工。
     *
     * @param employee 员工对象
     * @return 新增结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:employee:add')")
    public R<Boolean> add(@RequestBody MdmEmployee employee) {
        boolean success = employeeService.createEmployee(employee);
        return success ? R.success(true) : R.failed("新增员工失败，请检查编码唯一性和必填字段");
    }

    /**
     * 修改员工。
     *
     * @param employee 员工对象
     * @return 修改结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:employee:edit')")
    public R<Boolean> edit(@RequestBody MdmEmployee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return R.failed("员工ID不能为空");
        }
        boolean success = employeeService.updateEmployee(employee);
        return success ? R.success(true) : R.failed("修改员工失败，请检查参数");
    }

    /**
     * 员工离职。
     *
     * @param employeeId 员工ID
     * @return 离职结果
     */
    @PostMapping("/leave/{employeeId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:employee:leave')")
    public R<Boolean> leave(@PathVariable("employeeId") Long employeeId) {
        boolean success = employeeService.leaveEmployee(employeeId);
        return success ? R.success(true) : R.failed("员工离职处理失败");
    }

    /**
     * 删除员工（逻辑删除）。
     *
     * @param employeeId 员工ID
     * @return 删除结果
     */
    @DeleteMapping("/{employeeId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:employee:remove')")
    public R<Boolean> remove(@PathVariable("employeeId") Long employeeId) {
        boolean success = employeeService.removeEmployee(employeeId);
        return success ? R.success(true) : R.failed("删除员工失败，仅离职状态允许删除");
    }
}
