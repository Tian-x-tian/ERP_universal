package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmSupplier;
import com.erp.system.service.IMdmSupplierService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
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
 * MDM 供应商主数据控制层。
 */
@RestController
@RequestMapping("/system/mdm/supplier")
public class MdmSupplierController {
    private final IMdmSupplierService supplierService;

    public MdmSupplierController(IMdmSupplierService supplierService) {
        this.supplierService = supplierService;
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
    public R<List<MdmSupplier>> list(@RequestParam(value = "supplierCode", required = false) String supplierCode,
            @RequestParam(value = "supplierName", required = false) String supplierName,
            @RequestParam(value = "status", required = false) String status) {
        List<MdmSupplier> supplierList = supplierService.selectSupplierList(supplierCode, supplierName, status);
        return R.success(maskSensitiveFields(supplierList));
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
    public R<Boolean> disable(@PathVariable("supplierId") Long supplierId) {
        boolean success = supplierService.disableSupplier(supplierId);
        return success ? R.success(true) : R.failed("停用供应商失败");
    }

    /**
     * 删除供应商（逻辑删除）。
     *
     * @param supplierId 供应商ID
     * @return 删除结果
     */
    @DeleteMapping("/{supplierId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:supplier:remove')")
    public R<Boolean> remove(@PathVariable("supplierId") Long supplierId) {
        boolean success = supplierService.removeSupplier(supplierId);
        return success ? R.success(true) : R.failed("删除供应商失败，仅草稿状态允许删除");
    }

    /**
     * 批量脱敏敏感字段。
     *
     * @param supplierList 供应商列表
     * @return 脱敏列表
     */
    private List<MdmSupplier> maskSensitiveFields(List<MdmSupplier> supplierList) {
        if (supplierList == null || supplierList.isEmpty()) {
            return supplierList;
        }
        for (MdmSupplier supplier : supplierList) {
            maskSensitive(supplier);
        }
        return supplierList;
    }

    /**
     * 脱敏供应商税号与银行账号。
     *
     * @param supplier 供应商对象
     */
    private void maskSensitive(MdmSupplier supplier) {
        if (supplier == null) {
            return;
        }
        supplier.setTaxNo(maskWithFixedRange(supplier.getTaxNo(), 2, 2));
        supplier.setBankAccountInfo(maskWithFixedRange(supplier.getBankAccountInfo(), 4, 4));
    }

    /**
     * 固定前后保留位脱敏。
     *
     * @param source      原始文本
     * @param prefixCount 前缀保留位数
     * @param suffixCount 后缀保留位数
     * @return 脱敏文本
     */
    private String maskWithFixedRange(String source, int prefixCount, int suffixCount) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        String text = source.trim();
        if (text.length() <= prefixCount + suffixCount) {
            return "****";
        }
        return text.substring(0, prefixCount) + "****" + text.substring(text.length() - suffixCount);
    }
}
