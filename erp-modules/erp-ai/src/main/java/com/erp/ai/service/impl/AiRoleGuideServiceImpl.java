package com.erp.ai.service.impl;

import com.erp.ai.model.AiActionDescriptor;
import com.erp.ai.model.AiPolicySummaryVO;
import com.erp.ai.model.AiRoleProfileVO;
import com.erp.ai.security.service.SecurityUserResolver;
import com.erp.ai.service.AiRoleGuideService;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformAuthorityBundle;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI 角色引导服务实现。
 */
@Service
public class AiRoleGuideServiceImpl implements AiRoleGuideService {
    private final InternalSystemClient internalSystemClient;
    private final SecurityUserResolver securityUserResolver;

    public AiRoleGuideServiceImpl(InternalSystemClient internalSystemClient, SecurityUserResolver securityUserResolver) {
        this.internalSystemClient = internalSystemClient;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 构造当前用户角色画像。
     *
     * @return 角色画像
     */
    @Override
    public AiRoleProfileVO buildCurrentRoleProfile() {
        PlatformAuthorityBundle authorityBundle = resolveAuthorityBundle();
        List<String> roleKeys = authorityBundle.getRoleKeys() == null ? new ArrayList<>() : authorityBundle.getRoleKeys();
        List<String> permissions = authorityBundle.getPermissions() == null ? new ArrayList<>() : authorityBundle.getPermissions();

        String tenantId = trimToNull(securityUserResolver.getCurrentTenantId());
        String roleTag = resolveRoleTag(roleKeys, permissions, tenantId);

        AiRoleProfileVO roleProfile = new AiRoleProfileVO();
        roleProfile.setAiRoleTag(roleTag);
        roleProfile.setRoleLabel(resolveRoleLabel(roleTag));
        roleProfile.setLearningCards(resolveLearningCards(roleTag));
        roleProfile.setFirstWeekTasks(resolveFirstWeekTasks(roleTag));
        roleProfile.setCommonMistakes(resolveCommonMistakes(roleTag));
        roleProfile.setSuggestedQuestions(resolveSuggestedQuestions(roleTag));
        return roleProfile;
    }

    /**
     * 构造页面快捷提问模板。
     *
     * @param roleProfile 当前角色画像
     * @return 页面快捷提问模板
     */
    @Override
    public Map<String, List<String>> buildPageQuestionTemplates(AiRoleProfileVO roleProfile) {
        Map<String, List<String>> templates = new LinkedHashMap<>();
        templates.put("/home", buildList("概览我今天的核心工作", "按优先级给我三条建议", "有哪些风险需要先处理"));
        templates.put("/workbench/process-todo", buildList("总结我的待办", "我今天优先做什么", "帮我定位第一条高优先级待办"));
        templates.put("/workbench/system-notice", buildList("列出未读消息", "哪些消息最紧急", "帮我批量处理低优先级消息"));
        templates.put("/workflow-center/definition", buildList("有哪些待发布流程定义", "发布流程前要检查什么", "流程定义发布失败常见原因"));
        templates.put("/system/ai-config", buildList("当前策略对业务有什么影响", "如何配置更安全的动作策略", "如何评估AI使用效果"));

        String roleTag = roleProfile == null ? null : roleProfile.getAiRoleTag();
        if ("approver".equals(roleTag)) {
            templates.put("/workflow-center/instance", buildList("我负责的审批积压情况", "审批通过与驳回标准建议", "如何降低审批误操作"));
        } else if ("hr_specialist".equals(roleTag)) {
            templates.put("/business/hr/employee", buildList("入职流程常见问题", "员工档案维护要点", "本周HR高频任务建议"));
        } else if ("tenant_admin".equals(roleTag) || "platform_admin".equals(roleTag)) {
            templates.put("/system/config", buildList("系统参数变更风险提示", "近期配置变更建议", "如何做配置回滚预案"));
        }
        return templates;
    }

    /**
     * 构造策略摘要。
     *
     * @param actions 当前可执行动作
     * @return 策略摘要
     */
    @Override
    public AiPolicySummaryVO buildPolicySummary(List<AiActionDescriptor> actions) {
        AiPolicySummaryVO summary = new AiPolicySummaryVO();
        summary.setInteractionLevels(buildList("L1 只读解释", "L2 智能导航", "L3 受控执行"));
        summary.setStrategySummary("仅在权限和动作白名单范围内执行操作；高风险动作必须确认后执行。"
                + "命中多候选时先澄清，默认不执行隐式写操作。");
        summary.setAllowedActions(actions == null ? new ArrayList<>() : actions);
        return summary;
    }

    /**
     * 解析权限包。
     *
     * @return 权限包
     */
    private PlatformAuthorityBundle resolveAuthorityBundle() {
        try {
            PlatformAuthorityBundle bundle = internalSystemClient.getAuthorities();
            return bundle == null ? new PlatformAuthorityBundle() : bundle;
        } catch (Exception ignored) {
            return new PlatformAuthorityBundle();
        }
    }

    /**
     * 解析 AI 角色标签。
     *
     * @param roleKeys 角色编码列表
     * @param permissions 权限列表
     * @param tenantId 租户编号
     * @return AI 角色标签
     */
    private String resolveRoleTag(List<String> roleKeys, List<String> permissions, String tenantId) {
        List<String> normalizedRoleKeys = normalizeList(roleKeys);
        List<String> normalizedPermissions = normalizeList(permissions);

        if ("000000".equals(tenantId) && normalizedRoleKeys.contains("admin")) {
            return "platform_admin";
        }
        if (normalizedPermissions.stream().anyMatch(item -> item.startsWith("workflow:todo:approve"))
                || normalizedPermissions.stream().anyMatch(item -> item.startsWith("workflow:todo:reject"))
                || normalizedRoleKeys.stream().anyMatch(item -> item.contains("approver"))) {
            return "approver";
        }
        if (normalizedPermissions.stream().anyMatch(item -> item.startsWith("business:hr:"))) {
            return "hr_specialist";
        }
        if (normalizedRoleKeys.contains("admin")) {
            return "tenant_admin";
        }
        return "general_user";
    }

    /**
     * 解析角色标签名称。
     *
     * @param roleTag 角色标签
     * @return 角色名称
     */
    private String resolveRoleLabel(String roleTag) {
        if ("platform_admin".equals(roleTag)) {
            return "平台管理员";
        }
        if ("tenant_admin".equals(roleTag)) {
            return "租户管理员";
        }
        if ("approver".equals(roleTag)) {
            return "审批角色";
        }
        if ("hr_specialist".equals(roleTag)) {
            return "HR 专员";
        }
        return "业务用户";
    }

    /**
     * 构造学习卡片。
     *
     * @param roleTag 角色标签
     * @return 学习卡片
     */
    private List<String> resolveLearningCards(String roleTag) {
        if ("platform_admin".equals(roleTag) || "tenant_admin".equals(roleTag)) {
            return buildList("系统配置与权限边界", "AI策略与高风险动作治理", "审计指标与问题追溯");
        }
        if ("approver".equals(roleTag)) {
            return buildList("审批待办快速定位", "通过/驳回操作规范", "驳回原因与流程回退影响");
        }
        if ("hr_specialist".equals(roleTag)) {
            return buildList("员工档案维护要点", "HR流程与审批协作", "常见数据错误预防");
        }
        return buildList("系统导航与入口识别", "待办与消息协同处理", "操作前风险确认");
    }

    /**
     * 构造首周高频任务。
     *
     * @param roleTag 角色标签
     * @return 首周任务列表
     */
    private List<String> resolveFirstWeekTasks(String roleTag) {
        if ("platform_admin".equals(roleTag) || "tenant_admin".equals(roleTag)) {
            return buildList("检查租户 AI 开关与模型配置", "配置动作白名单与风险等级", "查看 AI 审计并建立巡检节奏");
        }
        if ("approver".equals(roleTag)) {
            return buildList("每日清理高优先级待办", "对驳回任务补充明确原因", "复盘审批积压来源");
        }
        if ("hr_specialist".equals(roleTag)) {
            return buildList("核对员工主数据完整性", "处理入转调离相关待办", "梳理本周异常提醒");
        }
        return buildList("熟悉菜单与业务入口", "建立每日待办清单", "掌握消息与待办联动处理");
    }

    /**
     * 构造常见错误提示。
     *
     * @param roleTag 角色标签
     * @return 常见错误列表
     */
    private List<String> resolveCommonMistakes(String roleTag) {
        if ("platform_admin".equals(roleTag) || "tenant_admin".equals(roleTag)) {
            return buildList("未区分租户直接修改配置", "高风险动作未确认就执行", "只看成功次数不看失败原因");
        }
        if ("approver".equals(roleTag)) {
            return buildList("未核对目标任务就审批", "驳回未写原因", "忽略超时与紧急标记");
        }
        if ("hr_specialist".equals(roleTag)) {
            return buildList("员工信息字段漏填", "流程节点理解偏差", "未先确认数据再执行动作");
        }
        return buildList("直接执行操作不先确认对象", "忽略页面上下文造成误解", "未及时处理高优先级提醒");
    }

    /**
     * 构造推荐问题。
     *
     * @param roleTag 角色标签
     * @return 推荐问题列表
     */
    private List<String> resolveSuggestedQuestions(String roleTag) {
        if ("platform_admin".equals(roleTag) || "tenant_admin".equals(roleTag)) {
            return buildList("给我一份本租户 AI 治理检查清单", "当前动作策略有哪些高风险项", "今天的 AI 审计里有哪些异常");
        }
        if ("approver".equals(roleTag)) {
            return buildList("总结我的审批待办", "我今天先处理哪三条任务", "帮我检查驳回操作注意点");
        }
        if ("hr_specialist".equals(roleTag)) {
            return buildList("我今天在人事模块先做什么", "员工档案常见错误有哪些", "帮我梳理本周HR待办");
        }
        return buildList("总结我的待办", "列出未读消息", "我今天优先做什么");
    }

    /**
     * 规范化字符串列表。
     *
     * @param source 原始列表
     * @return 规范化结果
     */
    private List<String> normalizeList(List<String> source) {
        List<String> normalized = new ArrayList<>();
        if (source == null) {
            return normalized;
        }
        for (String item : source) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            normalized.add(item.trim().toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    /**
     * 构造字符串列表。
     *
     * @param values 文本项
     * @return 列表
     */
    private List<String> buildList(String... values) {
        List<String> list = new ArrayList<>();
        if (values == null) {
            return list;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                list.add(normalized);
            }
        }
        return list;
    }

    /**
     * 去除空白并将空字符串转为 null。
     *
     * @param value 原始文本
     * @return 规范化文本
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
