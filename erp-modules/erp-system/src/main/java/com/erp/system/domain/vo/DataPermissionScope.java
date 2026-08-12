package com.erp.system.domain.vo;


import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 数据权限范围对象
 */
public class DataPermissionScope {

    /** 是否拥有全部数据权限 */
    private boolean allData;

    /** 可访问公司ID集合 */
    private Set<Long> companyIds = new LinkedHashSet<>();

    /** 可访问部门ID集合 */
    private Set<Long> deptIds = new LinkedHashSet<>();


    public boolean isAllData() {
        return allData;
    }

    public void setAllData(boolean allData) {
        this.allData = allData;
    }

    public Set<Long> getCompanyIds() {
        return companyIds;
    }

    public void setCompanyIds(Set<Long> companyIds) {
        this.companyIds = companyIds;
    }

    public Set<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(Set<Long> deptIds) {
        this.deptIds = deptIds;
    }
}
