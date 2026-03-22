package com.erp.workflow.contract.domain.vo;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程任务动态表单视图对象。
 */
public class WorkflowTaskFormVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程任务ID */
    private Long taskId;

    /** 流程实例ID */
    private Long instanceId;

    /** 流程定义ID */
    private Long definitionId;

    /** 流程定义版本号 */
    private Integer definitionVersion;

    /** 当前节点编码 */
    private String nodeKey;

    /** 当前节点名称 */
    private String nodeName;

    /** 表单版本号 */
    private Integer formVersion;

    /** 动态表单字段 */
    private List<WorkflowTaskFormFieldVO> fields = new ArrayList<>();


    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public Integer getDefinitionVersion() {
        return definitionVersion;
    }

    public void setDefinitionVersion(Integer definitionVersion) {
        this.definitionVersion = definitionVersion;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Integer getFormVersion() {
        return formVersion;
    }

    public void setFormVersion(Integer formVersion) {
        this.formVersion = formVersion;
    }

    public List<WorkflowTaskFormFieldVO> getFields() {
        return fields;
    }

    public void setFields(List<WorkflowTaskFormFieldVO> fields) {
        this.fields = fields;
    }
}

