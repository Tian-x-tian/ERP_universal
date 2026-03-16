package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeContract;
import com.erp.business.hr.domain.vo.HrEmployeeContractBody;
import com.erp.business.hr.domain.vo.HrEmployeeContractQuery;

/**
 * 员工合同服务接口。
 */
public interface IHrEmployeeContractService {

    /**
     * 分页查询合同。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    Page<HrEmployeeContract> selectPage(HrEmployeeContractQuery query);

    /**
     * 查询合同详情。
     *
     * @param contractId 合同ID
     * @return 合同详情
     */
    HrEmployeeContract getById(Long contractId);

    /**
     * 新增合同。
     *
     * @param body 保存参数
     * @return 合同详情
     */
    HrEmployeeContract createContract(HrEmployeeContractBody body);

    /**
     * 更新合同。
     *
     * @param contractId 合同ID
     * @param body 保存参数
     * @return 合同详情
     */
    HrEmployeeContract updateContract(Long contractId, HrEmployeeContractBody body);

    /**
     * 逻辑删除合同。
     *
     * @param contractId 合同ID
     * @return true 表示成功
     */
    boolean deleteContract(Long contractId);
}
