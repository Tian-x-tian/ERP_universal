package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.core.exception.ServiceException;
import com.erp.system.domain.SysUserUiPreference;
import com.erp.system.domain.vo.UiPreferenceBundle;
import com.erp.system.mapper.SysUserUiPreferenceMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysUserUiPreferenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI 个性化偏好服务实现。
 *
 * <p>后端在此只做「存储 + 合并 + 锁定项约束」，不校验具体配置项含义，
 * 便于前端新增视觉维度时无需同步改动后端。
 */
@Service
public class SysUserUiPreferenceServiceImpl
        extends ServiceImpl<SysUserUiPreferenceMapper, SysUserUiPreference>
        implements ISysUserUiPreferenceService {

    /** 作用域：租户默认策略 */
    private static final String SCOPE_TENANT = "0";
    /** 作用域：用户个人偏好 */
    private static final String SCOPE_USER = "1";
    /** 租户默认策略占位用户ID */
    private static final Long TENANT_SCOPE_USER_ID = 0L;

    private final ObjectMapper objectMapper;
    private final SecurityUserResolver securityUserResolver;

    public SysUserUiPreferenceServiceImpl(ObjectMapper objectMapper, SecurityUserResolver securityUserResolver) {
        this.objectMapper = objectMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询当前登录用户的生效 UI 偏好。
     *
     * @return 偏好下发包
     */
    @Override
    public UiPreferenceBundle getCurrentUserPreference() {
        SysUserUiPreference tenantEntity = findTenantDefaultEntity();
        SysUserUiPreference userEntity = findUserEntity(requireCurrentUserId());
        return buildBundle(tenantEntity, userEntity);
    }

    /**
     * 保存当前登录用户的个人 UI 偏好，锁定项会被自动剔除。
     *
     * @param preference 个人偏好
     * @return 保存后的偏好下发包
     */
    @Override
    public UiPreferenceBundle saveCurrentUserPreference(Map<String, Object> preference) {
        Long userId = requireCurrentUserId();
        SysUserUiPreference tenantEntity = findTenantDefaultEntity();
        List<String> lockedKeys = parseLockedKeys(tenantEntity);

        Map<String, Object> sanitized = new LinkedHashMap<>(safeMap(preference));
        lockedKeys.forEach(sanitized::remove);

        SysUserUiPreference userEntity = findUserEntity(userId);
        boolean isNew = userEntity == null;
        if (isNew) {
            userEntity = new SysUserUiPreference();
            userEntity.setTenantId(requireCurrentTenantId());
            userEntity.setScopeType(SCOPE_USER);
            userEntity.setUserId(userId);
            userEntity.setCreateBy(resolveOperator());
            userEntity.setCreateTime(new Date());
        }
        userEntity.setPreferenceJson(writeJson(sanitized));
        userEntity.setUpdateBy(resolveOperator());
        userEntity.setUpdateTime(new Date());

        if (isNew) {
            save(userEntity);
        } else {
            updateById(userEntity);
        }
        return buildBundle(tenantEntity, userEntity);
    }

    /**
     * 重置当前登录用户的个人 UI 偏好。
     *
     * @return 重置后的偏好下发包
     */
    @Override
    public UiPreferenceBundle resetCurrentUserPreference() {
        SysUserUiPreference userEntity = findUserEntity(requireCurrentUserId());
        if (userEntity != null) {
            removeById(userEntity.getPreferenceId());
        }
        return buildBundle(findTenantDefaultEntity(), null);
    }

    /**
     * 查询当前租户的默认 UI 策略。
     *
     * @return 偏好下发包
     */
    @Override
    public UiPreferenceBundle getTenantDefault() {
        return buildBundle(findTenantDefaultEntity(), null);
    }

    /**
     * 保存当前租户的默认 UI 策略与锁定项。
     *
     * @param preference 租户默认偏好
     * @param lockedKeys 锁定项
     * @return 保存后的偏好下发包
     */
    @Override
    public UiPreferenceBundle saveTenantDefault(Map<String, Object> preference, List<String> lockedKeys) {
        SysUserUiPreference tenantEntity = findTenantDefaultEntity();
        boolean isNew = tenantEntity == null;
        if (isNew) {
            tenantEntity = new SysUserUiPreference();
            tenantEntity.setTenantId(requireCurrentTenantId());
            tenantEntity.setScopeType(SCOPE_TENANT);
            tenantEntity.setUserId(TENANT_SCOPE_USER_ID);
            tenantEntity.setCreateBy(resolveOperator());
            tenantEntity.setCreateTime(new Date());
        }
        tenantEntity.setPreferenceJson(writeJson(safeMap(preference)));
        tenantEntity.setLockedKeys(joinLockedKeys(lockedKeys));
        tenantEntity.setUpdateBy(resolveOperator());
        tenantEntity.setUpdateTime(new Date());

        if (isNew) {
            save(tenantEntity);
        } else {
            updateById(tenantEntity);
        }
        return buildBundle(tenantEntity, null);
    }

    /**
     * 组装偏好下发包，执行三级合并与锁定项约束。
     *
     * @param tenantEntity 租户默认策略
     * @param userEntity   用户个人偏好
     * @return 偏好下发包
     */
    private UiPreferenceBundle buildBundle(SysUserUiPreference tenantEntity, SysUserUiPreference userEntity) {
        Map<String, Object> tenantDefault = readJson(tenantEntity == null ? null : tenantEntity.getPreferenceJson());
        Map<String, Object> personal = readJson(userEntity == null ? null : userEntity.getPreferenceJson());
        List<String> lockedKeys = parseLockedKeys(tenantEntity);

        Map<String, Object> merged = new LinkedHashMap<>(tenantDefault);
        merged.putAll(personal);
        for (String lockedKey : lockedKeys) {
            if (tenantDefault.containsKey(lockedKey)) {
                merged.put(lockedKey, tenantDefault.get(lockedKey));
            } else {
                // 锁定但租户未给值：回退系统默认，禁止个人值生效
                merged.remove(lockedKey);
            }
        }

        UiPreferenceBundle bundle = new UiPreferenceBundle();
        bundle.setPreference(merged);
        bundle.setPersonal(personal);
        bundle.setTenantDefault(tenantDefault);
        bundle.setLockedKeys(lockedKeys);
        return bundle;
    }

    /**
     * 查询当前租户的默认策略记录。
     *
     * @return 策略记录，不存在返回 null
     */
    private SysUserUiPreference findTenantDefaultEntity() {
        return getOne(new LambdaQueryWrapper<SysUserUiPreference>()
                .eq(SysUserUiPreference::getScopeType, SCOPE_TENANT)
                .eq(SysUserUiPreference::getUserId, TENANT_SCOPE_USER_ID)
                .last("LIMIT 1"));
    }

    /**
     * 查询指定用户的个人偏好记录。
     *
     * @param userId 用户ID
     * @return 偏好记录，不存在返回 null
     */
    private SysUserUiPreference findUserEntity(Long userId) {
        return getOne(new LambdaQueryWrapper<SysUserUiPreference>()
                .eq(SysUserUiPreference::getScopeType, SCOPE_USER)
                .eq(SysUserUiPreference::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * 解析锁定项集合。
     *
     * @param entity 租户策略记录
     * @return 锁定项列表
     */
    private List<String> parseLockedKeys(SysUserUiPreference entity) {
        String rawLockedKeys = entity == null ? null : entity.getLockedKeys();
        if (!StringUtils.hasText(rawLockedKeys)) {
            return new ArrayList<>();
        }
        return Arrays.stream(rawLockedKeys.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 序列化锁定项集合。
     *
     * @param lockedKeys 锁定项
     * @return 逗号分隔字符串
     */
    private String joinLockedKeys(List<String> lockedKeys) {
        if (lockedKeys == null || lockedKeys.isEmpty()) {
            return null;
        }
        String joined = lockedKeys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
        return StringUtils.hasText(joined) ? joined : null;
    }

    /**
     * 反序列化偏好 JSON。
     *
     * @param json JSON 文本
     * @return 偏好键值表，解析失败返回空表
     */
    private Map<String, Object> readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            return parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 序列化偏好键值表。
     *
     * @param preference 偏好键值表
     * @return JSON 文本
     */
    private String writeJson(Map<String, Object> preference) {
        try {
            return objectMapper.writeValueAsString(safeMap(preference));
        } catch (Exception ignored) {
            return "{}";
        }
    }

    /**
     * 空安全的键值表。
     *
     * @param preference 原始键值表
     * @return 非空键值表
     */
    private Map<String, Object> safeMap(Map<String, Object> preference) {
        return preference == null ? new LinkedHashMap<>() : preference;
    }

    /**
     * 获取当前登录用户ID，缺失时抛出业务异常。
     *
     * @return 用户ID
     */
    private Long requireCurrentUserId() {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null) {
            throw new ServiceException("未获取到当前登录用户，无法处理 UI 偏好");
        }
        return userId;
    }

    /**
     * 获取当前登录租户编号，缺失时抛出业务异常。
     *
     * @return 租户编号
     */
    private String requireCurrentTenantId() {
        String tenantId = securityUserResolver.getCurrentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new ServiceException("未获取到当前租户，无法处理 UI 偏好");
        }
        return tenantId;
    }

    /**
     * 解析当前操作人。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String userName = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(userName) ? userName.trim() : "system";
    }
}
