package com.erp.system.domain.vo;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 流程任务动态表单字段视图对象。
 */
public class WorkflowTaskFormFieldVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 字段编码 */
    private String fieldCode;

    /** 字段名称 */
    private String fieldLabel;

    /** 字段组件类型（input/textarea/number/select/date） */
    private String componentType;

    /** 字段权限（edit/read/hidden） */
    private String permission;

    /** 是否必填 */
    private Boolean required;

    /** 字段占位提示 */
    private String placeholder;

    /** 字段值 */
    private Object value;

    /** 选择项 */
    private List<Map<String, Object>> options = new ArrayList<>();


    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<Map<String, Object>> getOptions() {
        return options;
    }

    public void setOptions(List<Map<String, Object>> options) {
        this.options = options;
    }
}
