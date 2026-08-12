package com.erp.system.controller;

import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmSupplier;
import com.erp.system.domain.vo.MdmSupplierWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmVersionActionBody;
import com.erp.system.service.IMdmSupplierService;
import com.erp.system.service.IMdmSupplierWorkflowSubmitService;
import com.erp.system.support.MdmResponseSupport;
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

/**
 * MDM 供应商主数据控制层。
 */
@RestController
@RequestMapping("/system/mdm/supplier")
public class MdmSupplierController {
    private final IMdmSupplierService supplierService;
    private final IMdmSupplierWorkflowSubmitService supplierWorkflowSubmitService;

    public MdmSupplierController(IMdmSupplierService supplierService,
                                 IMdmSupplierWorkflowSubmitService supplierWorkflowSubmitService) {
        this.supplierService = supplierService;
        this.supplierWorkflowSubmitService = supplierWorkflowSubmitService;
    }

    /**
     * 查询供应商列表。
     *
     * @param supplierCode 供应商编码
     * @param supplierName 供应商名称
     * @param status       状态
     * @return 供应商列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:list')")
    public R<PageData<MdmSupplier>> list(@RequestParam(value = "supplierCode", required = false) String supplierCode,
            @RequestParam(value = "supplierName", required = false) String supplierName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return MdmResponseSupport.page(supplierService.selectSupplierList(supplierCode, supplierName, status), pageNum, pageSize);
    }

    /**
     * 查询供应商详情。
     *
     * @param supplierId 供应商ID
     * @return 供应商详情
     */
    @GetMapping("/{supplierId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:query')")
    public R<MdmSupplier> getInfo(@PathVariable("supplierId") Long supplierId) {
        MdmSupplier supplier = supplierService.getById(supplierId);
        if (supplier == null || "2".equals(supplier.getDelFlag())) {
            return R.failed("供应商不存在");
        }
        return R.success(supplier);
    }

    /**
     * 新增供应商。
     *
     * @param supplier 供应商对象
     * @return 新增结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:add')")
    public R<Boolean> add(@RequestBody MdmSupplier supplier) {
        boolean success = supplierService.createSupplier(supplier);
        return success ? R.success(true) : R.failed("新增供应商失败，请检查编码唯一性与税号格式");
    }

    /**
     * 修改供应商。
     *
     * @param supplier 供应商对象
     * @return 修改结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:edit')")
    public R<Boolean> edit(@RequestBody MdmSupplier supplier) {
        if (supplier == null || supplier.getSupplierId() == null) {
            return R.failed("供应商ID不能为空");
        }
        boolean success = supplierService.updateSupplier(supplier);
        return success ? R.success(true) : R.failed("修改供应商失败，请检查参数与税号格式");
    }

    /**
     * 停用供应商。
     *
     * @param supplierId 供应商ID
     * @return 停用结果
     */
    @PostMapping("/disable/{supplierId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:disable')")
    public R<Boolean> disable(@PathVariable("supplierId") Long supplierId,
                              @RequestBody(required = false) MdmVersionActionBody actionBody) {
        boolean success = supplierService.disableSupplier(supplierId, actionBody == null ? null : actionBody.getVersionNo());
        return success ? R.success(true) : R.failed("停用供应商失败");
    }

    /**
     * 提交供应商草稿生效审批。
     *
     * @param supplierId 供应商ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/submit/{supplierId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:edit')")
    public R<Boolean> submit(@PathVariable("supplierId") Long supplierId,
                             @RequestBody MdmSupplierWorkflowSubmitBody submitBody) {
        boolean success = supplierWorkflowSubmitService.submitDraftActivation(
                supplierId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交供应商审批失败");
    }

    /**
     * 提交供应商变更审批。
     *
     * @param supplierId 供应商ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/change/{supplierId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:edit')")
    public R<Boolean> submitChange(@PathVariable("supplierId") Long supplierId,
                                   @RequestBody MdmSupplierWorkflowSubmitBody submitBody) {
        boolean success = supplierWorkflowSubmitService.submitChange(
                supplierId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getSupplier(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交供应商变更审批失败");
    }

    /**
     * 提交供应商停用审批。
     *
     * @param supplierId 供应商ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/disable/submit/{supplierId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:disable')")
    public R<Boolean> submitDisable(@PathVariable("supplierId") Long supplierId,
                                    @RequestBody MdmSupplierWorkflowSubmitBody submitBody) {
        boolean success = supplierWorkflowSubmitService.submitDisable(
                supplierId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交供应商停用审批失败");
    }

    /**
     * 删除供应商（逻辑删除）。
     *
     * @param supplierId 供应商ID
     * @return 删除结果
     */
    @DeleteMapping("/{supplierId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:remove')")
    public R<Boolean> remove(@PathVariable("supplierId") Long supplierId,
                             @RequestBody(required = false) MdmVersionActionBody actionBody) {
        boolean success = supplierService.removeSupplier(supplierId, actionBody == null ? null : actionBody.getVersionNo());
        return success ? R.success(true) : R.failed("删除供应商失败，仅草稿状态允许删除");
    }
}
