package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程参与人配置选项集合。
 */
@Data
public class WorkflowParticipantOptionsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户选项 */
    private List<WorkflowParticipantOptionVO> users = new ArrayList<>();

    /** 部门选项 */
    private List<WorkflowParticipantOptionVO> depts = new ArrayList<>();

    /** 角色选项 */
    private List<WorkflowParticipantOptionVO> roles = new ArrayList<>();

    /** 岗位选项 */
    private List<WorkflowParticipantOptionVO> posts = new ArrayList<>();
}
