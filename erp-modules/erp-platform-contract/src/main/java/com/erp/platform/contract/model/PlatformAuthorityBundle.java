package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台权限聚合结果。
 */
public class PlatformAuthorityBundle implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> permissions = new ArrayList<>();
    private List<String> roleKeys = new ArrayList<>();

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<String> getRoleKeys() {
        return roleKeys;
    }

    public void setRoleKeys(List<String> roleKeys) {
        this.roleKeys = roleKeys;
    }
}

