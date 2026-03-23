package com.erp.workflow.service.platform.impl;

import com.erp.common.client.internal.InternalPlatformClient;
import com.erp.platform.contract.model.PlatformDeptView;
import com.erp.platform.contract.model.PlatformPostView;
import com.erp.platform.contract.model.PlatformRoleView;
import com.erp.platform.contract.model.PlatformUserPostLink;
import com.erp.platform.contract.model.PlatformUserRoleLink;
import com.erp.platform.contract.model.PlatformUserView;
import com.erp.workflow.service.platform.IWorkflowPlatformReadService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流模块平台只读适配服务实现。
 */
@Service
public class WorkflowPlatformReadServiceImpl implements IWorkflowPlatformReadService {
    private static final String STATUS_ENABLED = "0";
    private static final String DEL_FLAG_EXISTS = "0";

    private final InternalPlatformClient internalPlatformClient;

    public WorkflowPlatformReadServiceImpl(InternalPlatformClient internalPlatformClient) {
        this.internalPlatformClient = internalPlatformClient;
    }

    /**
     * 按租户和账号查询活动用户。
     *
     * @param tenantId 租户编号
     * @param userName 用户账号
     * @return 用户投影
     */
    @Override
    public PlatformUserView getActiveUserByUsername(String tenantId, String userName) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(userName)) {
            return null;
        }
        return internalPlatformClient.getActiveUserByUsername(tenantId.trim(), userName.trim());
    }

    /**
     * 按用户ID查询活动用户。
     *
     * @param userId 用户ID
     * @return 用户投影
     */
    @Override
    public PlatformUserView getUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return internalPlatformClient.getUser(userId);
    }

    /**
     * 批量查询活动用户并按用户ID建立映射。
     *
     * @param userIds 用户ID集合
     * @return 用户映射
     */
    @Override
    public Map<Long, PlatformUserView> getUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, PlatformUserView> result = new LinkedHashMap<>();
        for (PlatformUserView user : internalPlatformClient.listUsers(userIds)) {
            if (user != null && user.getUserId() != null) {
                result.put(user.getUserId(), user);
            }
        }
        return result;
    }

    /**
     * 查询当前租户活动用户列表。
     *
     * @return 用户列表
     */
    @Override
    public List<PlatformUserView> listUsers() {
        return filterActiveUsers(internalPlatformClient.listUsers(null));
    }

    /**
     * 查询指定部门下的活动用户列表。
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    @Override
    public List<PlatformUserView> listUsersByDeptId(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        return filterActiveUsers(internalPlatformClient.listUsersByDeptId(deptId));
    }

    /**
     * 按账号或昵称精确查找活动用户。
     *
     * @param keyword 账号或昵称
     * @return 用户投影
     */
    @Override
    public PlatformUserView findUserByUserNameOrNickName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalizedKeyword = keyword.trim();
        for (PlatformUserView user : listUsers()) {
            if (user == null) {
                continue;
            }
            if (normalizedKeyword.equals(user.getUserName()) || normalizedKeyword.equals(user.getNickName())) {
                return user;
            }
        }
        return null;
    }

    /**
     * 批量查询活动部门并按部门ID建立映射。
     *
     * @param deptIds 部门ID集合
     * @return 部门映射
     */
    @Override
    public Map<Long, PlatformDeptView> getDepartmentMap(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, PlatformDeptView> result = new LinkedHashMap<>();
        for (PlatformDeptView dept : internalPlatformClient.listDepartments(deptIds)) {
            if (dept != null && dept.getDeptId() != null) {
                result.put(dept.getDeptId(), dept);
            }
        }
        return result;
    }

    /**
     * 查询单个活动部门。
     *
     * @param deptId 部门ID
     * @return 部门投影
     */
    @Override
    public PlatformDeptView getDepartment(Long deptId) {
        if (deptId == null) {
            return null;
        }
        PlatformDeptView dept = internalPlatformClient.getDepartment(deptId);
        return isActiveDept(dept) ? dept : null;
    }

    /**
     * 查询当前租户活动部门列表。
     *
     * @return 部门列表
     */
    @Override
    public List<PlatformDeptView> listDepartments() {
        List<PlatformDeptView> result = new ArrayList<>();
        for (PlatformDeptView dept : internalPlatformClient.listDepartments(null)) {
            if (isActiveDept(dept)) {
                result.add(dept);
            }
        }
        return result;
    }

    /**
     * 查询当前租户活动角色列表。
     *
     * @return 角色列表
     */
    @Override
    public List<PlatformRoleView> listRoles() {
        List<PlatformRoleView> result = new ArrayList<>();
        for (PlatformRoleView role : internalPlatformClient.listRoles()) {
            if (isActiveRole(role)) {
                result.add(role);
            }
        }
        return result;
    }

    /**
     * 查询当前租户活动岗位列表。
     *
     * @return 岗位列表
     */
    @Override
    public List<PlatformPostView> listPosts() {
        List<PlatformPostView> result = new ArrayList<>();
        for (PlatformPostView post : internalPlatformClient.listPosts()) {
            if (isActivePost(post)) {
                result.add(post);
            }
        }
        return result;
    }

    /**
     * 按角色解析用户ID集合。
     *
     * @param roleIds 角色ID集合
     * @return 用户ID列表
     */
    @Override
    public List<Long> resolveUserIdsByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (PlatformUserRoleLink relation : internalPlatformClient.listUserRoleLinks(roleIds)) {
            if (relation != null && relation.getUserId() != null) {
                result.add(relation.getUserId());
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 按岗位解析用户ID集合。
     *
     * @param postIds 岗位ID集合
     * @return 用户ID列表
     */
    @Override
    public List<Long> resolveUserIdsByPostIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (PlatformUserPostLink relation : internalPlatformClient.listUserPostLinks(postIds)) {
            if (relation != null && relation.getUserId() != null) {
                result.add(relation.getUserId());
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 过滤活动用户集合。
     *
     * @param userList 原始用户列表
     * @return 活动用户列表
     */
    private List<PlatformUserView> filterActiveUsers(List<PlatformUserView> userList) {
        if (userList == null || userList.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlatformUserView> result = new ArrayList<>();
        for (PlatformUserView user : userList) {
            if (isActiveUser(user)) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * 判断用户是否处于活动状态。
     *
     * @param user 用户投影
     * @return true 表示活动
     */
    private boolean isActiveUser(PlatformUserView user) {
        return user != null
                && user.getUserId() != null
                && Objects.equals(STATUS_ENABLED, user.getStatus())
                && Objects.equals(DEL_FLAG_EXISTS, user.getDelFlag());
    }

    /**
     * 判断部门是否处于活动状态。
     *
     * @param dept 部门投影
     * @return true 表示活动
     */
    private boolean isActiveDept(PlatformDeptView dept) {
        return dept != null
                && dept.getDeptId() != null
                && Objects.equals(STATUS_ENABLED, dept.getStatus())
                && Objects.equals(DEL_FLAG_EXISTS, dept.getDelFlag());
    }

    /**
     * 判断角色是否处于活动状态。
     *
     * @param role 角色投影
     * @return true 表示活动
     */
    private boolean isActiveRole(PlatformRoleView role) {
        return role != null
                && role.getRoleId() != null
                && Objects.equals(STATUS_ENABLED, role.getStatus())
                && Objects.equals(DEL_FLAG_EXISTS, role.getDelFlag());
    }

    /**
     * 判断岗位是否处于活动状态。
     *
     * @param post 岗位投影
     * @return true 表示活动
     */
    private boolean isActivePost(PlatformPostView post) {
        return post != null
                && post.getPostId() != null
                && Objects.equals(STATUS_ENABLED, post.getStatus());
    }
}
