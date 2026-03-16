package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.vo.HrEmployeeArchiveBody;
import com.erp.business.hr.mapper.HrEmployeeArchiveMapper;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.mapper.HrSystemConfigMapper;
import com.erp.business.hr.service.IHrEmployeeArchiveService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 员工扩展档案服务实现。
 */
@Service
public class HrEmployeeArchiveServiceImpl implements IHrEmployeeArchiveService {
    private static final String CERT_UNIQUE_CONFIG_KEY = "hr.employee.cert_unique_enabled";
    private static final String DEFAULT_OPERATOR = "system";

    private final HrEmployeeArchiveMapper archiveMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final HrSystemConfigMapper systemConfigMapper;
    private final SecurityUserResolver securityUserResolver;

    public HrEmployeeArchiveServiceImpl(HrEmployeeArchiveMapper archiveMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            HrSystemConfigMapper systemConfigMapper,
            SecurityUserResolver securityUserResolver) {
        this.archiveMapper = archiveMapper;
        this.employeeCoreMapper = employeeCoreMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 新增员工扩展档案。
     *
     * @param archiveBody 档案参数
     * @return 落库后的档案对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeArchive createArchive(HrEmployeeArchiveBody archiveBody) {
        validateArchiveBody(archiveBody);
        Long employeeId = archiveBody.getEmployeeId();
        HrEmployeeCore employee = requireExistingEmployee(employeeId);
        if (archiveMapper.selectById(employeeId) != null) {
            throw new IllegalStateException("员工扩展档案已存在，请勿重复创建");
        }
        validateCertUniqueness(employeeId, archiveBody.getCertNo());
        Date now = new Date();
        String operator = resolveOperator();
        HrEmployeeArchive archive = new HrEmployeeArchive();
        archive.setEmployeeId(employeeId);
        archive.setTenantId(employee.getTenantId());
        archive.setCreateBy(operator);
        archive.setCreateTime(now);
        archive.setUpdateBy(operator);
        archive.setUpdateTime(now);
        applyArchiveBody(archive, archiveBody);
        archiveMapper.insert(archive);
        return archiveMapper.selectById(employeeId);
    }

    /**
     * 更新员工扩展档案，不存在时自动补建。
     *
     * @param employeeId  员工ID
     * @param archiveBody 档案参数
     * @return 更新后的档案对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeArchive updateArchive(Long employeeId, HrEmployeeArchiveBody archiveBody) {
        if (employeeId == null) {
            throw new IllegalArgumentException("员工ID不能为空");
        }
        validateArchiveBody(archiveBody);
        HrEmployeeCore employee = requireExistingEmployee(employeeId);
        if (HrEmployeeSupport.STATUS_ACTIVE.equalsIgnoreCase(employee.getStatus())) {
            throw new IllegalStateException("在职员工扩展档案变更需走审批流程");
        }
        validateCertUniqueness(employeeId, archiveBody.getCertNo());
        HrEmployeeArchive existedArchive = archiveMapper.selectById(employeeId);
        if (existedArchive == null) {
            HrEmployeeArchiveBody createBody = copyArchiveBody(archiveBody);
            createBody.setEmployeeId(employeeId);
            return createArchive(createBody);
        }
        HrEmployeeArchive archive = new HrEmployeeArchive();
        archive.setEmployeeId(employeeId);
        archive.setTenantId(employee.getTenantId());
        archive.setUpdateBy(resolveOperator());
        archive.setUpdateTime(new Date());
        applyArchiveBody(archive, archiveBody);
        archiveMapper.updateById(archive);
        return archiveMapper.selectById(employeeId);
    }

    /**
     * 审批通过后生效扩展档案。
     *
     * @param employeeId 员工ID
     * @param archiveBody 档案参数
     * @param operator 操作人
     * @return 更新后的档案对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeArchive applyApprovedArchive(Long employeeId, HrEmployeeArchiveBody archiveBody, String operator) {
        if (employeeId == null) {
            throw new IllegalArgumentException("员工ID不能为空");
        }
        validateArchiveBody(archiveBody);
        HrEmployeeCore employee = requireExistingEmployee(employeeId);
        validateCertUniqueness(employeeId, archiveBody.getCertNo());
        HrEmployeeArchive existedArchive = archiveMapper.selectById(employeeId);
        if (existedArchive == null) {
            HrEmployeeArchiveBody createBody = copyArchiveBody(archiveBody);
            createBody.setEmployeeId(employeeId);
            return createArchive(createBody);
        }
        HrEmployeeArchive archive = new HrEmployeeArchive();
        archive.setEmployeeId(employeeId);
        archive.setTenantId(employee.getTenantId());
        archive.setUpdateBy(StringUtils.hasText(operator) ? operator.trim() : resolveOperator());
        archive.setUpdateTime(new Date());
        applyArchiveBody(archive, archiveBody);
        archiveMapper.updateById(archive);
        return archiveMapper.selectById(employeeId);
    }

    /**
     * 按员工ID查询扩展档案。
     *
     * @param employeeId 员工ID
     * @return 档案对象
     */
    @Override
    public HrEmployeeArchive getArchiveByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return archiveMapper.selectById(employeeId);
    }

    /**
     * 按员工ID集合批量查询扩展档案。
     *
     * @param employeeIds 员工ID集合
     * @return 档案列表
     */
    @Override
    public List<HrEmployeeArchive> listArchivesByEmployeeIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return archiveMapper.selectBatchIds(employeeIds);
    }

    /**
     * 校验扩展档案参数基础合法性。
     *
     * @param archiveBody 档案参数
     */
    private void validateArchiveBody(HrEmployeeArchiveBody archiveBody) {
        if (archiveBody == null) {
            throw new IllegalArgumentException("员工扩展档案参数不能为空");
        }
    }

    /**
     * 校验员工核心主档是否存在。
     *
     * @param employeeId 员工ID
     * @return 员工主档
     */
    private HrEmployeeCore requireExistingEmployee(Long employeeId) {
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
     * 按配置校验证件号唯一性。
     *
     * @param employeeId 当前员工ID
     * @param certNo     证件号
     */
    private void validateCertUniqueness(Long employeeId, String certNo) {
        String normalizedCertNo = HrEmployeeSupport.trimToNull(certNo);
        if (!StringUtils.hasText(normalizedCertNo)) {
            return;
        }
        if (!HrEmployeeSupport.isCertUniqueEnabled(systemConfigMapper.selectConfigValue(CERT_UNIQUE_CONFIG_KEY))) {
            return;
        }
        Long duplicateCount = archiveMapper.selectCount(new LambdaQueryWrapper<HrEmployeeArchive>()
                .eq(HrEmployeeArchive::getCertNo, normalizedCertNo)
                .ne(employeeId != null, HrEmployeeArchive::getEmployeeId, employeeId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new IllegalStateException("证件号已存在，请检查后重试");
        }
    }

    /**
     * 将请求参数拷贝到档案对象。
     *
     * @param archive     档案对象
     * @param archiveBody 请求参数
     */
    private void applyArchiveBody(HrEmployeeArchive archive, HrEmployeeArchiveBody archiveBody) {
        archive.setCertType(HrEmployeeSupport.trimToNull(archiveBody.getCertType()));
        archive.setCertNo(HrEmployeeSupport.trimToNull(archiveBody.getCertNo()));
        archive.setGender(HrEmployeeSupport.trimToNull(archiveBody.getGender()));
        archive.setBirthDate(archiveBody.getBirthDate());
        archive.setEmploymentType(HrEmployeeSupport.trimToNull(archiveBody.getEmploymentType()));
        archive.setHireDate(archiveBody.getHireDate());
        archive.setProbationEndDate(archiveBody.getProbationEndDate());
        archive.setHighestEducation(HrEmployeeSupport.trimToNull(archiveBody.getHighestEducation()));
        archive.setEmergencyContact(HrEmployeeSupport.trimToNull(archiveBody.getEmergencyContact()));
        archive.setEmergencyPhone(HrEmployeeSupport.trimToNull(archiveBody.getEmergencyPhone()));
        archive.setHomeAddress(HrEmployeeSupport.trimToNull(archiveBody.getHomeAddress()));
        archive.setRemark(HrEmployeeSupport.trimToNull(archiveBody.getRemark()));
    }

    /**
     * 复制档案参数，避免直接修改原始请求对象。
     *
     * @param source 原始参数
     * @return 拷贝后的参数
     */
    private HrEmployeeArchiveBody copyArchiveBody(HrEmployeeArchiveBody source) {
        HrEmployeeArchiveBody target = new HrEmployeeArchiveBody();
        target.setEmployeeId(source.getEmployeeId());
        target.setCertType(source.getCertType());
        target.setCertNo(source.getCertNo());
        target.setGender(source.getGender());
        target.setBirthDate(source.getBirthDate());
        target.setEmploymentType(source.getEmploymentType());
        target.setHireDate(source.getHireDate());
        target.setProbationEndDate(source.getProbationEndDate());
        target.setHighestEducation(source.getHighestEducation());
        target.setEmergencyContact(source.getEmergencyContact());
        target.setEmergencyPhone(source.getEmergencyPhone());
        target.setHomeAddress(source.getHomeAddress());
        target.setRemark(source.getRemark());
        return target;
    }

    /**
     * 解析当前操作人。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : DEFAULT_OPERATOR;
    }
}
