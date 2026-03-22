package com.erp.system.domain.vo;

/**
 * 登录页公开租户选项。
 */
public class PublicTenantOptionVO {
    private String tenantId;
    private String name;
    private String status;
    private String optionLabel;

    /**
     * 获取租户编号。
     *
     * @return 租户编号
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户编号。
     *
     * @param tenantId 租户编号
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 获取租户名称。
     *
     * @return 租户名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置租户名称。
     *
     * @param name 租户名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取租户状态。
     *
     * @return 租户状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置租户状态。
     *
     * @param status 租户状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取下拉展示文案。
     *
     * @return 展示文案
     */
    public String getOptionLabel() {
        return optionLabel;
    }

    /**
     * 设置下拉展示文案。
     *
     * @param optionLabel 展示文案
     */
    public void setOptionLabel(String optionLabel) {
        this.optionLabel = optionLabel;
    }
}
