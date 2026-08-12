package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.vo.HrEmployeeAggregateQuery;
import com.erp.business.hr.domain.vo.HrEmployeeArchiveBody;
import com.erp.business.hr.domain.vo.HrEmployeeDetailVO;
import com.erp.business.hr.domain.vo.HrEmployeeListVO;
import com.erp.business.hr.service.IHrEmployeeArchiveService;
import com.erp.business.hr.service.IHrEmployeeService;
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
 * HR 员工聚合控制层。
 */
@RestController
@RequestMapping("/business/hr/employee")
public class HrEmployeeController {
    private final IHrEmployeeService hrEmployeeService;
    private final IHrEmployeeArchiveService employeeArchiveService;

    public HrEmployeeController(IHrEmployeeService hrEmployeeService,
            IHrEmployeeArchiveService employeeArchiveService) {
        this.hrEmployeeService = hrEmployeeService;
        this.employeeArchiveService = employeeArchiveService;
    }

    /**
     * 分页查询 HR 员工台账。
     *
     * @param query 查询参数
     * @return 台账分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:hr:employee:list')")
    public R<PageData<HrEmployeeListVO>> list(HrEmployeeAggregateQuery query) {
        Page<HrEmployeeListVO> page = hrEmployeeService.selectEmployeePage(query);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询各状态下的人员数量统计。
     *
     * @param query 查询参数
     * @return 统计映射
     */
    @GetMapping("/status-stats")
    @PreAuthorize("@ss.hasPermi('business:hr:employee:list')")
    public R<java.util.Map<String, Long>> getStatusStats(HrEmployeeAggregateQuery query) {
        return R.success(hrEmployeeService.selectEmployeeStatusStats(query));
    }

    /**
     * 查询 HR 员工详情。
     *
     * @param employeeId 员工ID
     * @return 聚合详情
     */
    @GetMapping("/{employeeId}")
    @PreAuthorize("@ss.hasPermi('business:hr:employee:query')")
    public R<HrEmployeeDetailVO> detail(@PathVariable("employeeId") Long employeeId) {
        return R.success(hrEmployeeService.getEmployeeDetail(employeeId));
    }

    /**
     * 新增员工扩展档案。
     *
     * @param archiveBody 档案参数
     * @return 创建后的档案
     */
    @PostMapping("/archive")
    @PreAuthorize("@ss.hasPermi('business:hr:employee:add')")
    public R<HrEmployeeArchive> createArchive(@RequestBody HrEmployeeArchiveBody archiveBody) {
        return R.success(employeeArchiveService.createArchive(archiveBody));
    }

    /**
     * 更新员工扩展档案。
     *
     * @param employeeId  员工ID
     * @param archiveBody 档案参数
     * @return 更新后的档案
     */
    @PutMapping("/archive/{employeeId}")
    @PreAuthorize("@ss.hasPermi('business:hr:employee:edit')")
    public R<HrEmployeeArchive> updateArchive(@PathVariable("employeeId") Long employeeId,
            @RequestBody HrEmployeeArchiveBody archiveBody) {
        return R.success(employeeArchiveService.updateArchive(employeeId, archiveBody));
    }
}
