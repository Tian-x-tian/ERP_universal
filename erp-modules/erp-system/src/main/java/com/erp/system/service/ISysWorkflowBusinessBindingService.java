package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysWorkflowBusinessBinding;

import java.util.List;

/**
 * 流程业务动作绑定服务接口。
 */
public interface ISysWorkflowBusinessBindingService extends IService<SysWorkflowBusinessBinding> {

    /**
     * 查询指定租户、业务域、动作下可用的流程绑定。
     *
     * @param tenantId   当前租户编号
     * @param domainType 业务域类型
     * @param actionCode 业务动作编码
     * @return 流程绑定列表
     */
    List<SysWorkflowBusinessBinding> selectActiveBindings(String tenantId, String domainType, String actionCode);
}

