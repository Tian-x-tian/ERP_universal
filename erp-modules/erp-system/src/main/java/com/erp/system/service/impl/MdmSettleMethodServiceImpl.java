package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmSettleMethod;
import com.erp.system.mapper.MdmSettleMethodMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmSettleMethodService;
import com.erp.system.service.IMdmReferenceCheckService;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * MDM 结算方式字典服务实现。
 */
@Service
public class MdmSettleMethodServiceImpl extends ServiceImpl<MdmSettleMethodMapper, MdmSettleMethod>
        implements IMdmSettleMethodService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;
    private final IMdmReferenceCheckService referenceCheckService;

    public MdmSettleMethodServiceImpl(IMdmAuditTrailService auditTrailService,
            SecurityUserResolver securityUserResolver,
            IMdmReferenceCheckService referenceCheckService) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
        this.referenceCheckService = referenceCheckService;
    }

    /**
     * 查询结算方式列表。
     *
     * @param settleCode 结算方式编码
     * @param settleName 结算方式名称
     * @param status     状态
     * @return 结算方式列表
     */
    @Override
    public List<MdmSettleMethod> selectList(String settleCode, String settleName, String status) {
        LambdaQueryWrapper<MdmSettleMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(settleCode)) {
            queryWrapper.like(MdmSettleMethod::getSettleCode, settleCode.trim());
        }
        if (StringUtils.hasText(settleName)) {
            queryWrapper.like(MdmSettleMethod::getSettleName, settleName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmSettleMethod::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmSettleMethod::getUpdateTime).orderByDesc(MdmSettleMethod::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增结算方式。
     *
     * @param settleMethod 结算方式对象
     * @return true 表示新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(MdmSettleMethod settleMethod) {
        if (settleMethod == null || !StringUtils.hasText(settleMethod.getSettleCode())
                || !StringUtils.hasText(settleMethod.getSettleName())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String settleCode = settleMethod.getSettleCode().trim();
        if (existsCode(settleCode, null)) {
            return false;
        }
        Date now = new Date();
        String operator = resolveOperator();
        settleMethod.setTenantId(tenantId);
        settleMethod.setSettleCode(settleCode);
        settleMethod.setSettleName(settleMethod.getSettleName().trim());
        settleMethod.setStatus(MdmStatusSupport.DRAFT);
        settleMethod.setVersionNo(1);
        settleMethod.setDelFlag(DEL_FLAG_EXIST);
        settleMethod.setCreateBy(operator);
        settleMethod.setUpdateBy(operator);
        settleMethod.setCreateTime(now);
        settleMethod.setUpdateTime(now);
        boolean saved = save(settleMethod);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.SETTLE_METHOD,
                    settleMethod.getSettleMethodId(),
                    MdmChangeTypeSupport.CREATE,
                    settleMethod.getVersionNo(),
                    settleMethod.getStatus(),
                    null,
                    settleMethod);
        }
        return saved;
    }

    /**
     * 修改结算方式。
     *
     * @param settleMethod 结算方式对象
     * @return true 表示修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modify(MdmSettleMethod settleMethod) {
        if (settleMethod == null || settleMethod.getSettleMethodId() == null) {
            return false;
        }
        MdmSettleMethod existed = getOne(new LambdaQueryWrapper<MdmSettleMethod>()
                .eq(MdmSettleMethod::getSettleMethodId, settleMethod.getSettleMethodId())
                .eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("结算方式审批中，暂不允许直接修改");
        }
        if (!MdmStatusSupport.isDraft(existed.getStatus())) {
            throw new IllegalStateException("已生效结算方式请通过审批流程提交变更");
        }
        MdmSettleMethod before = new MdmSettleMethod();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(settleMethod.getSettleCode())) {
            String settleCode = settleMethod.getSettleCode().trim();
            if (existsCode(settleCode, settleMethod.getSettleMethodId())) {
                return false;
            }
            settleMethod.setSettleCode(settleCode);
        }
        if (StringUtils.hasText(settleMethod.getSettleName())) {
            settleMethod.setSettleName(settleMethod.getSettleName().trim());
        }
        settleMethod
                .setStatus(MdmStatusSupport.normalizeStatusForUpdate(settleMethod.getStatus(), existed.getStatus()));
        settleMethod.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        settleMethod.setUpdateBy(resolveOperator());
        settleMethod.setUpdateTime(new Date());
        boolean updated = updateSettleMethodByVersion(settleMethod, existed.getVersionNo());
        if (updated) {
            MdmSettleMethod after = getById(settleMethod.getSettleMethodId());
            auditTrailService.record(MdmDomainTypeSupport.SETTLE_METHOD,
                    settleMethod.getSettleMethodId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? settleMethod.getVersionNo() : after.getVersionNo(),
                    after == null ? settleMethod.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用结算方式。
     *
     * @param settleMethodId 结算方式ID
     * @return true 表示停用成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long settleMethodId) {
        if (settleMethodId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.SETTLE_METHOD, settleMethodId);

        MdmSettleMethod existed = getOne(new LambdaQueryWrapper<MdmSettleMethod>()
                .eq(MdmSettleMethod::getSettleMethodId, settleMethodId)
                .eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("结算方式审批中，暂不允许直接停用");
        }
        if (MdmStatusSupport.isActive(existed.getStatus())) {
            throw new IllegalStateException("已生效结算方式请通过审批流程提交停用");
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmSettleMethod updateEntity = new MdmSettleMethod();
        updateEntity.setSettleMethodId(settleMethodId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateSettleMethodByVersion(updateEntity, existed.getVersionNo());
        if (updated) {
            MdmSettleMethod after = getById(settleMethodId);
            auditTrailService.record(MdmDomainTypeSupport.SETTLE_METHOD,
                    settleMethodId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除结算方式（逻辑删除）。
     *
     * @param settleMethodId 结算方式ID
     * @return true 表示删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long settleMethodId) {
        if (settleMethodId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.SETTLE_METHOD, settleMethodId);

        MdmSettleMethod existed = getOne(new LambdaQueryWrapper<MdmSettleMethod>()
                .eq(MdmSettleMethod::getSettleMethodId, settleMethodId)
                .eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            return false;
        }
        MdmSettleMethod updateEntity = new MdmSettleMethod();
        updateEntity.setSettleMethodId(settleMethodId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateSettleMethodByVersion(updateEntity, existed.getVersionNo());
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.SETTLE_METHOD,
                    settleMethodId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断编码是否重复。
     *
     * @param code      编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<MdmSettleMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmSettleMethod::getSettleCode, code);
        queryWrapper.eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmSettleMethod::getSettleMethodId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 按版本号执行乐观锁更新。
     *
     * @param settleMethod     更新对象
     * @param currentVersionNo 当前版本号
     * @return true 表示更新成功
     */
    private boolean updateSettleMethodByVersion(MdmSettleMethod settleMethod, Integer currentVersionNo) {
        if (settleMethod == null || settleMethod.getSettleMethodId() == null) {
            return false;
        }
        LambdaUpdateWrapper<MdmSettleMethod> updateWrapper = new LambdaUpdateWrapper<MdmSettleMethod>()
                .eq(MdmSettleMethod::getSettleMethodId, settleMethod.getSettleMethodId())
                .eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST);
        if (currentVersionNo != null) {
            updateWrapper.eq(MdmSettleMethod::getVersionNo, currentVersionNo);
        }
        boolean updated = update(settleMethod, updateWrapper);
        if (!updated) {
            throw new IllegalStateException("结算方式数据已被其他人更新，请刷新后重试");
        }
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
