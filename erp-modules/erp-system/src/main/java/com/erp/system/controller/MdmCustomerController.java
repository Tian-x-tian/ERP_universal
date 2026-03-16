package com.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.vo.MdmCustomerWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmVersionActionBody;
import com.erp.system.service.IMdmCustomerService;
import com.erp.system.service.IMdmCustomerWorkflowSubmitService;
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
 * MDM 客户主数据控制层。
 */
@RestController
@RequestMapping("/system/mdm/customer")
public class MdmCustomerController {

    private final IMdmCustomerService customerService;
    private final IMdmCustomerWorkflowSubmitService customerWorkflowSubmitService;

    public MdmCustomerController(IMdmCustomerService customerService,
                                 IMdmCustomerWorkflowSubmitService customerWorkflowSubmitService) {
        this.customerService = customerService;
        this.customerWorkflowSubmitService = customerWorkflowSubmitService;
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
    public R<PageData<MdmCustomer>> list(@RequestParam(value = "customerCode", required = false) String customerCode,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        Page<MdmCustomer> page = customerService.selectCustomerPage(
                new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize)),
                customerCode,
                customerName,
                status);
        return MdmResponseSupport.page(page);
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
    public R<Boolean> disable(@PathVariable("customerId") Long customerId,
                              @RequestBody(required = false) MdmVersionActionBody actionBody) {
        boolean success = customerService.disableCustomer(customerId, actionBody == null ? null : actionBody.getVersionNo());
        return success ? R.success(true) : R.failed("停用客户失败");
    }

    /**
     * 提交客户草稿生效审批。
     *
     * @param customerId 客户ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/submit/{customerId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:edit')")
    public R<Boolean> submit(@PathVariable("customerId") Long customerId,
                             @RequestBody MdmCustomerWorkflowSubmitBody submitBody) {
        boolean success = customerWorkflowSubmitService.submitDraftActivation(
                customerId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交客户审批失败");
    }

    /**
     * 提交客户变更审批。
     *
     * @param customerId 客户ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/change/{customerId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:edit')")
    public R<Boolean> submitChange(@PathVariable("customerId") Long customerId,
                                   @RequestBody MdmCustomerWorkflowSubmitBody submitBody) {
        boolean success = customerWorkflowSubmitService.submitChange(
                customerId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getCustomer(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交客户变更审批失败");
    }

    /**
     * 提交客户停用审批。
     *
     * @param customerId 客户ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/disable/submit/{customerId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:disable')")
    public R<Boolean> submitDisable(@PathVariable("customerId") Long customerId,
                                    @RequestBody MdmCustomerWorkflowSubmitBody submitBody) {
        boolean success = customerWorkflowSubmitService.submitDisable(
                customerId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交客户停用审批失败");
    }

    /**
     * 删除客户（逻辑删除）。
     *
     * @param customerId 客户ID
     * @return 删除结果
     */
    @DeleteMapping("/{customerId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:customer:remove')")
    public R<Boolean> remove(@PathVariable("customerId") Long customerId,
                             @RequestBody(required = false) MdmVersionActionBody actionBody) {
        boolean success = customerService.removeCustomer(customerId, actionBody == null ? null : actionBody.getVersionNo());
        return success ? R.success(true) : R.failed("删除客户失败，仅草稿状态允许删除");
    }

    /**
     * 规范分页页码。
     *
     * @param pageNum 原始页码
     * @return 有效页码
     */
    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范分页大小。
     *
     * @param pageSize 原始分页大小
     * @return 有效分页大小
     */
    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 200L);
    }
}
