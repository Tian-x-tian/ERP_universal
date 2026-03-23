package com.erp.workflow.service.platform.impl;

import com.erp.common.client.internal.InternalPlatformClient;
import com.erp.platform.contract.model.PlatformDeptView;
import com.erp.platform.contract.model.PlatformPostView;
import com.erp.platform.contract.model.PlatformRoleView;
import com.erp.platform.contract.model.PlatformUserPostLink;
import com.erp.platform.contract.model.PlatformUserRoleLink;
import com.erp.platform.contract.model.PlatformUserView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * 工作流平台只读适配服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowPlatformReadServiceImplTest {

    @Mock
    private InternalPlatformClient internalPlatformClient;

    /**
     * 验证用户、部门、角色、岗位列表只保留启用数据。
     */
    @Test
    void shouldFilterInactivePlatformData() {
        PlatformUserView enabledUser = buildUser(1L, "alpha", "阿尔法", 9L, "0", "0");
        PlatformUserView disabledUser = buildUser(2L, "beta", "贝塔", 9L, "1", "0");
        PlatformDeptView enabledDept = buildDept(9L, "研发部", 1L, "leader", "0", "0");
        PlatformDeptView deletedDept = buildDept(10L, "已删部门", 1L, "leader", "0", "2");
        PlatformRoleView enabledRole = buildRole(3L, "审批人", "approver", "0", "0");
        PlatformRoleView disabledRole = buildRole(4L, "停用角色", "disabled", "1", "0");
        PlatformPostView enabledPost = buildPost(5L, "POST_A", "经理", "0");
        PlatformPostView disabledPost = buildPost(6L, "POST_B", "停用岗位", "1");
        when(internalPlatformClient.listUsers(null)).thenReturn(Arrays.asList(enabledUser, disabledUser));
        when(internalPlatformClient.listDepartments(null)).thenReturn(Arrays.asList(enabledDept, deletedDept));
        when(internalPlatformClient.listRoles()).thenReturn(Arrays.asList(enabledRole, disabledRole));
        when(internalPlatformClient.listPosts()).thenReturn(Arrays.asList(enabledPost, disabledPost));

        WorkflowPlatformReadServiceImpl service = new WorkflowPlatformReadServiceImpl(internalPlatformClient);

        Assertions.assertEquals(1, service.listUsers().size());
        Assertions.assertEquals(1, service.listDepartments().size());
        Assertions.assertEquals(1, service.listRoles().size());
        Assertions.assertEquals(1, service.listPosts().size());
        Assertions.assertEquals(1L, service.listUsers().get(0).getUserId());
        Assertions.assertEquals(9L, service.listDepartments().get(0).getDeptId());
        Assertions.assertEquals(3L, service.listRoles().get(0).getRoleId());
        Assertions.assertEquals(5L, service.listPosts().get(0).getPostId());
    }

    /**
     * 验证角色、岗位解析会去重返回用户ID。
     */
    @Test
    void shouldResolveDistinctUserIdsFromRoleAndPostRelations() {
        PlatformUserRoleLink roleLinkA = new PlatformUserRoleLink();
        roleLinkA.setUserId(11L);
        roleLinkA.setRoleId(3L);
        PlatformUserRoleLink roleLinkB = new PlatformUserRoleLink();
        roleLinkB.setUserId(11L);
        roleLinkB.setRoleId(4L);
        PlatformUserPostLink postLinkA = new PlatformUserPostLink();
        postLinkA.setUserId(12L);
        postLinkA.setPostId(5L);
        PlatformUserPostLink postLinkB = new PlatformUserPostLink();
        postLinkB.setUserId(12L);
        postLinkB.setPostId(6L);
        when(internalPlatformClient.listUserRoleLinks(Arrays.asList(3L, 4L))).thenReturn(Arrays.asList(roleLinkA, roleLinkB));
        when(internalPlatformClient.listUserPostLinks(Arrays.asList(5L, 6L))).thenReturn(Arrays.asList(postLinkA, postLinkB));

        WorkflowPlatformReadServiceImpl service = new WorkflowPlatformReadServiceImpl(internalPlatformClient);

        Assertions.assertEquals(Collections.singletonList(11L), service.resolveUserIdsByRoleIds(Arrays.asList(3L, 4L)));
        Assertions.assertEquals(Collections.singletonList(12L), service.resolveUserIdsByPostIds(Arrays.asList(5L, 6L)));
    }

    /**
     * 验证按账号或昵称可以命中活动用户。
     */
    @Test
    void shouldFindUserByUserNameOrNickName() {
        PlatformUserView target = buildUser(1L, "alpha", "阿尔法", 9L, "0", "0");
        when(internalPlatformClient.listUsers(null)).thenReturn(Collections.singletonList(target));

        WorkflowPlatformReadServiceImpl service = new WorkflowPlatformReadServiceImpl(internalPlatformClient);

        Assertions.assertEquals(1L, service.findUserByUserNameOrNickName("alpha").getUserId());
        Assertions.assertEquals(1L, service.findUserByUserNameOrNickName("阿尔法").getUserId());
    }

    private PlatformUserView buildUser(Long userId, String userName, String nickName, Long deptId, String status, String delFlag) {
        PlatformUserView user = new PlatformUserView();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setNickName(nickName);
        user.setDeptId(deptId);
        user.setStatus(status);
        user.setDelFlag(delFlag);
        return user;
    }

    private PlatformDeptView buildDept(Long deptId, String deptName, Long parentId, String leader, String status, String delFlag) {
        PlatformDeptView dept = new PlatformDeptView();
        dept.setDeptId(deptId);
        dept.setDeptName(deptName);
        dept.setParentId(parentId);
        dept.setLeader(leader);
        dept.setStatus(status);
        dept.setDelFlag(delFlag);
        return dept;
    }

    private PlatformRoleView buildRole(Long roleId, String roleName, String roleKey, String status, String delFlag) {
        PlatformRoleView role = new PlatformRoleView();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        role.setStatus(status);
        role.setDelFlag(delFlag);
        return role;
    }

    private PlatformPostView buildPost(Long postId, String postCode, String postName, String status) {
        PlatformPostView post = new PlatformPostView();
        post.setPostId(postId);
        post.setPostCode(postCode);
        post.setPostName(postName);
        post.setStatus(status);
        return post;
    }
}
