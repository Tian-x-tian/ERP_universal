package com.erp.system.service;

/**
 * MDM 变更留痕服务接口。
 */
public interface IMdmAuditTrailService {

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
    void record(String domainType,
            Long bizId,
            String changeType,
            Integer versionNo,
            String status,
            Object beforeObj,
            Object afterObj);
}
