package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
import com.erp.platform.contract.model.PlatformAiAuditView;
import com.erp.system.domain.SysAiAudit;
import com.erp.system.mapper.SysAiAuditMapper;
import com.erp.system.service.ISysAiAuditService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AI 审计服务实现。
 */
@Service
public class SysAiAuditServiceImpl extends ServiceImpl<SysAiAuditMapper, SysAiAudit> implements ISysAiAuditService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    /**
     * 写入 AI 审计记录。
     *
     * @param tenantId 租户编号
     * @param userId 用户ID
     * @param userName 用户账号
     * @param request 审计写入请求
     */
    @Override
    public void record(String tenantId, Long userId, String userName, PlatformAiAuditCreateRequest request) {
        if (!StringUtils.hasText(tenantId) || request == null) {
            return;
        }
        SysAiAudit entity = new SysAiAudit();
        entity.setTenantId(tenantId.trim());
        entity.setUserId(userId);
        entity.setUserName(trimToNull(userName));
        entity.setQuestionType(limitLength(trimToNull(request.getQuestionType()), 64));
        entity.setInteractionLevel(limitLength(trimToNull(request.getInteractionLevel()), 16));
        entity.setActionKey(limitLength(trimToNull(request.getActionKey()), 64));
        entity.setActionConfirmed(Boolean.TRUE.equals(request.getActionConfirmed()) ? "1" : "0");
        entity.setSuccessFlag(Boolean.TRUE.equals(request.getSuccess()) ? "1" : "0");
        entity.setPromptInjectionFlag(Boolean.TRUE.equals(request.getPromptInjectionDetected()) ? "1" : "0");
        entity.setSensitiveHitFlag(Boolean.TRUE.equals(request.getSensitiveHit()) ? "1" : "0");
        entity.setRequestExcerpt(limitLength(trimToNull(request.getRequestExcerpt()), 500));
        entity.setResponseExcerpt(limitLength(trimToNull(request.getResponseExcerpt()), 500));
        entity.setDurationMs(request.getDurationMs());
        entity.setCreateTime(new Date());
        save(entity);
    }

    /**
     * 查询 AI 审计记录。
     *
     * @param tenantId 租户编号
     * @param limit 限制条数
     * @return 审计记录
     */
    @Override
    public List<PlatformAiAuditView> listByTenant(String tenantId, int limit) {
        if (!StringUtils.hasText(tenantId)) {
            return new ArrayList<>();
        }
        int safeLimit = normalizeLimit(limit);
        List<SysAiAudit> records = list(new LambdaQueryWrapper<SysAiAudit>()
                .eq(SysAiAudit::getTenantId, tenantId.trim())
                .orderByDesc(SysAiAudit::getCreateTime)
                .orderByDesc(SysAiAudit::getAuditId)
                .last("LIMIT " + safeLimit));
        List<PlatformAiAuditView> result = new ArrayList<>();
        for (SysAiAudit record : records) {
            PlatformAiAuditView view = new PlatformAiAuditView();
            view.setAuditId(record.getAuditId());
            view.setTenantId(record.getTenantId());
            view.setUserId(record.getUserId());
            view.setUserName(record.getUserName());
            view.setQuestionType(record.getQuestionType());
            view.setInteractionLevel(record.getInteractionLevel());
            view.setActionKey(record.getActionKey());
            view.setActionConfirmed(record.getActionConfirmed());
            view.setSuccessFlag(record.getSuccessFlag());
            view.setPromptInjectionFlag(record.getPromptInjectionFlag());
            view.setSensitiveHitFlag(record.getSensitiveHitFlag());
            view.setRequestExcerpt(record.getRequestExcerpt());
            view.setResponseExcerpt(record.getResponseExcerpt());
            view.setDurationMs(record.getDurationMs());
            view.setCreateTime(record.getCreateTime());
            result.add(view);
        }
        return result;
    }

    /**
     * 限制文本长度。
     *
     * @param value 原始文本
     * @param maxLength 最大长度
     * @return 限制后的文本
     */
    private String limitLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 规范化查询条数。
     *
     * @param limit 原始条数
     * @return 规范化条数
     */
    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
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
