package com.erp.system.service;

import com.erp.system.domain.vo.DataPermissionScope;

/**
 * 数据权限服务接口
 */
public interface IDataPermissionService {

    /**
     * 解析用户可访问的数据范围。
     *
     * @param userId 用户ID
     * @return 数据权限范围
     */
    DataPermissionScope resolveDataScope(Long userId);
}
