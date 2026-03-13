package com.erp.system.domain.vo;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程参与人配置选项集合。
 */
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


    public List<WorkflowParticipantOptionVO> getUsers() {
        return users;
    }

    public void setUsers(List<WorkflowParticipantOptionVO> users) {
        this.users = users;
    }

    public List<WorkflowParticipantOptionVO> getDepts() {
        return depts;
    }

    public void setDepts(List<WorkflowParticipantOptionVO> depts) {
        this.depts = depts;
    }

    public List<WorkflowParticipantOptionVO> getRoles() {
        return roles;
    }

    public void setRoles(List<WorkflowParticipantOptionVO> roles) {
        this.roles = roles;
    }

    public List<WorkflowParticipantOptionVO> getPosts() {
        return posts;
    }

    public void setPosts(List<WorkflowParticipantOptionVO> posts) {
        this.posts = posts;
    }
}
