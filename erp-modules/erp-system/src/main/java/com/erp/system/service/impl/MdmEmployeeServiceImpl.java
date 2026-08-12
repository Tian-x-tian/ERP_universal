package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmCostCenter;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.MdmOrg;
import com.erp.system.domain.MdmProject;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.MdmCostCenterMapper;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.mapper.MdmOrgMapper;
import com.erp.system.mapper.MdmProjectMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmEmployeeService;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.IMdmReferenceCheckService;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmEmployeeStatusSupport;
import com.erp.system.support.MdmOptimisticLockSupport;
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
    private final MdmOrgMapper orgMapper;
    private final MdmCostCenterMapper costCenterMapper;
    private final MdmWarehouseMapper warehouseMapper;
    private final MdmProjectMapper projectMapper;
    private final ISysDeptService deptService;
    private final ISysUserService userService;
    private final IMdmReferenceCheckService referenceCheckService;

    public MdmEmployeeServiceImpl(IMdmAuditTrailService auditTrailService,
            SecurityUserResolver securityUserResolver,
            MdmOrgMapper orgMapper,
            MdmCostCenterMapper costCenterMapper,
            MdmWarehouseMapper warehouseMapper,
            MdmProjectMapper projectMapper,
            ISysDeptService deptService,
            ISysUserService userService,
            IMdmReferenceCheckService referenceCheckService) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
        this.orgMapper = orgMapper;
        this.costCenterMapper = costCenterMapper;
        this.warehouseMapper = warehouseMapper;
        this.projectMapper = projectMapper;
        this.deptService = deptService;
        this.userService = userService;
        this.referenceCheckService = referenceCheckService;
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
        if (existsEmpCode(empCode, null) || !isReferenceValid(employee)) {
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
        employee.setStatus(MdmEmployeeStatusSupport.DRAFT);
        employee.setVersionNo(1);
        employee.setDelFlag(DEL_FLAG_EXIST);
        employee.setEffectiveTime(null);
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
        if (MdmEmployeeStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("员工审批中，暂不允许直接修改");
        }
        if (!MdmEmployeeStatusSupport.isDraft(existed.getStatus())) {
            throw new IllegalStateException("已生效员工请通过审批流程提交变更");
        }
        Integer expectedVersionNo = MdmOptimisticLockSupport.requireVersion(
                employee.getVersionNo(),
                existed.getVersionNo(),
                "员工");
        if (StringUtils.hasText(employee.getEmpCode())) {
            String empCode = employee.getEmpCode().trim();
            if (existsEmpCode(empCode, employee.getEmployeeId())) {
                return false;
            }
            employee.setEmpCode(empCode);
        }
        employee.setOrgId(employee.getOrgId() == null ? existed.getOrgId() : employee.getOrgId());
        employee.setDeptId(employee.getDeptId() == null ? existed.getDeptId() : employee.getDeptId());
        employee.setUserId(employee.getUserId() == null ? existed.getUserId() : employee.getUserId());
        employee.setCostCenterId(
                employee.getCostCenterId() == null ? existed.getCostCenterId() : employee.getCostCenterId());
        if (!isReferenceValid(employee)) {
            return false;
        }
        MdmEmployee before = new MdmEmployee();
        BeanUtils.copyProperties(existed, before);

        employee.setEmpName(MdmValueSupport.trimToNull(employee.getEmpName()));
        employee.setMobile(MdmValueSupport.trimToNull(employee.getMobile()));
        employee.setEmail(MdmValueSupport.trimToNull(employee.getEmail()));
        employee.setPosition(MdmValueSupport.trimToNull(employee.getPosition()));
        employee.setStatus(
                MdmEmployeeStatusSupport.normalizeStatusForUpdate(employee.getStatus(), existed.getStatus()));
        employee.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateEmployeeByVersion(employee, expectedVersionNo);
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
    public boolean leaveEmployee(Long employeeId, Integer versionNo) {
        if (employeeId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.EMPLOYEE, employeeId);

        MdmEmployee existed = getOne(new LambdaQueryWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmEmployeeStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("员工审批中，暂不允许直接发起离职");
        }
        if (MdmEmployeeStatusSupport.isActive(existed.getStatus())) {
            throw new IllegalStateException("在职员工请通过审批流程提交离职");
        }
        if (MdmEmployeeStatusSupport.isLeave(existed.getStatus())) {
            return true;
        }
        Integer expectedVersionNo = MdmOptimisticLockSupport.requireVersion(versionNo, existed.getVersionNo(), "员工");
        MdmEmployee updateEntity = new MdmEmployee();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setStatus(MdmEmployeeStatusSupport.LEAVE);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateEmployeeByVersion(updateEntity, expectedVersionNo);
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
    public boolean removeEmployee(Long employeeId, Integer versionNo) {
        if (employeeId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.EMPLOYEE, employeeId);

        MdmEmployee existed = getOne(new LambdaQueryWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmEmployeeStatusSupport.isSubmitted(existed.getStatus())) {
            return false;
        }
        if (!MdmEmployeeStatusSupport.isDraft(existed.getStatus())
                && !MdmEmployeeStatusSupport.isLeave(existed.getStatus())) {
            return false;
        }
        Integer expectedVersionNo = MdmOptimisticLockSupport.requireVersion(versionNo, existed.getVersionNo(), "员工");
        MdmEmployee updateEntity = new MdmEmployee();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateEmployeeByVersion(updateEntity, expectedVersionNo);
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
     * @param empCode   员工编码
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
     * 校验员工引用字段是否有效。
     *
     * @param employee 员工对象
     * @return true 表示有效
     */
    private boolean isReferenceValid(MdmEmployee employee) {
        if (employee == null) {
            return false;
        }
        return isOrgValid(employee.getOrgId())
                && isCostCenterValid(employee.getCostCenterId())
                && isDeptValid(employee.getDeptId())
                && isUserValid(employee.getUserId());
    }

    /**
     * 校验组织引用是否有效。
     *
     * @param orgId 组织ID
     * @return true 表示有效
     */
    private boolean isOrgValid(Long orgId) {
        if (orgId == null || orgId < 1) {
            return true;
        }
        MdmOrg org = orgMapper.selectById(orgId);
        return org != null && DEL_FLAG_EXIST.equals(org.getDelFlag());
    }

    /**
     * 校验成本中心引用是否有效。
     *
     * @param costCenterId 成本中心ID
     * @return true 表示有效
     */
    private boolean isCostCenterValid(Long costCenterId) {
        if (costCenterId == null || costCenterId < 1) {
            return true;
        }
        MdmCostCenter costCenter = costCenterMapper.selectById(costCenterId);
        return costCenter != null && DEL_FLAG_EXIST.equals(costCenter.getDelFlag());
    }

    /**
     * 校验部门引用是否有效。
     *
     * @param deptId 部门ID
     * @return true 表示有效
     */
    private boolean isDeptValid(Long deptId) {
        if (deptId == null || deptId < 1) {
            return true;
        }
        SysDept dept = deptService.getById(deptId);
        return dept != null;
    }

    /**
     * 校验用户引用是否有效。
     *
     * @param userId 用户ID
     * @return true 表示有效
     */
    private boolean isUserValid(Long userId) {
        if (userId == null || userId < 1) {
            return true;
        }
        SysUser user = userService.getById(userId);
        return user != null;
    }

    /**
     * 判断员工是否已被其他主数据引用。
     *
     * @param employeeId 员工ID
     * @return true 表示已引用
     */
    private boolean updateEmployeeByVersion(MdmEmployee employee, Integer currentVersionNo) {
        if (employee == null || employee.getEmployeeId() == null) {
            return false;
        }
        LambdaUpdateWrapper<MdmEmployee> updateWrapper = new LambdaUpdateWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employee.getEmployeeId())
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST);
        if (currentVersionNo != null) {
            updateWrapper.eq(MdmEmployee::getVersionNo, currentVersionNo);
        }
        boolean updated = update(employee, updateWrapper);
        MdmOptimisticLockSupport.ensureUpdated(updated, "员工");
        return updated;
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
