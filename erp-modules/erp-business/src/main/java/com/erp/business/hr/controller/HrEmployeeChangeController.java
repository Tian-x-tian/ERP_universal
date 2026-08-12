package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeChange;
import com.erp.business.hr.domain.vo.HrEmployeeChangeQuery;
import com.erp.business.hr.domain.vo.HrEmployeeChangeSubmitBody;
import com.erp.business.hr.service.IHrEmployeeChangeService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工异动控制层。
 */
@RestController
@RequestMapping("/business/hr/employee/change")
public class HrEmployeeChangeController {
    private final IHrEmployeeChangeService employeeChangeService;

    public HrEmployeeChangeController(IHrEmployeeChangeService employeeChangeService) {
        this.employeeChangeService = employeeChangeService;
    }

    /**
     * 分页查询异动记录。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:hr:change:list')")
    public R<PageData<HrEmployeeChange>> list(HrEmployeeChangeQuery query) {
        Page<HrEmployeeChange> page = employeeChangeService.selectPage(query);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 创建待审批异动记录。
     *
     * @param employeeId 员工ID
     * @param body 提交参数
     * @return 异动记录
     */
    @PostMapping("/{employeeId}/submit")
    @PreAuthorize("@ss.hasPermi('business:hr:change:submit')")
    public R<HrEmployeeChange> submit(@PathVariable("employeeId") Long employeeId,
            @RequestBody HrEmployeeChangeSubmitBody body) {
        return R.success(employeeChangeService.createChange(employeeId, body));
    }
}
