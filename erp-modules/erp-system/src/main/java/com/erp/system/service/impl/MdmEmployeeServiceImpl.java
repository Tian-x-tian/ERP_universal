package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmEmployeeService;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmEmployeeStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * MDM 员工主数据服务实现。
 */
@Service
public class MdmEmployeeServiceImpl extends ServiceImpl<MdmEmployeeMapper, MdmEmployee> implements IMdmEmployeeService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;

    public MdmEmployeeServiceImpl(IMdmAuditTrailService auditTrailService, SecurityUserResolver securityUserResolver) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询员工列表。
     *
     * @param empCode 员工编码
     * @param empName 员工名称
     * @param status  状态
     * @return 员工列表
     */
    @Override
    public List<MdmEmployee> selectEmployeeList(String empCode, String empName, String status) {
        LambdaQueryWrapper<MdmEmployee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(empCode)) {
            queryWrapper.like(MdmEmployee::getEmpCode, empCode.trim());
        }
        if (StringUtils.hasText(empName)) {
            queryWrapper.like(MdmEmployee::getEmpName, empName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmEmployee::getStatus, MdmEmployeeStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmEmployee::getUpdateTime).orderByDesc(MdmEmployee::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增员工。
     *
     * @param employee 员工对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createEmployee(MdmEmployee employee) {
        if (employee == null || !StringUtils.hasText(employee.getEmpCode())
                || !StringUtils.hasText(employee.getEmpName())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String empCode = employee.getEmpCode().trim();
        if (existsEmpCode(empCode, null)) {
            return false;
        }
        String operator = resolveOperator();
        Date now = new Date();
        employee.setTenantId(tenantId);
        employee.setEmpCode(empCode);
        employee.setEmpName(employee.getEmpName().trim());
        employee.setMobile(MdmValueSupport.trimToNull(employee.getMobile()));
        employee.setEmail(MdmValueSupport.trimToNull(employee.getEmail()));
        employee.setPosition(MdmValueSupport.trimToNull(employee.getPosition()));
        employee.setStatus(MdmEmployeeStatusSupport.normalizeStatus(employee.getStatus()));
        employee.setVersionNo(1);
        employee.setDelFlag(DEL_FLAG_EXIST);
        employee.setCreateBy(operator);
        employee.setUpdateBy(operator);
        employee.setCreateTime(now);
        employee.setUpdateTime(now);
        employee.setEffectiveTime(now);
        boolean saved = save(employee);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.EMPLOYEE,
                    employee.getEmployeeId(),
                    MdmChangeTypeSupport.CREATE,
                    employee.getVersionNo(),
                    employee.getStatus(),
                    null,
                    employee);
        }
        return saved;
    }

    /**
     * 修改员工。
     *
     * @param employee 员工对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEmployee(MdmEmployee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return false;
        }
        MdmEmployee existed = getOne(new LambdaQueryWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employee.getEmployeeId())
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        MdmEmployee before = new MdmEmployee();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(employee.getEmpCode())) {
            String empCode = employee.getEmpCode().trim();
            if (existsEmpCode(empCode, employee.getEmployeeId())) {
                return false;
            }
            employee.setEmpCode(empCode);
        }
        employee.setEmpName(MdmValueSupport.trimToNull(employee.getEmpName()));
        employee.setMobile(MdmValueSupport.trimToNull(employee.getMobile()));
        employee.setEmail(MdmValueSupport.trimToNull(employee.getEmail()));
        employee.setPosition(MdmValueSupport.trimToNull(employee.getPosition()));
        employee.setStatus(MdmEmployeeStatusSupport.normalizeStatusForUpdate(employee.getStatus(), existed.getStatus()));
        employee.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        employee.setUpdateBy(resolveOperator());
        employee.setUpdateTime(new Date());
        boolean updated = updateById(employee);
        if (updated) {
            MdmEmployee after = getById(employee.getEmployeeId());
            auditTrailService.record(MdmDomainTypeSupport.EMPLOYEE,
                    employee.getEmployeeId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? employee.getVersionNo() : after.getVersionNo(),
                    after == null ? employee.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 员工离职。
     *
     * @param employeeId 员工ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveEmployee(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        MdmEmployee existed = getOne(new LambdaQueryWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmEmployeeStatusSupport.isLeave(existed.getStatus())) {
            return true;
        }
        MdmEmployee updateEntity = new MdmEmployee();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setStatus(MdmEmployeeStatusSupport.LEAVE);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            MdmEmployee after = getById(employeeId);
            auditTrailService.record(MdmDomainTypeSupport.EMPLOYEE,
                    employeeId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除员工（逻辑删除）。
     *
     * @param employeeId 员工ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeEmployee(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        MdmEmployee existed = getOne(new LambdaQueryWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmEmployeeStatusSupport.isLeave(existed.getStatus())) {
            return false;
        }
        MdmEmployee updateEntity = new MdmEmployee();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.EMPLOYEE,
                    employeeId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断员工编码是否重复。
     *
     * @param empCode 员工编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsEmpCode(String empCode, Long excludeId) {
        LambdaQueryWrapper<MdmEmployee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmEmployee::getEmpCode, empCode);
        queryWrapper.eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmEmployee::getEmployeeId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 解析操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : DEFAULT_OPERATOR;
    }
}
