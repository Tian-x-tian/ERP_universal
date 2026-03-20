package com.erp.auth.security;

import com.erp.auth.domain.vo.LoginBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 验证码校验器（可开关，默认关闭）。
 */
@Component
public class CaptchaVerifier {

    @Value("${erp.auth.captcha.enabled:false}")
    private boolean enabled;

    @Value("${erp.auth.captcha.fixed-code:}")
    private String fixedCode;

    public boolean verify(LoginBody loginBody) {
        if (!enabled) {
            return true;
        }
        if (loginBody == null || !StringUtils.hasText(loginBody.getCode())) {
            return false;
        }
        if (!StringUtils.hasText(fixedCode)) {
            // 未接入真实验证码服务时，至少要求用户带验证码字段
            return loginBody.getCode().trim().length() >= 4;
        }
        return fixedCode.trim().equals(loginBody.getCode().trim());
    }

    public boolean isEnabled() {
        return enabled;
    }
}
