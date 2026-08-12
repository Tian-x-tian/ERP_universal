package com.erp.system.service;

/**
 * MDM 主数据引用检查服务。
 * 用于在删除或停用主数据前，检查其是否已被下游业务模块引用。
 */
public interface IMdmReferenceCheckService {

    /**
     * 检查主数据是否被引用。
     * 如果被引用，将抛出带特定错误码 (例如 40901) 的业务异常。
     *
     * @param domainType 主数据类型（如：CUSTOMER, SUPPLIER, ITEM, EMPLOYEE等）
     * @param id         主数据ID
     */
    void check(String domainType, Long id);
}
