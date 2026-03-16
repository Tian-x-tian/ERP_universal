package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeePosition;
import com.erp.business.hr.domain.vo.HrEmployeePositionBody;
import com.erp.business.hr.domain.vo.HrEmployeePositionQuery;
import com.erp.business.hr.service.IHrEmployeePositionService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工任职控制层。
 */
@RestController
@RequestMapping("/business/hr/employee/position")
public class HrEmployeePositionController {
    private final IHrEmployeePositionService employeePositionService;

    public HrEmployeePositionController(IHrEmployeePositionService employeePositionService) {
        this.employeePositionService = employeePositionService;
    }

    /**
     * 分页查询员工任职。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:hr:position:list')")
    public R<PageData<HrEmployeePosition>> list(HrEmployeePositionQuery query) {
        Page<HrEmployeePosition> page = employeePositionService.selectPage(query);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 新增员工任职。
     *
     * @param body 保存参数
     * @return 任职详情
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:hr:position:add')")
    public R<HrEmployeePosition> create(@RequestBody HrEmployeePositionBody body) {
        return R.success(employeePositionService.createPosition(body));
    }

    /**
     * 更新员工任职。
     *
     * @param positionId 任职ID
     * @param body 保存参数
     * @return 任职详情
     */
    @PutMapping("/{positionId}")
    @PreAuthorize("@ss.hasPermi('business:hr:position:edit')")
    public R<HrEmployeePosition> update(@PathVariable("positionId") Long positionId,
            @RequestBody HrEmployeePositionBody body) {
        return R.success(employeePositionService.updatePosition(positionId, body));
    }
}
