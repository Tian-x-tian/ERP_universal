package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.SysPost;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysUserPost;
import com.erp.system.domain.SysUserRole;
import com.erp.system.domain.vo.DataPermissionScope;
import com.erp.system.domain.vo.UserPasswordUpdateBody;
import com.erp.system.domain.vo.UserProfileUpdateBody;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IDataPermissionService;
import com.erp.system.service.ISysPostService;
import com.erp.system.service.ISysUserPostService;
import com.erp.system.service.ISysUserRoleService;
import com.erp.system.service.ISysUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysRoleService;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * 用户管理控制层
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final ISysUserService userService;
    private final ISysRoleService roleService;
    private final ISysMenuService menuService;
    private final ISysPostService postService;
    private final ISysUserPostService userPostService;
    private final ISysUserRoleService userRoleService;
    private final IDataPermissionService dataPermissionService;
    private final SecurityUserResolver securityUserResolver;
    private final PasswordEncoder passwordEncoder;

    public SysUserController(ISysUserService userService,
            ISysRoleService roleService,
            ISysMenuService menuService,
            ISysPostService postService,
            ISysUserPostService userPostService,
            ISysUserRoleService userRoleService,
            IDataPermissionService dataPermissionService,
            SecurityUserResolver securityUserResolver,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.menuService = menuService;
        this.postService = postService;
        this.userPostService = userPostService;
        this.userRoleService = userRoleService;
        this.dataPermissionService = dataPermissionService;
        this.securityUserResolver = securityUserResolver;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取当前用户信息（包含角色与权限）
     */
    @GetMapping("/getInfo")
    public R<Map<String, Object>> getInfo() {
        Long userId = resolveCurrentUserId();
        SysUser user = sanitizeUser(userService.getById(userId));

        Map<String, Object> ajax = new HashMap<>();
        ajax.put("user", user);
        ajax.put("roles", roleService.selectRoleKeysByUserId(userId));
        ajax.put("permissions", menuService.selectMenuPermsByUserId(userId));
        return R.success(ajax);
    }

    /**
     * 获取个人中心信息。
     *
     * @return 包含用户信息、角色组与岗位组
     */
    @GetMapping("/profile")
    public R<Map<String, Object>> profile() {
        Long userId = resolveCurrentUserId();
        SysUser user = sanitizeUser(userService.getById(userId));

        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("user", user);
        profileMap.put("roleGroup", String.join(",", roleService.selectRoleKeysByUserId(userId)));
        profileMap.put("postGroup", buildPostGroup(userId));
        return R.success(profileMap);
    }

    /**
     * 修改个人资料。
     *
     * @param profileBody 个人资料参数
     * @return 更新结果
     */
    @PutMapping("/profile")
    public R<Boolean> updateProfile(@RequestBody UserProfileUpdateBody profileBody) {
        if (profileBody == null || !StringUtils.hasText(profileBody.getNickName())) {
            return R.failed("用户昵称不能为空");
        }
        Long userId = resolveCurrentUserId();
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(userId);
        updateEntity.setNickName(profileBody.getNickName().trim());
        updateEntity.setEmail(profileBody.getEmail());
        updateEntity.setPhonenumber(profileBody.getPhonenumber());
        updateEntity.setSex(profileBody.getSex());
        updateEntity.setAvatar(profileBody.getAvatar());

        boolean success = userService.updateProfileByUserId(updateEntity);
        return success ? R.success(true) : R.failed("更新个人信息失败");
    }

    /**
     * 修改当前用户密码。
     *
     * @param passwordBody 密码参数
     * @return 更新结果
     */
    @PutMapping("/profile/updatePwd")
    public R<Boolean> updatePwd(@RequestBody UserPasswordUpdateBody passwordBody) {
        if (passwordBody == null || !StringUtils.hasText(passwordBody.getOldPassword())
                || !StringUtils.hasText(passwordBody.getNewPassword())
                || !StringUtils.hasText(passwordBody.getConfirmPassword())) {
            return R.failed("旧密码、新密码、确认密码不能为空");
        }
        if (!passwordBody.getNewPassword().equals(passwordBody.getConfirmPassword())) {
            return R.failed("两次输入的新密码不一致");
        }
        if (passwordBody.getNewPassword().length() < 6) {
            return R.failed("新密码长度不能小于6位");
        }
        Long userId = resolveCurrentUserId();
        SysUser currentUser = userService.getById(userId);
        if (currentUser == null) {
            return R.failed("当前用户不存在");
        }
        if (!StringUtils.hasText(currentUser.getPassword())
                || !passwordEncoder.matches(passwordBody.getOldPassword(), currentUser.getPassword())) {
            return R.failed("旧密码错误");
        }

        String encodedPassword = passwordEncoder.encode(passwordBody.getNewPassword());
        boolean success = userService.updatePasswordByUserId(userId, encodedPassword);
        return success ? R.success(true) : R.failed("修改密码失败");
    }

    /**
     * 获取路由信息
     */
    @GetMapping("/getRouters")
    public R<List<SysMenu>> getRouters() {
        Long userId = resolveCurrentUserId();
        return R.success(menuService.selectMenuTreeByUserId(userId));
    }

    /**
     * 查询用户列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public R<List<SysUser>> list() {
        DataPermissionScope dataScope = dataPermissionService.resolveDataScope(resolveCurrentUserId());
        if (dataScope.isAllData()) {
            return R.success(userService.list());
        }
        if (dataScope.getDeptIds().isEmpty()) {
            return R.success(Collections.emptyList());
        }
        return R.success(userService.list(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getDeptId, dataScope.getDeptIds())));
    }

    /**
     * 获取用户详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public R<SysUser> getInfo(@PathVariable("userId") Long userId) {
        SysUser user = sanitizeUser(userService.getById(userId));
        fillUserRelations(user);
        return R.success(user);
    }

    /**
     * 新增用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysUser user) {
        return R.success(userService.save(user));
    }

    /**
     * 修改用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysUser user) {
        return R.success(userService.updateById(user));
    }

    /**
     * 删除用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @DeleteMapping("/{userId}")
    public R<Boolean> remove(@PathVariable("userId") Long userId) {
        return R.success(userService.removeById(userId));
    }

    /**
     * 获取当前登录用户ID，解析失败时回退为默认管理员。
     *
     * @return 当前用户ID
     */
    private Long resolveCurrentUserId() {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        return currentUserId != null ? currentUserId : 1L;
    }

    /**
     * 组装当前用户岗位名称字符串。
     *
     * @param userId 用户ID
     * @return 逗号分隔的岗位名称
     */
    private String buildPostGroup(Long userId) {
        List<Long> postIds = userPostService
                .list(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId))
                .stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());
        if (postIds.isEmpty()) {
            return "";
        }
        return postService.listByIds(postIds).stream()
                .map(SysPost::getPostName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    /**
     * 移除用户对象中的敏感字段，避免密码哈希泄露。
     *
     * @param user 用户对象
     * @return 已脱敏的用户对象
     */
    private SysUser sanitizeUser(SysUser user) {
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 回填用户角色与岗位关联，供编辑页面回显。
     *
     * @param user 用户对象
     */
    private void fillUserRelations(SysUser user) {
        if (user == null || user.getUserId() == null) {
            return;
        }
        List<Long> roleIds = userRoleService
                .list(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getUserId()))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        user.setRoleIds(roleIds);

        List<Long> postIds = userPostService
                .list(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, user.getUserId()))
                .stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());
        user.setPostIds(postIds);
    }
}
