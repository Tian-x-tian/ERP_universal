package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmItem;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmItemService;
import com.erp.system.service.IMdmItemWorkflowSubmitService;
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
import java.util.Objects;

/**
 * 物料主数据审批提交流程服务实现。
 */
@Service
public class MdmItemWorkflowSubmitServiceImpl implements IMdmItemWorkflowSubmitService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String WORKFLOW_STATUS_RUNNING = "0";
    private static final String BUSINESS_TYPE = "MDM_ITEM";
    private static final String BUSINESS_NO_PREFIX = "MDM:ITEM:";
    private static final String META_KEY = "__mdmItemMeta";

    private final IMdmItemService itemService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;
    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final ObjectMapper objectMapper;

    public MdmItemWorkflowSubmitServiceImpl(IMdmItemService itemService,
                                            ISysWorkflowEngineService workflowEngineService,
                                            SecurityUserResolver securityUserResolver,
                                            ISysUserService userService,
                                            SysWorkflowInstanceMapper workflowInstanceMapper) {
        this.itemService = itemService;
        this.workflowEngineService = workflowEngineService;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDraftActivation(Long itemId, Integer versionNo, String processKey, String remark) {
        MdmItem item = loadEditableItem(itemId);
        MdmOptimisticLockSupport.requireVersion(versionNo, item.getVersionNo(), "物料");
        if (!MdmStatusSupport.isDraft(item.getStatus())) {
            throw new IllegalStateException("仅草稿物料允许提交生效审批");
        }
        if (hasRunningWorkflow(itemId)) {
            throw new IllegalStateException("该物料已有审批流程在处理中");
        }
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                itemId,
                MdmWorkflowActionSupport.ACTIVATE,
                item.getVersionNo(),
                null,
                item);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("物料审批流程发起失败");
        }
        if (!updateItemStatus(itemId, item.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("物料状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitChange(Long itemId, Integer versionNo, MdmItem targetItem, String processKey, String remark) {
        MdmItem currentItem = loadEditableItem(itemId);
        MdmOptimisticLockSupport.requireVersion(versionNo, currentItem.getVersionNo(), "物料");
        if (MdmStatusSupport.isSubmitted(currentItem.getStatus())) {
            throw new IllegalStateException("物料审批中，暂不允许提交新的变更");
        }
        if (!MdmStatusSupport.isActive(currentItem.getStatus())) {
            throw new IllegalStateException("仅已生效物料允许提交变更审批");
        }
        if (hasRunningWorkflow(itemId)) {
            throw new IllegalStateException("该物料已有审批流程在处理中");
        }
        MdmItem afterItem = normalizeTargetItem(currentItem, targetItem);
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                itemId,
                MdmWorkflowActionSupport.UPDATE,
                currentItem.getVersionNo(),
                currentItem,
                afterItem);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("物料变更审批流程发起失败");
        }
        if (!updateItemStatus(itemId, currentItem.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("物料状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDisable(Long itemId, Integer versionNo, String processKey, String remark) {
        MdmItem currentItem = loadEditableItem(itemId);
        MdmOptimisticLockSupport.requireVersion(versionNo, currentItem.getVersionNo(), "物料");
        if (MdmStatusSupport.isSubmitted(currentItem.getStatus())) {
            throw new IllegalStateException("物料审批中，暂不允许提交停用");
        }
        if (!MdmStatusSupport.isActive(currentItem.getStatus())) {
            throw new IllegalStateException("仅已生效物料允许提交停用审批");
        }
        if (hasRunningWorkflow(itemId)) {
            throw new IllegalStateException("该物料已有审批流程在处理中");
        }
        MdmItem afterItem = new MdmItem();
        BeanUtils.copyProperties(currentItem, afterItem);
        afterItem.setStatus(MdmStatusSupport.DISABLED);
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                itemId,
                MdmWorkflowActionSupport.DISABLE,
                currentItem.getVersionNo(),
                currentItem,
                afterItem);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("物料停用审批流程发起失败");
        }
        if (!updateItemStatus(itemId, currentItem.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("物料状态已变化，请刷新后重试");
        }
        return true;
    }

    private MdmItem loadEditableItem(Long itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("物料ID不能为空");
        }
        MdmItem item = itemService.getOne(new LambdaQueryWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST));
        if (item == null) {
            throw new IllegalArgumentException("物料不存在");
        }
        return item;
    }

    private boolean hasRunningWorkflow(Long itemId) {
        Long count = workflowInstanceMapper.selectCount(new LambdaQueryWrapper<SysWorkflowInstance>()
                .eq(SysWorkflowInstance::getBusinessType, BUSINESS_TYPE)
                .eq(SysWorkflowInstance::getBusinessNo, buildBusinessNo(itemId))
                .eq(SysWorkflowInstance::getStatus, WORKFLOW_STATUS_RUNNING));
        return count != null && count > 0;
    }

    private WorkflowStartBody buildStartBody(String processKey,
                                             String remark,
                                             Long itemId,
                                             String action,
                                             Integer baseVersionNo,
                                             MdmItem beforeItem,
                                             MdmItem afterItem) {
        if (!StringUtils.hasText(processKey)) {
            throw new IllegalArgumentException("流程标识不能为空");
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        if (afterItem != null) {
            formData.putAll(objectMapper.convertValue(afterItem, new TypeReference<Map<String, Object>>() { }));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("action", action);
        meta.put("itemId", itemId);
        meta.put("baseVersionNo", baseVersionNo);
        meta.put("beforeItem", beforeItem);
        meta.put("afterItem", afterItem);
        formData.put(META_KEY, meta);

        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey(processKey.trim());
        startBody.setBusinessNo(buildBusinessNo(itemId));
        startBody.setBusinessType(BUSINESS_TYPE);
        startBody.setRemark(MdmValueSupport.trimToNull(remark));
        startBody.setFormData(writeJson(formData));
        return startBody;
    }

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

    private boolean updateItemStatus(Long itemId, Integer currentVersion, String targetStatus) {
        MdmItem updateEntity = new MdmItem();
        updateEntity.setItemId(itemId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return itemService.update(updateEntity, new LambdaUpdateWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST)
                .eq(currentVersion != null, MdmItem::getVersionNo, currentVersion));
    }

    private MdmItem normalizeTargetItem(MdmItem currentItem, MdmItem targetItem) {
        if (currentItem == null || targetItem == null) {
            throw new IllegalArgumentException("物料变更数据不能为空");
        }
        MdmItem normalized = new MdmItem();
        BeanUtils.copyProperties(currentItem, normalized);
        normalized.setItemId(currentItem.getItemId());
        normalized.setTenantId(currentItem.getTenantId());
        normalized.setItemCode(currentItem.getItemCode());
        normalized.setItemName(trimRequired(targetItem.getItemName(), "物料名称不能为空"));
        normalized.setSpecModel(MdmValueSupport.trimToNull(targetItem.getSpecModel()));
        normalized.setBrand(MdmValueSupport.trimToNull(targetItem.getBrand()));
        normalized.setItemType(MdmValueSupport.trimToNull(targetItem.getItemType()));
        normalized.setCategoryId(targetItem.getCategoryId());
        normalized.setUnitId(targetItem.getUnitId());
        normalized.setUnitConvert(MdmValueSupport.trimToNull(targetItem.getUnitConvert()));
        normalized.setTaxRateId(targetItem.getTaxRateId());
        normalized.setBarcode(MdmValueSupport.trimToNull(targetItem.getBarcode()));
        normalized.setShelfLifeDays(targetItem.getShelfLifeDays());
        normalized.setBatchControl(MdmValueSupport.normalizeYN(targetItem.getBatchControl(), currentItem.getBatchControl()));
        normalized.setSerialControl(MdmValueSupport.normalizeYN(targetItem.getSerialControl(), currentItem.getSerialControl()));
        if (Objects.equals(currentItem.getBatchControl(), "Y") && Objects.equals(normalized.getBatchControl(), "N")) {
            throw new IllegalArgumentException("批次控制开启后不能关闭");
        }
        if (Objects.equals(currentItem.getSerialControl(), "Y") && Objects.equals(normalized.getSerialControl(), "N")) {
            throw new IllegalArgumentException("序列号控制开启后不能关闭");
        }
        normalized.setCostingMethod(MdmValueSupport.trimToNull(targetItem.getCostingMethod()));
        normalized.setRemark(MdmValueSupport.trimToNull(targetItem.getRemark()));
        normalized.setStatus(currentItem.getStatus());
        normalized.setVersionNo(currentItem.getVersionNo());
        return normalized;
    }

    private String buildBusinessNo(Long itemId) {
        return BUSINESS_NO_PREFIX + itemId;
    }

    private String writeJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("审批数据序列化失败", ex);
        }
    }

    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String resolveOperator() {
        String userName = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(userName) ? userName.trim() : "system";
    }
}
