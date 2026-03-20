package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.system.domain.SysWorkflowBusinessBinding;
import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.domain.vo.WorkflowProcessOptionVO;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysWorkflowBusinessBindingService;
import com.erp.system.service.ISysWorkflowDefinitionService;
import com.erp.system.service.IWorkflowBindingResolver;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmWorkflowBusinessSupport;
import com.erp.system.support.TenantWriteGuard;
import com.erp.system.support.WorkflowBindingActionSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 业务动作流程绑定解析器实现。
 */
@Service
public class WorkflowBindingResolverImpl implements IWorkflowBindingResolver {
    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String STATUS_ENABLED = "0";
    private static final String DEFAULT_BINDING_FLAG = "1";
    private static final String NON_DEFAULT_BINDING_FLAG = "0";
    private static final String DEFAULT_CATEGORY = "custom";
    private static final String LEGACY_EMPLOYEE_ONBOARD_KEY = "mdm_employee";

    private static final String EMPLOYEE_ONBOARD_KEY = "mdm_employee_onboard";
    private static final String EMPLOYEE_CHANGE_KEY = "mdm_employee_change";
    private static final String EMPLOYEE_LEAVE_KEY = "mdm_employee_leave";

    private final ISysWorkflowBusinessBindingService workflowBusinessBindingService;
    private final ISysWorkflowDefinitionService workflowDefinitionService;
    private final SecurityUserResolver securityUserResolver;

    public WorkflowBindingResolverImpl(ISysWorkflowBusinessBindingService workflowBusinessBindingService,
                                       ISysWorkflowDefinitionService workflowDefinitionService,
                                       SecurityUserResolver securityUserResolver) {
        this.workflowBusinessBindingService = workflowBusinessBindingService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询业务动作可选流程列表。
     *
     * @param domainType 业务域类型
     * @param actionCode 业务动作编码
     * @return 可选流程列表
     */
    @Override
    public List<WorkflowProcessOptionVO> listProcessOptions(String domainType, String actionCode) {
        String normalizedDomainType = normalizeDomainType(domainType);
        String normalizedActionCode = normalizeActionCode(actionCode);
        String tenantId = resolveTenantId();
        if (MdmDomainTypeSupport.EMPLOYEE.equals(normalizedDomainType)) {
            ensureEmployeeDefaultBindings(normalizedActionCode);
        }
        List<SysWorkflowBusinessBinding> rawBindingList = workflowBusinessBindingService.selectActiveBindings(
                tenantId,
                normalizedDomainType,
                normalizedActionCode);
        List<SysWorkflowBusinessBinding> bindingList = mergeBindingList(rawBindingList, tenantId);
        List<WorkflowProcessOptionVO> optionList = buildOptionList(normalizedDomainType, normalizedActionCode, bindingList);
        return normalizeDefaultOption(optionList);
    }

    /**
     * 解析并校验最终可用流程标识。
     *
     * @param domainType   业务域类型
     * @param actionCode   业务动作编码
     * @param requestedKey 前端请求流程标识
     * @return 最终流程标识
     */
    @Override
    public String resolveProcessKey(String domainType, String actionCode, String requestedKey) {
        String normalizedDomainType = normalizeDomainType(domainType);
        String normalizedActionCode = normalizeActionCode(actionCode);
        List<WorkflowProcessOptionVO> optionList = listProcessOptions(normalizedDomainType, normalizedActionCode);
        if (!StringUtils.hasText(requestedKey)) {
            if (optionList.isEmpty()) {
                String fallbackKey = resolveLegacyOnboardFallback(normalizedDomainType, normalizedActionCode);
                if (StringUtils.hasText(fallbackKey)) {
                    return fallbackKey;
                }
                throw new IllegalStateException("当前动作未配置可用审批流程，请联系管理员发布流程定义后重试");
            }
            return resolveDefaultProcessKey(optionList);
        }
        String targetProcessKey = requestedKey.trim();
        for (WorkflowProcessOptionVO option : optionList) {
            if (option != null && StringUtils.hasText(option.getProcessKey())
                    && option.getProcessKey().equalsIgnoreCase(targetProcessKey)) {
                return option.getProcessKey();
            }
        }
        if (isEmployeeOnboardLegacyKey(normalizedDomainType, normalizedActionCode, targetProcessKey)) {
            String fallbackKey = resolveLegacyOnboardFallback(normalizedDomainType, normalizedActionCode);
            if (StringUtils.hasText(fallbackKey)) {
                return fallbackKey;
            }
        }
        throw new IllegalArgumentException("流程标识不在当前动作可选范围内：" + targetProcessKey);
    }

    /**
     * 规范业务域类型。
     *
     * @param domainType 原始业务域类型
     * @return 规范化业务域类型
     */
    private String normalizeDomainType(String domainType) {
        String normalized = MdmWorkflowBusinessSupport.normalizeDomainType(domainType);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("业务域类型不能为空");
        }
        return normalized;
    }

    /**
     * 规范动作编码并校验。
     *
     * @param actionCode 原始动作编码
     * @return 规范化动作编码
     */
    private String normalizeActionCode(String actionCode) {
        String normalized = WorkflowBindingActionSupport.normalizeAction(actionCode);
        if (!WorkflowBindingActionSupport.isSupportedAction(normalized)) {
            throw new IllegalArgumentException("审批动作不合法，仅支持 ONBOARD/CHANGE/LEAVE");
        }
        return normalized;
    }

    /**
     * 合并绑定列表，优先使用当前租户绑定。
     *
     * @param rawBindingList 原始绑定列表
     * @param tenantId       当前租户编号
     * @return 合并后绑定列表
     */
    private List<SysWorkflowBusinessBinding> mergeBindingList(List<SysWorkflowBusinessBinding> rawBindingList, String tenantId) {
        if (rawBindingList == null || rawBindingList.isEmpty()) {
            return new ArrayList<>();
        }
        rawBindingList.sort(Comparator
                .comparingInt((SysWorkflowBusinessBinding binding) -> tenantRank(binding, tenantId))
                .thenComparingInt(binding -> binding.getPriority() == null ? Integer.MAX_VALUE : binding.getPriority())
                .thenComparing(binding -> DEFAULT_BINDING_FLAG.equals(binding.getIsDefault()) ? 0 : 1)
                .thenComparing(SysWorkflowBusinessBinding::getBindingId, Comparator.nullsLast(Long::compareTo)));
        Map<String, SysWorkflowBusinessBinding> bindingMap = new LinkedHashMap<>();
        for (SysWorkflowBusinessBinding binding : rawBindingList) {
            if (binding == null || !StringUtils.hasText(binding.getProcessKey())) {
                continue;
            }
            String processKey = binding.getProcessKey().trim().toLowerCase(Locale.ROOT);
            bindingMap.putIfAbsent(processKey, binding);
        }
        return new ArrayList<>(bindingMap.values());
    }

    /**
     * 构建流程选项列表。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @param bindingList 绑定列表
     * @return 流程选项列表
     */
    private List<WorkflowProcessOptionVO> buildOptionList(String domainType,
                                                          String actionCode,
                                                          List<SysWorkflowBusinessBinding> bindingList) {
        List<WorkflowProcessOptionVO> optionList = new ArrayList<>();
        for (SysWorkflowBusinessBinding binding : bindingList) {
            if (binding == null || !StringUtils.hasText(binding.getProcessKey())) {
                continue;
            }
            String processKey = binding.getProcessKey().trim();
            if (shouldHideLegacyInOption(domainType, actionCode, processKey)) {
                continue;
            }
            SysWorkflowDefinition definition = resolvePublishedDefinition(domainType, actionCode, processKey);
            if (definition == null) {
                continue;
            }
            WorkflowProcessOptionVO option = new WorkflowProcessOptionVO();
            option.setProcessKey(definition.getProcessKey());
            option.setProcessName(definition.getProcessName());
            option.setIsDefault(DEFAULT_BINDING_FLAG.equals(binding.getIsDefault()));
            optionList.add(option);
        }
        return optionList;
    }

    /**
     * 判断流程是否需在可选列表中隐藏。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @param processKey 流程标识
     * @return true 表示需要隐藏
     */
    private boolean shouldHideLegacyInOption(String domainType, String actionCode, String processKey) {
        return isEmployeeOnboardLegacyKey(domainType, actionCode, processKey);
    }

    /**
     * 规范默认选项标记，保证存在且仅存在一个默认值。
     *
     * @param optionList 流程选项列表
     * @return 规范化后的流程选项列表
     */
    private List<WorkflowProcessOptionVO> normalizeDefaultOption(List<WorkflowProcessOptionVO> optionList) {
        if (optionList == null || optionList.isEmpty()) {
            return new ArrayList<>();
        }
        int defaultIndex = -1;
        for (int index = 0; index < optionList.size(); index++) {
            WorkflowProcessOptionVO option = optionList.get(index);
            if (option != null && Boolean.TRUE.equals(option.getIsDefault())) {
                defaultIndex = index;
                break;
            }
        }
        if (defaultIndex < 0) {
            optionList.get(0).setIsDefault(true);
            defaultIndex = 0;
        }
        for (int index = 0; index < optionList.size(); index++) {
            if (index == defaultIndex) {
                continue;
            }
            optionList.get(index).setIsDefault(false);
        }
        return optionList;
    }

    /**
     * 解析默认流程标识。
     *
     * @param optionList 流程选项列表
     * @return 默认流程标识
     */
    private String resolveDefaultProcessKey(List<WorkflowProcessOptionVO> optionList) {
        for (WorkflowProcessOptionVO option : optionList) {
            if (option != null && Boolean.TRUE.equals(option.getIsDefault()) && StringUtils.hasText(option.getProcessKey())) {
                return option.getProcessKey();
            }
        }
        return optionList.get(0).getProcessKey();
    }

    /**
     * 查询流程定义，缺失时按规则自动补齐。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @param processKey 流程标识
     * @return 已发布流程定义
     */
    private SysWorkflowDefinition resolvePublishedDefinition(String domainType, String actionCode, String processKey) {
        SysWorkflowDefinition definition = workflowDefinitionService.selectLatestPublishedByProcessKey(processKey);
        if (definition != null) {
            return definition;
        }
        if (!canAutoCreateDefinition(domainType, actionCode, processKey)) {
            return null;
        }
        createEmployeeDefaultDefinition(processKey);
        return workflowDefinitionService.selectLatestPublishedByProcessKey(processKey);
    }

    /**
     * 判断是否允许自动创建流程定义。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @param processKey 流程标识
     * @return true 表示允许创建
     */
    private boolean canAutoCreateDefinition(String domainType, String actionCode, String processKey) {
        if (!MdmDomainTypeSupport.EMPLOYEE.equals(domainType)) {
            return false;
        }
        if (!WorkflowBindingActionSupport.isSupportedAction(actionCode)) {
            return false;
        }
        return EMPLOYEE_ONBOARD_KEY.equalsIgnoreCase(processKey)
                || EMPLOYEE_CHANGE_KEY.equalsIgnoreCase(processKey)
                || EMPLOYEE_LEAVE_KEY.equalsIgnoreCase(processKey);
    }

    /**
     * 按需补齐员工动作默认绑定。
     *
     * @param actionCode 动作编码
     */
    private void ensureEmployeeDefaultBindings(String actionCode) {
        ensureEmployeeBinding(WorkflowBindingActionSupport.ONBOARD, EMPLOYEE_ONBOARD_KEY, DEFAULT_BINDING_FLAG, 10, "员工入职审批流程默认绑定");
        ensureEmployeeBinding(WorkflowBindingActionSupport.CHANGE, EMPLOYEE_CHANGE_KEY, DEFAULT_BINDING_FLAG, 10, "员工变更审批流程默认绑定");
        ensureEmployeeBinding(WorkflowBindingActionSupport.LEAVE, EMPLOYEE_LEAVE_KEY, DEFAULT_BINDING_FLAG, 10, "员工离职审批流程默认绑定");
        ensureEmployeeBinding(WorkflowBindingActionSupport.ONBOARD, LEGACY_EMPLOYEE_ONBOARD_KEY, NON_DEFAULT_BINDING_FLAG, 90, "员工入职审批 legacy 兼容绑定");
        if (WorkflowBindingActionSupport.ONBOARD.equals(actionCode)) {
            createEmployeeDefaultDefinition(EMPLOYEE_ONBOARD_KEY);
        } else if (WorkflowBindingActionSupport.CHANGE.equals(actionCode)) {
            createEmployeeDefaultDefinition(EMPLOYEE_CHANGE_KEY);
        } else if (WorkflowBindingActionSupport.LEAVE.equals(actionCode)) {
            createEmployeeDefaultDefinition(EMPLOYEE_LEAVE_KEY);
        }
    }

    /**
     * 按需新增员工动作绑定记录。
     *
     * @param actionCode 动作编码
     * @param processKey 流程标识
     * @param isDefault  是否默认
     * @param priority   优先级
     * @param remark     备注
     */
    private void ensureEmployeeBinding(String actionCode, String processKey, String isDefault, Integer priority, String remark) {
        String tenantId = resolveTenantId();
        long bindingCount = workflowBusinessBindingService.count(new LambdaQueryWrapper<SysWorkflowBusinessBinding>()
                .eq(SysWorkflowBusinessBinding::getTenantId, tenantId)
                .eq(SysWorkflowBusinessBinding::getDomainType, MdmDomainTypeSupport.EMPLOYEE)
                .eq(SysWorkflowBusinessBinding::getActionCode, actionCode)
                .eq(SysWorkflowBusinessBinding::getProcessKey, processKey));
        if (bindingCount > 0L) {
            return;
        }
        Date now = new Date();
        String operator = resolveOperator();
        SysWorkflowBusinessBinding binding = new SysWorkflowBusinessBinding();
        binding.setTenantId(tenantId);
        binding.setDomainType(MdmDomainTypeSupport.EMPLOYEE);
        binding.setActionCode(actionCode);
        binding.setProcessKey(processKey);
        binding.setIsDefault(isDefault);
        binding.setStatus(STATUS_ENABLED);
        binding.setPriority(priority);
        binding.setRemark(remark);
        binding.setCreateBy(operator);
        binding.setCreateTime(now);
        binding.setUpdateBy(operator);
        binding.setUpdateTime(now);
        workflowBusinessBindingService.save(binding);
    }

    /**
     * 自动创建并发布员工默认流程定义。
     *
     * @param processKey 流程标识
     */
    private void createEmployeeDefaultDefinition(String processKey) {
        if (workflowDefinitionService.selectLatestPublishedByProcessKey(processKey) != null) {
            return;
        }
        String processName = resolveEmployeeProcessName(processKey);
        if (!StringUtils.hasText(processName)) {
            return;
        }
        String operator = resolveOperator();
        SysWorkflowDefinition definition = new SysWorkflowDefinition();
        definition.setProcessKey(processKey);
        definition.setProcessName(processName);
        definition.setCategory(DEFAULT_CATEGORY);
        definition.setStatus("1");
        definition.setFormSchema(resolveEmployeeFormSchema(processKey));
        definition.setModelContent(resolveEmployeeModelContent(processKey));
        definition.setRemark("系统按需补齐员工审批流程定义");
        workflowDefinitionService.createDefinition(definition, operator);
    }

    /**
     * 解析员工流程名称。
     *
     * @param processKey 流程标识
     * @return 流程名称
     */
    private String resolveEmployeeProcessName(String processKey) {
        if (EMPLOYEE_ONBOARD_KEY.equalsIgnoreCase(processKey)) {
            return "员工入职审批流程";
        }
        if (EMPLOYEE_CHANGE_KEY.equalsIgnoreCase(processKey)) {
            return "员工变更审批流程";
        }
        if (EMPLOYEE_LEAVE_KEY.equalsIgnoreCase(processKey)) {
            return "员工离职审批流程";
        }
        return null;
    }

    /**
     * 构建员工流程表单结构。
     *
     * @param processKey 流程标识
     * @return 表单结构 JSON
     */
    private String resolveEmployeeFormSchema(String processKey) {
        String actionLabel = "入职";
        if (EMPLOYEE_CHANGE_KEY.equalsIgnoreCase(processKey)) {
            actionLabel = "变更";
        } else if (EMPLOYEE_LEAVE_KEY.equalsIgnoreCase(processKey)) {
            actionLabel = "离职";
        }
        return "{\"version\":1,\"fields\":["
                + "{\"fieldCode\":\"empCode\",\"fieldLabel\":\"员工编码\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"系统自动带出\",\"options\":[]},"
                + "{\"fieldCode\":\"empName\",\"fieldLabel\":\"员工姓名\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"请输入员工姓名\",\"options\":[]},"
                + "{\"fieldCode\":\"position\",\"fieldLabel\":\"岗位\",\"componentType\":\"input\",\"required\":false,\"placeholder\":\"请输入岗位\",\"options\":[]},"
                + "{\"fieldCode\":\"action\",\"fieldLabel\":\"审批动作\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"" + actionLabel + "\",\"options\":[]}"
                + "],\"nodePermissions\":{}}";
    }

    /**
     * 构建员工流程模型定义。
     *
     * @param processKey 流程标识
     * @return 流程模型 JSON
     */
    private String resolveEmployeeModelContent(String processKey) {
        String approvalNodeName = "员工入职审批";
        if (EMPLOYEE_CHANGE_KEY.equalsIgnoreCase(processKey)) {
            approvalNodeName = "员工变更审批";
        } else if (EMPLOYEE_LEAVE_KEY.equalsIgnoreCase(processKey)) {
            approvalNodeName = "员工离职审批";
        }
        return "{\"startNodeKey\":\"START_EMPLOYEE_1\",\"nodes\":["
                + "{\"nodeKey\":\"START_EMPLOYEE_1\",\"nodeName\":\"开始节点\",\"nodeType\":\"start\",\"x\":40,\"y\":120},"
                + "{\"nodeKey\":\"APPROVAL_EMPLOYEE_2\",\"nodeName\":\"" + approvalNodeName + "\",\"nodeType\":\"approval\",\"assigneeType\":\"DIRECT_LEADER\",\"approveStrategy\":\"ALL\",\"x\":320,\"y\":120},"
                + "{\"nodeKey\":\"END_EMPLOYEE_3\",\"nodeName\":\"结束节点\",\"nodeType\":\"end\",\"x\":620,\"y\":120}"
                + "],\"edges\":["
                + "{\"from\":\"START_EMPLOYEE_1\",\"to\":\"APPROVAL_EMPLOYEE_2\"},"
                + "{\"from\":\"APPROVAL_EMPLOYEE_2\",\"to\":\"END_EMPLOYEE_3\"}"
                + "]}";
    }

    /**
     * 计算绑定租户优先级。
     *
     * @param binding 绑定对象
     * @param tenantId 当前租户
     * @return 排序权重（越小越优先）
     */
    private int tenantRank(SysWorkflowBusinessBinding binding, String tenantId) {
        if (binding == null || !StringUtils.hasText(binding.getTenantId())) {
            return 2;
        }
        if (Objects.equals(binding.getTenantId().trim(), tenantId)) {
            return 0;
        }
        if (Objects.equals(binding.getTenantId().trim(), DEFAULT_TENANT_ID)) {
            return 1;
        }
        return 2;
    }

    /**
     * 判断是否为员工入职 legacy 流程键。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @param processKey 流程标识
     * @return true 表示 legacy 入职键
     */
    private boolean isEmployeeOnboardLegacyKey(String domainType, String actionCode, String processKey) {
        return MdmDomainTypeSupport.EMPLOYEE.equals(domainType)
                && WorkflowBindingActionSupport.ONBOARD.equals(actionCode)
                && StringUtils.hasText(processKey)
                && LEGACY_EMPLOYEE_ONBOARD_KEY.equalsIgnoreCase(processKey.trim());
    }

    /**
     * 解析员工入职 legacy fallback 流程键。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @return fallback 流程键，不可用时返回 null
     */
    private String resolveLegacyOnboardFallback(String domainType, String actionCode) {
        if (!MdmDomainTypeSupport.EMPLOYEE.equals(domainType)
                || !WorkflowBindingActionSupport.ONBOARD.equals(actionCode)) {
            return null;
        }
        SysWorkflowDefinition legacyDefinition = workflowDefinitionService
                .selectLatestPublishedByProcessKey(LEGACY_EMPLOYEE_ONBOARD_KEY);
        return legacyDefinition == null ? null : legacyDefinition.getProcessKey();
    }

    /**
     * 解析当前租户编号。
     *
     * @return 租户编号
     */
    private String resolveTenantId() {
        String tenantId = TenantWriteGuard.currentTenantId();
        return StringUtils.hasText(tenantId) ? tenantId.trim() : DEFAULT_TENANT_ID;
    }

    /**
     * 解析当前操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String userName = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(userName) ? userName.trim() : "system";
    }
}
