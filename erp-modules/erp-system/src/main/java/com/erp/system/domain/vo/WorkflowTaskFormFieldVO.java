package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 流程任务动态表单字段视图对象。
 */
@Data
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
}
