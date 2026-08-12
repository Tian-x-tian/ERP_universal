package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.workflow.contract.domain.SysWorkflowBusinessBinding;
import com.erp.workflow.mapper.SysWorkflowBusinessBindingMapper;
import com.erp.workflow.service.ISysWorkflowBusinessBindingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 流程业务动作绑定服务实现。
 */
@Service
public class SysWorkflowBusinessBindingServiceImpl extends ServiceImpl<SysWorkflowBusinessBindingMapper, SysWorkflowBusinessBinding>
        implements ISysWorkflowBusinessBindingService {
    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String STATUS_ENABLED = "0";

    /**
     * 查询指定租户、业务域、动作下可用的流程绑定。
     *
     * @param tenantId   当前租户编号
     * @param domainType 业务域类型
     * @param actionCode 业务动作编码
     * @return 流程绑定列表
     */
    @Override
    public List<SysWorkflowBusinessBinding> selectActiveBindings(String tenantId, String domainType, String actionCode) {
        if (!StringUtils.hasText(domainType) || !StringUtils.hasText(actionCode)) {
            return Collections.emptyList();
        }
        String normalizedTenantId = StringUtils.hasText(tenantId) ? tenantId.trim() : DEFAULT_TENANT_ID;
        LambdaQueryWrapper<SysWorkflowBusinessBinding> queryWrapper = new LambdaQueryWrapper<SysWorkflowBusinessBinding>()
                .eq(SysWorkflowBusinessBinding::getDomainType, domainType.trim())
                .eq(SysWorkflowBusinessBinding::getActionCode, actionCode.trim())
                .eq(SysWorkflowBusinessBinding::getStatus, STATUS_ENABLED)
                .orderByAsc(SysWorkflowBusinessBinding::getPriority)
                .orderByDesc(SysWorkflowBusinessBinding::getIsDefault)
                .orderByAsc(SysWorkflowBusinessBinding::getBindingId);
        if (DEFAULT_TENANT_ID.equals(normalizedTenantId)) {
            queryWrapper.eq(SysWorkflowBusinessBinding::getTenantId, DEFAULT_TENANT_ID);
        } else {
            queryWrapper.in(SysWorkflowBusinessBinding::getTenantId, normalizedTenantId, DEFAULT_TENANT_ID);
        }
        return list(queryWrapper);
    }
}



