package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.platform.contract.model.PlatformAiBriefSaveRequest;
import com.erp.platform.contract.model.PlatformAiBriefView;
import com.erp.system.domain.SysAiBrief;
import com.erp.system.mapper.SysAiBriefMapper;
import com.erp.system.service.ISysAiBriefService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * AI 每日简报服务实现。
 */
@Service
public class SysAiBriefServiceImpl extends ServiceImpl<SysAiBriefMapper, SysAiBrief> implements ISysAiBriefService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_FAILED = "FAILED";
    private static final String DEFAULT_BRIEF_TYPE = "daily";
    private static final int MAX_SUMMARY_LENGTH = 4000;

    /**
     * 查询当日简报。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param briefType 简报类型
     * @return 简报视图
     */
    @Override
    public PlatformAiBriefView getToday(String tenantId, Long userId, String briefType) {
        SysAiBrief entity = findToday(tenantId, userId, normalizeType(briefType));
        return entity == null ? null : toView(entity);
    }

    /**
     * 抢占当日简报的生成权。
     *
     * @param tenantId     租户编号
     * @param userId       用户ID
     * @param briefType    简报类型
     * @param staleMinutes 陈旧判定分钟数
     * @return true 表示抢占成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimToday(String tenantId, Long userId, String briefType, int staleMinutes) {
        if (!StringUtils.hasText(tenantId) || userId == null) {
            return false;
        }
        String type = normalizeType(briefType);
        Date today = todayDate();
        Date staleBefore = new Date(System.currentTimeMillis() - Math.max(1, staleMinutes) * 60_000L);

        SysAiBrief existing = findToday(tenantId, userId, type);
        if (existing == null) {
            SysAiBrief entity = new SysAiBrief();
            entity.setTenantId(tenantId.trim());
            entity.setUserId(userId);
            entity.setBriefDate(today);
            entity.setBriefType(type);
            entity.setStatus(STATUS_PENDING);
            entity.setCreateTime(new Date());
            entity.setUpdateTime(new Date());
            try {
                save(entity);
                return true;
            } catch (DuplicateKeyException ex) {
                // 并发下另一个实例先建了记录，本次放弃生成权。
                return false;
            }
        }
        return baseMapper.claimExisting(existing.getBriefId(), staleBefore) > 0;
    }

    /**
     * 回写生成结果。
     *
     * @param tenantId 租户编号
     * @param userId   用户ID
     * @param request  回写请求
     * @return 回写后的简报视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformAiBriefView saveResult(String tenantId, Long userId, PlatformAiBriefSaveRequest request) {
        if (!StringUtils.hasText(tenantId) || userId == null || request == null) {
            return null;
        }
        String type = normalizeType(request.getBriefType());
        SysAiBrief entity = findToday(tenantId, userId, type);
        if (entity == null) {
            entity = new SysAiBrief();
            entity.setTenantId(tenantId.trim());
            entity.setUserId(userId);
            entity.setBriefDate(todayDate());
            entity.setBriefType(type);
            entity.setCreateTime(new Date());
        }
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setSummary(limitLength(request.getSummary(), MAX_SUMMARY_LENGTH));
        entity.setBlocksJson(request.getBlocksJson());
        entity.setModel(limitLength(request.getModel(), 128));
        entity.setGenerateMs(request.getGenerateMs());
        entity.setUpdateTime(new Date());
        saveOrUpdate(entity);
        return toView(entity);
    }

    /**
     * 查询当日简报实体。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param briefType 简报类型
     * @return 简报实体
     */
    private SysAiBrief findToday(String tenantId, Long userId, String briefType) {
        if (!StringUtils.hasText(tenantId) || userId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysAiBrief>()
                .eq(SysAiBrief::getTenantId, tenantId.trim())
                .eq(SysAiBrief::getUserId, userId)
                .eq(SysAiBrief::getBriefDate, todayDate())
                .eq(SysAiBrief::getBriefType, briefType)
                .last("LIMIT 1"), false);
    }

    /**
     * 取当天零点。
     *
     * @return 当天日期
     */
    private Date todayDate() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 规范化简报类型。
     *
     * @param briefType 原始类型
     * @return 规范化类型
     */
    private String normalizeType(String briefType) {
        return StringUtils.hasText(briefType) ? briefType.trim() : DEFAULT_BRIEF_TYPE;
    }

    /**
     * 规范化状态。
     *
     * @param status 原始状态
     * @return 规范化状态
     */
    private String normalizeStatus(String status) {
        if (STATUS_READY.equalsIgnoreCase(status)) {
            return STATUS_READY;
        }
        if (STATUS_FAILED.equalsIgnoreCase(status)) {
            return STATUS_FAILED;
        }
        return STATUS_PENDING;
    }

    /**
     * 转换简报视图。
     *
     * @param entity 简报实体
     * @return 简报视图
     */
    private PlatformAiBriefView toView(SysAiBrief entity) {
        PlatformAiBriefView view = new PlatformAiBriefView();
        view.setBriefId(entity.getBriefId());
        view.setTenantId(entity.getTenantId());
        view.setUserId(entity.getUserId());
        view.setBriefDate(entity.getBriefDate());
        view.setBriefType(entity.getBriefType());
        view.setStatus(entity.getStatus());
        view.setSummary(entity.getSummary());
        view.setBlocksJson(entity.getBlocksJson());
        view.setModel(entity.getModel());
        view.setGenerateMs(entity.getGenerateMs());
        view.setUpdateTime(entity.getUpdateTime());
        return view;
    }

    /**
     * 限制文本长度。
     *
     * @param value     原始文本
     * @param maxLength 最大长度
     * @return 限制后的文本
     */
    private String limitLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
