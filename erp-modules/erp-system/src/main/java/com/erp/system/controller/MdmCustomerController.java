package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.service.IMdmCustomerService;
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
 * MDM 客户主数据控制层。
 */
@RestController
@RequestMapping("/system/mdm/customer")
public class MdmCustomerController {

    private final IMdmCustomerService customerService;

    public MdmCustomerController(IMdmCustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 查询客户列表。
     *
     * @param customerCode 客户编码
     * @param customerName 客户名称
     * @param status       状态
     * @return 客户列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:list')")
    public R<List<MdmCustomer>> list(@RequestParam(value = "customerCode", required = false) String customerCode,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "status", required = false) String status) {
        return R.success(maskSensitiveFields(customerService.selectCustomerList(customerCode, customerName, status)));
    }

    /**
     * 查询客户详情。
     *
     * @param customerId 客户ID
     * @return 客户详情
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:query')")
    public R<MdmCustomer> getInfo(@PathVariable("customerId") Long customerId) {
        MdmCustomer customer = customerService.getById(customerId);
        if (customer == null || "2".equals(customer.getDelFlag())) {
            return R.failed("客户不存在");
        }
        return R.success(customer);
    }

    /**
     * 新增客户。
     *
     * @param customer 客户对象
     * @return 新增结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:add')")
    public R<Boolean> add(@RequestBody MdmCustomer customer) {
        if (customer == null) {
            return R.failed("客户参数不能为空");
        }
        boolean success = customerService.createCustomer(customer);
        return success ? R.success(true) : R.failed("新增客户失败，请检查编码唯一性与税号格式");
    }

    /**
     * 修改客户。
     *
     * @param customer 客户对象
     * @return 修改结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:edit')")
    public R<Boolean> edit(@RequestBody MdmCustomer customer) {
        if (customer == null || customer.getCustomerId() == null) {
            return R.failed("客户ID不能为空");
        }
        boolean success = customerService.updateCustomer(customer);
        return success ? R.success(true) : R.failed("修改客户失败，请检查编码唯一性与税号格式");
    }

    /**
     * 停用客户。
     *
     * @param customerId 客户ID
     * @return 停用结果
     */
    @PostMapping("/disable/{customerId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:disable')")
    public R<Boolean> disable(@PathVariable("customerId") Long customerId) {
        boolean success = customerService.disableCustomer(customerId);
        return success ? R.success(true) : R.failed("停用客户失败");
    }

    /**
     * 删除客户（逻辑删除）。
     *
     * @param customerId 客户ID
     * @return 删除结果
     */
    @DeleteMapping("/{customerId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:remove')")
    public R<Boolean> remove(@PathVariable("customerId") Long customerId) {
        boolean success = customerService.removeCustomer(customerId);
        return success ? R.success(true) : R.failed("删除客户失败，仅草稿状态允许删除");
    }

    /**
     * 批量脱敏客户税号。
     *
     * @param customerList 客户列表
     * @return 脱敏后的客户列表
     */
    private List<MdmCustomer> maskSensitiveFields(List<MdmCustomer> customerList) {
        if (customerList == null || customerList.isEmpty()) {
            return customerList;
        }
        for (MdmCustomer customer : customerList) {
            maskTaxNo(customer);
        }
        return customerList;
    }

    /**
     * 脱敏税号，保留前后各 2 位。
     *
     * @param customer 客户对象
     */
    private void maskTaxNo(MdmCustomer customer) {
        if (customer == null || !StringUtils.hasText(customer.getTaxNo())) {
            return;
        }
        String taxNo = customer.getTaxNo().trim();
        if (taxNo.length() <= 4) {
            customer.setTaxNo("****");
            return;
        }
        String prefix = taxNo.substring(0, 2);
        String suffix = taxNo.substring(taxNo.length() - 2);
        customer.setTaxNo(prefix + "****" + suffix);
    }
}
