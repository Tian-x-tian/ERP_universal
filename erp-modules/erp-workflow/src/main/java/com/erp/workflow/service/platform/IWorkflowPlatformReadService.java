package com.erp.workflow.service.platform;

import com.erp.platform.contract.model.PlatformDeptView;
import com.erp.platform.contract.model.PlatformPostView;
import com.erp.platform.contract.model.PlatformRoleView;
import com.erp.platform.contract.model.PlatformUserView;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 工作流模块平台只读适配服务接口。
 */
public interface IWorkflowPlatformReadService {

    /**
     * 按租户和账号查询活动用户。
     *
     * @param tenantId 租户编号
     * @param userName 用户账号
     * @return 用户投影
     */
    PlatformUserView getActiveUserByUsername(String tenantId, String userName);

    /**
     * 按用户ID查询活动用户。
     *
     * @param userId 用户ID
     * @return 用户投影
     */
    PlatformUserView getUser(Long userId);

    /**
     * 批量查询活动用户并按用户ID建立映射。
     *
     * @param userIds 用户ID集合
     * @return 用户映射
     */
    Map<Long, PlatformUserView> getUserMap(Collection<Long> userIds);

    /**
     * 查询当前租户活动用户列表。
     *
     * @return 用户列表
     */
    List<PlatformUserView> listUsers();

    /**
     * 查询指定部门下的活动用户列表。
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    List<PlatformUserView> listUsersByDeptId(Long deptId);

    /**
     * 按账号或昵称精确查找活动用户。
     *
     * @param keyword 账号或昵称
     * @return 用户投影
     */
    PlatformUserView findUserByUserNameOrNickName(String keyword);

    /**
     * 批量查询活动部门并按部门ID建立映射。
     *
     * @param deptIds 部门ID集合
     * @return 部门映射
     */
    Map<Long, PlatformDeptView> getDepartmentMap(Collection<Long> deptIds);

    /**
     * 查询单个活动部门。
     *
     * @param deptId 部门ID
     * @return 部门投影
     */
    PlatformDeptView getDepartment(Long deptId);

    /**
     * 查询当前租户活动部门列表。
     *
     * @return 部门列表
     */
    List<PlatformDeptView> listDepartments();

    /**
     * 查询当前租户活动角色列表。
     *
     * @return 角色列表
     */
    List<PlatformRoleView> listRoles();

    /**
     * 查询当前租户活动岗位列表。
     *
     * @return 岗位列表
     */
    List<PlatformPostView> listPosts();

    /**
     * 按角色解析用户ID集合。
     *
     * @param roleIds 角色ID集合
     * @return 用户ID列表
     */
    List<Long> resolveUserIdsByRoleIds(Collection<Long> roleIds);

    /**
     * 按岗位解析用户ID集合。
     *
     * @param postIds 岗位ID集合
     * @return 用户ID列表
     */
    List<Long> resolveUserIdsByPostIds(Collection<Long> postIds);
}
