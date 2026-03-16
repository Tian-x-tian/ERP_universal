package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeContract;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.vo.HrEmployeeContractBody;
import com.erp.business.hr.domain.vo.HrEmployeeContractQuery;
import com.erp.business.hr.mapper.HrEmployeeContractMapper;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.service.IHrEmployeeContractService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 员工合同服务实现。
 */
@Service
public class HrEmployeeContractServiceImpl implements IHrEmployeeContractService {
    private static final String CONTRACT_STATUS_DELETED = "DELETED";

    private final HrEmployeeContractMapper contractMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final SecurityUserResolver securityUserResolver;

    public HrEmployeeContractServiceImpl(HrEmployeeContractMapper contractMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            SecurityUserResolver securityUserResolver) {
        this.contractMapper = contractMapper;
        this.employeeCoreMapper = employeeCoreMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 分页查询合同。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public Page<HrEmployeeContract> selectPage(HrEmployeeContractQuery query) {
        HrEmployeeContractQuery safeQuery = query == null ? new HrEmployeeContractQuery() : query;
        Page<HrEmployeeContract> page = new Page<>(
                HrEmployeeSupport.normalizePageNum(safeQuery.getPageNum()),
                HrEmployeeSupport.normalizePageSize(safeQuery.getPageSize()));
        return contractMapper.selectPage(page, new LambdaQueryWrapper<HrEmployeeContract>()
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeContract::getTenantId, currentTenantId())
                .eq(safeQuery.getEmployeeId() != null, HrEmployeeContract::getEmployeeId, safeQuery.getEmployeeId())
                .like(StringUtils.hasText(safeQuery.getContractNo()), HrEmployeeContract::getContractNo,
                        HrEmployeeSupport.trimToNull(safeQuery.getContractNo()))
                .eq(StringUtils.hasText(safeQuery.getContractType()), HrEmployeeContract::getContractType,
                        HrEmployeeSupport.trimToNull(safeQuery.getContractType()))
                .eq(StringUtils.hasText(safeQuery.getStatus()), HrEmployeeContract::getStatus,
                        HrEmployeeSupport.normalizeStatus(safeQuery.getStatus()))
                .ge(safeQuery.getEndDateFrom() != null, HrEmployeeContract::getEndDate, safeQuery.getEndDateFrom())
                .le(safeQuery.getEndDateTo() != null, HrEmployeeContract::getEndDate, safeQuery.getEndDateTo())
                .ne(HrEmployeeContract::getStatus, CONTRACT_STATUS_DELETED)
                .orderByDesc(HrEmployeeContract::getEndDate)
                .orderByDesc(HrEmployeeContract::getUpdateTime));
    }

    /**
     * 查询合同详情。
     *
     * @param contractId 合同ID
     * @return 合同详情
     */
    @Override
    public HrEmployeeContract getById(Long contractId) {
        HrEmployeeContract contract = contractMapper.selectOne(new LambdaQueryWrapper<HrEmployeeContract>()
                .eq(HrEmployeeContract::getContractId, contractId)
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeContract::getTenantId, currentTenantId())
                .ne(HrEmployeeContract::getStatus, CONTRACT_STATUS_DELETED));
        if (contract == null) {
            throw new ServiceException("合同不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return contract;
    }

    /**
     * 新增合同。
     *
     * @param body 保存参数
     * @return 合同详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeContract createContract(HrEmployeeContractBody body) {
        HrEmployeeCore employee = requireEmployee(body == null ? null : body.getEmployeeId());
        Date now = new Date();
        HrEmployeeContract contract = new HrEmployeeContract();
        contract.setTenantId(employee.getTenantId());
        contract.setEmployeeId(employee.getEmployeeId());
        contract.setCreateBy(resolveOperator());
        contract.setCreateTime(now);
        contract.setUpdateBy(resolveOperator());
        contract.setUpdateTime(now);
        applyBody(contract, body);
        contractMapper.insert(contract);
        return contractMapper.selectById(contract.getContractId());
    }

    /**
     * 更新合同。
     *
     * @param contractId 合同ID
     * @param body 保存参数
     * @return 合同详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeContract updateContract(Long contractId, HrEmployeeContractBody body) {
        HrEmployeeContract existed = getById(contractId);
        HrEmployeeContract updateEntity = new HrEmployeeContract();
        updateEntity.setContractId(contractId);
        updateEntity.setTenantId(existed.getTenantId());
        updateEntity.setEmployeeId(existed.getEmployeeId());
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        applyBody(updateEntity, body);
        contractMapper.updateById(updateEntity);
        return contractMapper.selectById(contractId);
    }

    /**
     * 逻辑删除合同。
     *
     * @param contractId 合同ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteContract(Long contractId) {
        HrEmployeeContract existed = getById(contractId);
        HrEmployeeContract updateEntity = new HrEmployeeContract();
        updateEntity.setContractId(contractId);
        updateEntity.setTenantId(existed.getTenantId());
        updateEntity.setStatus(CONTRACT_STATUS_DELETED);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return contractMapper.updateById(updateEntity) > 0;
    }

    /**
     * 应用合同保存参数。
     *
     * @param contract 合同对象
     * @param body 保存参数
     */
    private void applyBody(HrEmployeeContract contract, HrEmployeeContractBody body) {
        if (body == null) {
            throw new IllegalArgumentException("合同参数不能为空");
        }
        contract.setContractNo(HrEmployeeSupport.trimToNull(body.getContractNo()));
        contract.setContractType(HrEmployeeSupport.trimToNull(body.getContractType()));
        contract.setStartDate(body.getStartDate());
        contract.setEndDate(body.getEndDate());
        contract.setStatus(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.normalizeStatus(body.getStatus()), HrEmployeeSupport.CONTRACT_STATUS_DRAFT));
        contract.setRemark(HrEmployeeSupport.trimToNull(body.getRemark()));
    }

    /**
     * 校验员工是否存在。
     *
     * @param employeeId 员工ID
     * @return 员工主档
     */
    private HrEmployeeCore requireEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("员工ID不能为空");
        }
        HrEmployeeCore employee = employeeCoreMapper.selectOne(new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getEmployeeId, employeeId)
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG));
        if (employee == null) {
            throw new ServiceException("员工不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return employee;
    }

    /**
     * 获取当前租户编号。
     *
     * @return 租户编号
     */
    private String currentTenantId() {
        return securityUserResolver.getCurrentTenantId();
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }
}
