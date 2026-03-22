package com.erp.business.hr.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户只读 DTO。
 */
@Data
public class HrSysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String tenantId;
    private String userName;
    private String nickName;
    private String status;
    private String delFlag;
}
