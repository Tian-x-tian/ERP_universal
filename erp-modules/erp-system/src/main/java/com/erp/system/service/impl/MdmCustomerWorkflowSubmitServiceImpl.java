package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.SysUser;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmCustomerService;
import com.erp.system.service.IMdmCustomerWorkflowSubmitService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.support.MdmOptimisticLockSupport;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.MdmWorkflowActionSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户主数据审批提交流程服务实现。
 */
@Service
public class MdmCustomerWorkflowSubmitServiceImpl implements IMdmCustomerWorkflowSubmitService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String WORKFLOW_STATUS_RUNNING = "0";
    private static final String BUSINESS_TYPE = "MDM_CUSTOMER";
    private static final String BUSINESS_NO_PREFIX = "MDM:CUSTOMER:";
    private static final String META_KEY = "__mdmCustomerMeta";

    private final IMdmCustomerService customerService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;
    private final MdmCustomerMapper customerMapper;
    private final ObjectMapper objectMapper;

    public MdmCustomerWorkflowSubmitServiceImpl(IMdmCustomerService customerService,
                                                ISysWorkflowEngineService workflowEngineService,
                                                SecurityUserResolver securityUserResolver,
                                                ISysUserService userService,
                                                MdmCustomerMapper customerMapper) {
        this.customerService = customerService;
        this.workflowEngineService = workflowEngineService;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
        this.customerMapper = customerMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 提交草稿客户生效审批。
     *
     * @param customerId 客户ID
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDraftActivation(Long customerId, Integer versionNo, String processKey, String remark) {
        MdmCustomer customer = loadEditableCustomer(customerId);
        MdmOptimisticLockSupport.requireVersion(versionNo, customer.getVersionNo(), "客户");
        if (!MdmStatusSupport.isDraft(customer.getStatus())) {
            throw new IllegalStateException("仅草稿客户允许提交生效审批");
        }
        if (hasRunningWorkflow(customerId)) {
            throw new IllegalStateException("该客户已有审批流程在处理中");
        }
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                customerId,
                MdmWorkflowActionSupport.ACTIVATE,
                customer.getVersionNo(),
                null,
                customer);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("客户审批流程发起失败");
        }
        if (!updateCustomerStatus(customerId, customer.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "客户状态更新失败，自动中止流程");
            throw new IllegalStateException("客户状态已变化，请刷新后重试");
        }
        return true;
    }

    /**
     * 提交客户变更审批。
     *
     * @param customerId     客户ID
     * @param targetCustomer 目标客户数据
     * @param processKey     流程标识
     * @param remark         提交备注
     * @return true 表示提交成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitChange(Long customerId, Integer versionNo, MdmCustomer targetCustomer, String processKey, String remark) {
        MdmCustomer currentCustomer = loadEditableCustomer(customerId);
        MdmOptimisticLockSupport.requireVersion(versionNo, currentCustomer.getVersionNo(), "客户");
        if (MdmStatusSupport.isSubmitted(currentCustomer.getStatus())) {
            throw new IllegalStateException("客户审批中，暂不允许提交新的变更");
        }
        if (!MdmStatusSupport.isActive(currentCustomer.getStatus())) {
            throw new IllegalStateException("仅已生效客户允许提交变更审批");
        }
        if (hasRunningWorkflow(customerId)) {
            throw new IllegalStateException("该客户已有审批流程在处理中");
        }
        MdmCustomer afterCustomer = normalizeTargetCustomer(currentCustomer, targetCustomer);
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                customerId,
                MdmWorkflowActionSupport.UPDATE,
                currentCustomer.getVersionNo(),
                currentCustomer,
                afterCustomer);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("客户变更审批流程发起失败");
        }
        if (!updateCustomerStatus(customerId, currentCustomer.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "客户状态更新失败，自动中止流程");
            throw new IllegalStateException("客户状态已变化，请刷新后重试");
        }
        return true;
    }

    /**
     * 提交客户停用审批。
     *
     * @param customerId 客户ID
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDisable(Long customerId, Integer versionNo, String processKey, String remark) {
        MdmCustomer currentCustomer = loadEditableCustomer(customerId);
        MdmOptimisticLockSupport.requireVersion(versionNo, currentCustomer.getVersionNo(), "客户");
        if (MdmStatusSupport.isSubmitted(currentCustomer.getStatus())) {
            throw new IllegalStateException("客户审批中，暂不允许提交停用");
        }
        if (!MdmStatusSupport.isActive(currentCustomer.getStatus())) {
            throw new IllegalStateException("仅已生效客户允许提交停用审批");
        }
        if (hasRunningWorkflow(customerId)) {
            throw new IllegalStateException("该客户已有审批流程在处理中");
        }
        MdmCustomer afterCustomer = new MdmCustomer();
        BeanUtils.copyProperties(currentCustomer, afterCustomer);
        afterCustomer.setStatus(MdmStatusSupport.DISABLED);
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                customerId,
                MdmWorkflowActionSupport.DISABLE,
                currentCustomer.getVersionNo(),
                currentCustomer,
                afterCustomer);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("客户停用审批流程发起失败");
        }
        if (!updateCustomerStatus(customerId, currentCustomer.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "客户状态更新失败，自动中止流程");
            throw new IllegalStateException("客户状态已变化，请刷新后重试");
        }
        return true;
    }

    /**
     * 加载可编辑客户。
     *
     * @param customerId 客户ID
     * @return 客户对象
     */
    private MdmCustomer loadEditableCustomer(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("客户ID不能为空");
        }
        MdmCustomer customer = customerService.getOne(new LambdaQueryWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        return customer;
    }

    /**
     * 检查客户是否存在运行中的审批流程。
     *
     * @param customerId 客户ID
     * @return true 表示存在
     */
    private boolean hasRunningWorkflow(Long customerId) {
        return workflowEngineService.hasRunningInstance(BUSINESS_TYPE, buildBusinessNo(customerId));
    }

    /**
     * 构建流程发起参数。
     *
     * @param processKey    流程标识
     * @param remark        备注
     * @param customerId    客户ID
     * @param action        审批动作
     * @param baseVersionNo 基础版本号
     * @param beforeCustomer 变更前客户
     * @param afterCustomer  变更后客户
     * @return 流程发起参数
     */
    private WorkflowStartBody buildStartBody(String processKey,
                                             String remark,
                                             Long customerId,
                                             String action,
                                             Integer baseVersionNo,
                                             MdmCustomer beforeCustomer,
                                             MdmCustomer afterCustomer) {
        if (!StringUtils.hasText(processKey)) {
            throw new IllegalArgumentException("流程标识不能为空");
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        if (afterCustomer != null) {
            formData.putAll(objectMapper.convertValue(afterCustomer, new TypeReference<Map<String, Object>>() { }));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("action", action);
        meta.put("customerId", customerId);
        meta.put("baseVersionNo", baseVersionNo);
        meta.put("beforeCustomer", beforeCustomer);
        meta.put("afterCustomer", afterCustomer);
        formData.put(META_KEY, meta);

        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey(processKey.trim());
        startBody.setRequestedProcessKey(processKey.trim());
        startBody.setOwnerService("system");
        startBody.setBusinessNo(buildBusinessNo(customerId));
        startBody.setBusinessType(BUSINESS_TYPE);
        startBody.setDomainType(BUSINESS_TYPE);
        startBody.setActionCode(action);
        startBody.setIdempotencyKey(buildBusinessNo(customerId));
        startBody.setOperator(resolveOperator());
        startBody.setRemark(MdmValueSupport.trimToNull(remark));
        startBody.setFormData(writeJson(formData));
        return startBody;
    }

    /**
     * 启动工作流实例。
     *
     * @param startBody 启动参数
     * @return true 表示成功
     */
    private boolean startWorkflow(WorkflowStartBody startBody) {
        Long userId = securityUserResolver.getCurrentUserId();
        String userName = securityUserResolver.getCurrentUsername();
        if (userId == null || !StringUtils.hasText(userName)) {
            throw new IllegalStateException("当前登录用户无效，无法提交审批");
        }
        SysUser user = userService.selectUserByUserName(userName);
        String nickName = user == null ? userName : user.getNickName();
        return workflowEngineService.startProcess(startBody, userId, userName, nickName);
    }

    /**
     * 在本地状态回写失败时中止已发起的流程实例。
     *
     * @param startBody 流程启动参数
     * @param remark 中止原因
     */
    private void abortWorkflow(WorkflowStartBody startBody, String remark) {
        if (startBody == null) {
            return;
        }
        workflowEngineService.abortProcess(startBody.getBusinessType(), startBody.getBusinessNo(), remark);
    }

    /**
     * 将草稿客户状态更新为审批中。
     *
     * @param customerId     客户ID
     * @param currentVersion 当前版本号
     * @param targetStatus   目标状态
     * @return true 表示更新成功
     */
    private boolean updateCustomerStatus(Long customerId, Integer currentVersion, String targetStatus) {
        MdmCustomer updateEntity = new MdmCustomer();
        updateEntity.setCustomerId(customerId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return customerService.update(updateEntity, new LambdaUpdateWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST)
                .eq(currentVersion != null, MdmCustomer::getVersionNo, currentVersion));
    }

    /**
     * 归一化审批后的客户目标数据。
     *
     * @param currentCustomer 当前客户
     * @param targetCustomer  目标客户
     * @return 归一化后的客户
     */
    private MdmCustomer normalizeTargetCustomer(MdmCustomer currentCustomer, MdmCustomer targetCustomer) {
        if (currentCustomer == null || targetCustomer == null) {
            throw new IllegalArgumentException("客户变更数据不能为空");
        }
        MdmCustomer normalized = new MdmCustomer();
        BeanUtils.copyProperties(currentCustomer, normalized);
        normalized.setCustomerId(currentCustomer.getCustomerId());
        normalized.setTenantId(currentCustomer.getTenantId());
        normalized.setCustomerCode(currentCustomer.getCustomerCode());
        normalized.setCustomerName(trimRequired(targetCustomer.getCustomerName(), "客户名称不能为空"));
        normalized.setShortName(MdmValueSupport.trimToNull(targetCustomer.getShortName()));
        normalized.setCustomerType(MdmValueSupport.trimToNull(targetCustomer.getCustomerType()));
        normalized.setTaxNo(MdmValueSupport.trimToNull(targetCustomer.getTaxNo()));
        if (StringUtils.hasText(normalized.getTaxNo()) && !MdmValueSupport.isValidTaxNo(normalized.getTaxNo())) {
            throw new IllegalArgumentException("税号格式不正确");
        }
        normalized.setInvoiceTitle(MdmValueSupport.trimToNull(targetCustomer.getInvoiceTitle()));
        normalized.setDefaultCurrency(MdmValueSupport.trimToNull(targetCustomer.getDefaultCurrency()));
        normalized.setDefaultTaxRate(targetCustomer.getDefaultTaxRate());
        normalized.setCreditLimit(targetCustomer.getCreditLimit());
        normalized.setCreditDays(targetCustomer.getCreditDays());
        normalized.setContactName(MdmValueSupport.trimToNull(targetCustomer.getContactName()));
        normalized.setContactPhone(MdmValueSupport.trimToNull(targetCustomer.getContactPhone()));
        normalized.setContactEmail(MdmValueSupport.trimToNull(targetCustomer.getContactEmail()));
        normalized.setProvince(MdmValueSupport.trimToNull(targetCustomer.getProvince()));
        normalized.setCity(MdmValueSupport.trimToNull(targetCustomer.getCity()));
        normalized.setDistrict(MdmValueSupport.trimToNull(targetCustomer.getDistrict()));
        normalized.setDetailAddress(MdmValueSupport.trimToNull(targetCustomer.getDetailAddress()));
        normalized.setSettleMethodId(targetCustomer.getSettleMethodId());
        normalized.setOrgId(targetCustomer.getOrgId());
        normalized.setRemark(MdmValueSupport.trimToNull(targetCustomer.getRemark()));
        normalized.setStatus(currentCustomer.getStatus());
        normalized.setVersionNo(currentCustomer.getVersionNo());
        return normalized;
    }

    /**
     * 构建业务单号。
     *
     * @param customerId 客户ID
     * @return 业务单号
     */
    private String buildBusinessNo(Long customerId) {
        return BUSINESS_NO_PREFIX + customerId;
    }

    /**
     * 序列化 JSON。
     *
     * @param data 数据对象
     * @return JSON 字符串
     */
    private String writeJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("审批数据序列化失败", ex);
        }
    }

    /**
     * 校验并裁剪必填文本。
     *
     * @param value   原始值
     * @param message 错误消息
     * @return 归一化文本
     */
    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 获取当前操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String userName = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(userName) ? userName.trim() : "system";
    }
}


