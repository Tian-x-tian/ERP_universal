package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.workflow.contract.domain.SysWorkflowDefinition;
import com.erp.workflow.contract.domain.vo.WorkflowTemplateActivateBody;
import com.erp.workflow.contract.domain.vo.WorkflowTemplateVO;
import com.erp.workflow.service.ISysWorkflowDefinitionService;
import com.erp.workflow.service.ISysWorkflowTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 流程模板服务实现。
 */
@Service
public class SysWorkflowTemplateServiceImpl implements ISysWorkflowTemplateService {

    private final ISysWorkflowDefinitionService workflowDefinitionService;

    public SysWorkflowTemplateServiceImpl(ISysWorkflowDefinitionService workflowDefinitionService) {
        this.workflowDefinitionService = workflowDefinitionService;
    }

    /**
     * 查询模板市场列表。
     *
     * @param industry 行业筛选
     * @return 模板列表
     */
    @Override
    public List<WorkflowTemplateVO> selectTemplateList(String industry) {
        List<WorkflowTemplateVO> templateList = buildTemplateCatalog();
        if (!StringUtils.hasText(industry)) {
            return templateList;
        }
        String targetIndustry = industry.trim().toLowerCase(Locale.ROOT);
        List<WorkflowTemplateVO> filteredList = new ArrayList<>();
        for (WorkflowTemplateVO template : templateList) {
            if (template.getIndustry() != null && template.getIndustry().toLowerCase(Locale.ROOT).contains(targetIndustry)) {
                filteredList.add(template);
            }
        }
        return filteredList;
    }

    /**
     * 启用模板并生成流程定义草稿。
     *
     * @param templateCode 模板编码
     * @param activateBody 启用参数
     * @param operator     操作人账号
     * @return 新增的流程定义
     */
    @Override
    public SysWorkflowDefinition activateTemplate(String templateCode, WorkflowTemplateActivateBody activateBody, String operator) {
        if (!StringUtils.hasText(templateCode) || !StringUtils.hasText(operator)) {
            return null;
        }
        WorkflowTemplateVO template = findTemplate(templateCode);
        if (template == null) {
            return null;
        }
        SysWorkflowDefinition definition = new SysWorkflowDefinition();
        String preferredProcessKey = activateBody == null ? null : activateBody.getProcessKey();
        String preferredProcessName = activateBody == null ? null : activateBody.getProcessName();
        definition.setProcessKey(resolveUniqueProcessKey(preferredProcessKey, template.getSuggestedProcessKey()));
        definition.setProcessName(resolveProcessName(preferredProcessName, template.getSuggestedProcessName()));
        definition.setCategory(template.getCategory());
        definition.setStatus("0");
        definition.setFormSchema(template.getFormSchema());
        definition.setModelContent(template.getModelContent());
        definition.setRemark("基于流程模板【" + template.getTemplateName() + "】启用生成");
        boolean success = workflowDefinitionService.createDefinition(definition, operator);
        if (!success) {
            return null;
        }
        return workflowDefinitionService.getOne(new LambdaQueryWrapper<SysWorkflowDefinition>()
                .eq(SysWorkflowDefinition::getProcessKey, definition.getProcessKey())
                .orderByDesc(SysWorkflowDefinition::getVersion)
                .last("LIMIT 1"));
    }

    /**
     * 查询单个模板。
     *
     * @param templateCode 模板编码
     * @return 模板对象
     */
    private WorkflowTemplateVO findTemplate(String templateCode) {
        for (WorkflowTemplateVO template : buildTemplateCatalog()) {
            if (templateCode.trim().equalsIgnoreCase(template.getTemplateCode())) {
                return template;
            }
        }
        return null;
    }

    /**
     * 生成唯一流程标识，避免直接复用模板标识导致串版本。
     *
     * @param preferredKey   用户传入标识
     * @param fallbackKey    模板默认标识
     * @return 可用流程标识
     */
    private String resolveUniqueProcessKey(String preferredKey, String fallbackKey) {
        String baseKey = sanitizeProcessKey(StringUtils.hasText(preferredKey) ? preferredKey : fallbackKey);
        String candidateKey = StringUtils.hasText(baseKey) ? baseKey : "workflow_template";
        baseKey = candidateKey;
        int suffix = 1;
        while (workflowDefinitionService.count(new LambdaQueryWrapper<SysWorkflowDefinition>()
                .eq(SysWorkflowDefinition::getProcessKey, candidateKey)) > 0) {
            candidateKey = baseKey + "_" + suffix;
            suffix += 1;
        }
        return candidateKey;
    }

    /**
     * 处理目标流程名称。
     *
     * @param preferredName 用户传入名称
     * @param fallbackName  模板默认名称
     * @return 最终名称
     */
    private String resolveProcessName(String preferredName, String fallbackName) {
        if (StringUtils.hasText(preferredName)) {
            return preferredName.trim();
        }
        if (StringUtils.hasText(fallbackName)) {
            return fallbackName.trim();
        }
        return "流程模板草稿";
    }

    /**
     * 清洗流程标识。
     *
     * @param processKey 原始流程标识
     * @return 清洗后的流程标识
     */
    private String sanitizeProcessKey(String processKey) {
        if (!StringUtils.hasText(processKey)) {
            return null;
        }
        return processKey.trim().replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * 构建模板市场目录。
     *
     * @return 模板列表
     */
    private List<WorkflowTemplateVO> buildTemplateCatalog() {
        List<WorkflowTemplateVO> templateList = new ArrayList<>();
        templateList.add(buildManufactureTemplate());
        templateList.add(buildTradeTemplate());
        templateList.add(buildServiceTemplate());
        return templateList;
    }

    /**
     * 构建制造行业模板。
     *
     * @return 模板对象
     */
    private WorkflowTemplateVO buildManufactureTemplate() {
        WorkflowTemplateVO template = new WorkflowTemplateVO();
        template.setTemplateCode("TPL_MANUFACTURE_PURCHASE");
        template.setTemplateName("制造行业-请购到采购审批");
        template.setIndustry("制造");
        template.setCategory("purchase");
        template.setDescription("覆盖制造企业的请购申请、供应链负责人复核与采购经理终审，默认带节点级 SLA 策略。");
        template.setSuggestedProcessKey("manufacture_purchase");
        template.setSuggestedProcessName("制造业请购审批");
        template.setTags(Arrays.asList("制造", "采购", "多级审批", "SLA"));
        template.setFormSchema("{\n" +
                "  \"version\": 1,\n" +
                "  \"fields\": [\n" +
                "    {\"fieldCode\":\"applyDept\",\"fieldLabel\":\"申请部门\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"请输入申请部门\",\"options\":[]},\n" +
                "    {\"fieldCode\":\"itemName\",\"fieldLabel\":\"物料名称\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"请输入物料名称\",\"options\":[]},\n" +
                "    {\"fieldCode\":\"amount\",\"fieldLabel\":\"请购金额\",\"componentType\":\"number\",\"required\":true,\"placeholder\":\"请输入金额\",\"options\":[]},\n" +
                "    {\"fieldCode\":\"urgent\",\"fieldLabel\":\"是否紧急\",\"componentType\":\"select\",\"required\":true,\"placeholder\":\"请选择\",\"options\":[\"是\",\"否\"]}\n" +
                "  ],\n" +
                "  \"nodePermissions\": {}\n" +
                "}");
        template.setModelContent("{\n" +
                "  \"startNodeKey\": \"START_1\",\n" +
                "  \"slaConfig\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"durationHours\": 24,\n" +
                "    \"reminderBeforeMinutes\": 120,\n" +
                "    \"actions\": [\"REMIND\"],\n" +
                "    \"channels\": [\"IN_APP\", \"WECOM\"]\n" +
                "  },\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeKey\":\"START_1\",\"nodeName\":\"开始节点\",\"nodeType\":\"start\",\"x\":40,\"y\":120},\n" +
                "    {\"nodeKey\":\"APPROVAL_2\",\"nodeName\":\"部门负责人审批\",\"nodeType\":\"approval\",\"assigneeType\":\"DIRECT_LEADER\",\"approveStrategy\":\"ALL\",\"x\":280,\"y\":120,\n" +
                "      \"slaConfig\":{\"enabled\":true,\"durationHours\":8,\"reminderBeforeMinutes\":60,\"actions\":[\"REMIND\",\"ESCALATE\"],\"escalateToUserId\":1,\"channels\":[\"IN_APP\"]}},\n" +
                "    {\"nodeKey\":\"APPROVAL_3\",\"nodeName\":\"采购经理审批\",\"nodeType\":\"approval\",\"assigneeType\":\"USER\",\"assigneeUserId\":1,\"approveStrategy\":\"ALL\",\"x\":540,\"y\":120,\n" +
                "      \"slaConfig\":{\"enabled\":true,\"durationHours\":12,\"reminderBeforeMinutes\":120,\"actions\":[\"REMIND\",\"TRANSFER\"],\"transferToUserId\":1,\"channels\":[\"IN_APP\",\"WECOM\"]}},\n" +
                "    {\"nodeKey\":\"END_4\",\"nodeName\":\"结束节点\",\"nodeType\":\"end\",\"x\":790,\"y\":120}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\":\"START_1\",\"to\":\"APPROVAL_2\"},\n" +
                "    {\"from\":\"APPROVAL_2\",\"to\":\"APPROVAL_3\"},\n" +
                "    {\"from\":\"APPROVAL_3\",\"to\":\"END_4\"}\n" +
                "  ]\n" +
                "}");
        return template;
    }

    /**
     * 构建贸易行业模板。
     *
     * @return 模板对象
     */
    private WorkflowTemplateVO buildTradeTemplate() {
        WorkflowTemplateVO template = new WorkflowTemplateVO();
        template.setTemplateCode("TPL_TRADE_CONTRACT");
        template.setTemplateName("贸易行业-合同审批");
        template.setIndustry("贸易");
        template.setCategory("contract");
        template.setDescription("适配贸易型企业合同审核场景，支持法务与业务并行复核，以及超时自动升级。");
        template.setSuggestedProcessKey("trade_contract");
        template.setSuggestedProcessName("贸易合同审批");
        template.setTags(Arrays.asList("贸易", "合同", "并行审批", "法务"));
        template.setFormSchema("{\n" +
                "  \"version\": 1,\n" +
                "  \"fields\": [\n" +
                "    {\"fieldCode\":\"contractNo\",\"fieldLabel\":\"合同编号\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"请输入合同编号\",\"options\":[]},\n" +
                "    {\"fieldCode\":\"customer\",\"fieldLabel\":\"客户名称\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"请输入客户名称\",\"options\":[]},\n" +
                "    {\"fieldCode\":\"contractAmount\",\"fieldLabel\":\"合同金额\",\"componentType\":\"number\",\"required\":true,\"placeholder\":\"请输入合同金额\",\"options\":[]},\n" +
                "    {\"fieldCode\":\"riskLevel\",\"fieldLabel\":\"风险等级\",\"componentType\":\"select\",\"required\":true,\"placeholder\":\"请选择风险等级\",\"options\":[\"低\",\"中\",\"高\"]}\n" +
                "  ],\n" +
                "  \"nodePermissions\": {}\n" +
                "}");
        template.setModelContent("{\n" +
                "  \"startNodeKey\": \"START_11\",\n" +
                "  \"slaConfig\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"durationHours\": 36,\n" +
                "    \"reminderBeforeMinutes\": 180,\n" +
                "    \"actions\": [\"REMIND\"],\n" +
                "    \"channels\": [\"IN_APP\", \"SMS\"]\n" +
                "  },\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeKey\":\"START_11\",\"nodeName\":\"开始节点\",\"nodeType\":\"start\",\"x\":40,\"y\":180},\n" +
                "    {\"nodeKey\":\"PARALLEL_12\",\"nodeName\":\"并行评审\",\"nodeType\":\"parallel\",\"x\":230,\"y\":180},\n" +
                "    {\"nodeKey\":\"APPROVAL_13\",\"nodeName\":\"业务总监审批\",\"nodeType\":\"approval\",\"assigneeType\":\"USER\",\"assigneeUserId\":1,\"approveStrategy\":\"ALL\",\"x\":470,\"y\":90,\n" +
                "      \"slaConfig\":{\"enabled\":true,\"durationHours\":10,\"reminderBeforeMinutes\":90,\"actions\":[\"REMIND\"]}},\n" +
                "    {\"nodeKey\":\"APPROVAL_14\",\"nodeName\":\"法务审批\",\"nodeType\":\"approval\",\"assigneeType\":\"USER\",\"assigneeUserId\":1,\"approveStrategy\":\"ALL\",\"x\":470,\"y\":270,\n" +
                "      \"slaConfig\":{\"enabled\":true,\"durationHours\":10,\"reminderBeforeMinutes\":90,\"actions\":[\"REMIND\",\"ESCALATE\"],\"escalateToUserId\":1}},\n" +
                "    {\"nodeKey\":\"PARALLEL_15\",\"nodeName\":\"并行汇聚\",\"nodeType\":\"parallel\",\"x\":700,\"y\":180},\n" +
                "    {\"nodeKey\":\"APPROVAL_16\",\"nodeName\":\"总经理审批\",\"nodeType\":\"approval\",\"assigneeType\":\"USER\",\"assigneeUserId\":1,\"approveStrategy\":\"ALL\",\"x\":930,\"y\":180},\n" +
                "    {\"nodeKey\":\"END_17\",\"nodeName\":\"结束节点\",\"nodeType\":\"end\",\"x\":1160,\"y\":180}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\":\"START_11\",\"to\":\"PARALLEL_12\"},\n" +
                "    {\"from\":\"PARALLEL_12\",\"to\":\"APPROVAL_13\"},\n" +
                "    {\"from\":\"PARALLEL_12\",\"to\":\"APPROVAL_14\"},\n" +
                "    {\"from\":\"APPROVAL_13\",\"to\":\"PARALLEL_15\"},\n" +
                "    {\"from\":\"APPROVAL_14\",\"to\":\"PARALLEL_15\"},\n" +
                "    {\"from\":\"PARALLEL_15\",\"to\":\"APPROVAL_16\"},\n" +
                "    {\"from\":\"APPROVAL_16\",\"to\":\"END_17\"}\n" +
                "  ]\n" +
                "}");
        return template;
    }

    /**
     * 构建服务行业模板。
     *
     * @return 模板对象
     */
    private WorkflowTemplateVO buildServiceTemplate() {
        WorkflowTemplateVO template = new WorkflowTemplateVO();
        template.setTemplateCode("TPL_SERVICE_EXPENSE");
        template.setTemplateName("服务行业-费用报销");
        template.setIndustry("服务");
        template.setCategory("expense");
        template.setDescription("覆盖服务型企业差旅/费用报销场景，支持会签、抄送与自动转办。");
        template.setSuggestedProcessKey("service_expense");
        template.setSuggestedProcessName("服务业费用报销");
        template.setTags(Arrays.asList("服务", "报销", "会签", "抄送"));
        template.setFormSchema("{\n" +
                "  \"version\": 1,\n" +
                "  \"fields\": [\n" +
                "    {\"fieldCode\":\"expenseType\",\"fieldLabel\":\"报销类型\",\"componentType\":\"select\",\"required\":true,\"placeholder\":\"请选择报销类型\",\"options\":[\"差旅\",\"办公\",\"招待\"]},\n" +
                "    {\"fieldCode\":\"expenseAmount\",\"fieldLabel\":\"报销金额\",\"componentType\":\"number\",\"required\":true,\"placeholder\":\"请输入报销金额\",\"options\":[]},\n" +
                "    {\"fieldCode\":\"expenseDesc\",\"fieldLabel\":\"费用说明\",\"componentType\":\"textarea\",\"required\":true,\"placeholder\":\"请输入费用说明\",\"options\":[]}\n" +
                "  ],\n" +
                "  \"nodePermissions\": {}\n" +
                "}");
        template.setModelContent("{\n" +
                "  \"startNodeKey\": \"START_21\",\n" +
                "  \"slaConfig\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"durationHours\": 24,\n" +
                "    \"reminderBeforeMinutes\": 120,\n" +
                "    \"actions\": [\"REMIND\"],\n" +
                "    \"channels\": [\"IN_APP\"]\n" +
                "  },\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeKey\":\"START_21\",\"nodeName\":\"开始节点\",\"nodeType\":\"start\",\"x\":40,\"y\":120},\n" +
                "    {\"nodeKey\":\"APPROVAL_22\",\"nodeName\":\"部门经理会签\",\"nodeType\":\"approval\",\"assigneeType\":\"USER\",\"candidateUserIds\":[1,2],\"approveStrategy\":\"ALL\",\"x\":290,\"y\":120,\n" +
                "      \"slaConfig\":{\"enabled\":true,\"durationHours\":6,\"reminderBeforeMinutes\":60,\"actions\":[\"REMIND\",\"TRANSFER\"],\"transferToUserId\":1}},\n" +
                "    {\"nodeKey\":\"CC_23\",\"nodeName\":\"财务抄送\",\"nodeType\":\"cc\",\"ccUserIds\":[1],\"x\":560,\"y\":120},\n" +
                "    {\"nodeKey\":\"APPROVAL_24\",\"nodeName\":\"财务复核\",\"nodeType\":\"approval\",\"assigneeType\":\"USER\",\"assigneeUserId\":1,\"approveStrategy\":\"ALL\",\"x\":790,\"y\":120,\n" +
                "      \"slaConfig\":{\"enabled\":true,\"durationHours\":8,\"reminderBeforeMinutes\":60,\"actions\":[\"REMIND\"]}},\n" +
                "    {\"nodeKey\":\"END_25\",\"nodeName\":\"结束节点\",\"nodeType\":\"end\",\"x\":1030,\"y\":120}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\":\"START_21\",\"to\":\"APPROVAL_22\"},\n" +
                "    {\"from\":\"APPROVAL_22\",\"to\":\"CC_23\"},\n" +
                "    {\"from\":\"CC_23\",\"to\":\"APPROVAL_24\"},\n" +
                "    {\"from\":\"APPROVAL_24\",\"to\":\"END_25\"}\n" +
                "  ]\n" +
                "}");
        return template;
    }
}


