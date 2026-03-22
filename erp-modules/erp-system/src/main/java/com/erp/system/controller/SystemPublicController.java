package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.vo.PublicTenantOptionVO;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 系统公共接口控制层。
 */
@RestController
@RequestMapping("/system/public")
public class SystemPublicController {
    private static final String STATUS_ENABLED = "0";
    private static final String DEL_FLAG_EXISTS = "0";
    private static final String ACTIVE_TENANTS_SQL = "SELECT id, tenant_id, name, status "
            + "FROM sys_tenant WHERE status = ? AND del_flag = ? ORDER BY id ASC";

    private final JdbcTemplate jdbcTemplate;

    public SystemPublicController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询登录页可见的有效租户选项。
     *
     * @return 登录页租户选项
     */
    @GetMapping("/tenants/active")
    public R<List<PublicTenantOptionVO>> activeTenants() {
        List<PublicTenantOptionVO> resultList = jdbcTemplate.query(
                ACTIVE_TENANTS_SQL,
                activeTenantOptionRowMapper(),
                STATUS_ENABLED,
                DEL_FLAG_EXISTS);
        resultList.removeIf(Objects::isNull);
        return R.success(resultList);
    }

    /**
     * 构建登录页租户选项映射器。
     *
     * @return RowMapper
     */
    private RowMapper<PublicTenantOptionVO> activeTenantOptionRowMapper() {
        return (rs, rowNum) -> {
            String tenantId = trimToNull(rs.getString("tenant_id"));
            if (!StringUtils.hasText(tenantId)) {
                return null;
            }
            String tenantName = trimToNull(rs.getString("name"));
            PublicTenantOptionVO option = new PublicTenantOptionVO();
            option.setTenantId(tenantId);
            option.setName(tenantName);
            option.setStatus(StatusFieldSupport.normalizeBinaryStatus(rs.getString("status")));
            option.setOptionLabel(buildOptionLabel(tenantId, tenantName));
            return option;
        };
    }

    /**
     * 构建租户下拉展示文案。
     *
     * @param tenantId 租户编号
     * @param tenantName 租户名称
     * @return 展示文案
     */
    private String buildOptionLabel(String tenantId, String tenantName) {
        if (StringUtils.hasText(tenantId) && StringUtils.hasText(tenantName)) {
            return tenantId + " - " + tenantName;
        }
        if (StringUtils.hasText(tenantId)) {
            return tenantId;
        }
        if (StringUtils.hasText(tenantName)) {
            return tenantName;
        }
        return "-";
    }

    /**
     * 去空白后为空时返回 null。
     *
     * @param value 原始值
     * @return 规范化结果
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
