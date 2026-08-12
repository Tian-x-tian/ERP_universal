package com.erp.business.config;

import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.mybatis.AuditMetaObjectHandlerSupport;
import org.springframework.stereotype.Component;

/**
 * 业务模块审计字段自动填充器。
 */
@Component
public class AuditMetaObjectHandler extends AuditMetaObjectHandlerSupport {

    private final SecurityUserResolver securityUserResolver;

    public AuditMetaObjectHandler(SecurityUserResolver securityUserResolver) {
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 解析当前操作人账号。
     *
     * @return 当前登录用户名
     */
    @Override
    protected String resolveOperator() {
        return securityUserResolver.getCurrentUsername();
    }
}
