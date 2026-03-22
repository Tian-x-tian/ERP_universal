package com.erp.workflow.domain.platform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 角色信息对象 sys_role
 */
@TableName("sys_role")
public class SysRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    @TableId(type = IdType.AUTO)
    private Long roleId;

    /** 租户编号 */
    private String tenantId;

    /** 菜单ID组 */
    @TableField(exist = false)
    private List<Long> menuIds;

    /** 部门ID组（用于自定义数据权限） */
    @TableField(exist = false)
    private List<Long> deptIds;

    /** 角色名称 */
    private String roleName;

    /** 角色权限字符串 */
    private String roleKey;

    /** 显示顺序 */
    private Integer roleSort;

    /** 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限） */
    private String dataScope;

    /** 角色状态（0正常 1停用） */
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;


    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<Long> getMenuIds() {
        return menuIds;
    }

    @JsonSetter("menuIds")
    public void setMenuIds(List<?> menuIds) {
        this.menuIds = parseIdList(menuIds);
    }

    public List<Long> getDeptIds() {
        return deptIds;
    }

    @JsonSetter("deptIds")
    public void setDeptIds(List<?> deptIds) {
        this.deptIds = parseIdList(deptIds);
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public Integer getRoleSort() {
        return roleSort;
    }

    @JsonSetter("roleSort")
    public void setRoleSort(Object roleSort) {
        this.roleSort = parseInteger(roleSort);
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 将任意 ID 列表转换为 Long 列表，忽略空值和非数字元素。
     *
     * @param rawList 原始列表
     * @return 解析后的 Long 列表
     */
    private List<Long> parseIdList(List<?> rawList) {
        if (rawList == null) {
            return null;
        }
        List<Long> parsed = new ArrayList<>();
        for (Object item : rawList) {
            Long value = parseLong(item);
            if (value != null) {
                parsed.add(value);
            }
        }
        return parsed;
    }

    /**
     * 将任意对象解析为 Long。
     *
     * @param value 原始值
     * @return 解析后的 Long，解析失败返回 null
     */
    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (value instanceof Map<?, ?> mapValue) {
            Long nestedId = parseLong(mapValue.get("id"));
            if (nestedId != null) {
                return nestedId;
            }
            nestedId = parseLong(mapValue.get("menuId"));
            if (nestedId != null) {
                return nestedId;
            }
            nestedId = parseLong(mapValue.get("deptId"));
            if (nestedId != null) {
                return nestedId;
            }
            return parseLong(mapValue.get("value"));
        }
        return null;
    }

    /**
     * 将任意对象解析为 Integer。
     *
     * @param value 原始值
     * @return 解析后的 Integer，解析失败返回 null
     */
    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }
}


