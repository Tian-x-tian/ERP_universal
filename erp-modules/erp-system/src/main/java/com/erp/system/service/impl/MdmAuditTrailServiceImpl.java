package com.erp.system.service.impl;

import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.system.domain.MdmChangeLog;
import com.erp.system.domain.MdmVersion;
import com.erp.system.mapper.MdmChangeLogMapper;
import com.erp.system.mapper.MdmVersionMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.support.TenantWriteGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * MDM 变更留痕服务实现。
 */
@Service
public class MdmAuditTrailServiceImpl implements IMdmAuditTrailService {
    private static final String SOURCE_API = "API";
    private static final String DEFAULT_OPERATOR = "system";

    private final MdmChangeLogMapper changeLogMapper;
    private final MdmVersionMapper versionMapper;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;

    public MdmAuditTrailServiceImpl(MdmChangeLogMapper changeLogMapper,
            MdmVersionMapper versionMapper,
            SecurityUserResolver securityUserResolver,
            ObjectMapper objectMapper) {
        this.changeLogMapper = changeLogMapper;
        this.versionMapper = versionMapper;
        this.securityUserResolver = securityUserResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录主数据变更日志与版本快照。
     *
     * @param domainType 域类型
     * @param bizId      业务主键ID
     * @param changeType 变更类型
     * @param versionNo  版本号
     * @param status     状态
     * @param beforeObj  变更前对象
     * @param afterObj   变更后对象
     */
    @Override
    public void record(String domainType,
            Long bizId,
            String changeType,
            Integer versionNo,
            String status,
            Object beforeObj,
            Object afterObj) {
        if (!StringUtils.hasText(domainType) || bizId == null || !StringUtils.hasText(changeType)) {
            return;
        }
        Date now = new Date();
        String tenantId = TenantWriteGuard.currentTenantId();
        String operator = resolveOperator();
        String beforeJson = toJson(beforeObj);
        String afterJson = toJson(afterObj);

        MdmChangeLog changeLog = new MdmChangeLog();
        changeLog.setTenantId(tenantId);
        changeLog.setDomainType(domainType);
        changeLog.setBizId(bizId);
        changeLog.setChangeType(changeType);
        changeLog.setBeforeJson(beforeJson);
        changeLog.setAfterJson(afterJson);
        changeLog.setOperator(operator);
        changeLog.setTraceId(RequestTraceContextHolder.getTraceId());
        changeLog.setSource(SOURCE_API);
        changeLog.setCreateTime(now);
        changeLogMapper.insert(changeLog);

        if (versionNo == null || afterObj == null) {
            return;
        }
        MdmVersion version = new MdmVersion();
        version.setTenantId(tenantId);
        version.setDomainType(domainType);
        version.setBizId(bizId);
        version.setVersionNo(versionNo);
        version.setStatus(status);
        if (status != null && "ACTIVE".equalsIgnoreCase(status)) {
            version.setEffectiveTime(now);
        }
        version.setSnapshotJson(afterJson);
        version.setCreateBy(operator);
        version.setCreateTime(now);
        versionMapper.insert(version);
    }

    /**
     * 将对象转换为 JSON 字符串。
     *
     * @param source 原始对象
     * @return JSON 字符串
     */
    private String toJson(Object source) {
        if (source == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"serialize-failed\"}";
        }
    }

    /**
     * 解析当前操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        if (!StringUtils.hasText(username)) {
            return DEFAULT_OPERATOR;
        }
        return username.trim();
    }
}
