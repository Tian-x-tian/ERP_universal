package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
import com.erp.platform.contract.model.PlatformAiAuditView;
import com.erp.system.domain.SysAiAudit;

import java.util.List;

/**
 * AI 审计服务。
 */
public interface ISysAiAuditService extends IService<SysAiAudit> {
    /**
     * 写入 AI 审计记录。
     *
     * @param tenantId 租户编号
     * @param userId 用户ID
     * @param userName 用户账号
     * @param request 审计写入请求
     */
    void record(String tenantId, Long userId, String userName, PlatformAiAuditCreateRequest request);

    /**
     * 查询 AI 审计记录。
     *
     * @param tenantId 租户编号
     * @param limit 限制条数
     * @return 审计记录
     */
    List<PlatformAiAuditView> listByTenant(String tenantId, int limit);
}
