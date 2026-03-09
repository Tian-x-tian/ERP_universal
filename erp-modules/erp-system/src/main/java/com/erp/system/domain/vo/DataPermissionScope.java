package com.erp.system.domain.vo;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 数据权限范围对象
 */
@Data
public class DataPermissionScope {

    /** 是否拥有全部数据权限 */
    private boolean allData;

    /** 可访问公司ID集合 */
    private Set<Long> companyIds = new LinkedHashSet<>();

    /** 可访问部门ID集合 */
    private Set<Long> deptIds = new LinkedHashSet<>();
}
