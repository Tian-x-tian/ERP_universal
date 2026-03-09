package com.erp.system.controller;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.R;
import com.erp.common.utils.JwtUtils;
import com.erp.system.domain.SysLoginLog;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.vo.LoginBody;
import com.erp.system.service.ISysLoginLogService;
import com.erp.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录验证控制层
 */
@RestController
public class LoginController {
    private static final String DEFAULT_LOG_TENANT_ID = "000000";

    private final ISysUserService userService;
    private final ISysLoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;

    public LoginController(ISysUserService userService,
            ISysLoginLogService loginLogService,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.loginLogService = loginLogService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginBody loginBody, HttpServletRequest request) {
        String username = loginBody == null ? null : trim(loginBody.getUsername());
        String loginTenantId = resolveTenantId(request);
        String requestIp = resolveRequestIp(request);
        if (!StringUtils.hasText(loginTenantId)) {
            recordLogin(null, username, "1", "租户编号不能为空", requestIp);
            return R.failed("租户编号不能为空");
        }
        if (loginBody == null || !StringUtils.hasText(loginBody.getUsername())
                || !StringUtils.hasText(loginBody.getPassword())) {
            recordLogin(loginTenantId, username, "1", "用户名或密码不能为空", requestIp);
            return R.failed("用户名或密码不能为空");
        }

        try {
            TenantContextHolder.setTenantId(loginTenantId);
            SysUser user = userService.selectUserByUserName(username);
            if (user == null || !passwordEncoder.matches(loginBody.getPassword(), user.getPassword())) {
                recordLogin(loginTenantId, username, "1", "用户名或密码错误", requestIp);
                return R.failed("用户名或密码错误");
            }

            if (!loginTenantId.equals(resolveTenantId(user.getTenantId()))) {
                recordLogin(loginTenantId, user.getUserName(), "1", "租户与账号不匹配", requestIp);
                return R.failed("租户与账号不匹配");
            }

            if (!"0".equals(user.getStatus())) {
                recordLogin(user.getTenantId(), user.getUserName(), "1", "账号已停用", requestIp);
                return R.failed("账号已停用");
            }

            String tenantId = resolveTenantId(user.getTenantId());
            String token = JwtUtils.createToken(user.getUserName(), tenantId);
            updateLoginInfo(user.getUserId(), requestIp);
            recordLogin(tenantId, user.getUserName(), "0", "登录成功", requestIp);

            Map<String, Object> ajax = new HashMap<>();
            ajax.put("token", token);
            ajax.put("tenantId", tenantId);
            return R.success(ajax);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * 登出方法。
     *
     * @return 结果
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        return R.success();
    }

    /**
     * 写入登录日志。
     *
     * @param tenantId 租户编号
     * @param userName 用户账号
     * @param status   状态（0成功 1失败）
     * @param msg      提示信息
     * @param ip       登录IP
     */
    private void recordLogin(String tenantId, String userName, String status, String msg, String ip) {
        String logTenantId = resolveTenantIdForLog(tenantId);
        String contextTenantId = resolveTenantId(TenantContextHolder.getTenantId());
        boolean contextInitialized = false;
        if (!StringUtils.hasText(contextTenantId)) {
            TenantContextHolder.setTenantId(logTenantId);
            contextInitialized = true;
        }
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setTenantId(logTenantId);
        loginLog.setUserName(StringUtils.hasText(userName) ? userName : "anonymous");
        loginLog.setStatus(status);
        loginLog.setMsg(msg);
        loginLog.setIpaddr(ip);
        loginLog.setLoginTime(new Date());
        try {
            loginLogService.save(loginLog);
        } finally {
            if (contextInitialized) {
                TenantContextHolder.clear();
            }
        }
    }

    /**
     * 更新用户最后登录信息。
     *
     * @param userId 用户ID
     * @param ip     登录IP
     */
    private void updateLoginInfo(Long userId, String ip) {
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(userId);
        updateEntity.setLoginIp(ip);
        updateEntity.setLoginDate(new Date());
        userService.updateProfileByUserId(updateEntity);
    }

    /**
     * 解析登录租户编号。
     *
     * @param request 请求对象
     * @return 租户编号
     */
    private String resolveTenantId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String tenantId = request.getHeader("tenantId");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getHeader("Tenantid");
        }
        return resolveTenantId(tenantId);
    }

    /**
     * 规范化租户编号。
     *
     * @param tenantId 原始租户编号
     * @return 规范后的租户编号
     */
    private String resolveTenantId(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    /**
     * 规范化日志场景下的租户编号。
     *
     * @param tenantId 原始租户编号
     * @return 日志租户编号
     */
    private String resolveTenantIdForLog(String tenantId) {
        String resolvedTenantId = resolveTenantId(tenantId);
        return StringUtils.hasText(resolvedTenantId) ? resolvedTenantId : DEFAULT_LOG_TENANT_ID;
    }

    /**
     * 解析客户端请求IP。
     *
     * @param request 请求对象
     * @return 客户端IP
     */
    private String resolveRequestIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 规范化字符串输入。
     *
     * @param value 输入值
     * @return 去除首尾空格后的字符串
     */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
