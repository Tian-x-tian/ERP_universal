package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 个人中心密码更新请求对象。
 */
@Data
public class UserPasswordUpdateBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 旧密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;

    /** 确认密码 */
    private String confirmPassword;
}
