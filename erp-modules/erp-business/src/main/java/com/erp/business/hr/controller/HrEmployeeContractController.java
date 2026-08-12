package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeContract;
import com.erp.business.hr.domain.vo.HrEmployeeContractBody;
import com.erp.business.hr.domain.vo.HrEmployeeContractQuery;
import com.erp.business.hr.service.IHrEmployeeContractService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工合同控制层。
 */
@RestController
@RequestMapping("/business/hr/employee/contract")
public class HrEmployeeContractController {
    private final IHrEmployeeContractService employeeContractService;

    public HrEmployeeContractController(IHrEmployeeContractService employeeContractService) {
        this.employeeContractService = employeeContractService;
    }

    /**
     * 分页查询合同。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:hr:contract:list')")
    public R<PageData<HrEmployeeContract>> list(HrEmployeeContractQuery query) {
        Page<HrEmployeeContract> page = employeeContractService.selectPage(query);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询合同详情。
     *
     * @param contractId 合同ID
     * @return 合同详情
     */
    @GetMapping("/{contractId}")
    @PreAuthorize("@ss.hasPermi('business:hr:contract:query')")
    public R<HrEmployeeContract> detail(@PathVariable("contractId") Long contractId) {
        return R.success(employeeContractService.getById(contractId));
    }

    /**
     * 新增合同。
     *
     * @param body 保存参数
     * @return 合同详情
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:hr:contract:add')")
    public R<HrEmployeeContract> create(@RequestBody HrEmployeeContractBody body) {
        return R.success(employeeContractService.createContract(body));
    }

    /**
     * 更新合同。
     *
     * @param contractId 合同ID
     * @param body 保存参数
     * @return 合同详情
     */
    @PutMapping("/{contractId}")
    @PreAuthorize("@ss.hasPermi('business:hr:contract:edit')")
    public R<HrEmployeeContract> update(@PathVariable("contractId") Long contractId,
            @RequestBody HrEmployeeContractBody body) {
        return R.success(employeeContractService.updateContract(contractId, body));
    }

    /**
     * 删除合同。
     *
     * @param contractId 合同ID
     * @return 删除结果
     */
    @DeleteMapping("/{contractId}")
    @PreAuthorize("@ss.hasPermi('business:hr:contract:remove')")
    public R<Boolean> delete(@PathVariable("contractId") Long contractId) {
        return R.success(employeeContractService.deleteContract(contractId));
    }
}
