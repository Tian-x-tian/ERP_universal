package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 个人中心资料更新请求对象。
 */
@Data
public class UserProfileUpdateBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户昵称 */
    private String nickName;

    /** 用户邮箱 */
    private String email;

    /** 手机号码 */
    private String phonenumber;

    /** 用户性别（0男 1女 2未知） */
    private String sex;

    /** 头像地址 */
    private String avatar;
}
