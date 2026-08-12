package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.utils.JwtUtils;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.vo.LoginBody;
import com.erp.system.service.ISysUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录验证控制层
 */
@RestController
public class LoginController {

    private final ISysUserService userService;
    private final PasswordEncoder passwordEncoder;

    public LoginController(ISysUserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginBody loginBody) {
        if (loginBody == null || !StringUtils.hasText(loginBody.getUsername())
                || !StringUtils.hasText(loginBody.getPassword())) {
            return R.failed("用户名或密码不能为空");
        }

        SysUser user = userService.selectUserByUserName(loginBody.getUsername());
        if (user == null || !passwordEncoder.matches(loginBody.getPassword(), user.getPassword())) {
            return R.failed("用户名或密码错误");
        }

        if (!"0".equals(user.getStatus())) {
            return R.failed("账号已停用");
        }

        String tenantId = StringUtils.hasText(user.getTenantId()) ? user.getTenantId() : "DEFAULT";
        String token = JwtUtils.createToken(user.getUserId(), user.getUserName(), tenantId, user.getTokenVersion());

        Map<String, Object> ajax = new HashMap<>();
        ajax.put("token", token);
        ajax.put("tenantId", tenantId);
        return R.success(ajax);
    }
}
